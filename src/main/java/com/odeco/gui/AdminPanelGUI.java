package com.odeco.gui;

import com.odeco.ODEco;
import com.odeco.economy.EconomyManager;
import com.odeco.utils.ColorUtils;
import com.odeco.utils.Heads;
import com.odeco.utils.ItemBuilder;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.text.SimpleDateFormat;
import java.util.*;

public class AdminPanelGUI implements InventoryHolder {

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
    private AdminPage currentPage = AdminPage.MAIN;

    private enum AdminPage {
        MAIN, BALANCE_EDIT, TRANSACTIONS, LOTTERY, INTEREST
    }

    private int pageOffset = 0;
    private final Map<Integer, UUID> slotToPlayer = new HashMap<>();

    public AdminPanelGUI(ODEco plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, ColorUtils.color("<dark_red>Admin Panel</dark_red>"));
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
        slotToPlayer.clear();
        EconomyManager economy = plugin.getEconomyManager();

        switch (currentPage) {
            case MAIN -> populateMain(economy);
            case BALANCE_EDIT -> populateBalanceEdit(economy);
            case TRANSACTIONS -> populateTransactions(economy);
            case LOTTERY -> populateLottery(economy);
            case INTEREST -> populateInterest(economy);
        }
    }

    private void populateMain(EconomyManager economy) {
        double totalBalance = 0;
        int playerCount = 0;
        for (var entry : economy.getTopBalances()) {
            totalBalance += entry.getValue();
            playerCount++;
        }

        // Row 0 — Header
        inventory.setItem(4, new ItemBuilder(Material.GOLD_BLOCK)
                .name(ColorUtils.color("<gold>Economy Overview</gold>"))
                .lore(
                    ColorUtils.color("<gray>Total Balance: " + economy.format(totalBalance) + "</gray>"),
                    ColorUtils.color("<gray>Players Tracked: " + playerCount + "</gray>"),
                    ColorUtils.color("<gray>Lottery Pot: " + economy.format(economy.getLotteryPot()) + "</gray>")
                )
                .build());

        // Row 2 — Core admin tools
        inventory.setItem(20, new ItemBuilder(Material.EMERALD_BLOCK)
                .name(ColorUtils.color("<green>Balance Editor</green>"))
                .lore(ColorUtils.color("<gray>Give, take, or set player balances</gray>"))
                .build());

        inventory.setItem(21, new ItemBuilder(Material.PLAYER_HEAD)
                .name(ColorUtils.color("<blue>Transaction Viewer</blue>"))
                .skull(Heads.TRANSACTIONS)
                .lore(ColorUtils.color("<gray>View recent transactions</gray>"))
                .build());

        inventory.setItem(23, new ItemBuilder(Material.PLAYER_HEAD)
                .name(ColorUtils.color("<dark_red>Configure Taxes</dark_red>"))
                .skull(Heads.TAXES)
                .lore(ColorUtils.color("<gray>Manage tax settings and debts</gray>"))
                .build());

        inventory.setItem(24, new ItemBuilder(Material.PLAYER_HEAD)
                .name(ColorUtils.color("<green>Interest Config</green>"))
                .skull(Heads.INTEREST)
                .lore(
                        ColorUtils.color("<gray>Enabled: " + (plugin.getConfigManager().isInterestEnabled() ? "<green>YES</green>" : "<red>NO</red>") + "</gray>"),
                        ColorUtils.color("<gray>Rate: " + plugin.getConfigManager().getInterestRate() + "%</gray>"),
                        ColorUtils.color("<gray>Interval: " + plugin.getConfigManager().getInterestInterval() + " min</gray>"),
                        ColorUtils.color("<gray>Click to configure</gray>")
                )
                .build());

        // Row 4 — Feature wizards
        inventory.setItem(30, new ItemBuilder(Material.PLAYER_HEAD)
                .name(ColorUtils.color("<light_purple>Manage Lottery</light_purple>"))
                .skull(Heads.LOTTERY)
                .lore(
                        ColorUtils.color("<gray>Current Pot: " + economy.format(economy.getLotteryPot()) + "</gray>"),
                        ColorUtils.color("<gray>Entries: " + economy.getLotteryEntryCount() + "</gray>"),
                        ColorUtils.color("<gray>Click to manage and draw</gray>")
                )
                .build());

        inventory.setItem(32, new ItemBuilder(Material.PLAYER_HEAD)
                .name(ColorUtils.color("<gold>Sell Setup Wizard</gold>"))
                .skull(Heads.COIN_PILE_BRONZE)
                .lore(
                        ColorUtils.color("<gray>Configure items players can sell</gray>"),
                        ColorUtils.color("<gray>and set sell prices</gray>")
                )
                .build());

        // Row 5 — Utilities
        inventory.setItem(52, new ItemBuilder(Material.COMPARATOR)
                .name(ColorUtils.color("<yellow>Reload Config</yellow>"))
                .lore(ColorUtils.color("<gray>Reload configuration from disk</gray>"))
                .build());

        inventory.setItem(53, new ItemBuilder(Material.KNOWLEDGE_BOOK)
                .name(ColorUtils.color("<gold>Force Save</gold>"))
                .lore(ColorUtils.color("<gray>Force save all data</gray>"))
                .build());
    }

    private void populateBalanceEdit(EconomyManager economy) {
        inventory.setItem(0, new ItemBuilder(Material.ARROW)
                .name(ColorUtils.color("<red>Back</red>"))
                .build());

        inventory.setItem(4, new ItemBuilder(Material.EMERALD_BLOCK)
                .name(ColorUtils.color("<green>Balance Editor</green>"))
                .lore(ColorUtils.color("<gray>Click a player head to edit their balance</gray>"))
                .build());

        int slot = 9;
        int start = pageOffset * 36;
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        for (int i = start; i < Math.min(players.size(), start + 36); i++) {
            if (slot >= 45) break;
            Player target = players.get(i);
            double bal = economy.getBalance(target.getUniqueId());

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
            var profile = Bukkit.createProfile(target.getUniqueId(), target.getName());
            if (target.getPlayerProfile().getProperties().isEmpty()) {
                profile.clearProperties();
            } else {
                profile.setProperties(target.getPlayerProfile().getProperties());
            }
            skullMeta.setPlayerProfile(profile);
            head.setItemMeta(skullMeta);

            head = new ItemBuilder(head)
                    .name(ColorUtils.color("<gold>" + target.getName() + "</gold>"))
                    .lore(
                        ColorUtils.color("<gray>Balance: " + economy.format(bal) + "</gray>"),
                        ColorUtils.color("<green>Left-click: Give $100</green>"),
                        ColorUtils.color("<red>Right-click: Take $100</red>"),
                        ColorUtils.color("<yellow>Shift+Left: Set balance</yellow>")
                    )
                    .build();
            inventory.setItem(slot, head);
            slotToPlayer.put(slot, target.getUniqueId());
            slot++;
        }

        if (pageOffset > 0) {
            inventory.setItem(45, new ItemBuilder(Material.ARROW)
                    .name(ColorUtils.color("<yellow>Previous Page</yellow>"))
                    .build());
        }
        if (players.size() > (pageOffset + 1) * 36) {
            inventory.setItem(53, new ItemBuilder(Material.ARROW)
                    .name(ColorUtils.color("<yellow>Next Page</yellow>"))
                    .build());
        }
    }

    private void populateTransactions(EconomyManager economy) {
        inventory.setItem(0, new ItemBuilder(Material.ARROW)
                .name(ColorUtils.color("<red>Back</red>"))
                .build());

        inventory.setItem(4, new ItemBuilder(Material.PLAYER_HEAD)
                .name(ColorUtils.color("<blue>Recent Transactions</blue>"))
                .skull(Heads.TRANSACTIONS)
                .build());

        var transactions = economy.getRecentTransactions(36);
        int slot = 9;
        int start = pageOffset * 36;
        for (int i = start; i < Math.min(transactions.size(), start + 36); i++) {
            if (slot >= 45) break;
            var entry = transactions.get(i);

            String playerName = "Unknown";
            var offlinePlayer = Bukkit.getOfflinePlayer(entry.playerId());
            if (offlinePlayer.getName() != null) {
                playerName = offlinePlayer.getName();
            }

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
            var profile = Bukkit.createProfile(entry.playerId(), null);
            profile.setProperty(new ProfileProperty("textures", getHeadTexture(entry.type())));
            skullMeta.setPlayerProfile(profile);
            head.setItemMeta(skullMeta);

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
            String typeName = formatTypeName(entry.type());
            String date = DATE_FORMAT.format(new Date(entry.timestamp()));

            head = new ItemBuilder(head)
                    .name(ColorUtils.color("<gold>" + playerName + "</gold>"))
                    .lore(
                            ColorUtils.color(color + typeName + "</" + color.replace("<", "").replace(">", "") + ">"),
                            ColorUtils.color("<yellow>" + amountStr + "</yellow>"),
                            ColorUtils.color("<dark_gray>" + date + "</dark_gray>"),
                            ColorUtils.color("<gray>" + entry.details() + "</gray>")
                    )
                    .build();
            inventory.setItem(slot, head);
            slot++;
        }

        if (transactions.isEmpty()) {
            inventory.setItem(22, new ItemBuilder(Material.BARRIER)
                    .name(ColorUtils.color("<gray>No Transactions</gray>"))
                    .build());
        }

        if (pageOffset > 0) {
            inventory.setItem(45, new ItemBuilder(Material.ARROW)
                    .name(ColorUtils.color("<yellow>Previous Page</yellow>"))
                    .build());
        }
        if (transactions.size() > (pageOffset + 1) * 36) {
            inventory.setItem(53, new ItemBuilder(Material.ARROW)
                    .name(ColorUtils.color("<yellow>Next Page</yellow>"))
                    .build());
        }
    }

    private void populateLottery(EconomyManager economy) {
        inventory.setItem(0, new ItemBuilder(Material.ARROW)
                .name(ColorUtils.color("<red>Back</red>"))
                .build());

        boolean enabled = plugin.getConfigManager().isLotteryEnabled();
        int entries = economy.getLotteryEntryCount();
        double pot = economy.getLotteryPot();
        double ticketPrice = plugin.getConfigManager().getLotteryTicketPrice();
        double payoutPercent = plugin.getConfigManager().getLotteryPayoutPercent();
        double lastPrize = economy.getLastLotteryPrize();
        UUID lastWinner = economy.getLastLotteryWinner();
        String lastWinnerName = lastWinner != null ? Bukkit.getOfflinePlayer(lastWinner).getName() : null;

        inventory.setItem(4, new ItemBuilder(Material.PLAYER_HEAD)
                .name(ColorUtils.color("<light_purple>Lottery Management</light_purple>"))
                .skull(Heads.LOTTERY)
                .lore(
                        ColorUtils.color("<gray>Status: " + (enabled ? "<green>ENABLED</green>" : "<red>DISABLED</red>") + "</gray>"),
                        ColorUtils.color("<gray>Pot: " + economy.format(pot) + "</gray>"),
                        ColorUtils.color("<gray>Entries: " + entries + "</gray>"),
                        ColorUtils.color("<gray>Ticket Price: " + economy.format(ticketPrice) + "</gray>"),
                        ColorUtils.color("<gray>Payout: " + payoutPercent + "%</gray>")
                )
                .build());

        inventory.setItem(20, new ItemBuilder(Material.SUNFLOWER)
                .name(ColorUtils.color("<gold>Draw Lottery</gold>"))
                .lore(
                        ColorUtils.color(entries >= 2
                                ? "<green>Click to draw a winner</green>"
                                : "<red>Need at least 2 entries to draw</red>"),
                        ColorUtils.color("<gray>Current Pot: " + economy.format(pot) + "</gray>")
                )
                .build());

        inventory.setItem(22, new ItemBuilder(Material.PAPER)
                .name(ColorUtils.color("<gold>Lottery Settings</gold>"))
                .lore(
                        ColorUtils.color("<gray>Ticket Price: " + economy.format(ticketPrice) + "</gray>"),
                        ColorUtils.color("<gray>Payout: " + payoutPercent + "%</gray>")
                )
                .build());

        inventory.setItem(24, new ItemBuilder(Material.BARRIER)
                .name(ColorUtils.color("<red>Reset Lottery</red>"))
                .lore(
                        ColorUtils.color("<gray>Clear all entries and reset pot</gray>"),
                        ColorUtils.color("<dark_gray>Click to confirm</dark_gray>")
                )
                .build());

        if (lastWinnerName != null) {
            inventory.setItem(31, new ItemBuilder(Material.PLAYER_HEAD)
                    .name(ColorUtils.color("<gold>Last Winner</gold>"))
                    .skull(Heads.LOTTERY)
                    .lore(
                            ColorUtils.color("<gray>" + lastWinnerName + "</gray>"),
                            ColorUtils.color("<gray>Won: " + economy.format(lastPrize) + "</gray>")
                    )
                    .build());
        }
    }

    private void populateInterest(EconomyManager economy) {
        inventory.setItem(0, new ItemBuilder(Material.ARROW)
                .name(ColorUtils.color("<red>Back</red>"))
                .build());

        boolean enabled = plugin.getConfigManager().isInterestEnabled();
        double rate = plugin.getConfigManager().getInterestRate();
        int interval = plugin.getConfigManager().getInterestInterval();

        inventory.setItem(4, new ItemBuilder(Material.PLAYER_HEAD)
                .name(ColorUtils.color("<green>Interest Configuration</green>"))
                .skull(Heads.INTEREST)
                .lore(
                        ColorUtils.color("<gray>Status: " + (enabled ? "<green>ENABLED</green>" : "<red>DISABLED</red>") + "</gray>"),
                        ColorUtils.color("<gray>Rate: " + rate + "%</gray>"),
                        ColorUtils.color("<gray>Interval: " + interval + " min</gray>")
                )
                .build());

        inventory.setItem(20, new ItemBuilder(enabled ? Material.LIME_DYE : Material.GRAY_DYE)
                .name(ColorUtils.color(enabled ? "<green>Interest: ENABLED</green>" : "<red>Interest: DISABLED</red>"))
                .lore(ColorUtils.color("<gray>Click to toggle</gray>"))
                .build());

        inventory.setItem(22, new ItemBuilder(Material.PAPER)
                .name(ColorUtils.color("<gold>Set Interest Rate</gold>"))
                .lore(
                        ColorUtils.color("<gray>Current: " + rate + "%</gray>"),
                        ColorUtils.color("<gray>Click to change</gray>")
                )
                .build());

        inventory.setItem(24, new ItemBuilder(Material.CLOCK)
                .name(ColorUtils.color("<gold>Set Interval</gold>"))
                .lore(
                        ColorUtils.color("<gray>Current: " + interval + " min</gray>"),
                        ColorUtils.color("<gray>Click to change</gray>")
                )
                .build());
    }

    @Override
    public Inventory getInventory() { return inventory; }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getSlot();
        Player clicker = (Player) event.getWhoClicked();

        switch (currentPage) {
            case MAIN -> handleMainClick(slot, clicker);
            case BALANCE_EDIT -> handleBalanceEditClick(slot, event, clicker);
            case TRANSACTIONS -> handleTransactionsClick(slot, clicker);
            case LOTTERY -> handleLotteryClick(slot, clicker);
            case INTEREST -> handleInterestClick(slot, clicker);
        }
    }

    private void handleMainClick(int slot, Player clicker) {
        switch (slot) {
            case 20 -> {
                currentPage = AdminPage.BALANCE_EDIT;
                pageOffset = 0;
                populate();
            }
            case 22 -> {
                currentPage = AdminPage.TRANSACTIONS;
                pageOffset = 0;
                populate();
            }
            case 23 -> {
                clicker.closeInventory();
                clicker.openInventory(new TaxManagerGUI(plugin, clicker).getInventory());
            }
            case 24 -> {
                currentPage = AdminPage.INTEREST;
                pageOffset = 0;
                populate();
            }
            case 30 -> {
                currentPage = AdminPage.LOTTERY;
                pageOffset = 0;
                populate();
            }
            case 32 -> {
                clicker.closeInventory();
                clicker.openInventory(new ItemSellSetupGUI(plugin, clicker).getInventory());
            }
            case 52 -> {
                plugin.getEconomyManager().saveData();
                plugin.getConfigManager().reload();
                plugin.getEconomyManager().loadData();
                clicker.sendMessage(ColorUtils.color("<green>Configuration reloaded.</green>"));
                clicker.closeInventory();
            }
            case 53 -> {
                plugin.getEconomyManager().saveData();
                clicker.sendMessage(ColorUtils.color("<green>Data saved.</green>"));
            }
        }
    }

    private void handleBalanceEditClick(int slot, InventoryClickEvent event, Player clicker) {
        if (slot == 0) {
            currentPage = AdminPage.MAIN;
            populate();
            return;
        }

        if (slot == 45 && pageOffset > 0) {
            pageOffset--;
            populate();
            return;
        }
        if (slot == 53) {
            pageOffset++;
            populate();
            return;
        }

        UUID targetId = slotToPlayer.get(slot);
        if (targetId == null) return;

        String name = Bukkit.getOfflinePlayer(targetId).getName();
        if (name == null) name = "Unknown";
        final String finalName = name;

        EconomyManager economy = plugin.getEconomyManager();

        switch (event.getClick()) {
            case LEFT -> {
                economy.deposit(targetId, 100);
                clicker.sendMessage(ColorUtils.color("<green>Gave $100 to " + finalName + "</green>"));
                populate();
            }
            case RIGHT -> {
                economy.withdraw(targetId, 100);
                clicker.sendMessage(ColorUtils.color("<red>Took $100 from " + finalName + "</red>"));
                populate();
            }
            case SHIFT_LEFT -> {
                clicker.closeInventory();
                plugin.getChatInputManager().requestInput(clicker,
                    "<gold>Enter the new balance for " + finalName + ":</gold>",
                    amountStr -> {
                        try {
                            double amount = Double.parseDouble(amountStr);
                            economy.setBalance(targetId, amount);
                            clicker.sendMessage(ColorUtils.color("<green>Set " + finalName + "'s balance to " + economy.format(amount) + "</green>"));
                        } catch (NumberFormatException e) {
                            clicker.sendMessage(ColorUtils.color("<red>Invalid amount.</red>"));
                        }
                    });
            }
        }
    }

    private void handleTransactionsClick(int slot, Player clicker) {
        if (slot == 0) {
            currentPage = AdminPage.MAIN;
            populate();
            return;
        }

        if (slot == 45 && pageOffset > 0) {
            pageOffset--;
            populate();
            return;
        }
        if (slot == 53) {
            pageOffset++;
            populate();
        }
    }

    private void handleLotteryClick(int slot, Player clicker) {
        EconomyManager economy = plugin.getEconomyManager();
        switch (slot) {
            case 0 -> {
                currentPage = AdminPage.MAIN;
                pageOffset = 0;
                populate();
            }
            case 20 -> {
                boolean drawn = economy.drawLottery();
                if (drawn) {
                    clicker.sendMessage(ColorUtils.color("<green>Lottery drawn! Check who won.</green>"));
                } else {
                    clicker.sendMessage(ColorUtils.color("<red>Not enough lottery entries (need at least 2).</red>"));
                }
                populate();
            }
            case 22 -> {
                plugin.getChatInputManager().requestInput(clicker,
                    "<gold>Enter new lottery ticket price:</gold>",
                    input -> {
                        try {
                            double val = Double.parseDouble(input);
                            if (val <= 0) {
                                clicker.sendMessage(ColorUtils.color("<red>Must be positive.</red>"));
                                return;
                            }
                            plugin.getConfigManager().setLotteryTicketPrice(val);
                            clicker.sendMessage(ColorUtils.color("<green>Ticket price set to " + economy.format(val) + "</green>"));
                        } catch (NumberFormatException e) {
                            clicker.sendMessage(ColorUtils.color("<red>Invalid number.</red>"));
                        }
                    }, () -> clicker.openInventory(new AdminPanelGUI(plugin, clicker).getInventory()));
            }
            case 24 -> {
                economy.resetLottery();
                clicker.sendMessage(ColorUtils.color("<green>Lottery reset.</green>"));
                populate();
            }
        }
    }

    private void handleInterestClick(int slot, Player clicker) {
        switch (slot) {
            case 0 -> {
                currentPage = AdminPage.MAIN;
                pageOffset = 0;
                populate();
            }
            case 20 -> {
                boolean current = plugin.getConfigManager().isInterestEnabled();
                plugin.getConfigManager().setInterestEnabled(!current);
                clicker.sendMessage(ColorUtils.color("<green>Interest " + (!current ? "enabled" : "disabled") + ".</green>"));
                populate();
            }
            case 22 -> {
                plugin.getChatInputManager().requestInput(clicker,
                    "<gold>Enter new interest rate (current: " + plugin.getConfigManager().getInterestRate() + "%):</gold>",
                    input -> {
                        try {
                            double val = Double.parseDouble(input);
                            if (val < 0 || val > 100) {
                                clicker.sendMessage(ColorUtils.color("<red>Rate must be 0-100.</red>"));
                                return;
                            }
                            plugin.getConfigManager().setInterestRate(val);
                            clicker.sendMessage(ColorUtils.color("<green>Interest rate set to " + val + "%</green>"));
                        } catch (NumberFormatException e) {
                            clicker.sendMessage(ColorUtils.color("<red>Invalid number.</red>"));
                        }
                    }, () -> clicker.openInventory(new AdminPanelGUI(plugin, clicker).getInventory()));
            }
            case 24 -> {
                plugin.getChatInputManager().requestInput(clicker,
                    "<gold>Enter new interval in minutes (current: " + plugin.getConfigManager().getInterestInterval() + "):</gold>",
                    input -> {
                        try {
                            int val = Integer.parseInt(input);
                            if (val <= 0) {
                                clicker.sendMessage(ColorUtils.color("<red>Must be positive.</red>"));
                                return;
                            }
                            plugin.getConfigManager().setInterestInterval(val);
                            clicker.sendMessage(ColorUtils.color("<green>Interest interval set to " + val + " minutes.</green>"));
                        } catch (NumberFormatException e) {
                            clicker.sendMessage(ColorUtils.color("<red>Invalid number.</red>"));
                        }
                    }, () -> clicker.openInventory(new AdminPanelGUI(plugin, clicker).getInventory()));
            }
        }
    }
}
