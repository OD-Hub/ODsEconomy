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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class AuctionHouseGUI implements InventoryHolder {

    private static final int PAGE_SIZE = 36;
    private static final int GRID_START = 9;
    private static final int GRID_END = 44;

    private final ODEco plugin;
    private final Player player;
    private final Inventory inventory;
    private int page = 0;
    private List<PendingOrder> auctions;

    public AuctionHouseGUI(ODEco plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, ColorUtils.color("<gold>Auction House</gold>"));
        loadAuctions();
        populate();
    }

    private void loadAuctions() {
        this.auctions = new ArrayList<>(plugin.getEconomyManager().getActiveAuctions());
    }

    private void populate() {
        for (int i = GRID_START; i <= GRID_END; i++) {
            inventory.setItem(i, null);
        }

        int totalPages = Math.max(1, (int) Math.ceil((double) auctions.size() / PAGE_SIZE));
        if (page >= totalPages) page = totalPages - 1;

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, auctions.size());

        for (int i = start; i < end; i++) {
            PendingOrder order = auctions.get(i);
            int slot = GRID_START + (i - start);

            ItemStack displayItem = order.getItem().clone();
            ItemMeta meta = displayItem.getItemMeta();
            List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
            lore.add(Component.empty());
            lore.add(ColorUtils.color("<gold>Price: " + plugin.getEconomyManager().format(order.getPrice()) + "</gold>"));
            lore.add(ColorUtils.color("<gray>Seller: " + order.getSellerName() + "</gray>"));
            lore.add(Component.empty());
            lore.add(ColorUtils.color("<green>Click to buy</green>"));
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

        inventory.setItem(48, new ItemBuilder(Material.PLAYER_HEAD)
                .name(ColorUtils.color("<gold>Manage Listings</gold>"))
                .lore(ColorUtils.color("<gray>View and manage your auctions</gray>"))
                .skull(Heads.AUCTION)
                .build());

        int totalPagesDisplay = Math.max(1, (int) Math.ceil((double) auctions.size() / PAGE_SIZE));
        inventory.setItem(51, new ItemBuilder(Material.BOOK)
                .name(ColorUtils.color("<gold>Auction Info</gold>"))
                .lore(
                        ColorUtils.color("<gray>Listings: " + auctions.size() + "</gray>"),
                        ColorUtils.color("<gray>Page " + (page + 1) + " of " + totalPagesDisplay + "</gray>")
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
            if (index < 0 || index >= auctions.size()) return;

            PendingOrder order = auctions.get(index);

            if (order.getSellerId().equals(clicker.getUniqueId())) {
                clicker.sendMessage(ColorUtils.color("<red>You cannot buy your own listing.</red>"));
                return;
            }

            if (!economy.hasBalance(clicker.getUniqueId(), order.getPrice())) {
                clicker.sendMessage(ColorUtils.color("<red>Insufficient balance.</red>"));
                return;
            }

            if (economy.buyAuction(order, clicker.getUniqueId())) {
                clicker.getInventory().addItem(order.getItem().clone());
                clicker.sendMessage(ColorUtils.color("<green>Bought " + order.getItem().getType().name() + " for " + economy.format(order.getPrice()) + "</green>"));
                Player seller = Bukkit.getPlayer(order.getSellerId());
                if (seller != null) {
                    seller.sendMessage(ColorUtils.color("<green>" + clicker.getName() + " bought your " + order.getItem().getType().name() + " for " + economy.format(order.getPrice()) + "!</green>"));
                }
                loadAuctions();
                populate();
            } else {
                clicker.sendMessage(ColorUtils.color("<red>Could not purchase item.</red>"));
            }
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
                int totalPages = (int) Math.ceil((double) auctions.size() / PAGE_SIZE);
                if (page < totalPages - 1) {
                    page++;
                    populate();
                }
            }
            case 49 -> {
                clicker.closeInventory();
                clicker.openInventory(new EcoPanelGUI(plugin, clicker).getInventory());
            }
            case 48 -> {
                clicker.closeInventory();
                clicker.openInventory(new AuctionManageGUI(plugin, clicker).getInventory());
            }
        }
    }
}
