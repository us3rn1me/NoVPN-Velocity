package com.us3rn1me.noVPN;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.List;

/**
 * Provides the {@code /novpn} command for in-game administration.
 *
 * <p>
 * Subcommands:
 * <ul>
 * <li>{@code /novpn reload} — reloads config and refreshes IP lists</li>
 * <li>{@code /novpn check <ip>} — checks whether an IP is blocked</li>
 * <li>{@code /novpn info} — prints version and list statistics</li>
 * </ul>
 *
 * All subcommands require the {@code novpn.admin} permission.
 */
public class NoVPNCommand implements SimpleCommand {

    private static final String PERM_ADMIN = "novpn.admin";
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final ConfigManager configManager;
    private final IpListManager ipListManager;

    public NoVPNCommand(ConfigManager configManager, IpListManager ipListManager) {
        this.configManager = configManager;
        this.ipListManager = ipListManager;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!source.hasPermission(PERM_ADMIN)) {
            source.sendMessage(MM.deserialize("<red>You don't have permission to use this command."));
            return;
        }

        if (args.length == 0) {
            sendUsage(source);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> handleReload(source);
            case "check" -> handleCheck(source, args);
            case "info" -> handleInfo(source);
            default -> sendUsage(source);
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length <= 1) {
            return List.of("reload", "check", "info");
        }
        return List.of();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission(PERM_ADMIN);
    }

    // ------------------------------------------------------------------

    private void handleReload(CommandSource source) {
        source.sendMessage(MM.deserialize("<gray>Reloading config and IP lists..."));
        configManager.load();
        ipListManager.refresh(configManager.get());
        ipListManager.reconfigure(configManager.get());
        source.sendMessage(MM.deserialize(
                "<green>Done! Loaded <white>" + ipListManager.getIpCount() + " IPs</white> and <white>"
                        + ipListManager.getCidrCount() + " CIDR ranges</white>."));
    }

    private void handleCheck(CommandSource source, String[] args) {
        if (args.length < 2) {
            source.sendMessage(MM.deserialize("<red>Usage: /novpn check <ip>"));
            return;
        }

        String ip = args[1];
        boolean blocked = ipListManager.isBlocked(ip);

        if (blocked) {
            source.sendMessage(MM.deserialize("<red><bold>" + ip + "</bold> is flagged as a VPN/proxy."));
        } else {
            source.sendMessage(MM.deserialize("<green><bold>" + ip + "</bold> is not in any block list."));
        }
    }

    private void handleInfo(CommandSource source) {
        source.sendMessage(MM.deserialize(
                "<aqua>NoVPN v<white>" + BuildConstants.VERSION + "</white>"
                        + " — <white>" + ipListManager.getIpCount() + "</white> IPs"
                        + ", <white>" + ipListManager.getCidrCount() + "</white> CIDR ranges loaded"
                        + " from <white>" + configManager.get().getLists().size() + "</white> source(s)."));
    }

    private void sendUsage(CommandSource source) {
        source.sendMessage(MM.deserialize(
                "<yellow>Usage: <white>/novpn <reload|check <ip>|info>"));
    }
}
