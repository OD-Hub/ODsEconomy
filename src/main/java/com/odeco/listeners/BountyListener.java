package com.odeco.listeners;

import com.odeco.ODEco;
import com.odeco.economy.EconomyManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class BountyListener implements Listener {

    private final ODEco plugin;

    public BountyListener(ODEco plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getPlayer();
        Player killer = victim.getKiller();
        if (killer == null) return;

        EconomyManager economy = plugin.getEconomyManager();
        double bounty = economy.getBounty(victim.getUniqueId());
        if (bounty <= 0) return;

        economy.redeemBounty(victim.getUniqueId(), killer.getUniqueId());
        killer.sendMessage(
            plugin.getMiniMessage().deserialize("<green>You collected a bounty of " + economy.format(bounty) + " for killing " + victim.getName() + "!</green>")
        );
    }
}
