package com.odeco.gui;

import com.odeco.ODEco;
import com.odeco.economy.BountyEntry;
import com.odeco.economy.EconomyManager;
import com.odeco.utils.ColorUtils;
import com.odeco.utils.Heads;
import com.odeco.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.*;

public class BountyManagerGUI implements InventoryHolder {

    private final ODEco plugin;
    private final Player player;
    private final Inventory inventory;
    private int page = 0;

    public BountyManagerGUI(ODEco plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, ColorUtils.color("<dark_red>Bounty Manager</dark_red>"));
        populate();
    }

    private void populate() {
        inventory.clear();
        EconomyManager economy = plugin.getEconomyManager();
        Map<UUID, BountyEntry> bountyEntries = economy.getAllBountyEntries();
        List<Map.Entry<UUID, BountyEntry>> entries = new ArrayList<>(bountyEntries.entrySet());

        int totalPages = Math.max(1, (int) Math.ceil((double) entries.size() / 45));
        if (page >= totalPages) page = totalPages - 1;

        int start = page * 45;
        int slot = 0;
        for (int i = start; i < Math.min(entries.size(), start + 45); i++) {
            if (slot >= 45) break;
            Map.Entry<UUID, BountyEntry> mapEntry = entries.get(i);
            UUID targetId = mapEntry.getKey();
            BountyEntry bounty = mapEntry.getValue();
            OfflinePlayer off = Bukkit.getOfflinePlayer(targetId);
            String name = off.getName() != null ? off.getName() : "Unknown";

            inventory.setItem(slot, new ItemBuilder(Material.PLAYER_HEAD)
                    .name(ColorUtils.color("<red>" + name + "</red>"))
                    .lore(
                            ColorUtils.color("<yellow>Bounty: " + economy.format(bounty.getAmount()) + "</yellow>"),
                            ColorUtils.color("<gray>Click to cancel & refund</gray>")
                    )
                    .build());
            slot++;
        }

        if (entries.isEmpty()) {
            inventory.setItem(22, new ItemBuilder(Material.LIME_DYE)
                    .name(ColorUtils.color("<green>No Active Bounties</green>"))
                    .lore(ColorUtils.color("<gray>Nothing to manage.</gray>"))
                    .build());
        }

        if (page > 0) {
            inventory.setItem(45, new ItemBuilder(Material.ARROW)
                    .name(ColorUtils.color("<yellow>Previous Page</yellow>"))
                    .build());
        }
        if (entries.size() > (page + 1) * 45) {
            inventory.setItem(53, new ItemBuilder(Material.ARROW)
                    .name(ColorUtils.color("<yellow>Next Page</yellow>"))
                    .build());
        }

        inventory.setItem(49, new ItemBuilder(Material.ARROW)
                .name(ColorUtils.color("<red>Back</red>"))
                .build());
    }

    @Override
    public Inventory getInventory() { return inventory; }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getSlot();
        Player clicker = (Player) event.getWhoClicked();
        EconomyManager economy = plugin.getEconomyManager();

        if (slot == 49) {
            clicker.closeInventory();
            clicker.openInventory(new BountyGUI(plugin, clicker).getInventory());
            return;
        }

        if (slot == 45 && page > 0) { page--; populate(); return; }
        if (slot == 53) { page++; populate(); return; }

        if (slot >= 0 && slot <= 44) {
            List<Map.Entry<UUID, BountyEntry>> entries = new ArrayList<>(economy.getAllBountyEntries().entrySet());
            int index = page * 45 + slot;
            if (index < 0 || index >= entries.size()) return;

            Map.Entry<UUID, BountyEntry> mapEntry = entries.get(index);
            UUID targetId = mapEntry.getKey();
            OfflinePlayer off = Bukkit.getOfflinePlayer(targetId);
            String name = off.getName() != null ? off.getName() : "Unknown";

            if (economy.cancelBounty(targetId, clicker.getUniqueId())) {
                clicker.sendMessage(ColorUtils.color("<green>Bounty on " + name + " cancelled. Refund issued to placer.</green>"));
                populate();
            } else {
                clicker.sendMessage(ColorUtils.color("<red>Could not cancel bounty.</red>"));
            }
        }
    }
}
