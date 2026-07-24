package com.otis.odseconomy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
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
        if (cmd.equals("sharedaccount") || cmd.equals("sa")) return handleSharedAccount(sender, args);
        if (cmd.equals("trade")) return handleTrade(sender, args);

        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        String symbol = plugin.getCurrencySymbol();

        if (!cmd.equals("ecohelp") && !checkToggleAndPermission(p, cmd)) return true;

        switch (cmd) {
            // ── Admin ──
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

            case "mreset":
                if (args.length == 1) {
                    Player target = Bukkit.getPlayer(args[0]);
                    if (target != null) {
                        double start = plugin.getConfig().getDouble("settings.starting-balance", 0.0);
                        plugin.setMoney(target.getUniqueId(), start);
                        p.sendMessage(ChatColor.GREEN + "Reset " + target.getName() + "'s balance to " + symbol + String.format("%.2f", start));
                    }
                }
                break;

            // ── Balance ──
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

            // ── Pay ──
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

            // ── Sell ──
            case "sell":
                handleSell(p, args, symbol);
                break;

            // ── Buy ──
            case "buy":
                handleBuy(p, args, symbol);
                break;

            // ── Worth ──
            case "worth":
                ItemStack held = p.getInventory().getItemInMainHand();
                if (held.getType() != Material.AIR && plugin.sellPrices.containsKey(held.getType())) {
                    double pricePer = plugin.sellPrices.get(held.getType());
                    p.sendMessage(ChatColor.YELLOW + "Worth: " + symbol + String.format("%.2f", pricePer) + " each / " + symbol + String.format("%.2f", pricePer * held.getAmount()) + " for stack");
                } else {
                    p.sendMessage(ChatColor.RED + "This item cannot be sold.");
                }
                break;

            // ── Reload ──
            case "mreload":
                plugin.reloadPluginConfig();
                p.sendMessage(ChatColor.GREEN + "Configuration reloaded.");
                break;

            // ── Kits ──
            case "kit":
                handleKit(p, args, symbol);
                break;

            // ── Warps ──
            case "warp":
                handleWarp(p, args, symbol);
                break;

            case "setwarp":
                if (args.length >= 1) {
                    String warpName = args[0].toLowerCase();
                    if (plugin.warps.containsKey(warpName)) {
                        p.sendMessage(ChatColor.RED + "Warp '" + warpName + "' already exists.");
                        break;
                    }
                    Location loc = p.getLocation();
                    Map<String, Object> wd = new java.util.HashMap<>();
                    wd.put("world", loc.getWorld().getName());
                    wd.put("x", loc.getX());
                    wd.put("y", loc.getY());
                    wd.put("z", loc.getZ());
                    wd.put("yaw", loc.getYaw());
                    wd.put("pitch", loc.getPitch());
                    wd.put("cost", plugin.getConfig().getDouble("warps.default-cost", 0.0));
                    plugin.warps.put(warpName, wd);
                    plugin.saveDataAsync();
                    p.sendMessage(ChatColor.GREEN + "Warp '" + warpName + "' created.");
                }
                break;

            case "delwarp":
                if (args.length >= 1) {
                    String warpName = args[0].toLowerCase();
                    if (plugin.warps.remove(warpName) != null) {
                        plugin.saveDataAsync();
                        p.sendMessage(ChatColor.GREEN + "Warp '" + warpName + "' deleted.");
                    } else {
                        p.sendMessage(ChatColor.RED + "Warp '" + warpName + "' not found.");
                    }
                }
                break;

            // ── Help ──
            case "ecohelp":
                List<String> helpLines = new ArrayList<>();
                helpLines.add(ChatColor.GOLD + "=== OD's Economy Commands ===");

                helpLines.addAll(buildHelp(p, "balance", "/balance [player]", "Check your or another player's balance"));
                helpLines.addAll(buildHelp(p, "baltop", "/baltop", "Show the richest players"));
                helpLines.addAll(buildHelp(p, "pay", "/mtransfer <player> <amount>", "Send money to another player"));
                helpLines.addAll(buildHelp(p, "sell", "/sell [all|<material>]", "Sell items from your inventory"));
                helpLines.addAll(buildHelp(p, "buy", "/buy <item> [amount]", "Buy an item from the server shop"));
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
                helpLines.addAll(buildHelp(p, "kit", "/kit [name]", "Claim a kit"));
                helpLines.addAll(buildHelp(p, "kit", "/kit list", "List available kits"));
                helpLines.addAll(buildHelp(p, "warp", "/warp [name]", "Teleport to a warp"));
                helpLines.addAll(buildHelp(p, "warp", "/warp list", "List available warps"));
                helpLines.addAll(buildHelp(p, "sharedaccount", "/sa create <name>", "Create a shared account"));
                helpLines.addAll(buildHelp(p, "sharedaccount", "/sa invite <player> <name>", "Invite a player"));
                helpLines.addAll(buildHelp(p, "sharedaccount", "/sa balance [name]", "View shared account balance"));
                helpLines.addAll(buildHelp(p, "sharedaccount", "/sa deposit <name> <amount>", "Deposit into a shared account"));
                helpLines.addAll(buildHelp(p, "sharedaccount", "/sa withdraw <name> <amount>", "Withdraw from a shared account"));
                helpLines.addAll(buildHelp(p, "sharedaccount", "/sa list", "List your shared accounts"));
                helpLines.addAll(buildHelp(p, "sharedaccount", "/sa info [name]", "View shared account details"));
                helpLines.addAll(buildHelp(p, "sharedaccount", "/sa setperm <player> <name> <perm>", "Set member permission"));
                helpLines.addAll(buildHelp(p, "trade", "/trade <player>", "Open a trade GUI with a player"));
                helpLines.addAll(buildHelp(p, "sharedaccount", "/sa setvisible <name> <true|false>", "Toggle account visibility"));

                if (p.hasPermission("odseconomy.admin")) {
                    helpLines.add(ChatColor.GREEN + "/mgive <player> <amount>" + ChatColor.GRAY + " - Grant money (admin)");
                    helpLines.add(ChatColor.GREEN + "/mregister <x> <z> <player>" + ChatColor.GRAY + " - Register chunk (admin)");
                    helpLines.add(ChatColor.GREEN + "/dgive <dept> <player>" + ChatColor.GRAY + " - Grant dealership (admin)");
                    helpLines.add(ChatColor.GREEN + "/mreload" + ChatColor.GRAY + " - Reload config (admin)");
                    helpLines.add(ChatColor.GREEN + "/mset <player> <amount>" + ChatColor.GRAY + " - Set balance (admin)");
                    helpLines.add(ChatColor.GREEN + "/mtake <player> <amount>" + ChatColor.GRAY + " - Remove money (admin)");
                    helpLines.add(ChatColor.GREEN + "/mreset <player>" + ChatColor.GRAY + " - Reset balance (admin)");
                    helpLines.add(ChatColor.GREEN + "/setwarp <name>" + ChatColor.GRAY + " - Create a warp (admin)");
                    helpLines.add(ChatColor.GREEN + "/warp remove <name>" + ChatColor.GRAY + " - Delete a warp (admin)");
                }

                for (String line : helpLines) p.sendMessage(line);
                break;

            // ── Land ──
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

            // ── Dealerships ──
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
                    int amount;
                    try { amount = Integer.parseInt(args[1]); } catch (NumberFormatException e) { break; }

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
                                    plugin.pendingOrders.put(p.getUniqueId(), new PendingOrder(dept, Material.valueOf(itemName), finalAmount, finalCost));
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

    // ════════════════════════════════════════════
    //  SELL
    // ════════════════════════════════════════════

    private void handleSell(Player p, String[] args, String symbol) {
        if (args.length == 0) {
            ItemStack hand = p.getInventory().getItemInMainHand();
            if (hand.getType() != Material.AIR && plugin.sellPrices.containsKey(hand.getType())) {
                double total = plugin.sellPrices.get(hand.getType()) * hand.getAmount();
                plugin.addMoney(p.getUniqueId(), total);
                p.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                p.sendMessage(ChatColor.GREEN + "Sold for " + symbol + String.format("%.2f", total));
            } else {
                p.sendMessage(ChatColor.RED + "This item cannot be sold.");
            }
            return;
        }

        if (args[0].equalsIgnoreCase("all")) {
            double totalEarned = 0;
            int soldCount = 0;
            ItemStack[] contents = p.getInventory().getStorageContents();
            for (int i = 0; i < contents.length; i++) {
                ItemStack item = contents[i];
                if (item == null || item.getType() == Material.AIR) continue;
                if (item.hasItemMeta()) continue;
                Double price = plugin.sellPrices.get(item.getType());
                if (price == null) continue;
                totalEarned += price * item.getAmount();
                soldCount += item.getAmount();
                p.getInventory().setItem(i, new ItemStack(Material.AIR));
            }
            if (soldCount > 0) {
                plugin.addMoney(p.getUniqueId(), totalEarned);
                p.sendMessage(ChatColor.GREEN + "Sold " + soldCount + " items for " + symbol + String.format("%.2f", totalEarned));
            } else {
                p.sendMessage(ChatColor.RED + "No sellable items in your inventory.");
            }
            return;
        }

        Material mat = Material.getMaterial(args[0].toUpperCase());
        if (mat == null || !plugin.sellPrices.containsKey(mat)) {
            p.sendMessage(ChatColor.RED + "That item cannot be sold.");
            return;
        }
        int count = 0;
        ItemStack[] contents = p.getInventory().getStorageContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == mat && !item.hasItemMeta()) {
                count += item.getAmount();
                p.getInventory().setItem(i, new ItemStack(Material.AIR));
            }
        }
        if (count > 0) {
            double total = plugin.sellPrices.get(mat) * count;
            plugin.addMoney(p.getUniqueId(), total);
            p.sendMessage(ChatColor.GREEN + "Sold " + count + " " + mat.name() + " for " + symbol + String.format("%.2f", total));
        } else {
            p.sendMessage(ChatColor.RED + "You don't have any " + mat.name() + " to sell.");
        }
    }

    // ════════════════════════════════════════════
    //  BUY
    // ════════════════════════════════════════════

    private void handleBuy(Player p, String[] args, String symbol) {
        if (args.length < 1) {
            p.sendMessage(ChatColor.RED + "Usage: /buy <item> [amount]");
            return;
        }
        Material mat = Material.getMaterial(args[0].toUpperCase());
        if (mat == null || !plugin.buyPrices.containsKey(mat)) {
            p.sendMessage(ChatColor.RED + "That item is not available for purchase.");
            return;
        }
        int amount = 1;
        if (args.length >= 2) {
            try { amount = Integer.parseInt(args[1]); } catch (NumberFormatException e) {
                p.sendMessage(ChatColor.RED + "Invalid amount.");
                return;
            }
        }
        if (amount <= 0) { p.sendMessage(ChatColor.RED + "Amount must be positive."); return; }

        double pricePer = plugin.buyPrices.get(mat);
        double totalCost = pricePer * amount;
        int maxStack = mat.getMaxStackSize();
        int neededSlots = (amount + maxStack - 1) / maxStack;
        int empty = 0;
        for (ItemStack item : p.getInventory().getStorageContents()) {
            if (item == null || item.getType() == Material.AIR) empty++;
        }
        if (empty < neededSlots) {
            p.sendMessage(ChatColor.RED + "Not enough inventory space.");
            return;
        }
        if (!plugin.removeMoney(p.getUniqueId(), totalCost)) {
            p.sendMessage(ChatColor.RED + "Insufficient funds. Need " + symbol + String.format("%.2f", totalCost));
            return;
        }
        int maxPerStack = mat.getMaxStackSize();
        int remaining = amount;
        while (remaining > 0) {
            int give = Math.min(remaining, maxPerStack);
            p.getInventory().addItem(new ItemStack(mat, give));
            remaining -= give;
        }
        p.sendMessage(ChatColor.GREEN + "Bought " + amount + " " + mat.name() + " for " + symbol + String.format("%.2f", totalCost));
    }

    // ════════════════════════════════════════════
    //  KITS
    // ════════════════════════════════════════════

    private void handleKit(Player p, String[] args, String symbol) {
        if (!plugin.getConfig().contains("kits") || plugin.getConfig().getConfigurationSection("kits").getKeys(false).isEmpty()) {
            p.sendMessage(ChatColor.RED + "No kits are configured.");
            return;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            p.sendMessage(ChatColor.GOLD + "=== Available Kits ===");
            for (String kitName : plugin.getConfig().getConfigurationSection("kits").getKeys(false)) {
                double cost = plugin.getConfig().getDouble("kits." + kitName + ".cost", 0.0);
                String perm = plugin.getConfig().getString("kits." + kitName + ".permission", "");
                if (!perm.isEmpty() && !p.hasPermission(perm)) continue;
                p.sendMessage(ChatColor.GREEN + kitName + ChatColor.GRAY + " - " + symbol + String.format("%.2f", cost));
            }
            return;
        }

        String kitName = args[0].toLowerCase();
        if (!plugin.getConfig().contains("kits." + kitName)) {
            p.sendMessage(ChatColor.RED + "Kit '" + kitName + "' not found.");
            return;
        }

        String perm = plugin.getConfig().getString("kits." + kitName + ".permission", "");
        if (!perm.isEmpty() && !p.hasPermission(perm)) {
            p.sendMessage(ChatColor.RED + "You don't have permission to claim this kit.");
            return;
        }

        long cooldownSecs = plugin.getConfig().getLong("kits." + kitName + ".cooldown-seconds", 0);
        if (cooldownSecs > 0 && !plugin.checkKitCooldown(kitName, p)) {
            long remaining = plugin.getKitCooldownRemaining(kitName, p) / 1000;
            long hours = remaining / 3600;
            long mins = (remaining % 3600) / 60;
            p.sendMessage(ChatColor.RED + "Kit is on cooldown. " + hours + "h " + mins + "m remaining.");
            return;
        }

        double cost = plugin.getConfig().getDouble("kits." + kitName + ".cost", 0.0);
        if (cost > 0 && !plugin.removeMoney(p.getUniqueId(), cost)) {
            p.sendMessage(ChatColor.RED + "Insufficient funds. Need " + symbol + String.format("%.2f", cost));
            return;
        }

        List<String> itemStrs = plugin.getConfig().getStringList("kits." + kitName + ".items");
        int given = 0;
        for (String itemStr : itemStrs) {
            String[] parts = itemStr.split(", ");
            if (parts.length < 2) continue;
            Material mat = Material.getMaterial(parts[0].toUpperCase());
            if (mat == null) continue;
            int amt;
            try { amt = Integer.parseInt(parts[1]); } catch (NumberFormatException e) { continue; }
            p.getInventory().addItem(new ItemStack(mat, amt));
            given++;
        }

        plugin.setKitCooldown(kitName, p, cooldownSecs);
        plugin.saveDataAsync();
        if (cost > 0) {
            p.sendMessage(ChatColor.GREEN + "Claimed kit '" + kitName + "' for " + symbol + String.format("%.2f", cost) + "!");
        } else {
            p.sendMessage(ChatColor.GREEN + "Claimed kit '" + kitName + "'!");
        }
    }

    // ════════════════════════════════════════════
    //  WARPS
    // ════════════════════════════════════════════

    private void handleWarp(Player p, String[] args, String symbol) {
        if (!plugin.getConfig().getBoolean("warps.enabled", true)) {
            p.sendMessage(ChatColor.RED + "Warps are disabled.");
            return;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("remove")) {
            if (!p.hasPermission("odseconomy.admin")) {
                p.sendMessage(ChatColor.RED + "Only admins can remove warps.");
                return;
            }
            String name = args[1].toLowerCase();
            if (plugin.warps.remove(name) != null) {
                plugin.saveDataAsync();
                p.sendMessage(ChatColor.GREEN + "Warp '" + name + "' removed.");
            } else {
                p.sendMessage(ChatColor.RED + "Warp '" + name + "' not found.");
            }
            return;
        }

        if (plugin.warps.isEmpty()) {
            p.sendMessage(ChatColor.RED + "No warps available.");
            return;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            p.sendMessage(ChatColor.GOLD + "=== Available Warps ===");
            for (String warpName : plugin.warps.keySet()) {
                Map<String, Object> wd = plugin.warps.get(warpName);
                double cost = wd.containsKey("cost") ? ((Number) wd.get("cost")).doubleValue() : 0.0;
                p.sendMessage(ChatColor.GREEN + warpName + ChatColor.GRAY + " - " + symbol + String.format("%.2f", cost));
            }
            return;
        }

        String warpName = args[0].toLowerCase();
        Map<String, Object> wd = plugin.warps.get(warpName);
        if (wd == null) {
            p.sendMessage(ChatColor.RED + "Warp '" + warpName + "' not found.");
            return;
        }

        double cost = wd.containsKey("cost") ? ((Number) wd.get("cost")).doubleValue() : 0.0;
        if (cost > 0 && !plugin.removeMoney(p.getUniqueId(), cost)) {
            p.sendMessage(ChatColor.RED + "Insufficient funds. Need " + symbol + String.format("%.2f", cost));
            return;
        }

        World world = Bukkit.getWorld((String) wd.get("world"));
        if (world == null) {
            p.sendMessage(ChatColor.RED + "Warp world not found.");
            return;
        }
        double x = ((Number) wd.get("x")).doubleValue();
        double y = ((Number) wd.get("y")).doubleValue();
        double z = ((Number) wd.get("z")).doubleValue();
        float yaw = wd.containsKey("yaw") ? ((Number) wd.get("yaw")).floatValue() : 0;
        float pitch = wd.containsKey("pitch") ? ((Number) wd.get("pitch")).floatValue() : 0;
        Location loc = new Location(world, x, y, z, yaw, pitch);
        p.teleport(loc);
        if (cost > 0) {
            p.sendMessage(ChatColor.GREEN + "Warped to '" + warpName + "' for " + symbol + String.format("%.2f", cost));
        } else {
            p.sendMessage(ChatColor.GREEN + "Warped to '" + warpName + "'.");
        }
    }

    // ════════════════════════════════════════════
    //  SHARED ACCOUNTS
    // ════════════════════════════════════════════

    private boolean handleSharedAccount(CommandSender sender, String[] args) {
        if (!plugin.getConfig().getBoolean("shared-accounts.enabled", false)) {
            sender.sendMessage(ChatColor.RED + "Shared accounts are disabled.");
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use shared accounts.");
            return true;
        }
        Player p = (Player) sender;
        if (!p.hasPermission("odseconomy.sharedaccount")) {
            p.sendMessage(ChatColor.RED + "You don't have permission.");
            return true;
        }
        String symbol = plugin.getCurrencySymbol();

        if (args.length < 1) {
            p.sendMessage(ChatColor.RED + "Usage: /sa <create|invite|balance|deposit|withdraw|list|info|setperm|setvisible>");
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "create": {
                if (args.length < 2) { p.sendMessage(ChatColor.RED + "Usage: /sa create <name>"); break; }
                String name = args[1].toLowerCase();
                if (plugin.sharedAccounts.containsKey(name)) {
                    p.sendMessage(ChatColor.RED + "An account with that name already exists.");
                    break;
                }
                int maxAccts = plugin.getConfig().getInt("shared-accounts.max-accounts-per-player", 5);
                long owned = plugin.sharedAccounts.values().stream().filter(a -> a.getOwner().equals(p.getUniqueId())).count();
                if (owned >= maxAccts) {
                    p.sendMessage(ChatColor.RED + "You can only own up to " + maxAccts + " shared accounts.");
                    break;
                }
                SharedAccount acct = new SharedAccount(name, p.getUniqueId());
                plugin.sharedAccounts.put(name, acct);
                plugin.saveDataAsync();
                p.sendMessage(ChatColor.GREEN + "Shared account '" + name + "' created!");
                break;
            }

            case "invite": {
                if (args.length < 3) { p.sendMessage(ChatColor.RED + "Usage: /sa invite <player> <name>"); break; }
                Player target = Bukkit.getPlayer(args[1]);
                String name = args[2].toLowerCase();
                SharedAccount acct = plugin.sharedAccounts.get(name);
                if (acct == null) { p.sendMessage(ChatColor.RED + "Account not found."); break; }
                if (!acct.getOwner().equals(p.getUniqueId())) { p.sendMessage(ChatColor.RED + "Only the owner can invite."); break; }
                if (target == null) { p.sendMessage(ChatColor.RED + "Player not found."); break; }
                int maxMembers = plugin.getConfig().getInt("shared-accounts.max-members-per-account", 10);
                if (acct.getMembers().size() >= maxMembers) {
                    p.sendMessage(ChatColor.RED + "Account has reached the max member limit.");
                    break;
                }
                acct.addMember(target.getUniqueId(), "view_only");
                plugin.saveDataAsync();
                p.sendMessage(ChatColor.GREEN + "Invited " + target.getName() + " to '" + name + "'.");
                target.sendMessage(ChatColor.GREEN + "You were invited to shared account '" + name + "' by " + p.getName() + ".");
                break;
            }

            case "balance": {
                String name = args.length >= 2 ? args[1].toLowerCase() : null;
                if (name == null) {
                    p.sendMessage(ChatColor.GOLD + "=== Your Shared Accounts ===");
                    for (SharedAccount a : plugin.sharedAccounts.values()) {
                        if (!a.canView(p.getUniqueId())) continue;
                        p.sendMessage(ChatColor.GREEN + a.getName() + ChatColor.GRAY + " - " + symbol + String.format("%.2f", a.getBalance()));
                    }
                    break;
                }
                SharedAccount acct = plugin.sharedAccounts.get(name);
                if (acct == null) { p.sendMessage(ChatColor.RED + "Account not found."); break; }
                if (!acct.canView(p.getUniqueId())) { p.sendMessage(ChatColor.RED + "You don't have access to this account."); break; }
                p.sendMessage(ChatColor.GOLD + acct.getName() + " balance: " + symbol + String.format("%.2f", acct.getBalance()));
                break;
            }

            case "deposit": {
                if (args.length < 3) { p.sendMessage(ChatColor.RED + "Usage: /sa deposit <name> <amount>"); break; }
                String name = args[1].toLowerCase();
                double amount;
                try { amount = Double.parseDouble(args[2]); } catch (NumberFormatException e) { p.sendMessage(ChatColor.RED + "Invalid amount."); break; }
                if (amount <= 0) { p.sendMessage(ChatColor.RED + "Amount must be positive."); break; }
                SharedAccount acct = plugin.sharedAccounts.get(name);
                if (acct == null) { p.sendMessage(ChatColor.RED + "Account not found."); break; }
                if (!acct.canDonate(p.getUniqueId())) { p.sendMessage(ChatColor.RED + "You don't have permission to deposit."); break; }
                if (!plugin.removeMoney(p.getUniqueId(), amount)) { p.sendMessage(ChatColor.RED + "Insufficient funds."); break; }
                acct.deposit(amount);
                plugin.saveDataAsync();
                p.sendMessage(ChatColor.GREEN + "Deposited " + symbol + String.format("%.2f", amount) + " into '" + name + "'.");
                break;
            }

            case "withdraw": {
                if (args.length < 3) { p.sendMessage(ChatColor.RED + "Usage: /sa withdraw <name> <amount>"); break; }
                String name = args[1].toLowerCase();
                double amount;
                try { amount = Double.parseDouble(args[2]); } catch (NumberFormatException e) { p.sendMessage(ChatColor.RED + "Invalid amount."); break; }
                if (amount <= 0) { p.sendMessage(ChatColor.RED + "Amount must be positive."); break; }
                SharedAccount acct = plugin.sharedAccounts.get(name);
                if (acct == null) { p.sendMessage(ChatColor.RED + "Account not found."); break; }
                if (!acct.canSpend(p.getUniqueId())) { p.sendMessage(ChatColor.RED + "You don't have permission to withdraw."); break; }
                if (!acct.withdraw(amount)) { p.sendMessage(ChatColor.RED + "Insufficient funds in account."); break; }
                plugin.addMoney(p.getUniqueId(), amount);
                plugin.saveDataAsync();
                p.sendMessage(ChatColor.GREEN + "Withdrew " + symbol + String.format("%.2f", amount) + " from '" + name + "'.");
                break;
            }

            case "list": {
                p.sendMessage(ChatColor.GOLD + "=== Your Shared Accounts ===");
                for (SharedAccount a : plugin.sharedAccounts.values()) {
                    if (!a.canView(p.getUniqueId())) continue;
                    String role = a.getOwner().equals(p.getUniqueId()) ? "&b[Owner]" : "&7[Member]";
                    p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a" + a.getName() + " &7- " + symbol + String.format("%.2f", a.getBalance()) + " " + role));
                }
                break;
            }

            case "info": {
                String name = args.length >= 2 ? args[1].toLowerCase() : null;
                if (name == null) { p.sendMessage(ChatColor.RED + "Usage: /sa info <name>"); break; }
                SharedAccount acct = plugin.sharedAccounts.get(name);
                if (acct == null) { p.sendMessage(ChatColor.RED + "Account not found."); break; }
                if (!acct.canView(p.getUniqueId())) { p.sendMessage(ChatColor.RED + "You don't have access."); break; }
                String ownerName = Bukkit.getOfflinePlayer(acct.getOwner()).getName();
                p.sendMessage(ChatColor.GOLD + "=== " + acct.getName() + " ===");
                p.sendMessage(ChatColor.GREEN + "Owner: " + ownerName);
                p.sendMessage(ChatColor.GREEN + "Balance: " + symbol + String.format("%.2f", acct.getBalance()));
                p.sendMessage(ChatColor.GREEN + "Visible: " + (acct.isVisible() ? "Yes" : "No"));
                if (!acct.getMembers().isEmpty()) {
                    p.sendMessage(ChatColor.GREEN + "Members:");
                    for (Map.Entry<UUID, String> m : acct.getMembers().entrySet()) {
                        String mName = Bukkit.getOfflinePlayer(m.getKey()).getName();
                        p.sendMessage(ChatColor.GRAY + "  " + mName + " - " + m.getValue());
                    }
                }
                break;
            }

            case "setperm": {
                if (args.length < 4) { p.sendMessage(ChatColor.RED + "Usage: /sa setperm <player> <name> <perm>"); break; }
                Player target = Bukkit.getPlayer(args[1]);
                String name = args[2].toLowerCase();
                String perm = args[3].toLowerCase();
                if (!perm.equals("donate_only") && !perm.equals("spend_only") && !perm.equals("view_only")) {
                    p.sendMessage(ChatColor.RED + "Permission must be: donate_only, spend_only, or view_only.");
                    break;
                }
                SharedAccount acct = plugin.sharedAccounts.get(name);
                if (acct == null) { p.sendMessage(ChatColor.RED + "Account not found."); break; }
                if (!acct.getOwner().equals(p.getUniqueId())) { p.sendMessage(ChatColor.RED + "Only the owner can change permissions."); break; }
                if (target == null) { p.sendMessage(ChatColor.RED + "Player not found."); break; }
                if (!acct.getMembers().containsKey(target.getUniqueId())) { p.sendMessage(ChatColor.RED + "That player is not a member."); break; }
                acct.addMember(target.getUniqueId(), perm);
                plugin.saveDataAsync();
                p.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s permission to " + perm + " on '" + name + "'.");
                target.sendMessage(ChatColor.GREEN + "Your permission on shared account '" + name + "' was changed to " + perm + ".");
                break;
            }

            case "setvisible": {
                if (args.length < 3) { p.sendMessage(ChatColor.RED + "Usage: /sa setvisible <name> <true|false>"); break; }
                String name = args[1].toLowerCase();
                SharedAccount acct = plugin.sharedAccounts.get(name);
                if (acct == null) { p.sendMessage(ChatColor.RED + "Account not found."); break; }
                if (!acct.getOwner().equals(p.getUniqueId())) { p.sendMessage(ChatColor.RED + "Only the owner can change visibility."); break; }
                boolean visible = args[2].equalsIgnoreCase("true");
                acct.setVisible(visible);
                plugin.saveDataAsync();
                p.sendMessage(ChatColor.GREEN + "Account '" + name + "' visibility set to " + visible + ".");
                break;
            }

            default:
                p.sendMessage(ChatColor.RED + "Usage: /sa <create|invite|balance|deposit|withdraw|list|info|setperm|setvisible>");
        }
        return true;
    }

    // ════════════════════════════════════════════
    //  DICE
    // ════════════════════════════════════════════

    private boolean handleDice(CommandSender sender, String[] args) {
        if (!plugin.getConfig().getBoolean("gambling.enabled", false)) {
            sender.sendMessage(ChatColor.RED + "Gambling is disabled.");
            return true;
        }
        Player target;
        double amount;
        if (sender instanceof Player) {
            if (args.length < 1) { sender.sendMessage(ChatColor.RED + "Usage: /dice <amount>"); return true; }
            target = (Player) sender;
            if (!target.hasPermission("odseconomy.dice")) { target.sendMessage(ChatColor.RED + "You don't have permission."); return true; }
            if (!plugin.isInGamblingLocation(target)) { target.sendMessage(ChatColor.RED + "You must be at a gambling location."); return true; }
            try { amount = Double.parseDouble(args[0]); } catch (NumberFormatException e) { target.sendMessage(ChatColor.RED + "Invalid amount."); return true; }
        } else {
            if (args.length < 2) { sender.sendMessage(ChatColor.RED + "Usage: /dice <player> <amount>"); return true; }
            target = Bukkit.getPlayer(args[0]);
            if (target == null) { sender.sendMessage(ChatColor.RED + "Player not found."); return true; }
            try { amount = Double.parseDouble(args[1]); } catch (NumberFormatException e) { sender.sendMessage(ChatColor.RED + "Invalid amount."); return true; }
        }
        if (amount <= 0) { target.sendMessage(ChatColor.RED + "Amount must be positive."); return true; }
        if (!plugin.removeMoney(target.getUniqueId(), amount)) { target.sendMessage(ChatColor.RED + "Insufficient funds."); return true; }
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

    // ════════════════════════════════════════════
    //  LOTTERY
    // ════════════════════════════════════════════

    private boolean handleLottery(CommandSender sender, String[] args) {
        if (!plugin.getConfig().getBoolean("lottery.enabled", false)) {
            sender.sendMessage(ChatColor.RED + "Lottery is disabled.");
            return true;
        }
        if (args.length < 1) { sender.sendMessage(ChatColor.RED + "Usage: /lottery buy [tickets] | /lottery info"); return true; }
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
            if (!(sender instanceof Player)) { sender.sendMessage(ChatColor.RED + "Only players can buy tickets."); return true; }
            Player p = (Player) sender;
            if (!p.hasPermission("odseconomy.lottery")) { p.sendMessage(ChatColor.RED + "You don't have permission."); return true; }
            int count = 1;
            if (args.length >= 2) {
                try { count = Integer.parseInt(args[1]); } catch (NumberFormatException e) { p.sendMessage(ChatColor.RED + "Invalid ticket count."); return true; }
            }
            if (count < 1) { p.sendMessage(ChatColor.RED + "Must buy at least 1 ticket."); return true; }
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

    // ════════════════════════════════════════════
    //  BOUNTY
    // ════════════════════════════════════════════

    private boolean handleBounty(CommandSender sender, String[] args) {
        if (!plugin.getConfig().getBoolean("bounties.enabled", false)) {
            sender.sendMessage(ChatColor.RED + "Bounties are disabled.");
            return true;
        }
        if (args.length < 1) { sender.sendMessage(ChatColor.RED + "Usage: /bounty set <player> <amount> | /bounty list [player]"); return true; }
        String sub = args[0].toLowerCase();
        if (sub.equals("set")) {
            if (!(sender instanceof Player)) { sender.sendMessage(ChatColor.RED + "Only players can place bounties."); return true; }
            Player p = (Player) sender;
            if (!p.hasPermission("odseconomy.bounty")) { p.sendMessage(ChatColor.RED + "You don't have permission."); return true; }
            if (args.length < 3) { p.sendMessage(ChatColor.RED + "Usage: /bounty set <player> <amount>"); return true; }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) { p.sendMessage(ChatColor.RED + "Player not found."); return true; }
            if (target.equals(p)) { p.sendMessage(ChatColor.RED + "You cannot place a bounty on yourself."); return true; }
            double amount;
            try { amount = Double.parseDouble(args[2]); } catch (NumberFormatException e) { p.sendMessage(ChatColor.RED + "Invalid amount."); return true; }
            double min = plugin.getConfig().getDouble("bounties.min-amount", 1.0);
            double max = plugin.getConfig().getDouble("bounties.max-amount", 100000.0);
            if (amount < min || amount > max) {
                p.sendMessage(ChatColor.RED + "Bounty must be between " + plugin.getCurrencySymbol() + String.format("%.2f", min) + " and " + plugin.getCurrencySymbol() + String.format("%.2f", max) + ".");
                return true;
            }
            if (!plugin.removeMoney(p.getUniqueId(), amount)) { p.sendMessage(ChatColor.RED + "Insufficient funds."); return true; }
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
            if (!sender.hasPermission("odseconomy.bounty")) { sender.sendMessage(ChatColor.RED + "You don't have permission."); return true; }
            if (args.length >= 2) {
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { sender.sendMessage(ChatColor.RED + "Player not found."); return true; }
                double bounty = plugin.bounties.getOrDefault(target.getUniqueId(), 0.0);
                sender.sendMessage(ChatColor.GOLD + target.getName() + "'s bounty: " + plugin.getCurrencySymbol() + String.format("%.2f", bounty));
            } else {
                if (plugin.bounties.isEmpty()) { sender.sendMessage(ChatColor.YELLOW + "No active bounties."); return true; }
                String sym = plugin.getCurrencySymbol();
                sender.sendMessage(ChatColor.GOLD + "=== Active Bounties ===");
                for (Map.Entry<UUID, Double> entry : plugin.bounties.entrySet()) {
                    String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                    sender.sendMessage(ChatColor.GREEN + (name == null ? "Unknown" : name) + " - " + sym + String.format("%.2f", entry.getValue()));
                }
            }
            return true;
        }
        sender.sendMessage(ChatColor.RED + "Usage: /bounty set <player> <amount> | /bounty list [player]");
        return true;
    }

    // ════════════════════════════════════════════
    //  BANK NOTE
    // ════════════════════════════════════════════

    private boolean handleNote(CommandSender sender, String[] args) {
        if (!plugin.getConfig().getBoolean("banknotes.enabled", false)) { sender.sendMessage(ChatColor.RED + "Banknotes are disabled."); return true; }
        if (!(sender instanceof Player)) { sender.sendMessage(ChatColor.RED + "Only players can create banknotes."); return true; }
        Player p = (Player) sender;
        if (!p.hasPermission("odseconomy.banknote")) { p.sendMessage(ChatColor.RED + "You don't have permission."); return true; }
        if (args.length < 1) { p.sendMessage(ChatColor.RED + "Usage: /mnote <amount>"); return true; }
        double amount;
        try { amount = Double.parseDouble(args[0]); } catch (NumberFormatException e) { p.sendMessage(ChatColor.RED + "Invalid amount."); return true; }
        double min = plugin.getConfig().getDouble("banknotes.min-amount", 1.0);
        double max = plugin.getConfig().getDouble("banknotes.max-amount", 100000.0);
        if (amount < min || amount > max) {
            p.sendMessage(ChatColor.RED + "Amount must be between " + plugin.getCurrencySymbol() + String.format("%.2f", min) + " and " + plugin.getCurrencySymbol() + String.format("%.2f", max) + ".");
            return true;
        }
        if (!plugin.removeMoney(p.getUniqueId(), amount)) { p.sendMessage(ChatColor.RED + "Insufficient funds."); return true; }
        ItemStack note = plugin.createBanknote(amount);
        p.getInventory().addItem(note);
        p.sendMessage(ChatColor.GREEN + "Created banknote for " + plugin.getCurrencySymbol() + String.format("%.2f", amount) + ".");
        return true;
    }

    // ════════════════════════════════════════════
    //  TRADE (GUI)
    // ════════════════════════════════════════════

    private boolean handleTrade(CommandSender sender, String[] args) {
        if (!plugin.getConfig().getBoolean("trades.enabled", true)) {
            sender.sendMessage(ChatColor.RED + "Trades are disabled.");
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can trade.");
            return true;
        }
        Player p = (Player) sender;
        if (!p.hasPermission("odseconomy.trade")) {
            p.sendMessage(ChatColor.RED + "You don't have permission.");
            return true;
        }
        if (args.length < 1) {
            p.sendMessage(ChatColor.RED + "Usage: /trade <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            p.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }
        if (target.equals(p)) {
            p.sendMessage(ChatColor.RED + "You cannot trade with yourself.");
            return true;
        }
        if (plugin.activeTrades.containsKey(p.getUniqueId()) || plugin.activeTrades.containsKey(target.getUniqueId())) {
            p.sendMessage(ChatColor.RED + "One of you is already in a trade.");
            return true;
        }

        TradeSession ts = new TradeSession(p, target, plugin);
        plugin.activeTrades.put(p.getUniqueId(), ts);
        plugin.activeTrades.put(target.getUniqueId(), ts);

        p.sendMessage(ChatColor.GREEN + "Trade started with " + target.getName() + "!");
        target.sendMessage(ChatColor.GREEN + p.getName() + " wants to trade with you!");
        return true;
    }

    // ════════════════════════════════════════════
    //  PERMISSION & TOGGLE CHECKS
    // ════════════════════════════════════════════

    private boolean checkToggleAndPermission(Player p, String cmd) {
        String permNode;
        switch (cmd) {
            case "balance": permNode = "odseconomy.balance"; break;
            case "baltop":  permNode = "odseconomy.baltop"; break;
            case "mtransfer": permNode = "odseconomy.pay"; break;
            case "sell":    permNode = "odseconomy.sell"; break;
            case "buy":     permNode = "odseconomy.buy"; break;
            case "worth":   permNode = "odseconomy.worth"; break;
            case "mcertification": permNode = "odseconomy.certification"; break;
            case "dorder":  permNode = "odseconomy.order"; break;
            case "dinquire": permNode = "odseconomy.inquire"; break;
            case "kit":     permNode = "odseconomy.kit"; break;
            case "warp":    permNode = "odseconomy.warp"; break;
            case "mgive": case "mregister": case "dgive": case "mreload":
            case "mset": case "mtake": case "mreset":
            case "setwarp": case "delwarp":
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
            case "buy":     return plugin.isFeatureEnabled("buy");
            case "mreset":  return plugin.isFeatureEnabled("mreset");
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
            case "buy":     permNode = "odseconomy.buy"; break;
            case "worth":   permNode = "odseconomy.worth"; break;
            case "certification": permNode = "odseconomy.certification"; break;
            case "order":   permNode = "odseconomy.order"; break;
            case "inquire": permNode = "odseconomy.inquire"; break;
            case "dice":    permNode = "odseconomy.dice"; break;
            case "lottery": permNode = "odseconomy.lottery"; break;
            case "bounty":  permNode = "odseconomy.bounty"; break;
            case "banknote": permNode = "odseconomy.banknote"; break;
            case "kit":     permNode = "odseconomy.kit"; break;
            case "warp":    permNode = "odseconomy.warp"; break;
            case "trade":   permNode = "odseconomy.trade"; break;
            case "sharedaccount": permNode = "odseconomy.sharedaccount"; break;
            default:        permNode = null;
        }
        if (permNode != null && !p.hasPermission(permNode)) return result;
        if (toggleKey.equals("dice") && !plugin.getConfig().getBoolean("gambling.enabled", false)) return result;
        if (toggleKey.equals("lottery") && !plugin.getConfig().getBoolean("lottery.enabled", false)) return result;
        if (toggleKey.equals("bounty") && !plugin.getConfig().getBoolean("bounties.enabled", false)) return result;
        if (toggleKey.equals("banknote") && !plugin.getConfig().getBoolean("banknotes.enabled", false)) return result;
        if (toggleKey.equals("kit") && (plugin.getConfig().getConfigurationSection("kits") == null || plugin.getConfig().getConfigurationSection("kits").getKeys(false).isEmpty())) return result;
        if (toggleKey.equals("warp") && !plugin.getConfig().getBoolean("warps.enabled", true)) return result;
        if (toggleKey.equals("trade") && !plugin.getConfig().getBoolean("trades.enabled", true)) return result;
        if (toggleKey.equals("sharedaccount") && !plugin.getConfig().getBoolean("shared-accounts.enabled", false)) return result;
        if (toggleKey.equals("balance") && !plugin.isFeatureEnabled("balance")) return result;
        if (toggleKey.equals("baltop") && !plugin.isFeatureEnabled("baltop")) return result;
        if (toggleKey.equals("pay") && !plugin.isFeatureEnabled("pay")) return result;
        if (toggleKey.equals("sell") && !plugin.isFeatureEnabled("sell")) return result;
        if (toggleKey.equals("buy") && !plugin.isFeatureEnabled("buy")) return result;
        if (toggleKey.equals("worth") && !plugin.isFeatureEnabled("worth")) return result;
        if (toggleKey.equals("certification") && !plugin.isFeatureEnabled("certification")) return result;
        if (toggleKey.equals("order") && !plugin.isFeatureEnabled("order")) return result;
        if (toggleKey.equals("inquire") && !plugin.isFeatureEnabled("inquire")) return result;
        result.add(ChatColor.GREEN + usage + ChatColor.GRAY + " - " + desc);
        return result;
    }

    // ════════════════════════════════════════════
    //  TAB COMPLETION
    // ════════════════════════════════════════════

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) return Collections.emptyList();
        Player p = (Player) sender;
        String cmd = command.getName().toLowerCase();

        if (cmd.equals("sharedaccount") || cmd.equals("sa")) {
            return tabSharedAccount(p, args);
        }
        if (cmd.equals("trade")) {
            return tabTrade(p, args);
        }
        if (!checkToggleAndPermission(p, cmd)) return Collections.emptyList();

        switch (cmd) {
            case "mgive": case "mset": case "mtake":
                if (args.length == 1)
                    return onlinePlayers(args[0]);
                if (args.length == 2) return List.of("<amount>");
                break;

            case "mtransfer":
                if (args.length == 1) return onlinePlayers(args[0]);
                if (args.length == 2) return List.of("<amount>");
                break;

            case "mreset":
                if (args.length == 1) return onlinePlayers(args[0]);
                break;

            case "balance":
                if (args.length == 1) return onlinePlayers(args[0]);
                break;

            case "mregister":
                if (args.length == 1) return List.of("<x_position>");
                if (args.length == 2) return List.of("<z_position>");
                if (args.length == 3) return onlinePlayers(args[2]);
                break;

            case "mcertification":
                if (args.length == 1) {
                    List<String> suggestions = new ArrayList<>(onlinePlayers(args[0]));
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
                if (args.length == 2) return onlinePlayers(args[1]);
                break;

            case "dorder":
                if (args.length == 1) {
                    List<String> suggestions = new ArrayList<>();
                    suggestions.add("confirm");
                    if (plugin.getConfig().contains("dealerships")) {
                        for (String dept : plugin.getConfig().getConfigurationSection("dealerships").getKeys(false)) {
                            String ownerUUID = plugin.getConfig().getString("dealerships." + dept + ".owner");
                            if (ownerUUID != null && ownerUUID.equals(p.getUniqueId().toString())) {
                                for (String itemStr : plugin.getConfig().getStringList("dealerships." + dept + ".items")) {
                                    String itemName = itemStr.split(", ")[0];
                                    if (!suggestions.contains(itemName)) suggestions.add(itemName);
                                }
                            }
                        }
                    }
                    return suggestions.stream().filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
                }
                if (args.length == 2) return List.of("<amount>");
                break;

            case "dinquire":
                if (args.length == 1) {
                    List<String> suggestions = new ArrayList<>(onlinePlayers(args[0]));
                    if (plugin.getConfig().contains("dealerships")) {
                        for (String dept : plugin.getConfig().getConfigurationSection("dealerships").getKeys(false)) {
                            if (dept.toLowerCase().startsWith(args[0].toLowerCase())) suggestions.add(dept);
                        }
                    }
                    return suggestions;
                }
                break;

            case "dice":
                if (args.length == 1) return List.of("<amount>");
                break;

            case "lottery":
                if (args.length == 1) return List.of("buy", "info").stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
                if (args.length == 2 && args[0].equalsIgnoreCase("buy")) return List.of("<tickets>");
                break;

            case "bounty":
                if (args.length == 1) return List.of("set", "list").stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
                if (args.length == 2 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("list"))) return onlinePlayers(args[1]);
                if (args.length == 3 && args[0].equalsIgnoreCase("set")) return List.of("<amount>");
                break;

            case "mnote":
                if (args.length == 1) return List.of("<amount>");
                break;

            case "buy":
                if (args.length == 1) return tabMaterials(plugin.buyPrices.keySet(), args[0]);
                if (args.length == 2) return List.of("<amount>");
                break;

            case "sell":
                if (args.length == 1) {
                    List<String> suggestions = new ArrayList<>(List.of("all"));
                    suggestions.addAll(tabMaterials(plugin.sellPrices.keySet(), args[0]));
                    return suggestions.stream().filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
                }
                break;

            case "kit":
                if (args.length == 1) {
                    List<String> kits = new ArrayList<>(List.of("list"));
                    if (plugin.getConfig().contains("kits")) {
                        for (String k : plugin.getConfig().getConfigurationSection("kits").getKeys(false)) {
                            String perm = plugin.getConfig().getString("kits." + k + ".permission", "");
                            if (perm.isEmpty() || p.hasPermission(perm)) kits.add(k);
                        }
                    }
                    return kits.stream().filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
                }
                break;

            case "warp":
                if (args.length == 1) {
                    List<String> warpSugs = new ArrayList<>(plugin.warps.keySet());
                    warpSugs.add("list");
                    if (p.hasPermission("odseconomy.admin")) warpSugs.add("remove");
                    return warpSugs.stream().filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
                }
                if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
                    return plugin.warps.keySet().stream().filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
                }
                break;

            case "setwarp":
                if (args.length == 1) return List.of("<name>");
                break;

            case "delwarp":
                if (args.length == 1) return plugin.warps.keySet().stream().filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
                break;
        }
        return Collections.emptyList();
    }

    private List<String> tabTrade(Player p, String[] args) {
        if (args.length == 1) return onlinePlayers(args[0]);
        return Collections.emptyList();
    }

    private List<String> tabSharedAccount(Player p, String[] args) {
        if (args.length == 1) {
            return List.of("create", "invite", "balance", "deposit", "withdraw", "list", "info", "setperm", "setvisible")
                    .stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        String sub = args[0].toLowerCase();
        if (args.length == 2) {
            switch (sub) {
                case "invite": case "setperm":
                    return onlinePlayers(args[1]);
                case "balance": case "deposit": case "withdraw": case "info":
                    return plugin.sharedAccounts.keySet().stream()
                            .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                case "setvisible":
                    return List.of("true", "false").stream().filter(s -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
            }
        }
        if (args.length == 3) {
            switch (sub) {
                case "deposit": case "withdraw":
                    return List.of("<amount>");
                case "setperm":
                    return plugin.sharedAccounts.keySet().stream()
                            .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
            }
        }
        if (args.length == 4 && sub.equals("setperm")) {
            return List.of("donate_only", "spend_only", "view_only").stream()
                    .filter(s -> s.startsWith(args[3].toLowerCase())).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private List<String> onlinePlayers(String prefix) {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                .filter(n -> n.toLowerCase().startsWith(prefix.toLowerCase())).collect(Collectors.toList());
    }

    private List<String> tabMaterials(java.util.Set<Material> materials, String prefix) {
        return materials.stream().map(m -> m.name().toLowerCase())
                .filter(n -> n.startsWith(prefix.toLowerCase())).collect(Collectors.toList());
    }
}
