package com.us3rn1me.noVPN;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * Handles loading and reloading of the plugin configuration from
 * {@code config.toml}.
 *
 * On first startup a default config is extracted from the plugin JAR so users
 * have a fully documented starting point.
 */
public class ConfigManager {

    private static final String CONFIG_FILE = "config.toml";

    private final Path dataDirectory;
    private final Logger logger;

    private Config config;

    public ConfigManager(Path dataDirectory, Logger logger) {
        this.dataDirectory = dataDirectory;
        this.logger = logger;
    }

    /**
     * Loads the configuration from disk, extracting defaults if needed.
     * Safe to call multiple times — subsequent calls act as a reload.
     */
    public void load() {
        Path configPath = dataDirectory.resolve(CONFIG_FILE);

        if (!Files.exists(configPath)) {
            extractDefault(configPath);
        }

        try (CommentedFileConfig raw = CommentedFileConfig.builder(configPath.toFile())
                .preserveInsertionOrder()
                .build()) {

            raw.load();

            String kickMessage = raw.getOrElse(
                    "kick-message",
                    "<red>You are not allowed to connect using a VPN or proxy.");

            String bypassPermission = raw.getOrElse("bypass-permission", "novpn.bypass");
            boolean logBlocked = raw.getOrElse("log-blocked", true);
            int refreshInterval = raw.getOrElse("refresh-interval-minutes", 60);
            int connectTimeout = raw.getOrElse("connect-timeout-seconds", 10);

            List<String> lists = raw.getOrElse("lists", Collections.emptyList());

            config = new Config(kickMessage, bypassPermission, logBlocked,
                    refreshInterval, connectTimeout, lists);

        } catch (Exception e) {
            logger.error("Failed to load config.toml — using built-in defaults.", e);
            config = defaultConfig();
        }
    }

    /** Returns the currently loaded configuration. */
    public Config get() {
        return config;
    }

    // ------------------------------------------------------------------

    private void extractDefault(Path target) {
        try {
            Files.createDirectories(target.getParent());
            try (InputStream in = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
                if (in == null) {
                    logger.warn("Bundled config.toml not found — writing empty config.");
                    Files.createFile(target);
                    return;
                }
                Files.copy(in, target);
            }
        } catch (IOException e) {
            logger.error("Could not extract default config.toml.", e);
        }
    }

    private Config defaultConfig() {
        return new Config(
                "<red>You are not allowed to connect using a VPN or proxy.",
                "novpn.bypass",
                true,
                60,
                10,
                Collections.emptyList());
    }
}
