package com.odeco.listeners;

import com.odeco.ODEco;
import com.odeco.economy.EconomyManager;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class BanknoteListener implements Listener {

    private final ODEco plugin;

    public BanknoteListener(ODEco plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.PAPER) return;
        if (!item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return;
        if (meta.getCustomModelData() != 1001) return;

        double value = plugin.getEconomyManager().getBanknoteValue(item);
        if (value <= 0) return;

        event.setCancelled(true);

        plugin.getEconomyManager().deposit(player.getUniqueId(), value);
        plugin.getEconomyManager().logTransaction(player.getUniqueId(), "banknote_redeem", value, "Redeemed banknote");

        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        player.sendMessage(com.odeco.utils.ColorUtils.color(
                "<green>Redeemed banknote for " + plugin.getEconomyManager().format(value) + "!</green>"));
    }
}
