package com.odeco.gui;

import com.odeco.ODEco;
import com.odeco.economy.EconomyManager;
import com.odeco.utils.ColorUtils;
import com.odeco.utils.Heads;
import com.odeco.utils.ItemBuilder;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.text.SimpleDateFormat;
import java.util.*;

public class TransactionGUI implements InventoryHolder {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd HH:mm");

    private static final Set<String> GAIN_TYPES = Set.of(
            "deposit", "interest", "pay_received", "shared_deposit",
            "lottery_win", "dice_win", "bounty_redeem", "auction_sell",
            "banknote_redeem", "bounty_refund"
    );
    private static final Set<String> LOSS_TYPES = Set.of(
            "withdraw", "pay_sent", "shared_withdraw",
            "lottery_ticket", "dice_lose", "bounty_place", "auction_buy",
            "banknote_withdraw", "tax_paid"
    );

    private final ODEco plugin;
    private final Player player;
    private final Inventory inventory;
    private int page = 0;

    public TransactionGUI(ODEco plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, ColorUtils.color("<blue>Transactions</blue>"));
        populate();
    }

    private String getHeadTexture(String type) {
        return switch (type) {
            case "pay_sent", "pay_received" -> Heads.PAY;
            case "lottery_ticket", "lottery_win" -> Heads.LOTTERY;
            case "dice_lose", "dice_win" -> Heads.DICE;
            case "bounty_place", "bounty_redeem" -> Heads.BOUNTY;
            case "bounty_refund" -> Heads.BOUNTY;
            case "auction_list", "auction_buy", "auction_sell", "auction_cancel" -> Heads.AUCTION;
            case "shared_withdraw", "shared_deposit" -> Heads.SHARED_ACCOUNT;
            case "interest" -> Heads.INTEREST;
            case "deposit", "withdraw" -> Heads.BALTOP;
            case "banknote_withdraw", "banknote_redeem" -> Heads.MONEY_STACK;
            case "tax_paid", "tax_owed" -> Heads.TAXES;
            default -> Heads.TRANSACTIONS;
        };
    }

    private boolean isGain(String type) { return GAIN_TYPES.contains(type); }
    private boolean isLoss(String type) { return LOSS_TYPES.contains(type); }

    private String formatTypeName(String type) {
        return switch (type) {
            case "pay_sent" -> "Pay Sent";
            case "pay_received" -> "Pay Received";
            case "lottery_ticket" -> "Lottery Ticket";
            case "lottery_win" -> "Lottery Win";
            case "dice_lose" -> "Dice Loss";
            case "dice_win" -> "Dice Win";
            case "bounty_place" -> "Bounty Placed";
            case "bounty_redeem" -> "Bounty Claimed";
            case "bounty_refund" -> "Bounty Refund";
            case "auction_list" -> "Auction Listed";
            case "auction_buy" -> "Auction Purchase";
            case "auction_sell" -> "Auction Sale";
            case "auction_cancel" -> "Auction Cancelled";
            case "shared_withdraw" -> "Shared Withdraw";
            case "shared_deposit" -> "Shared Deposit";
            case "banknote_withdraw" -> "Banknote Created";
            case "banknote_redeem" -> "Banknote Redeemed";
            case "tax_paid" -> "Tax Paid";
            case "tax_owed" -> "Tax Owed";
            default -> type.substring(0, 1).toUpperCase() + type.substring(1);
        };
    }

    private void populate() {
        inventory.clear();
        EconomyManager economy = plugin.getEconomyManager();
        List<EconomyManager.TransactionEntry> transactions = economy.getTransactionLog(player.getUniqueId());

        int totalPages = Math.max(1, (int) Math.ceil((double) transactions.size() / 36));
        if (page >= totalPages) page = totalPages - 1;

        int start = page * 36;
        int slot = 9;
        for (int i = start; i < Math.min(transactions.size(), start + 36); i++) {
            if (slot >= 45) break;
            EconomyManager.TransactionEntry entry = transactions.get(i);

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
            var profile = Bukkit.createProfile(UUID.randomUUID(), null);
            profile.setProperty(new ProfileProperty("textures", getHeadTexture(entry.type())));
            skullMeta.setPlayerProfile(profile);
            head.setItemMeta(skullMeta);

            String date = DATE_FORMAT.format(new Date(entry.timestamp()));
            String typeName = formatTypeName(entry.type());

            boolean gain = isGain(entry.type());
            boolean loss = isLoss(entry.type());
            String amountStr;
            if (gain) {
                amountStr = "[+" + economy.formatCompact(entry.amount()) + "]";
            } else if (loss) {
                amountStr = "[-" + economy.formatCompact(entry.amount()) + "]";
            } else {
                amountStr = economy.format(entry.amount());
            }
            String color = gain ? "<green>" : (loss ? "<red>" : "<gray>");

            head = new ItemBuilder(head)
                    .name(ColorUtils.color(color + typeName + "<reset>"))
                    .lore(
                            ColorUtils.color("<yellow>" + amountStr + "</yellow>"),
                            ColorUtils.color("<dark_gray>" + date + "</dark_gray>"),
                            Component.empty(),
                            ColorUtils.color("<gray>" + entry.details() + "</gray>")
                    )
                    .build();
            inventory.setItem(slot, head);
            slot++;
        }

        if (transactions.isEmpty()) {
            inventory.setItem(22, new ItemBuilder(Material.BARRIER)
                    .name(ColorUtils.color("<gray>No Transactions</gray>"))
                    .lore(ColorUtils.color("<dark_gray>Your transaction history is empty</dark_gray>"))
                    .build());
        }

        if (page > 0) {
            inventory.setItem(45, new ItemBuilder(Material.ARROW)
                    .name(ColorUtils.color("<yellow>Previous Page</yellow>"))
                    .lore(ColorUtils.color("<gray>Page " + page + " of " + totalPages + "</gray>"))
                    .build());
        } else {
            inventory.setItem(45, null);
        }

        if (transactions.size() > (page + 1) * 36) {
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
    }

    @Override
    public Inventory getInventory() { return inventory; }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getSlot();
        Player clicker = (Player) event.getWhoClicked();

        switch (slot) {
            case 45 -> { if (page > 0) { page--; populate(); } }
            case 53 -> {
                List<EconomyManager.TransactionEntry> tx = plugin.getEconomyManager().getTransactionLog(player.getUniqueId());
                if (tx.size() > (page + 1) * 36) { page++; populate(); }
            }
            case 49 -> {
                clicker.closeInventory();
                clicker.openInventory(new EcoPanelGUI(plugin, clicker).getInventory());
            }
        }
    }
}
