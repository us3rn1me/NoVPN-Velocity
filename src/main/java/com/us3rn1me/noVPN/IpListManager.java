package com.us3rn1me.noVPN;

import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/**
 * Downloads and manages the remote IP/CIDR block lists used for VPN detection.
 *
 * Lists are refreshed on a configurable schedule. Lookups are thread-safe and
 * use an atomically swapped snapshot so login checks are never blocked by a
 * refresh operation.
 */
public class IpListManager {

    // Matches a plain IPv4 address, optionally followed by :port
    private static final Pattern IP_PATTERN = Pattern.compile("^(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})(:\\d+)?$");

    // Matches IPv4 CIDR notation
    private static final Pattern CIDR_PATTERN = Pattern
            .compile("^(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})/(\\d{1,2})$");

    // Matches an IP inside JSON (from monosans/proxy-list JSON format)
    private static final Pattern JSON_IP_PATTERN = Pattern.compile("\"(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})\"");

    private record Snapshot(Set<String> ips, List<InetAddressRange> cidrs) {
    }

    private final Logger logger;
    private final AtomicReference<Snapshot> snapshot = new AtomicReference<>(new Snapshot(Set.of(), List.of()));

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "novpn-refresh");
        t.setDaemon(true);
        return t;
    });

    private ScheduledFuture<?> scheduledTask;

    public IpListManager(Logger logger) {
        this.logger = logger;
    }

    /**
     * Kicks off an initial list fetch and schedules periodic refreshes.
     *
     * @param config the plugin configuration
     */
    public void start(Config config) {
        // Run sync for the first load so the plugin is ready before players connect.
        refresh(config);
        scheduleRefresh(config);
    }

    /**
     * Cancels the refresh scheduler and frees resources.
     */
    public void shutdown() {
        scheduler.shutdownNow();
    }

    /**
     * Reschedules the refresh task (called after a config reload).
     *
     * @param config updated configuration
     */
    public void reconfigure(Config config) {
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
        }
        scheduleRefresh(config);
    }

    /**
     * Checks whether the given IP string is in any of the loaded lists.
     *
     * @param rawIp the player's IP address as a string
     * @return {@code true} if the IP is flagged
     */
    public boolean isBlocked(String rawIp) {
        String ip = rawIp.contains(":") && !rawIp.startsWith("[")
                ? rawIp // IPv6 — pass through (we only block IPv4)
                : rawIp;

        Snapshot snap = snapshot.get();

        if (snap.ips().contains(ip))
            return true;

        InetAddress addr;
        try {
            addr = InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            return false;
        }

        for (InetAddressRange range : snap.cidrs()) {
            if (range.contains(addr))
                return true;
        }

        return false;
    }

    /** Returns the number of plain IPs currently loaded. */
    public int getIpCount() {
        return snapshot.get().ips().size();
    }

    /** Returns the number of CIDR ranges currently loaded. */
    public int getCidrCount() {
        return snapshot.get().cidrs().size();
    }

    // ------------------------------------------------------------------

    /** Downloads all configured lists and atomically replaces the snapshot. */
    public void refresh(Config config) {
        logger.info("Refreshing IP lists ({} sources)...", config.getLists().size());

        Set<String> ips = new HashSet<>();
        List<InetAddressRange> cidrs = new ArrayList<>();
        int failedSources = 0;

        for (String url : config.getLists()) {
            try {
                fetchList(url, config.getConnectTimeoutSeconds() * 1000, ips, cidrs);
            } catch (Exception e) {
                failedSources++;
                logger.debug("Failed to fetch {}: {}", url, e.getMessage());
            }
        }

        snapshot.set(new Snapshot(Set.copyOf(ips), List.copyOf(cidrs)));

        logger.info("IP lists refreshed — {} IPs, {} CIDR ranges loaded ({} source(s) failed).",
                ips.size(), cidrs.size(), failedSources);
    }

    private void scheduleRefresh(Config config) {
        int interval = config.getRefreshIntervalMinutes();
        if (interval <= 0)
            return;

        scheduledTask = scheduler.scheduleAtFixedRate(
                () -> {
                    try {
                        refresh(config);
                    } catch (Exception e) {
                        logger.error("Scheduled IP list refresh failed.", e);
                    }
                },
                interval, interval, TimeUnit.MINUTES);
    }

    private void fetchList(String rawUrl, int timeoutMs, Set<String> ips, List<InetAddressRange> cidrs)
            throws Exception {

        URL url = new java.net.URI(rawUrl).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(timeoutMs);
        conn.setReadTimeout(timeoutMs);
        conn.setRequestProperty("User-Agent",
                "NoVPN-Velocity/" + BuildConstants.VERSION + " (github.com/us3rn1me/NoVPN-Velocity)");
        conn.setInstanceFollowRedirects(true);

        int status = conn.getResponseCode();
        if (status < 200 || status >= 300) {
            throw new Exception("HTTP " + status);
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {

            String line;
            while ((line = reader.readLine()) != null) {
                parseLine(line.trim(), ips, cidrs);
            }
        } finally {
            conn.disconnect();
        }
    }

    private void parseLine(String line, Set<String> ips, List<InetAddressRange> cidrs) {
        if (line.isEmpty() || line.startsWith("#") || line.startsWith(";"))
            return;

        // JSON array entry — e.g. monosans format: {"host":"1.2.3.4",...}
        if (line.startsWith("{") || line.startsWith("[")) {
            var matcher = JSON_IP_PATTERN.matcher(line);
            while (matcher.find()) {
                ips.add(matcher.group(1));
            }
            return;
        }

        // CIDR notation
        if (CIDR_PATTERN.matcher(line).matches()) {
            InetAddressRange range = InetAddressRange.parse(line);
            if (range != null) {
                cidrs.add(range);
            }
            return;
        }

        // Plain IP or ip:port
        var matcher = IP_PATTERN.matcher(line);
        if (matcher.matches()) {
            ips.add(matcher.group(1));
        }
    }
}
