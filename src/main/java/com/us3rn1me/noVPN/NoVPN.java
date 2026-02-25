package com.us3rn1me.noVPN;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(id = "novpn", name = "NoVPN", version = BuildConstants.VERSION, description = "Simple, free and open-source Anti-VPN plugin for Velocity.", url = "https://modrinth.com/project/Qn6G3lhH", authors = {
        "us3rn1me" })
public class NoVPN {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    private ConfigManager configManager;
    private IpListManager ipListManager;

    @Inject
    public NoVPN(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        configManager = new ConfigManager(dataDirectory, logger);
        configManager.load();

        ipListManager = new IpListManager(logger);
        ipListManager.start(configManager.get());

        server.getEventManager().register(this,
                new VpnListener(configManager, ipListManager, logger));

        CommandManager cmdManager = server.getCommandManager();
        CommandMeta meta = cmdManager.metaBuilder("novpn")
                .aliases("anvpn")
                .plugin(this)
                .build();
        cmdManager.register(meta, new NoVPNCommand(configManager, ipListManager));

        logger.info("NoVPN v{} enabled.", BuildConstants.VERSION);
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (ipListManager != null) {
            ipListManager.shutdown();
        }
    }
}
