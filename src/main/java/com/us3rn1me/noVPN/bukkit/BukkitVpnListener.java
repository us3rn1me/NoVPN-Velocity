package com.us3rn1me.noVPN.bukkit;

import com.us3rn1me.noVPN.Config;
import com.us3rn1me.noVPN.IpListManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.logging.Logger;

/**
 * Checks connecting players against the VPN block lists.
 *
 * {@link AsyncPlayerPreLoginEvent} fires off the main thread on both Spigot and
 * Folia, which is exactly where we want our check — no thread concerns, no lag
 * on the main thread, and no need to defer the decision to a callback.
 */
public class BukkitVpnListener implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final BukkitConfigManager configManager;
    private final IpListManager ipListManager;

    public BukkitVpnListener(BukkitConfigManager configManager, IpListManager ipListManager) {
        this.configManager = configManager;
        this.ipListManager = ipListManager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        Config config = configManager.get();

        String ip = event.getAddress().getHostAddress();

        if (!ipListManager.isBlocked(ip))
            return;

        // Convert MiniMessage format to legacy string for broad Spigot compatibility.
        String legacyKick = LEGACY.serialize(MM.deserialize(config.getKickMessage()));

        if (config.isLogBlocked()) {
            Logger.getLogger("NoVPN").info(
                    String.format("Blocked %s (%s) — VPN/proxy detected.",
                            event.getName(), ip));
        }

        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, legacyKick);
    }
}
