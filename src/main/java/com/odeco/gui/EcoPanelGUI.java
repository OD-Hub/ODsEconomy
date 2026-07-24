package com.odeco.gui;

import com.odeco.ODEco;
import com.odeco.economy.EconomyManager;
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

import java.util.*;

public class EcoPanelGUI implements InventoryHolder {

    private final ODEco plugin;
    private final Player player;
    private final Inventory inventory;

    public EcoPanelGUI(ODEco plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 45, ColorUtils.color("<dark_blue>Eco Panel</dark_blue>"));
        populate();
    }

    private void populate() {
        inventory.clear();
        EconomyManager economy = plugin.getEconomyManager();
        double balance = economy.getBalance(player.getUniqueId());
        int position = economy.getBalancePosition(player.getUniqueId());

        // Row 1 — Header
        inventory.setItem(13, new ItemBuilder(Material.PLAYER_HEAD)
                .name(ColorUtils.color("<gold>Your Balance</gold>"))
                .skull(Heads.BALANCE)
                .lore(
                    ColorUtils.color("<green>" + economy.format(balance) + "</green>"),
                    ColorUtils.color("<gray>Rank: #" + (position > 0 ? position : "N/A") + "</gray>")
                )
                .build());

        // Row 2 — Financial tools (left) & Games (right)
        inventory.setItem(18, new ItemBuilder(Material.PLAYER_HEAD)
                .name(ColorUtils.color("<green>Pay</green>"))
                .skull(Heads.PAY)
                .lore(ColorUtils.color("<gray>Click to pay another player</gray>"))
                .build());

        inventory.setItem(19, new ItemBuilder(Material.PLAYER_HEAD)
                .name(ColorUtils.color("<gold>Banknotes</gold>"))
                .skull(Heads.BANKNOTE)
                .lore(ColorUtils.color("<gray>Create or redeem banknotes</gray>"))
                .build());

        if (plugin.getConfigManager().isSellEnabled()) {
            inventory.setItem(20, new ItemBuilder(Material.PLAYER_HEAD)
                    .name(ColorUtils.color("<gold>Sell</gold>"))
                    .skull(Heads.COIN_PILE_BRONZE)
                    .lore(ColorUtils.color("<gray>Sell the item in your hand</gray>"))
                    .build());
        }

        if (plugin.getConfigManager().isLotteryEnabled()) {
            int entries = economy.getLotteryEntryCount();
            inventory.setItem(24, new ItemBuilder(Material.PLAYER_HEAD)
                    .name(ColorUtils.color("<light_purple>Lottery</light_purple>"))
                    .skull(Heads.LOTTERY)
                    .lore(
                        entries > 0
                            ? ColorUtils.color("<gray>Pot: " + economy.format(economy.getLotteryPot()) + "</gray>")
                            : ColorUtils.color("<gray>No entries yet</gray>"),
                        ColorUtils.color("<gray>Entries: " + entries + "</gray>"),
                        ColorUtils.color("<gray>Click to buy a ticket</gray>")
                    )
                    .build());
        }

        if (plugin.getConfigManager().isDiceEnabled()) {
            inventory.setItem(25, new ItemBuilder(Material.PLAYER_HEAD)
                    .name(ColorUtils.color("<red>Dice Game</red>"))
                    .skull(Heads.DICE)
                    .lore(ColorUtils.color("<gray>Roll the dice for a chance to win!</gray>"))
                    .build());
        }

        if (plugin.getConfigManager().isAuctionsEnabled()) {
            inventory.setItem(26, new ItemBuilder(Material.PLAYER_HEAD)
                    .name(ColorUtils.color("<gold>Auction House</gold>"))
                    .skull(Heads.AUCTION)
                    .lore(ColorUtils.color("<gray>Browse, list, and buy items</gray>"))
                    .build());
        }

        // Row 3 — Marketplaces, info & taxes
        if (plugin.getConfigManager().isBountiesEnabled()) {
            inventory.setItem(28, new ItemBuilder(Material.PLAYER_HEAD)
                    .name(ColorUtils.color("<red>Bounties</red>"))
                    .skull(Heads.BOUNTY)
                    .lore(
                        ColorUtils.color("<gray>Active bounties: " + economy.getAllBounties().size() + "</gray>"),
                        ColorUtils.color("<gray>Click to manage</gray>")
                    )
                    .build());
        }

        if (plugin.getConfigManager().isSharedAccountsEnabled()) {
            inventory.setItem(29, new ItemBuilder(Material.PLAYER_HEAD)
                    .name(ColorUtils.color("<aqua>Shared Accounts</aqua>"))
                    .skull(Heads.SHARED_ACCOUNT)
                    .lore(ColorUtils.color("<gray>View and manage your shared accounts</gray>"))
                    .build());
        }

        if (plugin.getConfigManager().isDealershipsEnabled()) {
            inventory.setItem(30, new ItemBuilder(Material.PLAYER_HEAD)
                    .name(ColorUtils.color("<dark_aqua>Dealerships</dark_aqua>"))
                    .skull(Heads.VILLAGER_HOUSE)
                    .lore(ColorUtils.color("<gray>Browse and purchase from dealerships</gray>"))
                    .build());
        }

        inventory.setItem(31, new ItemBuilder(Material.PLAYER_HEAD)
                .name(ColorUtils.color("<blue>Transactions</blue>"))
                .skull(Heads.TRANSACTIONS)
                .lore(ColorUtils.color("<gray>View your recent transactions</gray>"))
                .build());

        inventory.setItem(32, new ItemBuilder(Material.PLAYER_HEAD)
                .name(ColorUtils.color("<gold>Balance Top</gold>"))
                .skull(Heads.BALTOP)
                .lore(ColorUtils.color("<gray>View the richest players</gray>"))
                .build());

        if (plugin.getConfigManager().isInterestEnabled()) {
            inventory.setItem(33, new ItemBuilder(Material.PLAYER_HEAD)
                    .name(ColorUtils.color("<green>Interest</green>"))
                    .skull(Heads.INTEREST)
                    .lore(ColorUtils.color("<gray>View interest information</gray>"))
                    .build());
        }

        // Tax button — conditional logic
        populateTaxButton();
    }

    private void populateTaxButton() {
        var config = plugin.getConfigManager();
        EconomyManager economy = plugin.getEconomyManager();

        if (!config.isTaxesEnabled()) return;
        if ("NONE".equals(config.getTaxMode())) return;

        String method = config.getTaxPaymentMethod();
        double debt = economy.getTaxDebt(player.getUniqueId());
        boolean graceActive = economy.isGracePeriodActive(player.getUniqueId());
        boolean showInfo = config.isShowInfoButton();

        boolean showManageTaxes = false;
        boolean showTaxInfo = false;

        if ("MANUAL".equals(method) && graceActive) {
            showManageTaxes = true;
        } else if (showInfo) {
            showTaxInfo = true;
        }

        if (showManageTaxes) {
            inventory.setItem(34, new ItemBuilder(Material.PLAYER_HEAD)
                    .name(ColorUtils.color("<red>Manage Taxes</red>"))
                    .skull(Heads.TAXES)
                    .lore(
                            ColorUtils.color("<gray>View and pay your taxes</gray>"),
                            debt > 0
                                ? ColorUtils.color("<red>Debt: " + economy.format(debt) + "</red>")
                                : ColorUtils.color("<green>No debt</green>")
                    )
                    .build());
        } else if (showTaxInfo) {
            inventory.setItem(34, new ItemBuilder(Material.PLAYER_HEAD)
                    .name(ColorUtils.color("<gray>Tax Info</gray>"))
                    .skull(Heads.TAXES)
                    .lore(
                            ColorUtils.color("<gray>View tax information</gray>"),
                            debt > 0
                                ? ColorUtils.color("<red>Debt: " + economy.format(debt) + "</red>")
                                : ColorUtils.color("<green>No debt</green>")
                    )
                    .build());
        }
        // else: slot 25 stays empty
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getSlot();
        Player clicker = (Player) event.getWhoClicked();

        switch (slot) {
            case 18 -> promptPay(clicker);
            case 19 -> promptBanknote(clicker);
            case 20 -> {
                clicker.closeInventory();
                sellHandItem(clicker);
            }
            case 24 -> {
                clicker.closeInventory();
                boolean bought = plugin.getEconomyManager().buyLotteryTicket(clicker.getUniqueId());
                if (bought) {
                    clicker.sendMessage(ColorUtils.color("<green>Bought a lottery ticket!</green>"));
                } else {
                    clicker.sendMessage(ColorUtils.color("<red>Could not buy ticket. Check your balance.</red>"));
                }
            }
            case 25 -> promptDice(clicker);
            case 26 -> {
                clicker.closeInventory();
                clicker.openInventory(new AuctionHouseGUI(plugin, clicker).getInventory());
            }
            case 28 -> {
                clicker.closeInventory();
                clicker.openInventory(new BountyGUI(plugin, clicker).getInventory());
            }
            case 29 -> openSharedAccounts(clicker);
            case 30 -> {
                if (plugin.getConfigManager().isDealershipsEnabled()) {
                    clicker.closeInventory();
                    clicker.openInventory(new DealershipGUI(plugin, clicker).getInventory());
                }
            }
            case 31 -> {
                clicker.closeInventory();
                clicker.openInventory(new TransactionGUI(plugin, clicker).getInventory());
            }
            case 32 -> showBaltop(clicker);
            case 33 -> showInterest(clicker);
            case 34 -> {
                clicker.closeInventory();
                clicker.openInventory(new TaxManagerGUI(plugin, clicker, true).getInventory());
            }
        }
    }

    private void promptPay(Player clicker) {
        plugin.getChatInputManager().requestInput(clicker,
            "<gold>Enter the player name to pay:</gold>",
            name -> {
                Player target = Bukkit.getPlayerExact(name);
                if (target == null) {
                    clicker.sendMessage(ColorUtils.color("<red>Player not found.</red>"));
                    return;
                }
                plugin.getChatInputManager().requestInput(clicker,
                    "<gold>Enter the amount to pay " + target.getName() + ":</gold>",
                    amountStr -> {
                        try {
                            double amount = Double.parseDouble(amountStr);
                            if (amount <= 0) {
                                clicker.sendMessage(ColorUtils.color("<red>Amount must be positive.</red>"));
                                return;
                            }
                            if (plugin.getEconomyManager().transfer(clicker.getUniqueId(), target.getUniqueId(), amount)) {
                                clicker.sendMessage(ColorUtils.color("<green>You paid " + plugin.getEconomyManager().format(amount) + " to " + target.getName() + "</green>"));
                                target.sendMessage(ColorUtils.color("<green>You received " + plugin.getEconomyManager().format(amount) + " from " + clicker.getName() + "</green>"));
                            } else {
                                clicker.sendMessage(ColorUtils.color("<red>Insufficient balance.</red>"));
                            }
                        } catch (NumberFormatException e) {
                            clicker.sendMessage(ColorUtils.color("<red>Invalid amount.</red>"));
                        }
                    });
            });
    }

    private void promptBanknote(Player clicker) {
        if (!plugin.getConfigManager().isBanknotesEnabled()) {
            clicker.sendMessage(ColorUtils.color("<red>Banknotes are disabled.</red>"));
            return;
        }
        plugin.getChatInputManager().requestInput(clicker,
            "<gold>Enter the value of the banknote:</gold>",
            amountStr -> {
                try {
                    double amount = Double.parseDouble(amountStr);
                    if (amount <= 0) {
                        clicker.sendMessage(ColorUtils.color("<red>Amount must be positive.</red>"));
                        return;
                    }
                    double fee = amount * (plugin.getConfigManager().getBanknoteFeePercent() / 100.0);
                    double total = amount + fee;
                    if (!plugin.getEconomyManager().hasBalance(clicker.getUniqueId(), total)) {
                        clicker.sendMessage(ColorUtils.color("<red>Insufficient balance (need " + plugin.getEconomyManager().format(total) + " including fee of " + plugin.getEconomyManager().format(fee) + ").</red>"));
                        return;
                    }
                    plugin.getEconomyManager().withdraw(clicker.getUniqueId(), total);
                    ItemStack banknote = plugin.getEconomyManager().createBanknote(amount);
                    if (banknote == null) {
                        plugin.getEconomyManager().deposit(clicker.getUniqueId(), total);
                        clicker.sendMessage(ColorUtils.color("<red>Could not create banknote.</red>"));
                        return;
                    }
                    clicker.getInventory().addItem(banknote);
                    clicker.sendMessage(ColorUtils.color("<green>Created banknote for " + plugin.getEconomyManager().format(amount) + "</green>"));
                } catch (NumberFormatException e) {
                    clicker.sendMessage(ColorUtils.color("<red>Invalid amount.</red>"));
                }
            });
    }

    private void promptDice(Player clicker) {
        if (!plugin.getConfigManager().isDiceEnabled()) {
            clicker.sendMessage(ColorUtils.color("<red>Dice is disabled.</red>"));
            return;
        }
        plugin.getChatInputManager().requestInput(clicker,
            "<gold>Enter your bet amount (max " + plugin.getEconomyManager().format(plugin.getConfigManager().getDiceMaxBet()) + "):</gold>",
            amountStr -> {
                try {
                    double amount = Double.parseDouble(amountStr);
                    if (amount <= 0) {
                        clicker.sendMessage(ColorUtils.color("<red>Amount must be positive.</red>"));
                        return;
                    }
                    if (amount > plugin.getConfigManager().getDiceMaxBet()) {
                        clicker.sendMessage(ColorUtils.color("<red>Maximum bet is " + plugin.getEconomyManager().format(plugin.getConfigManager().getDiceMaxBet()) + "</red>"));
                        return;
                    }
                    var game = plugin.getEconomyManager().playDice(clicker.getUniqueId(), amount);
                    if (game == null) {
                        clicker.sendMessage(ColorUtils.color("<red>Insufficient balance.</red>"));
                        return;
                    }
                    if (game.isWon()) {
                        clicker.sendMessage(ColorUtils.color("<green>You rolled a " + game.getRoll() + "! You won " + plugin.getEconomyManager().format(game.getPrize()) + "!</green>"));
                    } else {
                        clicker.sendMessage(ColorUtils.color("<red>You rolled a " + game.getRoll() + ". You lost " + plugin.getEconomyManager().format(game.getBet()) + ".</red>"));
                    }
                } catch (NumberFormatException e) {
                    clicker.sendMessage(ColorUtils.color("<red>Invalid amount.</red>"));
                }
            });
    }

    private void openSharedAccounts(Player clicker) {
        clicker.closeInventory();
        clicker.openInventory(new SharedAccountListGUI(plugin, clicker).getInventory());
    }

    private void showInterest(Player clicker) {
        if (!plugin.getConfigManager().isInterestEnabled()) {
            clicker.sendMessage(ColorUtils.color("<red>Interest is disabled.</red>"));
            return;
        }
        double balance = plugin.getEconomyManager().getBalance(clicker.getUniqueId());
        double rate = plugin.getConfigManager().getInterestRate();
        int interval = plugin.getConfigManager().getInterestInterval();
        clicker.sendMessage(ColorUtils.color("<gold>Interest Information:</gold>"));
        clicker.sendMessage(ColorUtils.color("<gray>Rate: " + rate + "% per " + interval + " minute(s)</gray>"));
        clicker.sendMessage(ColorUtils.color("<gray>Current Balance: " + plugin.getEconomyManager().format(balance) + "</gray>"));
        clicker.sendMessage(ColorUtils.color("<gray>Next Interest: ~" + plugin.getEconomyManager().format(balance * rate / 100.0) + "</gray>"));
    }

    private void showBaltop(Player clicker) {
        var top = plugin.getEconomyManager().getTopBalances();
        if (top.isEmpty()) {
            clicker.sendMessage(ColorUtils.color("<gray>No balance data yet.</gray>"));
            return;
        }
        clicker.sendMessage(ColorUtils.color("<gold>══ Balance Top ══</gold>"));
        for (int i = 0; i < top.size(); i++) {
            var entry = top.get(i);
            String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            if (name == null) name = "Unknown";
            clicker.sendMessage(ColorUtils.color("<gray>" + (i + 1) + ". " + name + " - " + plugin.getEconomyManager().format(entry.getValue()) + "</gray>"));
        }
    }

    private void sellHandItem(Player clicker) {
        ItemStack held = clicker.getInventory().getItemInMainHand();
        if (held == null || held.getType() == Material.AIR) {
            clicker.sendMessage(ColorUtils.color("<red>You are not holding any item.</red>"));
            clicker.openInventory(new EcoPanelGUI(plugin, clicker).getInventory());
            return;
        }
        double worth = plugin.getConfigManager().getWorth(held.getType().name());
        if (worth <= 0) {
            clicker.sendMessage(ColorUtils.color("<red>This item has no worth.</red>"));
            clicker.openInventory(new EcoPanelGUI(plugin, clicker).getInventory());
            return;
        }
        double multiplier = plugin.getConfigManager().getSellMultiplier();
        double total = worth * held.getAmount() * multiplier;
        plugin.getEconomyManager().deposit(clicker.getUniqueId(), total);
        plugin.getEconomyManager().logTransaction(clicker.getUniqueId(), "sell", total, "Sold " + held.getType().name() + " x" + held.getAmount());
        held.setAmount(0);
        clicker.sendMessage(ColorUtils.color("<green>Sold for " + plugin.getEconomyManager().format(total) + "</green>"));
        clicker.openInventory(new EcoPanelGUI(plugin, clicker).getInventory());
    }
}
