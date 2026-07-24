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

import java.util.*;

public class DealershipSetupGUI implements InventoryHolder {

    private final ODEco plugin;
    private final Player player;
    private final Inventory inventory;
    private SetupPage currentPage = SetupPage.MAIN;
    private String selectedDealership = null;
    private int pageOffset = 0;

    private enum SetupPage { MAIN, EDIT_ITEMS, EDIT_MEMBERS }

    public DealershipSetupGUI(ODEco plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, ColorUtils.color("<dark_green>Dealership Setup</dark_green>"));
        populate();
    }

    private void populate() {
        inventory.clear();
        switch (currentPage) {
            case MAIN -> populateMain();
            case EDIT_ITEMS -> populateEditItems();
            case EDIT_MEMBERS -> populateEditMembers();
        }
    }

    private void populateMain() {
        inventory.setItem(4, new ItemBuilder(Material.PLAYER_HEAD)
                .name(ColorUtils.color("<dark_green>Dealership Management</dark_green>"))
                .skull(Heads.AUCTION)
                .lore(ColorUtils.color("<gray>Manage all server dealerships</gray>"))
                .build());

        inventory.setItem(12, new ItemBuilder(Material.CHEST)
                .name(ColorUtils.color("<green>Create Dealership</green>"))
                .lore(ColorUtils.color("<gray>Click to create a new dealership</gray>"))
                .build());

        inventory.setItem(14, new ItemBuilder(Material.BARRIER)
                .name(ColorUtils.color("<red>Delete Dealership</red>"))
                .lore(ColorUtils.color("<gray>Click to delete an existing dealership</gray>"))
                .build());

        Collection<Dealership> dealerships = plugin.getDealershipManager().getAllDealerships();
        List<Dealership> sorted = new ArrayList<>(dealerships);
        sorted.sort(Comparator.comparing(Dealership::getName, String.CASE_INSENSITIVE_ORDER));
        int slot = 18;
        for (Dealership d : sorted) {
            if (slot >= 45) break;
            int accessCount = d.getAccessCount();
            boolean showOwner = accessCount == 1;

            List<Component> lore = new ArrayList<>();
            if (showOwner) {
                String ownerName = Bukkit.getOfflinePlayer(d.getOwner()).getName();
                lore.add(ColorUtils.color("<gray>Owner: " + (ownerName != null ? ownerName : "Unknown") + "</gray>"));
            }
            lore.add(ColorUtils.color("<gray>Items: " + d.getItems().size() + "</gray>"));
            lore.add(ColorUtils.color("<gray>Access: " + (d.isOpenToAll() ? "Everyone" : d.getAllowedPlayers().size() + " players") + "</gray>"));
            lore.add(ColorUtils.color("<dark_gray>Left-click: Edit Items</dark_gray>"));
            lore.add(ColorUtils.color("<dark_gray>Right-click: Edit Members</dark_gray>"));
            lore.add(ColorUtils.color("<dark_gray>Shift+Right: Change Icon</dark_gray>"));

            ItemStack displayItem;
            if (d.getIcon() != null) {
                displayItem = d.getIcon().clone();
            } else {
                displayItem = new ItemStack(Material.CHEST);
            }
            inventory.setItem(slot, new ItemBuilder(displayItem)
                    .name(ColorUtils.color("<gold>" + d.getName() + "</gold>"))
                    .lore(lore.toArray(new Component[0]))
                    .build());
            slot++;
        }
    }

    private void populateEditItems() {
        Dealership d = plugin.getDealershipManager().getDealership(selectedDealership);
        if (d == null) { currentPage = SetupPage.MAIN; populate(); return; }

        inventory.setItem(0, new ItemBuilder(Material.ARROW)
                .name(ColorUtils.color("<red>Back</red>"))
                .build());

        inventory.setItem(4, new ItemBuilder(Material.CHEST)
                .name(ColorUtils.color("<gold>Edit Items: " + d.getName() + "</gold>"))
                .lore(ColorUtils.color("<gray>Click items below or add new ones</gray>"))
                .build());

        inventory.setItem(48, new ItemBuilder(Material.PLAYER_HEAD)
                .name(ColorUtils.color("<aqua>Edit Members</aqua>"))
                .skull(Heads.BOUNTY)
                .lore(ColorUtils.color("<gray>Manage who can access this dealership</gray>"))
                .build());

        List<DealershipItem> items = d.getItems();
        int slot = 9;
        int start = pageOffset * 36;
        for (int i = start; i < Math.min(items.size(), start + 36); i++) {
            if (slot >= 45) break;
            DealershipItem di = items.get(i);
            String stockStr = di.getStock() == -1 ? "Unlimited" : (di.getStock() + " left");

            inventory.setItem(slot, new ItemBuilder(di.getItem().clone())
                    .name(ColorUtils.color("<gold>" + di.getItem().getType().name() + "</gold>"))
                    .lore(
                            ColorUtils.color("<gray>Price: " + plugin.getEconomyManager().format(di.getPrice()) + "</gray>"),
                            ColorUtils.color("<gray>Stock: " + stockStr + "</gray>"),
                            ColorUtils.color("<dark_gray>Left-click: Set Price</dark_gray>"),
                            ColorUtils.color("<dark_gray>Right-click: Set Stock</dark_gray>"),
                            ColorUtils.color("<red>Shift+Right-click: Remove</red>")
                    )
                    .build());
            slot++;
        }

        inventory.setItem(49, new ItemBuilder(Material.EMERALD)
                .name(ColorUtils.color("<green>Add Item</green>"))
                .lore(
                        ColorUtils.color("<gray>Hold the item you want to sell</gray>"),
                        ColorUtils.color("<gray>then click here</gray>")
                )
                .build());

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

    private void populateEditMembers() {
        Dealership d = plugin.getDealershipManager().getDealership(selectedDealership);
        if (d == null) { currentPage = SetupPage.MAIN; populate(); return; }

        inventory.setItem(0, new ItemBuilder(Material.ARROW)
                .name(ColorUtils.color("<red>Back</red>"))
                .build());

        inventory.setItem(4, new ItemBuilder(Material.PLAYER_HEAD)
                .name(ColorUtils.color("<gold>Edit Members: " + d.getName() + "</gold>"))
                .skull(Heads.BOUNTY)
                .lore(ColorUtils.color("<gray>Manage who can access this dealership</gray>"))
                .build());

        inventory.setItem(20, new ItemBuilder(d.isOpenToAll() ? Material.LIME_DYE : Material.GRAY_DYE)
                .name(ColorUtils.color(d.isOpenToAll() ? "<green>Open to Everyone</green>" : "<red>Restricted Access</red>"))
                .lore(ColorUtils.color("<gray>Click to toggle</gray>"))
                .build());

        inventory.setItem(22, new ItemBuilder(Material.PLAYER_HEAD)
                .name(ColorUtils.color("<gold>Add Allowed Player</gold>"))
                .skull(Heads.PAY)
                .lore(ColorUtils.color("<gray>Type a player name in chat</gray>"))
                .build());

        inventory.setItem(24, new ItemBuilder(Material.RED_BED)
                .name(ColorUtils.color("<gold>Remove Allowed Player</gold>"))
                .lore(ColorUtils.color("<gray>Type a player name in chat</gray>"))
                .build());

        if (!d.isOpenToAll() && !d.getAllowedPlayers().isEmpty()) {
            int slot = 27;
            for (UUID id : d.getAllowedPlayers()) {
                if (slot >= 45) break;
                String name = Bukkit.getOfflinePlayer(id).getName();
                inventory.setItem(slot, new ItemBuilder(Material.PLAYER_HEAD)
                        .name(ColorUtils.color("<gold>" + (name != null ? name : "Unknown") + "</gold>"))
                        .lore(ColorUtils.color("<red>Click to remove</red>"))
                        .build());
                slot++;
            }
        }
    }

    @Override
    public Inventory getInventory() { return inventory; }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getSlot();
        Player clicker = (Player) event.getWhoClicked();

        switch (currentPage) {
            case MAIN -> handleMainClick(slot, event, clicker);
            case EDIT_ITEMS -> handleEditItemsClick(slot, event, clicker);
            case EDIT_MEMBERS -> handleEditMembersClick(slot, event, clicker);
        }
    }

    private void handleMainClick(int slot, InventoryClickEvent event, Player clicker) {
        switch (slot) {
            case 12 -> {
                clicker.closeInventory();
                plugin.getChatInputManager().requestInput(clicker,
                    "<gold>Enter a name for the new dealership:</gold>",
                    name -> {
                        if (name.isEmpty()) {
                            clicker.sendMessage(ColorUtils.color("<red>Name cannot be empty.</red>"));
                            return;
                        }
                        if (plugin.getDealershipManager().createDealership(name, clicker.getUniqueId())) {
                            clicker.sendMessage(ColorUtils.color("<green>Dealership '" + name + "' created!</green>"));
                        } else {
                            clicker.sendMessage(ColorUtils.color("<red>A dealership with that name already exists.</red>"));
                        }
                    }, () -> clicker.openInventory(new DealershipSetupGUI(plugin, clicker).getInventory()));
            }
            case 14 -> {
                Collection<Dealership> all = plugin.getDealershipManager().getAllDealerships();
                if (all.isEmpty()) {
                    clicker.sendMessage(ColorUtils.color("<red>No dealerships to delete.</red>"));
                    return;
                }
                clicker.closeInventory();
                StringBuilder names = new StringBuilder();
                for (Dealership d : all) {
                    if (!names.isEmpty()) names.append(", ");
                    names.append(d.getName());
                }
                plugin.getChatInputManager().requestInput(clicker,
                    "<gold>Enter the name of the dealership to delete:</gold>",
                    name -> {
                        if (plugin.getDealershipManager().deleteDealership(name)) {
                            clicker.sendMessage(ColorUtils.color("<green>Dealership '" + name + "' deleted.</green>"));
                        } else {
                            clicker.sendMessage(ColorUtils.color("<red>Dealership not found.</red>"));
                        }
                    }, () -> clicker.openInventory(new DealershipSetupGUI(plugin, clicker).getInventory()));
            }
        }

        if (slot >= 18 && slot <= 44) {
            List<Dealership> list = new ArrayList<>(plugin.getDealershipManager().getAllDealerships());
            int index = slot - 18;
            if (index >= 0 && index < list.size()) {
                selectedDealership = list.get(index).getName();
                if (event.isShiftClick() && event.isRightClick()) {
                    handleChangeIcon(clicker);
                    return;
                }
                if (event.isRightClick()) {
                    currentPage = SetupPage.EDIT_MEMBERS;
                } else {
                    currentPage = SetupPage.EDIT_ITEMS;
                }
                pageOffset = 0;
                populate();
            }
        }
    }

    private void handleChangeIcon(Player clicker) {
        ItemStack held = clicker.getInventory().getItemInMainHand();
        if (held == null || held.getType() == Material.AIR) {
            clicker.sendMessage(ColorUtils.color("<red>Hold an item to set as the dealership icon.</red>"));
            return;
        }
        Dealership d = plugin.getDealershipManager().getDealership(selectedDealership);
        if (d == null) return;
        d.setIcon(held.clone());
        plugin.getDealershipManager().saveData();
        clicker.sendMessage(ColorUtils.color("<green>Icon set to " + held.getType().name() + " for " + d.getName() + "!</green>"));
        populate();
    }

    private void handleEditItemsClick(int slot, InventoryClickEvent event, Player clicker) {
        Dealership d = plugin.getDealershipManager().getDealership(selectedDealership);
        if (d == null) return;

        if (slot == 0) {
            currentPage = SetupPage.MAIN;
            pageOffset = 0;
            populate();
            return;
        }

        if (slot == 45 && pageOffset > 0) { pageOffset--; populate(); return; }
        if (slot == 53) { pageOffset++; populate(); return; }

        if (slot == 48) {
            currentPage = SetupPage.EDIT_MEMBERS;
            pageOffset = 0;
            populate();
            return;
        }

        if (slot == 49) {
            ItemStack held = clicker.getInventory().getItemInMainHand();
            if (held == null || held.getType() == Material.AIR) {
                clicker.sendMessage(ColorUtils.color("<red>Hold an item in your hand first.</red>"));
                return;
            }
            clicker.closeInventory();
            plugin.getChatInputManager().requestInput(clicker,
                "<gold>Enter the price for " + held.getType().name() + ":</gold>",
                priceStr -> {
                    try {
                        double price = Double.parseDouble(priceStr);
                        if (price <= 0) {
                            clicker.sendMessage(ColorUtils.color("<red>Price must be positive.</red>"));
                            return;
                        }
                        plugin.getDealershipManager().addItemToDealership(selectedDealership, held.clone(), price, -1);
                        clicker.sendMessage(ColorUtils.color("<green>Added " + held.getType().name() + " for " + plugin.getEconomyManager().format(price) + "</green>"));
                    } catch (NumberFormatException e) {
                        clicker.sendMessage(ColorUtils.color("<red>Invalid number.</red>"));
                    }
                }, () -> clicker.openInventory(new DealershipSetupGUI(plugin, clicker).getInventory()));
            return;
        }

        if (slot >= 9 && slot <= 44) {
            int index = pageOffset * 36 + (slot - 9);
            if (index < 0 || index >= d.getItems().size()) return;
            DealershipItem di = d.getItems().get(index);

            if (event.isShiftClick() && event.isRightClick()) {
                d.removeItem(index);
                plugin.getDealershipManager().saveData();
                clicker.sendMessage(ColorUtils.color("<red>Removed item.</red>"));
                populate();
                return;
            }

            if (event.isLeftClick()) {
                clicker.closeInventory();
                plugin.getChatInputManager().requestInput(clicker,
                    "<gold>Enter new price for " + di.getItem().getType().name() + ":</gold>",
                    priceStr -> {
                        try {
                            double price = Double.parseDouble(priceStr);
                            if (price <= 0) {
                                clicker.sendMessage(ColorUtils.color("<red>Price must be positive.</red>"));
                                return;
                            }
                            di.setPrice(price);
                            plugin.getDealershipManager().saveData();
                            clicker.sendMessage(ColorUtils.color("<green>Price set to " + plugin.getEconomyManager().format(price) + "</green>"));
                        } catch (NumberFormatException e) {
                            clicker.sendMessage(ColorUtils.color("<red>Invalid number.</red>"));
                        }
                    }, () -> clicker.openInventory(new DealershipSetupGUI(plugin, clicker).getInventory()));
            } else if (event.isRightClick()) {
                clicker.closeInventory();
                plugin.getChatInputManager().requestInput(clicker,
                    "<gold>Enter stock amount for " + di.getItem().getType().name() + " (-1 = unlimited):</gold>",
                    stockStr -> {
                        try {
                            int stock = Integer.parseInt(stockStr);
                            di.setStock(stock);
                            plugin.getDealershipManager().saveData();
                            clicker.sendMessage(ColorUtils.color("<green>Stock set to " + (stock == -1 ? "unlimited" : stock) + "</green>"));
                        } catch (NumberFormatException e) {
                            clicker.sendMessage(ColorUtils.color("<red>Invalid number.</red>"));
                        }
                    }, () -> clicker.openInventory(new DealershipSetupGUI(plugin, clicker).getInventory()));
            }
        }
    }

    private void handleEditMembersClick(int slot, InventoryClickEvent event, Player clicker) {
        Dealership d = plugin.getDealershipManager().getDealership(selectedDealership);
        if (d == null) return;

        if (slot == 0) {
            currentPage = SetupPage.MAIN;
            pageOffset = 0;
            populate();
            return;
        }

        switch (slot) {
            case 20 -> {
                d.setOpenToAll(!d.isOpenToAll());
                plugin.getDealershipManager().saveData();
                clicker.sendMessage(ColorUtils.color("<green>Access set to: " + (d.isOpenToAll() ? "Everyone" : "Restricted") + "</green>"));
                populate();
            }
            case 22 -> {
                clicker.closeInventory();
                plugin.getChatInputManager().requestInput(clicker,
                    "<gold>Enter player name to allow:</gold>",
                    name -> {
                        org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(name);
                        if (target == null) {
                            clicker.sendMessage(ColorUtils.color("<red>Player not found.</red>"));
                            return;
                        }
                        d.getAllowedPlayers().add(target.getUniqueId());
                        plugin.getDealershipManager().saveData();
                        clicker.sendMessage(ColorUtils.color("<green>Added " + name + " to " + d.getName() + "</green>"));
                    }, () -> clicker.openInventory(new DealershipSetupGUI(plugin, clicker).getInventory()));
            }
            case 24 -> {
                clicker.closeInventory();
                plugin.getChatInputManager().requestInput(clicker,
                    "<gold>Enter player name to remove:</gold>",
                    name -> {
                        org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(name);
                        if (target == null) {
                            clicker.sendMessage(ColorUtils.color("<red>Player not found.</red>"));
                            return;
                        }
                        d.getAllowedPlayers().remove(target.getUniqueId());
                        plugin.getDealershipManager().saveData();
                        clicker.sendMessage(ColorUtils.color("<green>Removed " + name + " from " + d.getName() + "</green>"));
                    }, () -> clicker.openInventory(new DealershipSetupGUI(plugin, clicker).getInventory()));
            }
        }

        if (slot >= 27 && slot <= 44 && !d.isOpenToAll()) {
            List<UUID> allowedList = new ArrayList<>(d.getAllowedPlayers());
            int index = slot - 27;
            if (index >= 0 && index < allowedList.size()) {
                UUID removeId = allowedList.get(index);
                d.getAllowedPlayers().remove(removeId);
                plugin.getDealershipManager().saveData();
                String name = Bukkit.getOfflinePlayer(removeId).getName();
                clicker.sendMessage(ColorUtils.color("<red>Removed " + (name != null ? name : "Unknown") + "</red>"));
                populate();
            }
        }
    }
}
