package com.us3rn1me.noVPN;

import java.util.List;

/**
 * Holds all configuration values loaded from config.toml.
 */
public class Config {

    private final String kickMessage;
    private final String bypassPermission;
    private final boolean logBlocked;
    private final int refreshIntervalMinutes;
    private final int connectTimeoutSeconds;
    private final List<String> lists;

    public Config(
            String kickMessage,
            String bypassPermission,
            boolean logBlocked,
            int refreshIntervalMinutes,
            int connectTimeoutSeconds,
            List<String> lists) {
        this.kickMessage = kickMessage;
        this.bypassPermission = bypassPermission;
        this.logBlocked = logBlocked;
        this.refreshIntervalMinutes = refreshIntervalMinutes;
        this.connectTimeoutSeconds = connectTimeoutSeconds;
        this.lists = List.copyOf(lists);
    }

    public String getKickMessage() {
        return kickMessage;
    }

    public String getBypassPermission() {
        return bypassPermission;
    }

    public boolean isLogBlocked() {
        return logBlocked;
    }

    public int getRefreshIntervalMinutes() {
        return refreshIntervalMinutes;
    }

    public int getConnectTimeoutSeconds() {
        return connectTimeoutSeconds;
    }

    public List<String> getLists() {
        return lists;
    }
}
