package com.odeco.gui;

import com.odeco.ODEco;
import com.odeco.utils.ColorUtils;
import com.odeco.utils.Heads;
import com.odeco.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ItemSellSetupGUI implements InventoryHolder {

    private final ODEco plugin;
    private final Player player;
    private final Inventory inventory;
    private int pageOffset = 0;

    public ItemSellSetupGUI(ODEco plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, ColorUtils.color("<gold>Item Sell Setup</gold>"));
        populate();
    }

    private void populate() {
        inventory.clear();

        inventory.setItem(0, new ItemBuilder(Material.ARROW)
                .name(ColorUtils.color("<red>Back</red>"))
                .build());

        inventory.setItem(4, new ItemBuilder(Material.PLAYER_HEAD)
                .name(ColorUtils.color("<gold>Sellable Items</gold>"))
                .skull(Heads.COIN_PILE_BRONZE)
                .lore(ColorUtils.color("<gray>Manage items players can sell</gray>"))
                .build());

        Map<String, Double> worth = plugin.getConfigManager().getWorth();
        List<Map.Entry<String, Double>> entries = new ArrayList<>(worth.entrySet());

        int slot = 9;
        int start = pageOffset * 36;
        for (int i = start; i < Math.min(entries.size(), start + 36); i++) {
            if (slot >= 45) break;
            Map.Entry<String, Double> entry = entries.get(i);
            String materialName = entry.getKey();
            double price = entry.getValue();
            Material mat = Material.getMaterial(materialName);

            inventory.setItem(slot, new ItemBuilder(mat != null ? mat : Material.PAPER)
                    .name(ColorUtils.color("<gold>" + materialName + "</gold>"))
                    .lore(
                            ColorUtils.color("<gray>Sell Price: " + plugin.getEconomyManager().format(price) + "</gray>"),
                            ColorUtils.color("<dark_gray>Left-click: Change Price</dark_gray>"),
                            ColorUtils.color("<red>Right-click: Remove</red>")
                    )
                    .build());
            slot++;
        }

        if (entries.isEmpty()) {
            inventory.setItem(22, new ItemBuilder(Material.BARRIER)
                    .name(ColorUtils.color("<gray>No Sellable Items</gray>"))
                    .lore(ColorUtils.color("<gray>Click Add Item to add one</gray>"))
                    .build());
        }

        inventory.setItem(49, new ItemBuilder(Material.EMERALD)
                .name(ColorUtils.color("<green>Add Item</green>"))
                .lore(
                        ColorUtils.color("<gray>Hold the item in your hand</gray>"),
                        ColorUtils.color("<gray>then click here to set a sell price</gray>")
                )
                .build());

        if (pageOffset > 0) {
            inventory.setItem(45, new ItemBuilder(Material.ARROW)
                    .name(ColorUtils.color("<yellow>Previous Page</yellow>"))
                    .build());
        }
        if (entries.size() > (pageOffset + 1) * 36) {
            inventory.setItem(53, new ItemBuilder(Material.ARROW)
                    .name(ColorUtils.color("<yellow>Next Page</yellow>"))
                    .build());
        }
    }

    @Override
    public Inventory getInventory() { return inventory; }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getSlot();
        Player clicker = (Player) event.getWhoClicked();

        if (slot == 0) {
            clicker.closeInventory();
            clicker.openInventory(new AdminPanelGUI(plugin, clicker).getInventory());
            return;
        }

        if (slot == 45 && pageOffset > 0) { pageOffset--; populate(); return; }
        if (slot == 53) { pageOffset++; populate(); return; }

        if (slot == 49) {
            ItemStack held = clicker.getInventory().getItemInMainHand();
            if (held == null || held.getType() == Material.AIR) {
                clicker.sendMessage(ColorUtils.color("<red>Hold an item in your hand first.</red>"));
                return;
            }
            String materialName = held.getType().name();
            clicker.closeInventory();
            plugin.getChatInputManager().requestInput(clicker,
                "<gold>Enter sell price for " + materialName + ":</gold>",
                priceStr -> {
                    try {
                        double price = Double.parseDouble(priceStr);
                        if (price <= 0) {
                            clicker.sendMessage(ColorUtils.color("<red>Price must be positive.</red>"));
                            return;
                        }
                        plugin.getConfigManager().setWorth(materialName, price);
                        clicker.sendMessage(ColorUtils.color("<green>" + materialName + " sell price set to " + plugin.getEconomyManager().format(price) + "</green>"));
                    } catch (NumberFormatException e) {
                        clicker.sendMessage(ColorUtils.color("<red>Invalid number.</red>"));
                    }
                }, () -> clicker.openInventory(new ItemSellSetupGUI(plugin, clicker).getInventory()));
            return;
        }

        if (slot >= 9 && slot <= 44) {
            Map<String, Double> worth = plugin.getConfigManager().getWorth();
            List<Map.Entry<String, Double>> entries = new ArrayList<>(worth.entrySet());
            int index = pageOffset * 36 + (slot - 9);
            if (index < 0 || index >= entries.size()) return;

            Map.Entry<String, Double> entry = entries.get(index);
            String materialName = entry.getKey();

            if (event.isRightClick()) {
                plugin.getConfigManager().removeWorth(materialName);
                clicker.sendMessage(ColorUtils.color("<red>Removed " + materialName + " from sell list.</red>"));
                populate();
                return;
            }

            if (event.isLeftClick()) {
                clicker.closeInventory();
                plugin.getChatInputManager().requestInput(clicker,
                    "<gold>Enter new sell price for " + materialName + ":</gold>",
                    priceStr -> {
                        try {
                            double price = Double.parseDouble(priceStr);
                            if (price <= 0) {
                                clicker.sendMessage(ColorUtils.color("<red>Price must be positive.</red>"));
                                return;
                            }
                            plugin.getConfigManager().setWorth(materialName, price);
                            clicker.sendMessage(ColorUtils.color("<green>" + materialName + " sell price set to " + plugin.getEconomyManager().format(price) + "</green>"));
                        } catch (NumberFormatException e) {
                            clicker.sendMessage(ColorUtils.color("<red>Invalid number.</red>"));
                        }
                    }, () -> clicker.openInventory(new ItemSellSetupGUI(plugin, clicker).getInventory()));
            }
        }
    }
}
