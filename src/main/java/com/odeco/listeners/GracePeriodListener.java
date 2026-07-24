package com.odeco.listeners;

import com.odeco.ODEco;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class GracePeriodListener implements Listener {

    private final ODEco plugin;

    public GracePeriodListener(ODEco plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getEconomyManager().onPlayerJoin(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getEconomyManager().onPlayerQuit(event.getPlayer().getUniqueId());
    }
}
