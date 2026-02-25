package com.us3rn1me.noVPN;

import com.google.inject.Inject;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import org.slf4j.Logger;

@Plugin(id = "novpn", name = "NoVPN", version = BuildConstants.VERSION, description = "A simple, free and open-source Anti-VPN plugin for Spigot/Velocity.", url = "https://modrinth.com/project/Qn6G3lhH", authors = {"us3rn1me"})
public class NoVPN {

    @Inject
    private Logger logger;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
    }
}
