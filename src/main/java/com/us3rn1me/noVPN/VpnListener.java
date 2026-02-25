package com.us3rn1me.noVPN;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;

/**
 * Intercepts login events and kicks players whose IP is in the VPN block lists.
 */
public class VpnListener {

    private final ConfigManager configManager;
    private final IpListManager ipListManager;
    private final Logger logger;

    public VpnListener(ConfigManager configManager, IpListManager ipListManager, Logger logger) {
        this.configManager = configManager;
        this.ipListManager = ipListManager;
        this.logger = logger;
    }

    @Subscribe
    public void onLogin(LoginEvent event) {
        Player player = event.getPlayer();
        Config config = configManager.get();

        // Skip the check if the player has the bypass permission.
        if (player.hasPermission(config.getBypassPermission()))
            return;

        String ip = player.getRemoteAddress().getAddress().getHostAddress();

        if (!ipListManager.isBlocked(ip))
            return;

        if (config.isLogBlocked()) {
            logger.info("Blocked {} ({}) — VPN/proxy detected.", player.getUsername(), ip);
        }

        event.setResult(
                LoginEvent.ComponentResult.denied(
                        MiniMessage.miniMessage().deserialize(config.getKickMessage())));
    }
}
