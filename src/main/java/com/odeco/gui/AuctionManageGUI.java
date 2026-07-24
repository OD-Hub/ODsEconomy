package com.odeco.gui;

import com.odeco.ODEco;
import com.odeco.economy.EconomyManager;
import com.odeco.economy.PendingOrder;
import com.odeco.utils.ColorUtils;
import com.odeco.utils.Heads;
import com.odeco.utils.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AuctionManageGUI implements InventoryHolder {

    private static final int PAGE_SIZE = 36;
    private static final int GRID_START = 9;
    private static final int GRID_END = 44;

    private final ODEco plugin;
    private final Player player;
    private final Inventory inventory;
    private int page = 0;
    private List<PendingOrder> myAuctions;

    public AuctionManageGUI(ODEco plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, ColorUtils.color("<gold>Manage Listings</gold>"));
        loadAuctions();
        populate();
    }

    private void loadAuctions() {
        UUID sellerId = player.getUniqueId();
        this.myAuctions = plugin.getEconomyManager().getActiveAuctions().stream()
                .filter(order -> order.getSellerId().equals(sellerId))
                .toList();
    }

    private void populate() {
        for (int i = GRID_START; i <= GRID_END; i++) {
            inventory.setItem(i, null);
        }

        int totalPages = Math.max(1, (int) Math.ceil((double) myAuctions.size() / PAGE_SIZE));
        if (page >= totalPages) page = totalPages - 1;

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, myAuctions.size());

        for (int i = start; i < end; i++) {
            PendingOrder order = myAuctions.get(i);
            int slot = GRID_START + (i - start);

            ItemStack displayItem = order.getItem().clone();
            ItemMeta meta = displayItem.getItemMeta();
            List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
            lore.add(Component.empty());
            lore.add(ColorUtils.color("<gold>Price: " + plugin.getEconomyManager().format(order.getPrice()) + "</gold>"));
            lore.add(Component.empty());
            lore.add(ColorUtils.color("<yellow>Click to edit price</yellow>"));
            lore.add(ColorUtils.color("<red>Right-click to delist</red>"));
            meta.lore(lore);
            displayItem.setItemMeta(meta);
            inventory.setItem(slot, displayItem);
        }

        if (page > 0) {
            inventory.setItem(45, new ItemBuilder(Material.ARROW)
                    .name(ColorUtils.color("<yellow>Previous Page</yellow>"))
                    .lore(ColorUtils.color("<gray>Page " + page + " of " + totalPages + "</gray>"))
                    .build());
        } else {
            inventory.setItem(45, null);
        }

        if (page < totalPages - 1) {
            inventory.setItem(53, new ItemBuilder(Material.ARROW)
                    .name(ColorUtils.color("<yellow>Next Page</yellow>"))
                    .lore(ColorUtils.color("<gray>Page " + (page + 2) + " of " + totalPages + "</gray>"))
                    .build());
        } else {
            inventory.setItem(53, null);
        }

        inventory.setItem(49, new ItemBuilder(Material.ARROW)
                .name(ColorUtils.color("<red>Back</red>"))
                .build());

        inventory.setItem(50, new ItemBuilder(Material.EMERALD_BLOCK)
                .name(ColorUtils.color("<green>List Item</green>"))
                .lore(
                        ColorUtils.color("<gray>Hold an item and click</gray>"),
                        ColorUtils.color("<gray>to list it for sale</gray>")
                )
                .build());

        inventory.setItem(51, new ItemBuilder(Material.BOOK)
                .name(ColorUtils.color("<gold>Your Listings</gold>"))
                .lore(
                        ColorUtils.color("<gray>Listings: " + myAuctions.size() + "</gray>"),
                        ColorUtils.color("<gray>Page " + (page + 1) + " of " + totalPages + "</gray>")
                )
                .build());
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getSlot();
        Player clicker = (Player) event.getWhoClicked();
        EconomyManager economy = plugin.getEconomyManager();

        if (slot >= GRID_START && slot <= GRID_END) {
            int index = page * PAGE_SIZE + (slot - GRID_START);
            if (index < 0 || index >= myAuctions.size()) return;

            PendingOrder order = myAuctions.get(index);

            if (event.getClick() == ClickType.RIGHT) {
                if (economy.cancelAuction(order)) {
                    clicker.getInventory().addItem(order.getItem().clone());
                    clicker.sendMessage(ColorUtils.color("<green>Auction delisted. Item returned.</green>"));
                    loadAuctions();
                    populate();
                } else {
                    clicker.sendMessage(ColorUtils.color("<red>Could not delist item.</red>"));
                }
                return;
            }

            plugin.getChatInputManager().requestInput(clicker,
                    "<gold>Enter new price for " + order.getItem().getType().name() + ":</gold>",
                    priceStr -> {
                        try {
                            double newPrice = Double.parseDouble(priceStr);
                            if (newPrice <= 0) {
                                clicker.sendMessage(ColorUtils.color("<red>Price must be positive.</red>"));
                                return;
                            }
                            ItemStack item = order.getItem().clone();
                            economy.cancelAuction(order);
                            economy.createAuction(clicker.getUniqueId(), clicker.getName(), item, newPrice);
                            clicker.sendMessage(ColorUtils.color("<green>Price updated to " + economy.format(newPrice) + "</green>"));
                            loadAuctions();
                            populate();
                        } catch (NumberFormatException e) {
                            clicker.sendMessage(ColorUtils.color("<red>Invalid price.</red>"));
                        }
                    });
            return;
        }

        switch (slot) {
            case 45 -> {
                if (page > 0) {
                    page--;
                    populate();
                }
            }
            case 53 -> {
                int totalPages = (int) Math.ceil((double) myAuctions.size() / PAGE_SIZE);
                if (page < totalPages - 1) {
                    page++;
                    populate();
                }
            }
            case 49 -> {
                clicker.closeInventory();
                clicker.openInventory(new AuctionHouseGUI(plugin, clicker).getInventory());
            }
            case 50 -> {
                ItemStack held = clicker.getInventory().getItemInMainHand();
                if (held == null || held.getType() == Material.AIR) {
                    clicker.sendMessage(ColorUtils.color("<red>You are not holding any item.</red>"));
                    return;
                }
                plugin.getChatInputManager().requestInput(clicker,
                        "<gold>Enter the price for your " + held.getType().name() + ":</gold>",
                        priceStr -> {
                            try {
                                double price = Double.parseDouble(priceStr);
                                if (price <= 0) {
                                    clicker.sendMessage(ColorUtils.color("<red>Price must be positive.</red>"));
                                    return;
                                }
                                ItemStack toList = held.clone();
                                held.setAmount(0);
                                if (economy.createAuction(clicker.getUniqueId(), clicker.getName(), toList, price)) {
                                    clicker.sendMessage(ColorUtils.color("<green>Item listed for " + economy.format(price) + "</green>"));
                                } else {
                                    clicker.getInventory().addItem(toList);
                                    clicker.sendMessage(ColorUtils.color("<red>Could not list item.</red>"));
                                }
                            } catch (NumberFormatException e) {
                                clicker.sendMessage(ColorUtils.color("<red>Invalid price.</red>"));
                            }
                        });
            }
        }
    }
}
