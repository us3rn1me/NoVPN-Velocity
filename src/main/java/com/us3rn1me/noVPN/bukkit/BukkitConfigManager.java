package com.us3rn1me.noVPN.bukkit;

import com.us3rn1me.noVPN.Config;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.List;

/**
 * Loads and exposes plugin configuration from Bukkit's built-in
 * {@code config.yml}.
 *
 * Bukkit automatically extracts a bundled {@code config.yml} from the JAR on
 * first
 * startup via {@link JavaPlugin#saveDefaultConfig()}, so no manual extraction
 * is needed.
 */
public class BukkitConfigManager {

    private final JavaPlugin plugin;
    private Config config;

    public BukkitConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Loads (or reloads) configuration from disk.
     * Safe to call multiple times.
     */
    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        FileConfiguration raw = plugin.getConfig();

        String kickMessage = raw.getString("kick-message",
                "<red>You are not allowed to connect using a VPN or proxy.");
        String bypassPermission = raw.getString("bypass-permission", "novpn.bypass");
        boolean logBlocked = raw.getBoolean("log-blocked", true);
        int refreshInterval = raw.getInt("refresh-interval-minutes", 60);
        int connectTimeout = raw.getInt("connect-timeout-seconds", 10);
        List<String> lists = raw.getStringList("lists");

        if (lists.isEmpty()) {
            lists = Collections.emptyList();
        }

        config = new Config(kickMessage, bypassPermission, logBlocked,
                refreshInterval, connectTimeout, lists);
    }

    /** Returns the currently loaded configuration. */
    public Config get() {
        return config;
    }
}
