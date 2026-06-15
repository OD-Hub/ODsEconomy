package com.otis.odseconomy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class CommandManager implements CommandExecutor, TabCompleter {

    private final OdsEconomy plugin;

    public CommandManager(OdsEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = command.getName().toLowerCase();

        if (cmd.equals("dice")) return handleDice(sender, args);
        if (cmd.equals("lottery")) return handleLottery(sender, args);
        if (cmd.equals("bounty")) return handleBounty(sender, args);
        if (cmd.equals("mnote")) return handleNote(sender, args);

        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        String symbol = plugin.getCurrencySymbol();

        if (!checkToggleAndPermission(p, cmd)) return true;

        switch (cmd) {
            case "mgive":
                if (args.length == 2) {
                    Player target = Bukkit.getPlayer(args[0]);
                    try {
                        double amount = Double.parseDouble(args[1]);
                        if (target != null) {
                            plugin.addMoney(target.getUniqueId(), amount);
                            p.sendMessage(ChatColor.GREEN + "Granted " + symbol + amount + " to " + target.getName());
                        }
                    } catch (NumberFormatException ignored) {}
                }
                break;

            case "mset":
                if (args.length == 2) {
                    Player target = Bukkit.getPlayer(args[0]);
                    try {
                        double amount = Double.parseDouble(args[1]);
                        if (target != null) {
                            plugin.setMoney(target.getUniqueId(), amount);
                            p.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s balance to " + symbol + String.format("%.2f", amount));
                        }
                    } catch (NumberFormatException ignored) {}
                }
                break;

            case "mtake":
                if (args.length == 2) {
                    Player target = Bukkit.getPlayer(args[0]);
                    try {
                        double amount = Double.parseDouble(args[1]);
                        if (target != null) {
                            if (plugin.removeMoney(target.getUniqueId(), amount)) {
                                p.sendMessage(ChatColor.GREEN + "Took " + symbol + String.format("%.2f", amount) + " from " + target.getName());
                            } else {
                                p.sendMessage(ChatColor.RED + target.getName() + " doesn't have enough money.");
                            }
                        }
                    } catch (NumberFormatException ignored) {}
                }
                break;

            case "balance":
                Player balTarget = args.length > 0 ? Bukkit.getPlayer(args[0]) : p;
                if (balTarget != null) {
                    double bal = plugin.balances.getOrDefault(balTarget.getUniqueId(), 0.0);
                    p.sendMessage(ChatColor.GOLD + balTarget.getName() + "'s balance: " + symbol + String.format("%.2f", bal));
                }
                break;

            case "baltop":
                List<Map.Entry<UUID, Double>> sorted = plugin.balances.entrySet().stream()
                        .sorted(Comparator.<Map.Entry<UUID, Double>>comparingDouble(Map.Entry::getValue).reversed())
                        .limit(10).collect(Collectors.toList());
                p.sendMessage(ChatColor.GOLD + "=== Top Balances ===");
                int rank = 1;
                for (Map.Entry<UUID, Double> entry : sorted) {
                    String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                    if (name == null) name = "Unknown";
                    p.sendMessage(ChatColor.GREEN + "#" + rank + " " + name + " - " + symbol + String.format("%.2f", entry.getValue()));
                    rank++;
                }
                break;

            case "worth":
                ItemStack held = p.getInventory().getItemInMainHand();
                if (held.getType() != Material.AIR && plugin.sellPrices.containsKey(held.getType())) {
                    double pricePer = plugin.sellPrices.get(held.getType());
                    p.sendMessage(ChatColor.YELLOW + "Worth: " + symbol + String.format("%.2f", pricePer) + " each / " + symbol + String.format("%.2f", pricePer * held.getAmount()) + " for stack");
                } else {
                    p.sendMessage(ChatColor.RED + "This item cannot be sold.");
                }
                break;

            case "mtransfer":
                if (args.length == 2) {
                    Player target = Bukkit.getPlayer(args[0]);
                    try {
                        double amount = Double.parseDouble(args[1]);
                        if (target != null && plugin.removeMoney(p.getUniqueId(), amount)) {
                            plugin.addMoney(target.getUniqueId(), amount);
                            p.sendMessage(ChatColor.GREEN + "Sent " + symbol + amount + " to " + target.getName());
                            target.sendMessage(ChatColor.GREEN + "Received " + symbol + amount + " from " + p.getName());
                        } else {
                            p.sendMessage(ChatColor.RED + "Insufficient funds or player not found.");
                        }
                    } catch (NumberFormatException ignored) {}
                }
                break;

            case "sell":
                ItemStack hand = p.getInventory().getItemInMainHand();
                if (hand.getType() != Material.AIR && plugin.sellPrices.containsKey(hand.getType())) {
                    double pricePerItem = plugin.sellPrices.get(hand.getType());
                    double total = pricePerItem * hand.getAmount();
                    plugin.addMoney(p.getUniqueId(), total);
                    p.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                    p.sendMessage(ChatColor.GREEN + "Sold for " + symbol + String.format("%.2f", total));
                } else {
                    p.sendMessage(ChatColor.RED + "This item cannot be sold.");
                }
                break;

            case "mreload":
                plugin.reloadPluginConfig();
                p.sendMessage(ChatColor.GREEN + "Configuration reloaded.");
                break;

            case "ecohelp":
                List<String> helpLines = new ArrayList<>();
                helpLines.add(ChatColor.GOLD + "=== OD's Economy Commands ===");

                helpLines.addAll(buildHelp(p, "balance", "/balance [player]", "Check your or another player's balance"));
                helpLines.addAll(buildHelp(p, "baltop", "/baltop", "Show the richest players"));
                helpLines.addAll(buildHelp(p, "pay", "/mtransfer <player> <amount>", "Send money to another player"));
                helpLines.addAll(buildHelp(p, "sell", "/sell", "Sell the item in your hand"));
                helpLines.addAll(buildHelp(p, "worth", "/worth", "Check the sell value of the item in your hand"));
                helpLines.addAll(buildHelp(p, "certification", "/mcertification <x> <z>", "Check chunk owner"));
                helpLines.addAll(buildHelp(p, "certification", "/mcertification <player>", "List player's chunks"));
                helpLines.addAll(buildHelp(p, "order", "/dorder <item> <amount>", "Place an order"));
                helpLines.addAll(buildHelp(p, "order", "/dorder confirm", "Confirm pending order"));
                helpLines.addAll(buildHelp(p, "inquire", "/dinquire <player|dept>", "Check dealership info"));

                helpLines.addAll(buildHelp(p, "dice", "/dice <amount>", "Gamble your money on a dice roll"));
                helpLines.addAll(buildHelp(p, "lottery", "/lottery buy [tickets]", "Buy lottery tickets"));
                helpLines.addAll(buildHelp(p, "lottery", "/lottery info", "Check lottery details"));
                helpLines.addAll(buildHelp(p, "bounty", "/bounty set <player> <amount>", "Place a bounty on a player"));
                helpLines.addAll(buildHelp(p, "bounty", "/bounty list [player]", "List active bounties"));
                helpLines.addAll(buildHelp(p, "banknote", "/mnote <amount>", "Create a physical banknote"));

                if (p.hasPermission("odseconomy.admin")) {
                    helpLines.add(ChatColor.GREEN + "/mgive <player> <amount>" + ChatColor.GRAY + " - Grant money (admin)");
                    helpLines.add(ChatColor.GREEN + "/mregister <x> <z> <player>" + ChatColor.GRAY + " - Register chunk (admin)");
                    helpLines.add(ChatColor.GREEN + "/dgive <dept> <player>" + ChatColor.GRAY + " - Grant dealership (admin)");
                    helpLines.add(ChatColor.GREEN + "/mreload" + ChatColor.GRAY + " - Reload config (admin)");
                    helpLines.add(ChatColor.GREEN + "/mset <player> <amount>" + ChatColor.GRAY + " - Set balance (admin)");
                    helpLines.add(ChatColor.GREEN + "/mtake <player> <amount>" + ChatColor.GRAY + " - Remove money (admin)");
                }

                for (String line : helpLines) p.sendMessage(line);
                break;

            case "mregister":
                if (args.length == 3) {
                    try {
                        int blockX = Integer.parseInt(args[0]);
                        int blockZ = Integer.parseInt(args[1]);
                        String chunkKey = OdsEconomy.chunkKeyFromBlock(blockX, blockZ);
                        Player target = Bukkit.getPlayer(args[2]);
                        if (target != null) {
                            if (plugin.landClaims.containsKey(chunkKey)) {
                                UUID existingOwner = plugin.landClaims.get(chunkKey);
                                String ownerName = Bukkit.getOfflinePlayer(existingOwner).getName();
                                p.sendMessage(ChatColor.RED + "They already own that chunk! (" + ownerName + ")");
                            } else {
                                plugin.landClaims.put(chunkKey, target.getUniqueId());
                                p.sendMessage(ChatColor.GREEN + "Registered chunk " + chunkKey + " to " + target.getName());
                            }
                        }
                    } catch (NumberFormatException e) {
                        p.sendMessage(ChatColor.RED + "Invalid coordinates.");
                    }
                }
                break;

            case "mcertification":
                if (args.length == 2) {
                    try {
                        int blockX = Integer.parseInt(args[0]);
                        int blockZ = Integer.parseInt(args[1]);
                        String chunkKey = OdsEconomy.chunkKeyFromBlock(blockX, blockZ);
                        UUID owner = plugin.landClaims.get(chunkKey);
                        p.sendMessage(ChatColor.YELLOW + "Chunk " + chunkKey + " is owned by: " +
                                (owner != null ? Bukkit.getOfflinePlayer(owner).getName() : "None"));
                    } catch (NumberFormatException e) {
                        p.sendMessage(ChatColor.RED + "Invalid coordinates.");
                    }
                } else if (args.length == 1) {
                    Player target = Bukkit.getPlayer(args[0]);
                    if (target != null) {
                        p.sendMessage(ChatColor.YELLOW + target.getName() + "'s chunks:");
                        for (Map.Entry<String, UUID> entry : plugin.landClaims.entrySet()) {
                            if (entry.getValue().equals(target.getUniqueId())) {
                                p.sendMessage(ChatColor.GRAY + "- " + entry.getKey());
                            }
                        }
                    }
                }
                break;

            case "dgive":
                if (args.length == 2) {
                    String dept = args[0];
                    Player target = Bukkit.getPlayer(args[1]);
                    if (target != null && plugin.getConfig().contains("dealerships." + dept)) {
                        plugin.getConfig().set("dealerships." + dept + ".owner", target.getUniqueId().toString());
                        plugin.saveConfig();
                        p.sendMessage(ChatColor.GREEN + "Given dealership " + dept + " to " + target.getName());
                    }
                }
                break;

            case "dorder":
                if (args.length == 1 && args[0].equalsIgnoreCase("confirm")) {
                    PendingOrder order = plugin.pendingOrders.get(p.getUniqueId());
                    if (order != null) {
                        if (plugin.removeMoney(p.getUniqueId(), order.totalCost)) {
                            p.getInventory().addItem(new ItemStack(order.material, order.amount));
                            plugin.pendingOrders.remove(p.getUniqueId());
                            p.sendMessage(ChatColor.GREEN + "Order confirmed! Items delivered.");
                        } else {
                            p.sendMessage(ChatColor.RED + "Insufficient funds.");
                        }
                    } else {
                        p.sendMessage(ChatColor.RED + "No pending order.");
                    }
                } else if (args.length == 2) {
                    String itemName = args[0].toUpperCase();
                    int amount = Integer.parseInt(args[1]);

                    for (String dept : plugin.getConfig().getConfigurationSection("dealerships").getKeys(false)) {
                        String ownerUUID = plugin.getConfig().getString("dealerships." + dept + ".owner");
                        if (ownerUUID != null && ownerUUID.equals(p.getUniqueId().toString())) {
                            List<String> items = plugin.getConfig().getStringList("dealerships." + dept + ".items");
                            for (String itemStr : items) {
                                String[] split = itemStr.split(", ");
                                if (split[0].equalsIgnoreCase(itemName)) {
                                    int configQty = Integer.parseInt(split[1]);
                                    double configCost = Double.parseDouble(split[2]);

                                    int orderSets = (int) Math.ceil((double)amount / configQty);
                                    int finalAmount = orderSets * configQty;
                                    double finalCost = orderSets * configCost;

                                    PendingOrder po = new PendingOrder(dept, Material.valueOf(itemName), finalAmount, finalCost);
                                    plugin.pendingOrders.put(p.getUniqueId(), po);
                                    p.sendMessage(ChatColor.YELLOW + "Order requires " + plugin.getCurrencySymbol() + finalCost + " for " + finalAmount + " " + itemName + ". Type /dorder confirm to finalize.");
                                    return true;
                                }
                            }
                        }
                    }
                    p.sendMessage(ChatColor.RED + "You don't own a department that sells this, or it doesn't exist.");
                }
                break;

            case "dinquire":
                if (args.length == 1) {
                    String query = args[0];
                    Player target = Bukkit.getPlayer(query);
                    if (target != null) {
                        p.sendMessage(ChatColor.YELLOW + target.getName() + " owns:");
                        for (String dept : plugin.getConfig().getConfigurationSection("dealerships").getKeys(false)) {
                            if (target.getUniqueId().toString().equals(plugin.getConfig().getString("dealerships." + dept + ".owner"))) {
                                p.sendMessage(ChatColor.GRAY + "- " + dept);
                            }
                        }
                    } else {
                        String ownerUUID = plugin.getConfig().getString("dealerships." + query + ".owner");
                        if (ownerUUID != null && !ownerUUID.isEmpty()) {
                            p.sendMessage(ChatColor.YELLOW + query + " is owned by: " + Bukkit.getOfflinePlayer(UUID.fromString(ownerUUID)).getName());
                        } else {
                            p.sendMessage(ChatColor.GRAY + query + " has no owner or doesn't exist.");
                        }
                    }
                }
                break;
        }
        return true;
    }

    // ──────────────── Dice ────────────────

    private boolean handleDice(CommandSender sender, String[] args) {
        if (!plugin.getConfig().getBoolean("gambling.enabled", false)) {
            sender.sendMessage(ChatColor.RED + "Gambling is disabled.");
            return true;
        }

        Player target;
        double amount;

        if (sender instanceof Player) {
            if (args.length < 1) {
                sender.sendMessage(ChatColor.RED + "Usage: /dice <amount>");
                return true;
            }
            target = (Player) sender;
            if (!target.hasPermission("odseconomy.dice")) {
                target.sendMessage(ChatColor.RED + "You don't have permission.");
                return true;
            }
            if (!plugin.isInGamblingLocation(target)) {
                target.sendMessage(ChatColor.RED + "You must be at a gambling location to use this command.");
                return true;
            }
            try { amount = Double.parseDouble(args[0]); } catch (NumberFormatException e) {
                target.sendMessage(ChatColor.RED + "Invalid amount.");
                return true;
            }
        } else {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /dice <player> <amount>");
                return true;
            }
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found.");
                return true;
            }
            try { amount = Double.parseDouble(args[1]); } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid amount.");
                return true;
            }
        }

        if (amount <= 0) {
            target.sendMessage(ChatColor.RED + "Amount must be positive.");
            return true;
        }

        if (!plugin.removeMoney(target.getUniqueId(), amount)) {
            target.sendMessage(ChatColor.RED + "Insufficient funds.");
            return true;
        }

        double multiplier = plugin.getConfig().getDouble("gambling.dice-multiplier", 2.0);
        String sym = plugin.getCurrencySymbol();

        if (plugin.getRandom().nextBoolean()) {
            double winnings = amount * multiplier;
            plugin.addMoney(target.getUniqueId(), winnings);
            target.sendMessage(ChatColor.GREEN + "You won " + sym + String.format("%.2f", winnings) + "!");
        } else {
            target.sendMessage(ChatColor.RED + "You lost " + sym + String.format("%.2f", amount) + ".");
        }
        return true;
    }

    // ──────────────── Lottery ────────────────

    private boolean handleLottery(CommandSender sender, String[] args) {
        if (!plugin.getConfig().getBoolean("lottery.enabled", false)) {
            sender.sendMessage(ChatColor.RED + "Lottery is disabled.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /lottery buy [tickets] | /lottery info");
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("info")) {
            int totalTickets = plugin.lotteryTickets.values().stream().mapToInt(Integer::intValue).sum();
            double ticketPrice = plugin.getConfig().getDouble("lottery.ticket-price", 100.0);
            String sym = plugin.getCurrencySymbol();
            sender.sendMessage(ChatColor.GOLD + "=== Lottery Info ===");
            sender.sendMessage(ChatColor.GREEN + "Pot: " + sym + String.format("%.2f", plugin.lotteryPot));
            sender.sendMessage(ChatColor.GREEN + "Tickets sold: " + totalTickets);
            sender.sendMessage(ChatColor.GREEN + "Ticket price: " + sym + String.format("%.2f", ticketPrice));
            if (plugin.nextLotteryDrawTime > 0) {
                long remaining = plugin.nextLotteryDrawTime - (System.currentTimeMillis() / 1000);
                if (remaining > 0) {
                    long hours = remaining / 3600;
                    long mins = (remaining % 3600) / 60;
                    sender.sendMessage(ChatColor.GREEN + "Next draw: ~" + hours + "h " + mins + "m");
                } else {
                    sender.sendMessage(ChatColor.GREEN + "Draw is imminent!");
                }
            }
            return true;
        }

        if (sub.equals("buy")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can buy tickets.");
                return true;
            }
            Player p = (Player) sender;
            if (!p.hasPermission("odseconomy.lottery")) {
                p.sendMessage(ChatColor.RED + "You don't have permission.");
                return true;
            }
            int count = 1;
            if (args.length >= 2) {
                try { count = Integer.parseInt(args[1]); } catch (NumberFormatException e) {
                    p.sendMessage(ChatColor.RED + "Invalid ticket count.");
                    return true;
                }
            }
            if (count < 1) {
                p.sendMessage(ChatColor.RED + "Must buy at least 1 ticket.");
                return true;
            }
            double ticketPrice = plugin.getConfig().getDouble("lottery.ticket-price", 100.0);
            double cost = ticketPrice * count;
            String sym = plugin.getCurrencySymbol();

            if (plugin.buyLotteryTickets(p, count)) {
                p.sendMessage(ChatColor.GREEN + "Bought " + count + " lottery ticket(s) for " + sym + String.format("%.2f", cost) + ".");
                if (plugin.getConfig().getBoolean("lottery.broadcast-ticket-purchase", false)) {
                    Bukkit.broadcastMessage(ChatColor.GOLD + p.getName() + " bought " + count + " lottery ticket(s)!");
                }
            } else {
                p.sendMessage(ChatColor.RED + "Could not buy tickets. Check your balance and max ticket limit.");
            }
            return true;
        }

        sender.sendMessage(ChatColor.RED + "Usage: /lottery buy [tickets] | /lottery info");
        return true;
    }

    // ──────────────── Bounty ────────────────

    private boolean handleBounty(CommandSender sender, String[] args) {
        if (!plugin.getConfig().getBoolean("bounties.enabled", false)) {
            sender.sendMessage(ChatColor.RED + "Bounties are disabled.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /bounty set <player> <amount> | /bounty list [player]");
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("set")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can place bounties.");
                return true;
            }
            Player p = (Player) sender;
            if (!p.hasPermission("odseconomy.bounty")) {
                p.sendMessage(ChatColor.RED + "You don't have permission.");
                return true;
            }
            if (args.length < 3) {
                p.sendMessage(ChatColor.RED + "Usage: /bounty set <player> <amount>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                p.sendMessage(ChatColor.RED + "Player not found.");
                return true;
            }
            if (target.equals(p)) {
                p.sendMessage(ChatColor.RED + "You cannot place a bounty on yourself.");
                return true;
            }
            double amount;
            try { amount = Double.parseDouble(args[2]); } catch (NumberFormatException e) {
                p.sendMessage(ChatColor.RED + "Invalid amount.");
                return true;
            }
            double min = plugin.getConfig().getDouble("bounties.min-amount", 1.0);
            double max = plugin.getConfig().getDouble("bounties.max-amount", 100000.0);
            if (amount < min || amount > max) {
                p.sendMessage(ChatColor.RED + "Bounty must be between " + plugin.getCurrencySymbol() + String.format("%.2f", min) + " and " + plugin.getCurrencySymbol() + String.format("%.2f", max) + ".");
                return true;
            }
            if (!plugin.removeMoney(p.getUniqueId(), amount)) {
                p.sendMessage(ChatColor.RED + "Insufficient funds.");
                return true;
            }
            double existing = plugin.bounties.getOrDefault(target.getUniqueId(), 0.0);
            plugin.bounties.put(target.getUniqueId(), existing + amount);

            String sym = plugin.getCurrencySymbol();
            Bukkit.broadcastMessage(ChatColor.GOLD + p.getName() + " placed a bounty of " + sym + String.format("%.2f", amount) + " on " + target.getName() + "!");

            if (plugin.getConfig().getBoolean("bounties.notify-target", true)) {
                target.sendMessage(ChatColor.RED + "A bounty of " + sym + String.format("%.2f", amount) + " has been placed on you by " + p.getName() + "!");
            }
            return true;
        }

        if (sub.equals("list")) {
            if (!sender.hasPermission("odseconomy.bounty")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission.");
                return true;
            }
            if (args.length >= 2) {
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found.");
                    return true;
                }
                double bounty = plugin.bounties.getOrDefault(target.getUniqueId(), 0.0);
                String sym = plugin.getCurrencySymbol();
                sender.sendMessage(ChatColor.GOLD + target.getName() + "'s bounty: " + sym + String.format("%.2f", bounty));
            } else {
                if (plugin.bounties.isEmpty()) {
                    sender.sendMessage(ChatColor.YELLOW + "No active bounties.");
                    return true;
                }
                String sym = plugin.getCurrencySymbol();
                sender.sendMessage(ChatColor.GOLD + "=== Active Bounties ===");
                for (Map.Entry<UUID, Double> entry : plugin.bounties.entrySet()) {
                    String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                    if (name == null) name = "Unknown";
                    sender.sendMessage(ChatColor.GREEN + name + " - " + sym + String.format("%.2f", entry.getValue()));
                }
            }
            return true;
        }

        sender.sendMessage(ChatColor.RED + "Usage: /bounty set <player> <amount> | /bounty list [player]");
        return true;
    }

    // ──────────────── Bank Note ────────────────

    private boolean handleNote(CommandSender sender, String[] args) {
        if (!plugin.getConfig().getBoolean("banknotes.enabled", false)) {
            sender.sendMessage(ChatColor.RED + "Banknotes are disabled.");
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can create banknotes.");
            return true;
        }
        Player p = (Player) sender;
        if (!p.hasPermission("odseconomy.banknote")) {
            p.sendMessage(ChatColor.RED + "You don't have permission.");
            return true;
        }
        if (args.length < 1) {
            p.sendMessage(ChatColor.RED + "Usage: /mnote <amount>");
            return true;
        }
        double amount;
        try { amount = Double.parseDouble(args[0]); } catch (NumberFormatException e) {
            p.sendMessage(ChatColor.RED + "Invalid amount.");
            return true;
        }
        double min = plugin.getConfig().getDouble("banknotes.min-amount", 1.0);
        double max = plugin.getConfig().getDouble("banknotes.max-amount", 100000.0);
        if (amount < min || amount > max) {
            p.sendMessage(ChatColor.RED + "Amount must be between " + plugin.getCurrencySymbol() + String.format("%.2f", min) + " and " + plugin.getCurrencySymbol() + String.format("%.2f", max) + ".");
            return true;
        }
        if (!plugin.removeMoney(p.getUniqueId(), amount)) {
            p.sendMessage(ChatColor.RED + "Insufficient funds.");
            return true;
        }
        ItemStack note = plugin.createBanknote(amount);
        p.getInventory().addItem(note);
        p.sendMessage(ChatColor.GREEN + "Created banknote for " + plugin.getCurrencySymbol() + String.format("%.2f", amount) + ".");
        return true;
    }

    // ──────────────── Permission & Toggle Checks ────────────────

    private boolean checkToggleAndPermission(Player p, String cmd) {
        String permNode;
        switch (cmd) {
            case "balance": permNode = "odseconomy.balance"; break;
            case "baltop":  permNode = "odseconomy.baltop"; break;
            case "mtransfer": permNode = "odseconomy.pay"; break;
            case "sell":    permNode = "odseconomy.sell"; break;
            case "worth":   permNode = "odseconomy.worth"; break;
            case "mcertification": permNode = "odseconomy.certification"; break;
            case "dorder":  permNode = "odseconomy.order"; break;
            case "dinquire": permNode = "odseconomy.inquire"; break;
            case "mgive": case "mregister": case "dgive": case "mreload": case "mset": case "mtake":
                permNode = "odseconomy.admin"; break;
            default:
                permNode = null;
        }
        if (permNode != null && !p.hasPermission(permNode)) return false;

        switch (cmd) {
            case "balance": return plugin.isFeatureEnabled("balance");
            case "baltop":  return plugin.isFeatureEnabled("baltop");
            case "mset":    return plugin.isFeatureEnabled("mset");
            case "mtake":   return plugin.isFeatureEnabled("mtake");
            case "worth":   return plugin.isFeatureEnabled("worth");
            case "mtransfer": return plugin.isFeatureEnabled("pay");
            case "mcertification": return plugin.isFeatureEnabled("certification");
            case "dorder":  return plugin.isFeatureEnabled("order");
            case "dinquire": return plugin.isFeatureEnabled("inquire");
        }
        return true;
    }

    private List<String> buildHelp(Player p, String toggleKey, String usage, String desc) {
        List<String> result = new ArrayList<>();
        String permNode;
        switch (toggleKey) {
            case "balance": permNode = "odseconomy.balance"; break;
            case "baltop":  permNode = "odseconomy.baltop"; break;
            case "pay":     permNode = "odseconomy.pay"; break;
            case "sell":    permNode = "odseconomy.sell"; break;
            case "worth":   permNode = "odseconomy.worth"; break;
            case "certification": permNode = "odseconomy.certification"; break;
            case "order":   permNode = "odseconomy.order"; break;
            case "inquire": permNode = "odseconomy.inquire"; break;
            case "dice":    permNode = "odseconomy.dice"; break;
            case "lottery": permNode = "odseconomy.lottery"; break;
            case "bounty":  permNode = "odseconomy.bounty"; break;
            case "banknote": permNode = "odseconomy.banknote"; break;
            default:        permNode = null;
        }
        if (permNode != null && !p.hasPermission(permNode)) return result;
        if (toggleKey.equals("dice") && !plugin.getConfig().getBoolean("gambling.enabled", false)) return result;
        if (toggleKey.equals("lottery") && !plugin.getConfig().getBoolean("lottery.enabled", false)) return result;
        if (toggleKey.equals("bounty") && !plugin.getConfig().getBoolean("bounties.enabled", false)) return result;
        if (toggleKey.equals("banknote") && !plugin.getConfig().getBoolean("banknotes.enabled", false)) return result;
        if (toggleKey.equals("balance") && !plugin.isFeatureEnabled("balance")) return result;
        if (toggleKey.equals("baltop") && !plugin.isFeatureEnabled("baltop")) return result;
        if (toggleKey.equals("pay") && !plugin.isFeatureEnabled("pay")) return result;
        if (toggleKey.equals("sell") && !plugin.isFeatureEnabled("sell")) return result;
        if (toggleKey.equals("worth") && !plugin.isFeatureEnabled("worth")) return result;
        if (toggleKey.equals("certification") && !plugin.isFeatureEnabled("certification")) return result;
        if (toggleKey.equals("order") && !plugin.isFeatureEnabled("order")) return result;
        if (toggleKey.equals("inquire") && !plugin.isFeatureEnabled("inquire")) return result;
        result.add(ChatColor.GREEN + usage + ChatColor.GRAY + " - " + desc);
        return result;
    }

    // ──────────────── Tab Completion ────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) return Collections.emptyList();
        Player p = (Player) sender;
        String cmd = command.getName().toLowerCase();

        if (!checkToggleAndPermission(p, cmd)) return Collections.emptyList();

        switch (cmd) {
            case "mgive":
            case "mset":
            case "mtake":
                if (args.length == 1)
                    return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
                if (args.length == 2)
                    return List.of("<amount>");
                break;

            case "mtransfer":
                if (args.length == 1)
                    return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
                if (args.length == 2)
                    return List.of("<amount>");
                break;

            case "balance":
                if (args.length == 1)
                    return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
                break;

            case "mregister":
                if (args.length == 1) return List.of("<x_position>");
                if (args.length == 2) return List.of("<z_position>");
                if (args.length == 3)
                    return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase())).collect(Collectors.toList());
                break;

            case "mcertification":
                if (args.length == 1) {
                    List<String> suggestions = new ArrayList<>();
                    Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase())).forEach(suggestions::add);
                    if ("<x_position>".startsWith(args[0].toLowerCase())) suggestions.add("<x_position>");
                    return suggestions;
                }
                if (args.length == 2) return List.of("<z_position>");
                break;

            case "dgive":
                if (args.length == 1) {
                    List<String> depts = new ArrayList<>(plugin.getConfig().getConfigurationSection("dealerships").getKeys(false));
                    return depts.stream().filter(d -> d.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
                }
                if (args.length == 2)
                    return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
                break;

            case "dorder":
                if (args.length == 1) {
                    List<String> suggestions = new ArrayList<>();
                    suggestions.add("confirm");
                    for (String dept : plugin.getConfig().getConfigurationSection("dealerships").getKeys(false)) {
                        String ownerUUID = plugin.getConfig().getString("dealerships." + dept + ".owner");
                        if (ownerUUID != null && ownerUUID.equals(p.getUniqueId().toString())) {
                            for (String itemStr : plugin.getConfig().getStringList("dealerships." + dept + ".items")) {
                                String itemName = itemStr.split(", ")[0];
                                if (!suggestions.contains(itemName))
                                    suggestions.add(itemName);
                            }
                        }
                    }
                    return suggestions.stream().filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
                }
                if (args.length == 2) return List.of("<amount>");
                break;

            case "dinquire":
                if (args.length == 1) {
                    List<String> suggestions = new ArrayList<>();
                    Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase())).forEach(suggestions::add);
                    for (String dept : plugin.getConfig().getConfigurationSection("dealerships").getKeys(false)) {
                        if (dept.toLowerCase().startsWith(args[0].toLowerCase()))
                            suggestions.add(dept);
                    }
                    return suggestions;
                }
                break;

            case "dice":
                if (args.length == 1)
                    return List.of("<amount>");
                break;

            case "lottery":
                if (args.length == 1) {
                    return List.of("buy", "info").stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
                }
                if (args.length == 2 && args[0].equalsIgnoreCase("buy")) {
                    return List.of("<tickets>");
                }
                break;

            case "bounty":
                if (args.length == 1) {
                    return List.of("set", "list").stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
                }
                if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
                    return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
                }
                if (args.length == 2 && args[0].equalsIgnoreCase("list")) {
                    return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
                }
                if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
                    return List.of("<amount>");
                }
                break;

            case "mnote":
                if (args.length == 1)
                    return List.of("<amount>");
                break;
        }
        return Collections.emptyList();
    }
}
