package com.us3rn1me.noVPN.bukkit;

import com.tcoded.folialib.FoliaLib;
import com.us3rn1me.noVPN.BuildConstants;
import com.us3rn1me.noVPN.IpListManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Bukkit/Spigot/Paper/Folia entry point for NoVPN.
 *
 * On Bukkit/Spigot/Paper the scheduler runs on the main server thread.
 * On Folia it delegates to the GlobalRegionScheduler via FoliaLib, which is
 * the correct scheduler for non-entity, non-block work such as HTTP requests.
 */
public class BukkitNoVPN extends JavaPlugin {

    private BukkitConfigManager configManager;
    private IpListManager ipListManager;
    private FoliaLib foliaLib;

    @Override
    public void onEnable() {
        foliaLib = new FoliaLib(this);

        configManager = new BukkitConfigManager(this);
        configManager.load();

        ipListManager = new IpListManager(getLogger());
        ipListManager.start(configManager.get());

        getServer().getPluginManager().registerEvents(
                new BukkitVpnListener(configManager, ipListManager), this);

        PluginCommand cmd = getCommand("novpn");
        if (cmd != null) {
            BukkitNoVPNCommand executor = new BukkitNoVPNCommand(configManager, ipListManager);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }

        getLogger().info("NoVPN v" + BuildConstants.VERSION + " enabled.");
    }

    @Override
    public void onDisable() {
        if (ipListManager != null) {
            ipListManager.shutdown();
        }
        if (foliaLib != null) {
            foliaLib.getScheduler().cancelAllTasks();
        }
    }

    public FoliaLib getFoliaLib() {
        return foliaLib;
    }
}
