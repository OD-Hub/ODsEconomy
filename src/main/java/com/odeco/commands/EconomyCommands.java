package com.odeco.commands;

import com.odeco.ODEco;
import com.odeco.economy.EconomyManager;
import com.odeco.economy.PendingOrder;
import com.odeco.gui.AdminPanelGUI;
import com.odeco.gui.DealershipGUI;
import com.odeco.gui.DealershipSetupGUI;
import com.odeco.gui.EcoPanelGUI;
import com.odeco.gui.SharedAccountListGUI;
import com.odeco.gui.TaxManagerGUI;
import com.odeco.gui.TaxSetupWizardGUI;
import com.odeco.utils.ColorUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class EconomyCommands implements CommandExecutor, TabCompleter {

    private final ODEco plugin;

    public EconomyCommands(ODEco plugin) {
        this.plugin = plugin;
    }

    private EconomyManager economy() { return plugin.getEconomyManager(); }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (command.getName().toLowerCase()) {
            case "eco" -> handleEco(sender, args);
            case "pay" -> handlePay(sender, args);
            case "sell" -> handleSell(sender, args);
            case "worth" -> handleWorth(sender, args);
            case "baltop" -> handleBaltop(sender, args);
            case "bounty" -> handleBounty(sender, args);
            case "lottery" -> handleLottery(sender, args);
            case "dice" -> handleDice(sender, args);
            case "banknote" -> handleBanknote(sender, args);
            case "auction" -> handleAuction(sender, args);
            case "interest" -> handleInterest(sender, args);
            case "ecoadmin" -> handleEcoAdmin(sender, args);
            case "taxmanager" -> handleTaxManager(sender, args);
            case "taxsetup" -> handleTaxSetup(sender, args);
            case "dealership" -> handleDealership(sender, args);
            case "dealershipsetup" -> handleDealershipSetup(sender, args);
            default -> sender.sendMessage(ColorUtils.color("<red>Unknown command.</red>"));
        }
        return true;
    }

    // ═══════════════════════════════════════════
    //  /taxmanager
    // ═══════════════════════════════════════════

    private void handleTaxManager(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtils.color("<red>Only players can use this command.</red>"));
            return;
        }

        if (args.length == 0) {
            player.openInventory(new TaxManagerGUI(plugin, player).getInventory());
            return;
        }

        switch (args[0].toLowerCase()) {
            case "pay" -> {
                if (args.length < 2) {
                    player.sendMessage(ColorUtils.color("<red>Usage: /taxmanager pay <amount|all></red>"));
                    return;
                }
                EconomyManager economy = economy();
                double debt = economy.getTaxDebt(player.getUniqueId());
                if (debt <= 0) {
                    player.sendMessage(ColorUtils.color("<green>You have no tax debt!</green>"));
                    return;
                }

                double amount;
                if ("all".equalsIgnoreCase(args[1])) {
                    amount = debt;
                } else {
                    try {
                        amount = Double.parseDouble(args[1]);
                    } catch (NumberFormatException e) {
                        player.sendMessage(ColorUtils.color("<red>Invalid amount.</red>"));
                        return;
                    }
                }

                if (amount <= 0) {
                    player.sendMessage(ColorUtils.color("<red>Amount must be positive.</red>"));
                    return;
                }

                if (economy.payTaxDebt(player.getUniqueId(), amount)) {
                    double remaining = economy.getTaxDebt(player.getUniqueId());
                    player.sendMessage(ColorUtils.color("<green>Paid " + economy.format(Math.min(amount, debt)) + " in taxes.</green>"));
                    if (remaining > 0) {
                        player.sendMessage(ColorUtils.color("<yellow>Remaining debt: " + economy.format(remaining) + "</yellow>"));
                    }
                } else {
                    player.sendMessage(ColorUtils.color("<red>Could not pay tax. Insufficient balance.</red>"));
                }
            }
            case "pay-shared" -> {
                if (args.length < 2) {
                    player.sendMessage(ColorUtils.color("<red>Usage: /taxmanager pay-shared <amount|all></red>"));
                    return;
                }
                EconomyManager economy = economy();
                double debt = economy.getTaxDebt(player.getUniqueId());
                if (debt <= 0) {
                    player.sendMessage(ColorUtils.color("<green>You have no tax debt!</green>"));
                    return;
                }

                double amount;
                if ("all".equalsIgnoreCase(args[1])) {
                    amount = debt;
                } else {
                    try {
                        amount = Double.parseDouble(args[1]);
                    } catch (NumberFormatException e) {
                        player.sendMessage(ColorUtils.color("<red>Invalid amount.</red>"));
                        return;
                    }
                }

                if (amount <= 0) {
                    player.sendMessage(ColorUtils.color("<red>Amount must be positive.</red>"));
                    return;
                }

                if (economy.payTaxDebtToSharedAccount(player.getUniqueId(), amount)) {
                    double remaining = economy.getTaxDebt(player.getUniqueId());
                    player.sendMessage(ColorUtils.color("<green>Paid " + economy.format(Math.min(amount, debt)) + " taxes to shared account.</green>"));
                    if (remaining > 0) {
                        player.sendMessage(ColorUtils.color("<yellow>Remaining debt: " + economy.format(remaining) + "</yellow>"));
                    }
                } else {
                    player.sendMessage(ColorUtils.color("<red>Could not pay tax. Insufficient balance.</red>"));
                }
            }
            case "check" -> {
                double debt = economy().getTaxDebt(player.getUniqueId());
                if (debt <= 0) {
                    player.sendMessage(ColorUtils.color("<green>You have no tax debt.</green>"));
                } else {
                    player.sendMessage(ColorUtils.color("<red>You owe " + economy().format(debt) + " in taxes.</red>"));
                    player.sendMessage(ColorUtils.color("<gray>Use /taxmanager pay <amount> to pay.</gray>"));
                }
            }
            case "clear" -> {
                if (!sender.hasPermission("odeco.admin")) {
                    sender.sendMessage(ColorUtils.color("<red>You don't have permission.</red>"));
                    return;
                }
                if (args.length < 2) {
                    sender.sendMessage(ColorUtils.color("<red>Usage: /taxmanager clear <player></red>"));
                    return;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(args[1]);
                if (target == null) {
                    sender.sendMessage(ColorUtils.color("<red>Player not found.</red>"));
                    return;
                }
                economy().clearPlayerTaxDebt(target.getUniqueId());
                sender.sendMessage(ColorUtils.color("<green>Cleared tax debt for " + target.getName() + "</green>"));
            }
            case "assess" -> {
                if (!sender.hasPermission("odeco.admin")) {
                    sender.sendMessage(ColorUtils.color("<red>You don't have permission.</red>"));
                    return;
                }
                economy().assessTaxes();
                sender.sendMessage(ColorUtils.color("<green>Taxes assessed for all eligible players.</green>"));
            }
            default -> sender.sendMessage(ColorUtils.color("<red>Usage: /taxmanager [pay|pay-shared|check|clear|assess]</red>"));
        }
    }

    // ═══════════════════════════════════════════
    //  /taxsetup
    // ═══════════════════════════════════════════

    private void handleTaxSetup(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtils.color("<red>Only players can use this command.</red>"));
            return;
        }
        if (!sender.hasPermission("odeco.admin")) {
            sender.sendMessage(ColorUtils.color("<red>You don't have permission.</red>"));
            return;
        }
        player.openInventory(new TaxSetupWizardGUI(plugin, player).getInventory());
    }

    // ═══════════════════════════════════════════
    //  /dealership
    // ═══════════════════════════════════════════

    private void handleDealership(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtils.color("<red>Only players can use this command.</red>"));
            return;
        }
        if (!plugin.getConfigManager().isDealershipsEnabled()) {
            player.sendMessage(ColorUtils.color("<red>Dealerships are disabled.</red>"));
            return;
        }

        if (args.length == 0) {
            player.openInventory(new DealershipGUI(plugin, player).getInventory());
            return;
        }

        String dealershipName = String.join(" ", args);
        var dealership = plugin.getDealershipManager().getDealership(dealershipName);
        if (dealership == null) {
            player.sendMessage(ColorUtils.color("<red>Dealership '" + dealershipName + "' not found.</red>"));
            return;
        }
        if (!dealership.canAccess(player.getUniqueId())) {
            player.sendMessage(ColorUtils.color("<red>You don't have access to this dealership.</red>"));
            return;
        }
        player.openInventory(new DealershipGUI(plugin, player, dealershipName).getInventory());
    }

    // ═══════════════════════════════════════════
    //  /dealershipsetup
    // ═══════════════════════════════════════════

    private void handleDealershipSetup(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtils.color("<red>Only players can use this command.</red>"));
            return;
        }
        if (!sender.hasPermission("odeco.admin")) {
            sender.sendMessage(ColorUtils.color("<red>You don't have permission.</red>"));
            return;
        }
        player.openInventory(new DealershipSetupGUI(plugin, player).getInventory());
    }

    // ═══════════════════════════════════════════
    //  /eco
    // ═══════════════════════════════════════════

    private void handleEco(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtils.color("<red>Only players can use this command.</red>"));
            return;
        }

        if (args.length == 0) {
            player.openInventory(new EcoPanelGUI(plugin, player).getInventory());
            return;
        }

        switch (args[0].toLowerCase()) {
            case "shared" -> handleEcoShared(player, args);
            case "transactions" -> handleEcoTransactions(player, args);
            case "balance" -> {
                if (args.length >= 2) {
                    OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(args[1]);
                    if (target != null) {
                        player.sendMessage(ColorUtils.color("<gold>" + target.getName() + "'s balance: " + economy().format(economy().getBalance(target.getUniqueId())) + "</gold>"));
                    } else {
                        player.sendMessage(ColorUtils.color("<red>Player not found.</red>"));
                    }
                } else {
                    player.sendMessage(ColorUtils.color("<gold>Your balance: " + economy().format(economy().getBalance(player.getUniqueId())) + "</gold>"));
                }
            }
            default -> player.sendMessage(ColorUtils.color("<red>Usage: /eco [shared|transactions|balance]</red>"));
        }
    }

    private void handleEcoShared(Player player, String[] args) {
        if (!plugin.getConfigManager().isSharedAccountsEnabled()) {
            player.sendMessage(ColorUtils.color("<red>Shared accounts are disabled.</red>"));
            return;
        }
        player.openInventory(new SharedAccountListGUI(plugin, player).getInventory());
    }

    private void handleEcoTransactions(Player player, String[] args) {
        var transactions = economy().getTransactionLog(player.getUniqueId());
        if (transactions.isEmpty()) {
            player.sendMessage(ColorUtils.color("<gray>No transactions found.</gray>"));
            return;
        }
        player.sendMessage(ColorUtils.color("<gold>Recent Transactions:</gold>"));
        for (var entry : transactions) {
            String date = new java.text.SimpleDateFormat("MMM dd HH:mm").format(new java.util.Date(entry.timestamp()));
            player.sendMessage(ColorUtils.color(" <gray>" + date + " | " + entry.type() + " | " + economy().format(entry.amount()) + " | " + entry.details() + "</gray>"));
        }
    }

    // ═══════════════════════════════════════════
    //  /pay
    // ═══════════════════════════════════════════

    private void handlePay(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtils.color("<red>Only players can use this command.</red>"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(ColorUtils.color("<red>Usage: /pay <player> <amount></red>"));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage(ColorUtils.color("<red>Player not found.</red>"));
            return;
        }
        if (target.equals(player)) {
            player.sendMessage(ColorUtils.color("<red>You cannot pay yourself.</red>"));
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(ColorUtils.color("<red>Invalid amount.</red>"));
            return;
        }
        if (amount <= 0) {
            player.sendMessage(ColorUtils.color("<red>Amount must be positive.</red>"));
            return;
        }

        if (economy().transfer(player.getUniqueId(), target.getUniqueId(), amount)) {
            player.sendMessage(ColorUtils.color("<green>You paid " + economy().format(amount) + " to " + target.getName() + "</green>"));
            target.sendMessage(ColorUtils.color("<green>You received " + economy().format(amount) + " from " + player.getName() + "</green>"));
        } else {
            player.sendMessage(ColorUtils.color("<red>Insufficient balance.</red>"));
        }
    }

    // ═══════════════════════════════════════════
    //  /sell
    // ═══════════════════════════════════════════

    private void handleSell(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtils.color("<red>Only players can use this command.</red>"));
            return;
        }
        if (!plugin.getConfigManager().isSellEnabled()) {
            player.sendMessage(ColorUtils.color("<red>Selling is disabled.</red>"));
            return;
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType() == Material.AIR) {
            player.sendMessage(ColorUtils.color("<red>You are not holding any item.</red>"));
            return;
        }

        double worth = plugin.getConfigManager().getWorth(held.getType().name());
        if (worth <= 0) {
            player.sendMessage(ColorUtils.color("<red>This item has no worth.</red>"));
            return;
        }

        double multiplier = plugin.getConfigManager().getSellMultiplier();
        double total = worth * held.getAmount() * multiplier;
        economy().deposit(player.getUniqueId(), total);
        String itemName = held.getType().name();
        int itemAmount = held.getAmount();
        held.setAmount(0);
        player.sendMessage(ColorUtils.color("<green>Sold " + itemName + " x" + itemAmount + " for " + economy().format(total) + "</green>"));
    }

    // ═══════════════════════════════════════════
    //  /worth
    // ═══════════════════════════════════════════

    private void handleWorth(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtils.color("<red>Only players can use this command.</red>"));
            return;
        }

        Material material;
        if (args.length > 0) {
            material = Material.getMaterial(args[0].toUpperCase());
            if (material == null) {
                player.sendMessage(ColorUtils.color("<red>Invalid item name.</red>"));
                return;
            }
        } else {
            ItemStack held = player.getInventory().getItemInMainHand();
            if (held == null || held.getType() == Material.AIR) {
                player.sendMessage(ColorUtils.color("<red>You are not holding any item.</red>"));
                return;
            }
            material = held.getType();
        }

        double worth = plugin.getConfigManager().getWorth(material.name());
        if (worth <= 0) {
            player.sendMessage(ColorUtils.color("<red>" + material.name() + " has no worth.</red>"));
        } else {
            player.sendMessage(ColorUtils.color("<gold>" + material.name() + " is worth " + economy().format(worth) + " each.</gold>"));
        }
    }

    // ═══════════════════════════════════════════
    //  /baltop
    // ═══════════════════════════════════════════

    private void handleBaltop(CommandSender sender, String[] args) {
        int page = 1;
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        }

        var top = economy().getTopBalances();
        if (top.isEmpty()) {
            sender.sendMessage(ColorUtils.color("<gray>No balance data yet.</gray>"));
            return;
        }

        int start = (page - 1) * 10;
        if (start >= top.size()) {
            sender.sendMessage(ColorUtils.color("<red>Page not found.</red>"));
            return;
        }

        sender.sendMessage(ColorUtils.color("<gold>══ Balance Top (Page " + page + ") ══</gold>"));
        for (int i = start; i < Math.min(start + 10, top.size()); i++) {
            var entry = top.get(i);
            String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            if (name == null) name = "Unknown";
            sender.sendMessage(ColorUtils.color("<gray>" + (i + 1) + ". " + name + " - " + economy().format(entry.getValue()) + "</gray>"));
        }
    }

    // ═══════════════════════════════════════════
    //  /bounty
    // ═══════════════════════════════════════════

    private void handleBounty(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtils.color("<red>Only players can use this command.</red>"));
            return;
        }
        if (!plugin.getConfigManager().isBountiesEnabled()) {
            player.sendMessage(ColorUtils.color("<red>Bounties are disabled.</red>"));
            return;
        }

        if (args.length == 0) {
            var bounties = economy().getAllBounties();
            if (bounties.isEmpty()) {
                player.sendMessage(ColorUtils.color("<gray>No active bounties.</gray>"));
                return;
            }
            player.sendMessage(ColorUtils.color("<gold>Active Bounties:</gold>"));
            for (var entry : bounties.entrySet()) {
                String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                player.sendMessage(ColorUtils.color(" <gray>" + (name != null ? name : "Unknown") + " - " + economy().format(entry.getValue()) + "</gray>"));
            }
            return;
        }

        switch (args[0].toLowerCase()) {
            case "set" -> {
                if (args.length < 3) {
                    player.sendMessage(ColorUtils.color("<red>Usage: /bounty set <player> <amount> [anonymous]</red>"));
                    return;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    player.sendMessage(ColorUtils.color("<red>Player not found.</red>"));
                    return;
                }
                double amount;
                try {
                    amount = Double.parseDouble(args[2]);
                } catch (NumberFormatException e) {
                    player.sendMessage(ColorUtils.color("<red>Invalid amount.</red>"));
                    return;
                }
                boolean anonymous = args.length >= 4 && "anonymous".equalsIgnoreCase(args[3]);
                if (economy().setBounty(target.getUniqueId(), player.getUniqueId(), amount, anonymous)) {
                    player.sendMessage(ColorUtils.color("<green>Bounty of " + economy().format(amount) + " placed on " + target.getName() + "!</green>"));
                    if (!anonymous) {
                        target.sendMessage(ColorUtils.color("<red>A bounty of " + economy().format(amount) + " has been placed on you by " + player.getName() + "!</red>"));
                    }
                } else {
                    player.sendMessage(ColorUtils.color("<red>Could not place bounty. Check your balance and the minimum amount.</red>"));
                }
            }
            case "check" -> {
                if (args.length < 2) {
                    player.sendMessage(ColorUtils.color("<red>Usage: /bounty check <player></red>"));
                    return;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    player.sendMessage(ColorUtils.color("<red>Player not found.</red>"));
                    return;
                }
                double bounty = economy().getBounty(target.getUniqueId());
                if (bounty > 0) {
                    player.sendMessage(ColorUtils.color("<gold>" + target.getName() + " has a bounty of " + economy().format(bounty) + "</gold>"));
                } else {
                    player.sendMessage(ColorUtils.color("<gray>" + target.getName() + " has no bounty.</gray>"));
                }
            }
            default -> player.sendMessage(ColorUtils.color("<red>Usage: /bounty <set|check|list></red>"));
        }
    }

    // ═══════════════════════════════════════════
    //  /lottery
    // ═══════════════════════════════════════════

    private void handleLottery(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtils.color("<red>Only players can use this command.</red>"));
            return;
        }
        if (!plugin.getConfigManager().isLotteryEnabled()) {
            player.sendMessage(ColorUtils.color("<red>Lottery is disabled.</red>"));
            return;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("buy")) {
            if (economy().buyLotteryTicket(player.getUniqueId())) {
                player.sendMessage(ColorUtils.color("<green>You bought a lottery ticket for " + economy().format(plugin.getConfigManager().getLotteryTicketPrice()) + "!</green>"));
            } else {
                player.sendMessage(ColorUtils.color("<red>Could not buy a ticket. Check your balance.</red>"));
            }
            return;
        }

        player.sendMessage(ColorUtils.color("<gold>══ Lottery ══</gold>"));
        player.sendMessage(ColorUtils.color("<gray>Pot: " + economy().format(economy().getLotteryPot()) + "</gray>"));
        player.sendMessage(ColorUtils.color("<gray>Entries: " + economy().getLotteryEntryCount() + "</gray>"));
        player.sendMessage(ColorUtils.color("<gray>Ticket Price: " + economy().format(plugin.getConfigManager().getLotteryTicketPrice()) + "</gray>"));
        if (economy().getLastLotteryWinner() != null) {
            String winnerName = Bukkit.getOfflinePlayer(economy().getLastLotteryWinner()).getName();
            player.sendMessage(ColorUtils.color("<gray>Last Winner: " + (winnerName != null ? winnerName : "Unknown") + " (" + economy().format(economy().getLastLotteryPrize()) + ")</gray>"));
        }
        player.sendMessage(ColorUtils.color("<green>/lottery buy</green> <gray>- Buy a ticket</gray>"));
    }

    // ═══════════════════════════════════════════
    //  /dice
    // ═══════════════════════════════════════════

    private void handleDice(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtils.color("<red>Only players can use this command.</red>"));
            return;
        }
        if (!plugin.getConfigManager().isDiceEnabled()) {
            player.sendMessage(ColorUtils.color("<red>Dice is disabled.</red>"));
            return;
        }

        if (args.length < 1) {
            player.sendMessage(ColorUtils.color("<red>Usage: /dice <amount></red>"));
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage(ColorUtils.color("<red>Invalid amount.</red>"));
            return;
        }

        double maxBet = plugin.getConfigManager().getDiceMaxBet();
        if (amount > maxBet) {
            player.sendMessage(ColorUtils.color("<red>Maximum bet is " + economy().format(maxBet) + "</red>"));
            return;
        }

        var game = economy().playDice(player.getUniqueId(), amount);
        if (game == null) {
            player.sendMessage(ColorUtils.color("<red>Could not play. Check your balance.</red>"));
            return;
        }

        if (game.isWon()) {
            player.sendMessage(ColorUtils.color("<green>You rolled a " + game.getRoll() + "! You won " + economy().format(game.getPrize()) + "!</green>"));
        } else {
            player.sendMessage(ColorUtils.color("<red>You rolled a " + game.getRoll() + ". You lost " + economy().format(game.getBet()) + ".</red>"));
        }
    }

    // ═══════════════════════════════════════════
    //  /banknote
    // ═══════════════════════════════════════════

    private void handleBanknote(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtils.color("<red>Only players can use this command.</red>"));
            return;
        }
        if (!plugin.getConfigManager().isBanknotesEnabled()) {
            player.sendMessage(ColorUtils.color("<red>Banknotes are disabled.</red>"));
            return;
        }

        if (args.length < 1) {
            player.sendMessage(ColorUtils.color("<red>Usage: /banknote <amount></red>"));
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage(ColorUtils.color("<red>Invalid amount.</red>"));
            return;
        }
        if (amount <= 0) {
            player.sendMessage(ColorUtils.color("<red>Amount must be positive.</red>"));
            return;
        }

        double fee = amount * (plugin.getConfigManager().getBanknoteFeePercent() / 100.0);
        double total = amount + fee;
        if (!economy().hasBalance(player.getUniqueId(), total)) {
            player.sendMessage(ColorUtils.color("<red>Insufficient balance (need " + economy().format(total) + " including fee of " + economy().format(fee) + ").</red>"));
            return;
        }

        economy().withdraw(player.getUniqueId(), total);

        ItemStack banknote = economy().createBanknote(amount);
        if (banknote == null) {
            economy().deposit(player.getUniqueId(), total);
            player.sendMessage(ColorUtils.color("<red>Could not create banknote.</red>"));
            return;
        }

        economy().logTransaction(player.getUniqueId(), "banknote_withdraw", amount, "Created banknote for " + economy().format(amount));
        player.getInventory().addItem(banknote);
        player.sendMessage(ColorUtils.color("<green>Created banknote for " + economy().format(amount) + "</green>"));
    }

    // ═══════════════════════════════════════════
    //  /auction
    // ═══════════════════════════════════════════

    private void handleAuction(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtils.color("<red>Only players can use this command.</red>"));
            return;
        }
        if (!plugin.getConfigManager().isAuctionsEnabled()) {
            player.sendMessage(ColorUtils.color("<red>Auctions are disabled.</red>"));
            return;
        }

        if (args.length == 0) {
            var auctions = economy().getActiveAuctions();
            if (auctions.isEmpty()) {
                player.sendMessage(ColorUtils.color("<gray>No active auctions.</gray>"));
            } else {
                player.sendMessage(ColorUtils.color("<gold>Active Auctions:</gold>"));
                for (int i = 0; i < auctions.size(); i++) {
                    PendingOrder order = auctions.get(i);
                    player.sendMessage(ColorUtils.color(" <gray>" + (i + 1) + ". " + order.getItem().getType().name() + " x" + order.getItem().getAmount() + " - " + economy().format(order.getPrice()) + " (by " + order.getSellerName() + ")</gray>"));
                }
            }
            player.sendMessage(ColorUtils.color("<gray>Use /auction sell <price> to list an item</gray>"));
            player.sendMessage(ColorUtils.color("<gray>Use /auction buy <id> to buy an item</gray>"));
            return;
        }

        switch (args[0].toLowerCase()) {
            case "sell" -> {
                if (args.length < 2) {
                    player.sendMessage(ColorUtils.color("<red>Usage: /auction sell <price></red>"));
                    return;
                }
                ItemStack held = player.getInventory().getItemInMainHand();
                if (held == null || held.getType() == Material.AIR) {
                    player.sendMessage(ColorUtils.color("<red>You are not holding any item.</red>"));
                    return;
                }
                double price;
                try {
                    price = Double.parseDouble(args[1]);
                } catch (NumberFormatException e) {
                    player.sendMessage(ColorUtils.color("<red>Invalid price.</red>"));
                    return;
                }
                if (price <= 0) {
                    player.sendMessage(ColorUtils.color("<red>Price must be positive.</red>"));
                    return;
                }
                if (economy().createAuction(player.getUniqueId(), player.getName(), held.clone(), price)) {
                    String itemName = held.getType().name();
                    int itemAmount = held.getAmount();
                    held.setAmount(0);
                    player.sendMessage(ColorUtils.color("<green>Listed " + itemName + " x" + itemAmount + " for " + economy().format(price) + "</green>"));
                } else {
                    player.sendMessage(ColorUtils.color("<red>Could not list item. Check your balance for listing fee.</red>"));
                }
            }
            case "buy" -> {
                if (args.length < 2) {
                    player.sendMessage(ColorUtils.color("<red>Usage: /auction buy <id></red>"));
                    return;
                }
                try {
                    int id = Integer.parseInt(args[1]) - 1;
                    var auctions = economy().getActiveAuctions();
                    if (id < 0 || id >= auctions.size()) {
                        player.sendMessage(ColorUtils.color("<red>Invalid auction ID.</red>"));
                        return;
                    }
                    PendingOrder order = auctions.get(id);
                    if (economy().buyAuction(order, player.getUniqueId())) {
                        player.getInventory().addItem(order.getItem());
                        player.sendMessage(ColorUtils.color("<green>Bought " + order.getItem().getType().name() + " for " + economy().format(order.getPrice()) + "</green>"));
                    } else {
                        player.sendMessage(ColorUtils.color("<red>Could not buy item. Check your balance.</red>"));
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(ColorUtils.color("<red>Invalid ID.</red>"));
                }
            }
            case "cancel" -> {
                if (args.length < 2) {
                    player.sendMessage(ColorUtils.color("<red>Usage: /auction cancel <id></red>"));
                    return;
                }
                try {
                    int id = Integer.parseInt(args[1]) - 1;
                    var auctions = economy().getActiveAuctions();
                    if (id < 0 || id >= auctions.size()) {
                        player.sendMessage(ColorUtils.color("<red>Invalid auction ID.</red>"));
                        return;
                    }
                    PendingOrder order = auctions.get(id);
                    if (!order.getSellerId().equals(player.getUniqueId()) && !player.hasPermission("odeco.admin")) {
                        player.sendMessage(ColorUtils.color("<red>This is not your auction.</red>"));
                        return;
                    }
                    if (economy().cancelAuction(order)) {
                        player.getInventory().addItem(order.getItem());
                        player.sendMessage(ColorUtils.color("<green>Auction cancelled. Item returned.</green>"));
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(ColorUtils.color("<red>Invalid ID.</red>"));
                }
            }
            default -> player.sendMessage(ColorUtils.color("<red>Usage: /auction <sell|buy|cancel></red>"));
        }
    }

    // ═══════════════════════════════════════════
    //  /interest
    // ═══════════════════════════════════════════

    private void handleInterest(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtils.color("<red>Only players can use this command.</red>"));
            return;
        }
        if (!plugin.getConfigManager().isInterestEnabled()) {
            player.sendMessage(ColorUtils.color("<red>Interest is disabled.</red>"));
            return;
        }

        double balance = economy().getBalance(player.getUniqueId());
        double rate = plugin.getConfigManager().getInterestRate();
        int interval = plugin.getConfigManager().getInterestInterval();

        player.sendMessage(ColorUtils.color("<gold>Interest Information:</gold>"));
        player.sendMessage(ColorUtils.color("<gray>Rate: " + rate + "% per " + interval + " minute(s)</gray>"));
        player.sendMessage(ColorUtils.color("<gray>Current Balance: " + economy().format(balance) + "</gray>"));
        player.sendMessage(ColorUtils.color("<gray>Next Interest: ~" + economy().format(balance * rate / 100.0) + "</gray>"));
    }

    // ═══════════════════════════════════════════
    //  /ecoadmin
    // ═══════════════════════════════════════════

    private void handleEcoAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("odeco.admin")) {
            sender.sendMessage(ColorUtils.color("<red>You don't have permission.</red>"));
            return;
        }

        if (args.length == 0) {
            if (sender instanceof Player player) {
                player.openInventory(new AdminPanelGUI(plugin, player).getInventory());
            } else {
                sender.sendMessage(ColorUtils.color("<red>Usage: /ecoadmin <give|take|set|reload></red>"));
            }
            return;
        }

        switch (args[0].toLowerCase()) {
            case "give" -> {
                if (args.length < 3) {
                    sender.sendMessage(ColorUtils.color("<red>Usage: /ecoadmin give <player> <amount></red>"));
                    return;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(args[1]);
                if (target == null) {
                    sender.sendMessage(ColorUtils.color("<red>Player not found.</red>"));
                    return;
                }
                double amount;
                try {
                    amount = Double.parseDouble(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(ColorUtils.color("<red>Invalid amount.</red>"));
                    return;
                }
                economy().deposit(target.getUniqueId(), amount);
                sender.sendMessage(ColorUtils.color("<green>Gave " + economy().format(amount) + " to " + target.getName() + "</green>"));
            }
            case "take" -> {
                if (args.length < 3) {
                    sender.sendMessage(ColorUtils.color("<red>Usage: /ecoadmin take <player> <amount></red>"));
                    return;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(args[1]);
                if (target == null) {
                    sender.sendMessage(ColorUtils.color("<red>Player not found.</red>"));
                    return;
                }
                double amount;
                try {
                    amount = Double.parseDouble(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(ColorUtils.color("<red>Invalid amount.</red>"));
                    return;
                }
                if (economy().withdraw(target.getUniqueId(), amount)) {
                    sender.sendMessage(ColorUtils.color("<green>Took " + economy().format(amount) + " from " + target.getName() + "</green>"));
                } else {
                    sender.sendMessage(ColorUtils.color("<red>" + target.getName() + " doesn't have enough money.</red>"));
                }
            }
            case "set" -> {
                if (args.length < 3) {
                    sender.sendMessage(ColorUtils.color("<red>Usage: /ecoadmin set <player> <amount></red>"));
                    return;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(args[1]);
                if (target == null) {
                    sender.sendMessage(ColorUtils.color("<red>Player not found.</red>"));
                    return;
                }
                double amount;
                try {
                    amount = Double.parseDouble(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(ColorUtils.color("<red>Invalid amount.</red>"));
                    return;
                }
                economy().setBalance(target.getUniqueId(), amount);
                sender.sendMessage(ColorUtils.color("<green>Set " + target.getName() + "'s balance to " + economy().format(amount) + "</green>"));
            }
            case "reload" -> {
                plugin.getConfigManager().reload();
                plugin.getEconomyManager().loadData();
                sender.sendMessage(ColorUtils.color("<green>Configuration and data reloaded.</green>"));
            }
            default -> sender.sendMessage(ColorUtils.color("<red>Usage: /ecoadmin <give|take|set|reload></red>"));
        }
    }

    // ═══════════════════════════════════════════
    //  Tab Completion
    // ═══════════════════════════════════════════

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();

        switch (command.getName().toLowerCase()) {
            case "eco" -> {
                if (args.length == 1) {
                    suggestions.addAll(List.of("shared", "transactions", "balance", "panel"));
                } else if (args.length == 2 && args[0].equalsIgnoreCase("shared")) {
                    suggestions.addAll(List.of("create", "delete", "add", "remove"));
                } else if (args.length == 3 && args[0].equalsIgnoreCase("shared") && (args[1].equalsIgnoreCase("delete") || args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("remove"))) {
                    suggestions.addAll(economy().getSharedAccountNames());
                } else if (args.length == 4 && args[0].equalsIgnoreCase("shared") && (args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("remove"))) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        suggestions.add(p.getName());
                    }
                } else if (args.length == 2 && args[0].equalsIgnoreCase("balance")) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        suggestions.add(p.getName());
                    }
                }
            }
            case "pay" -> {
                if (args.length == 1) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        suggestions.add(p.getName());
                    }
                }
            }
            case "worth" -> {
                if (args.length == 1) {
                    for (Material m : Material.values()) {
                        suggestions.add(m.name());
                    }
                }
            }
            case "bounty" -> {
                if (args.length == 1) {
                    suggestions.addAll(List.of("set", "check", "list"));
                } else if (args.length == 2 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("check"))) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        suggestions.add(p.getName());
                    }
                } else if (args.length == 4 && args[0].equalsIgnoreCase("set")) {
                    suggestions.addAll(List.of("anonymous"));
                }
            }
            case "auction" -> {
                if (args.length == 1) {
                    suggestions.addAll(List.of("sell", "buy", "cancel"));
                }
            }
            case "ecoadmin" -> {
                if (args.length == 1) {
                    suggestions.addAll(List.of("give", "take", "set", "reload"));
                } else if (args.length == 2 && !args[0].equalsIgnoreCase("reload")) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        suggestions.add(p.getName());
                    }
                }
            }
            case "taxmanager" -> {
                if (args.length == 1) {
                    suggestions.addAll(List.of("pay", "pay-shared", "check", "clear", "assess"));
                } else if (args.length == 2 && (args[0].equalsIgnoreCase("pay") || args[0].equalsIgnoreCase("pay-shared"))) {
                    suggestions.add("all");
                } else if (args.length == 2 && args[0].equalsIgnoreCase("clear")) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        suggestions.add(p.getName());
                    }
                }
            }
            case "taxsetup" -> { }
            case "dealership" -> {
                if (args.length == 1) {
                    for (var d : plugin.getDealershipManager().getAllDealerships()) {
                        suggestions.add(d.getName());
                    }
                }
            }
            case "dealershipsetup" -> { }
        }

        return suggestions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}
