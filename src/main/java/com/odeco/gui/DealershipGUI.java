package com.odeco.gui;

import com.odeco.ODEco;
import com.odeco.economy.Dealership;
import com.odeco.economy.DealershipItem;
import com.odeco.utils.ColorUtils;
import com.odeco.utils.Heads;
import com.odeco.utils.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.Comparator;
import java.util.List;

import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.inventory.meta.SkullMeta;
import com.destroystokyo.paper.profile.PlayerProfile;

public class DealershipGUI implements InventoryHolder {

    private final ODEco plugin;
    private final Player player;
    private final Inventory inventory;
    private DealershipPage currentPage = DealershipPage.LIST;
    private String selectedDealership = null;
    private int pageOffset = 0;

    private enum DealershipPage { LIST, ITEMS }

    public DealershipGUI(ODEco plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, ColorUtils.color("<dark_aqua>Dealerships</dark_aqua>"));
        populate();
    }

    public DealershipGUI(ODEco plugin, Player player, String dealershipName) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, ColorUtils.color("<dark_aqua>Dealerships</dark_aqua>"));
        this.currentPage = DealershipPage.ITEMS;
        this.selectedDealership = dealershipName;
        populate();
    }

    private void populate() {
        inventory.clear();
        switch (currentPage) {
            case LIST -> populateList();
            case ITEMS -> populateItems();
        }
    }

    private void populateList() {
        List<Dealership> dealerships = plugin.getDealershipManager().getDealershipsForPlayer(player.getUniqueId());
        dealerships.sort(Comparator.comparing(Dealership::getName, String.CASE_INSENSITIVE_ORDER));

        inventory.setItem(4, new ItemBuilder(Material.PLAYER_HEAD)
                .name(ColorUtils.color("<dark_aqua>Your Dealerships</dark_aqua>"))
                .skull(Heads.AUCTION)
                .lore(ColorUtils.color("<gray>Dealerships you have access to</gray>"))
                .build());

        if (dealerships.isEmpty()) {
            inventory.setItem(22, new ItemBuilder(Material.BARRIER)
                    .name(ColorUtils.color("<gray>No Dealerships Available</gray>"))
                    .lore(ColorUtils.color("<gray>You don't have access to any dealerships.</gray>"))
                    .build());
        } else {
            int slot = 9;
            int start = pageOffset * 36;
            for (int i = start; i < Math.min(dealerships.size(), start + 36); i++) {
                if (slot >= 45) break;
                Dealership d = dealerships.get(i);
                int accessCount = d.getAccessCount();
                boolean showOwner = accessCount == 1;

                List<Component> lore = new java.util.ArrayList<>();
                if (showOwner) {
                    String ownerName = Bukkit.getOfflinePlayer(d.getOwner()).getName();
                    lore.add(ColorUtils.color("<gray>Owner: " + (ownerName != null ? ownerName : "Unknown") + "</gray>"));
                }
                lore.add(ColorUtils.color("<gray>Items: " + d.getItems().size() + "</gray>"));
                lore.add(ColorUtils.color("<gray>Click to browse</gray>"));

                ItemStack displayItem;
                if (d.getIcon() != null) {
                    displayItem = d.getIcon().clone();
                } else {
                    displayItem = new ItemStack(Material.CHEST);
                }
                displayItem = new ItemBuilder(displayItem)
                        .name(ColorUtils.color("<gold>" + d.getName() + "</gold>"))
                        .lore(lore.toArray(new Component[0]))
                        .build();
                inventory.setItem(slot, displayItem);
                slot++;
            }

            if (pageOffset > 0) {
                inventory.setItem(45, new ItemBuilder(Material.ARROW)
                        .name(ColorUtils.color("<yellow>Previous Page</yellow>"))
                        .build());
            }
            if (dealerships.size() > (pageOffset + 1) * 36) {
                inventory.setItem(53, new ItemBuilder(Material.ARROW)
                        .name(ColorUtils.color("<yellow>Next Page</yellow>"))
                        .build());
            }
        }

        inventory.setItem(49, new ItemBuilder(Material.ARROW)
                .name(ColorUtils.color("<red>Back</red>"))
                .build());
    }

    private void populateItems() {
        Dealership d = plugin.getDealershipManager().getDealership(selectedDealership);
        if (d == null) {
            currentPage = DealershipPage.LIST;
            populate();
            return;
        }

        inventory.setItem(0, new ItemBuilder(Material.ARROW)
                .name(ColorUtils.color("<red>Back</red>"))
                .build());

        ItemStack headerItem;
        if (d.getIcon() != null) {
            headerItem = d.getIcon().clone();
        } else {
            headerItem = new ItemStack(Material.CHEST);
        }
        inventory.setItem(4, new ItemBuilder(headerItem)
                .name(ColorUtils.color("<gold>" + d.getName() + "</gold>"))
                .lore(ColorUtils.color("<gray>Click an item to purchase</gray>"))
                .build());

        List<DealershipItem> items = d.getItems();
        int slot = 9;
        int start = pageOffset * 36;
        for (int i = start; i < Math.min(items.size(), start + 36); i++) {
            if (slot >= 45) break;
            DealershipItem di = items.get(i);
            boolean hasStock = di.hasStock();
            String stockStr = di.getStock() == -1 ? "Unlimited" : (di.getStock() > 0 ? di.getStock() + " left" : "Out of stock");
            String stockColor = hasStock ? "<green>" : "<red>";

            ItemStack displayItem = di.getItem().clone();
            inventory.setItem(slot, new ItemBuilder(displayItem)
                    .name(ColorUtils.color("<gold>" + di.getItem().getType().name() + "</gold>"))
                    .lore(
                            ColorUtils.color("<gray>Price: " + plugin.getEconomyManager().format(di.getPrice()) + " each</gray>"),
                            ColorUtils.color(stockColor + stockStr + "</" + stockColor.replace("<", "").replace(">", "") + ">"),
                            ColorUtils.color("<gray>Click to buy</gray>")
                    )
                    .build());
            slot++;
        }

        if (items.isEmpty()) {
            inventory.setItem(22, new ItemBuilder(Material.BARRIER)
                    .name(ColorUtils.color("<gray>No Items</gray>"))
                    .lore(ColorUtils.color("<gray>This dealership has no items for sale.</gray>"))
                    .build());
        }

        if (pageOffset > 0) {
            inventory.setItem(45, new ItemBuilder(Material.ARROW)
                    .name(ColorUtils.color("<yellow>Previous Page</yellow>"))
                    .build());
        }
        if (items.size() > (pageOffset + 1) * 36) {
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

        switch (currentPage) {
            case LIST -> {
                if (slot == 49) {
                    clicker.closeInventory();
                    clicker.openInventory(new EcoPanelGUI(plugin, clicker).getInventory());
                    return;
                }
                if (slot == 45 && pageOffset > 0) { pageOffset--; populate(); return; }
                if (slot == 53) { pageOffset++; populate(); return; }

                if (slot >= 9 && slot <= 44) {
                    List<Dealership> dealerships = plugin.getDealershipManager().getDealershipsForPlayer(clicker.getUniqueId());
                    int index = pageOffset * 36 + (slot - 9);
                    if (index >= 0 && index < dealerships.size()) {
                        selectedDealership = dealerships.get(index).getName();
                        currentPage = DealershipPage.ITEMS;
                        pageOffset = 0;
                        populate();
                    }
                }
            }
            case ITEMS -> {
                if (slot == 0) {
                    currentPage = DealershipPage.LIST;
                    pageOffset = 0;
                    populate();
                    return;
                }
                if (slot == 45 && pageOffset > 0) { pageOffset--; populate(); return; }
                if (slot == 53) { pageOffset++; populate(); return; }

                if (slot >= 9 && slot <= 44) {
                    Dealership d = plugin.getDealershipManager().getDealership(selectedDealership);
                    if (d == null) return;
                    int index = pageOffset * 36 + (slot - 9);
                    if (index >= 0 && index < d.getItems().size()) {
                        DealershipItem di = d.getItems().get(index);
                        if (!di.hasStock()) {
                            clicker.sendMessage(ColorUtils.color("<red>This item is out of stock.</red>"));
                            return;
                        }

                        double balance = plugin.getEconomyManager().getBalance(clicker.getUniqueId());
                        int maxAffordable = (int) Math.floor(balance / di.getPrice());
                        int maxAvailable = di.getStock() == -1 ? maxAffordable : Math.min(maxAffordable, di.getStock());
                        if (maxAvailable <= 0) {
                            clicker.sendMessage(ColorUtils.color("<red>You cannot afford any of this item.</red>"));
                            return;
                        }

                        clicker.closeInventory();
                        plugin.getChatInputManager().requestInput(clicker,
                            "<gold>Enter quantity for " + di.getItem().getType().name() + " (1-" + maxAvailable + ", price: " + plugin.getEconomyManager().format(di.getPrice()) + " each):</gold>",
                            input -> {
                                try {
                                    int quantity = Integer.parseInt(input);
                                    if (quantity <= 0) {
                                        clicker.sendMessage(ColorUtils.color("<red>Quantity must be positive.</red>"));
                                        return;
                                    }
                                    if (quantity > maxAvailable) {
                                        clicker.sendMessage(ColorUtils.color("<red>Maximum quantity is " + maxAvailable + ".</red>"));
                                        return;
                                    }
                                    boolean success = plugin.getDealershipManager().buyItemQuantity(selectedDealership, index, clicker.getUniqueId(), quantity);
                                    if (success) {
                                        clicker.sendMessage(ColorUtils.color("<green>Purchased " + quantity + "x " + di.getItem().getType().name() + " for " + plugin.getEconomyManager().format(di.getPrice() * quantity) + "!</green>"));
                                    } else {
                                        clicker.sendMessage(ColorUtils.color("<red>Could not purchase. Check your balance.</red>"));
                                    }
                                } catch (NumberFormatException e) {
                                    clicker.sendMessage(ColorUtils.color("<red>Invalid number.</red>"));
                                }
                            }, () -> clicker.openInventory(new DealershipGUI(plugin, clicker, selectedDealership).getInventory()));
                    }
                }
            }
        }
    }
}
