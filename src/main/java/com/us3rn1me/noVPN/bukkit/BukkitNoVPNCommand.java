package com.us3rn1me.noVPN.bukkit;

import com.us3rn1me.noVPN.BuildConstants;
import com.us3rn1me.noVPN.IpListManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Handles the {@code /novpn} command for in-game administration.
 *
 * <p>
 * Subcommands:
 * <ul>
 * <li>{@code /novpn reload} — reloads config and re-fetches IP lists</li>
 * <li>{@code /novpn check <ip>} — checks whether an IP is blocked</li>
 * <li>{@code /novpn info} — prints version and list statistics</li>
 * </ul>
 */
public class BukkitNoVPNCommand implements CommandExecutor, TabCompleter {

    private final BukkitConfigManager configManager;
    private final IpListManager ipListManager;

    public BukkitNoVPNCommand(BukkitConfigManager configManager, IpListManager ipListManager) {
        this.configManager = configManager;
        this.ipListManager = ipListManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("novpn.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> handleReload(sender);
            case "check" -> handleCheck(sender, args);
            case "info" -> handleInfo(sender);
            default -> sendUsage(sender);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("novpn.admin"))
            return Collections.emptyList();
        if (args.length == 1)
            return Arrays.asList("reload", "check", "info");
        return Collections.emptyList();
    }

    // ------------------------------------------------------------------

    private void handleReload(CommandSender sender) {
        sender.sendMessage(ChatColor.GRAY + "Reloading config and IP lists...");
        configManager.load();
        ipListManager.refresh(configManager.get());
        ipListManager.reconfigure(configManager.get());
        sender.sendMessage(ChatColor.GREEN + "Done! Loaded "
                + ChatColor.WHITE + ipListManager.getIpCount() + ChatColor.GREEN + " IPs and "
                + ChatColor.WHITE + ipListManager.getCidrCount() + ChatColor.GREEN + " CIDR ranges.");
    }

    private void handleCheck(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /novpn check <ip>");
            return;
        }
        String ip = args[1];
        if (ipListManager.isBlocked(ip)) {
            sender.sendMessage(ChatColor.RED + ip + " is flagged as a VPN/proxy.");
        } else {
            sender.sendMessage(ChatColor.GREEN + ip + " is not in any block list.");
        }
    }

    private void handleInfo(CommandSender sender) {
        sender.sendMessage(ChatColor.AQUA + "NoVPN v" + ChatColor.WHITE + BuildConstants.VERSION
                + ChatColor.AQUA + " — "
                + ChatColor.WHITE + ipListManager.getIpCount() + ChatColor.AQUA + " IPs, "
                + ChatColor.WHITE + ipListManager.getCidrCount() + ChatColor.AQUA + " CIDR ranges"
                + " from " + ChatColor.WHITE + configManager.get().getLists().size()
                + ChatColor.AQUA + " source(s).");
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "Usage: " + ChatColor.WHITE
                + "/novpn <reload|check <ip>|info>");
    }
}
