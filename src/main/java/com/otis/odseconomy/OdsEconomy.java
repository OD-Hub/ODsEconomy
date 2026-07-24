package com.otis.odseconomy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public final class OdsEconomy extends JavaPlugin implements Listener {

    private File dataFile;
    private FileConfiguration dataConfig;

    public Map<UUID, Double> balances = new HashMap<>();
    public Map<String, UUID> landClaims = new HashMap<>();
    public Map<UUID, PendingOrder> pendingOrders = new HashMap<>();
    public Map<Material, Double> sellPrices = new HashMap<>();
    public Map<Material, Double> buyPrices = new HashMap<>();

    public Map<UUID, Double> bounties = new HashMap<>();
    public Map<UUID, Integer> lotteryTickets = new HashMap<>();
    public double lotteryPot;
    public long nextLotteryDrawTime;

    // Kit cooldowns: kit_name -> (uuid -> expiry_timestamp)
    public Map<String, Map<UUID, Long>> kitCooldowns = new HashMap<>();

    // Shared accounts: account_name -> SharedAccount
    public Map<String, SharedAccount> sharedAccounts = new HashMap<>();

    // Warps: warp_name -> properties map
    public Map<String, Map<String, Object>> warps = new HashMap<>();

    // Active GUI trades: player_uuid -> TradeSession
    public Map<UUID, TradeSession> activeTrades = new HashMap<>();

    private NamespacedKey banknoteKey;
    private File txLogFile;
    private DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private int interestTaskId = -1;
    private int lotteryTaskId = -1;

    private final Random random = new Random();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        banknoteKey = new NamespacedKey(this, "banknote-amount");
        loadData();
        setupSellPrices();
        setupBuyPrices();

        getServer().getPluginManager().registerEvents(this, this);

        CommandManager cmdManager = new CommandManager(this);
        String[] commands = {"mgive", "mtransfer", "sell", "buy", "mregister",
                "mcertification", "dgive", "dorder", "dinquire",
                "mreload", "mreset", "ecohelp", "balance", "baltop",
                "mset", "mtake", "worth", "dice", "lottery", "bounty",
                "mnote", "kit", "warp", "setwarp", "delwarp", "sharedaccount",
                "trade"};
        for (String cmd : commands) {
            getCommand(cmd).setExecutor(cmdManager);
            getCommand(cmd).setTabCompleter(cmdManager);
        }

        startTabListTask();
        startInterestTask();
        startLotteryTask();
        startTradeCleanupTask();
        getLogger().info("OD's Economy enabled successfully.");
    }

    @Override
    public void onDisable() {
        saveData();
        getLogger().info("OD's Economy disabled.");
    }

    // ──────────────── Events ────────────────

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!balances.containsKey(player.getUniqueId())) {
            double start = getConfig().getDouble("settings.starting-balance", 0.0);
            balances.put(player.getUniqueId(), start);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!getConfig().getBoolean("bounties.enabled", false)) return;
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer != null && bounties.containsKey(victim.getUniqueId())) {
            double reward = bounties.remove(victim.getUniqueId());
            addMoney(killer.getUniqueId(), reward);
            String sym = getCurrencySymbol();
            killer.sendMessage(ChatColor.GREEN + "You collected a bounty of " + sym + String.format("%.2f", reward) + " for killing " + victim.getName() + "!");
            if (getConfig().getBoolean("bounties.broadcast-collect", true)) {
                Bukkit.broadcastMessage(ChatColor.GOLD + victim.getName() + "'s bounty of " + sym + String.format("%.2f", reward) + " was collected by " + killer.getName() + "!");
            }
        }
    }

    // ──────────────── Trade GUI Events ────────────────

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player p = (Player) event.getWhoClicked();
        TradeSession ts = activeTrades.get(p.getUniqueId());
        if (ts == null) return;
        Inventory inv = event.getInventory();
        if (!TradeSession.isTradeInv(inv)) return;

        int slot = event.getSlot();
        boolean shift = event.isShiftClick();
        boolean right = event.getClick().isRightClick();

        // Money slot click
        if (slot == TradeSession.A_MONEY || slot == TradeSession.B_MONEY) {
            event.setCancelled(true);
            ts.handleMoneyClick(p, slot, shift, right);
            return;
        }

        // Everything else
        boolean allowed = ts.handleClick(p, slot);
        event.setCancelled(!allowed);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player p = (Player) event.getPlayer();
        TradeSession ts = activeTrades.get(p.getUniqueId());
        if (ts == null) return;
        if (!TradeSession.isTradeInv(event.getInventory())) return;
        ts.cancel();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        TradeSession ts = activeTrades.get(p.getUniqueId());
        if (ts != null) ts.cancel();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.getAction().name().contains("RIGHT_CLICK")) return;
        Player player = event.getPlayer();

        // Banknote deposit
        if (getConfig().getBoolean("banknotes.enabled", false)) {
            ItemStack item = event.getItem();
            if (item != null && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                if (meta.getPersistentDataContainer().has(banknoteKey, PersistentDataType.DOUBLE)) {
                    event.setCancelled(true);
                    double amount = meta.getPersistentDataContainer().get(banknoteKey, PersistentDataType.DOUBLE);
                    if (amount <= 0) return;
                    if (item.getAmount() > 1) {
                        player.sendMessage(ChatColor.RED + "Please split the banknotes before depositing.");
                        return;
                    }
                    addMoney(player.getUniqueId(), amount);
                    player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                    player.sendMessage(ChatColor.GREEN + "Deposited " + getCurrencySymbol() + String.format("%.2f", amount) + " from banknote.");
                    return;
                }
            }
        }

        // Sign shop interaction
        if (getConfig().getBoolean("sign-shops.enabled", false)) {
            Block block = event.getClickedBlock();
            if (block != null && block.getState() instanceof Sign) {
                Sign sign = (Sign) block.getState();
                String[] lines = sign.getLines();
                String type = ChatColor.stripColor(lines[0]).toLowerCase();
                if (type.equals("[buy]") || type.equals("[sell]")) {
                    event.setCancelled(true);
                    handleSignShop(player, lines);
                }
            }
        }
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        if (!getConfig().getBoolean("sign-shops.enabled", false)) return;
        Player player = event.getPlayer();
        String firstLine = ChatColor.stripColor(event.getLine(0));
        if (firstLine == null) return;
        String type = firstLine.toLowerCase();
        if (!type.equals("[buy]") && !type.equals("[sell]")) return;

        boolean allowAll = getConfig().getBoolean("sign-shops.allow-all", true);
        String reqPerm = getConfig().getString("sign-shops.create-permission", "odseconomy.signshop.create");
        if (!allowAll && !player.hasPermission(reqPerm)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You don't have permission to create shop signs.");
            return;
        }

        String amountStr = event.getLine(1);
        String priceStr = event.getLine(2);
        String materialStr = event.getLine(3);

        if (amountStr == null || priceStr == null || materialStr == null) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Format: [" + type + "]\n<amount>\n<price>\n<MATERIAL>");
            return;
        }

        try {
            int amount = Integer.parseInt(amountStr);
            double price = Double.parseDouble(priceStr);
            if (amount <= 0 || price <= 0) throw new NumberFormatException();
            Material mat = Material.getMaterial(materialStr.toUpperCase());
            if (mat == null) throw new IllegalArgumentException();
        } catch (Exception e) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Invalid sign shop format. Use: [" + type + "]\n<amount>\n<price>\n<MATERIAL>");
            return;
        }

        player.sendMessage(ChatColor.GREEN + "Shop sign created!");
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!getConfig().getBoolean("sign-shops.enabled", false)) return;
        Block block = event.getBlock();
        if (block.getState() instanceof Sign) {
            Sign sign = (Sign) block.getState();
            String firstLine = ChatColor.stripColor(sign.getLine(0));
            if (firstLine != null) {
                String type = firstLine.toLowerCase();
                if (type.equals("[buy]") || type.equals("[sell]")) {
                    Player player = event.getPlayer();
                    if (!player.hasPermission("odseconomy.admin")) {
                        player.sendMessage(ChatColor.RED + "Only admins can break shop signs.");
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    private void handleSignShop(Player player, String[] lines) {
        try {
            String type = ChatColor.stripColor(lines[0]).toLowerCase();
            int amount = Integer.parseInt(lines[1]);
            double price = Double.parseDouble(lines[2]);
            Material mat = Material.getMaterial(lines[3].toUpperCase());

            if (mat == null || amount <= 0 || price <= 0) return;
            String sym = getCurrencySymbol();

            if (type.equals("[buy]")) {
                ItemStack stack = new ItemStack(mat, amount);
                int maxStack = mat.getMaxStackSize();
                int neededSlots = (amount + maxStack - 1) / maxStack;
                int empty = 0;
                for (ItemStack item : player.getInventory().getStorageContents()) {
                    if (item == null || item.getType() == Material.AIR) empty++;
                }
                if (empty < neededSlots) {
                    player.sendMessage(ChatColor.RED + "Not enough inventory space.");
                    return;
                }
                if (!removeMoney(player.getUniqueId(), price)) {
                    player.sendMessage(ChatColor.RED + "Insufficient funds. Need " + sym + String.format("%.2f", price));
                    return;
                }
                player.getInventory().addItem(stack);
                player.sendMessage(ChatColor.GREEN + "Bought " + amount + " " + mat.name() + " for " + sym + String.format("%.2f", price));
            } else if (type.equals("[sell]")) {
                int count = 0;
                for (ItemStack item : player.getInventory().getStorageContents()) {
                    if (item != null && item.getType() == mat && !item.hasItemMeta()) {
                        count += item.getAmount();
                    }
                }
                if (count < amount) {
                    player.sendMessage(ChatColor.RED + "You don't have enough " + mat.name() + ". Need " + amount + ", have " + count);
                    return;
                }
                int toRemove = amount;
                ItemStack[] contents = player.getInventory().getStorageContents();
                for (int i = 0; i < contents.length && toRemove > 0; i++) {
                    ItemStack item = contents[i];
                    if (item != null && item.getType() == mat && !item.hasItemMeta()) {
                        int taken = Math.min(toRemove, item.getAmount());
                        item.setAmount(item.getAmount() - taken);
                        toRemove -= taken;
                        if (item.getAmount() <= 0) {
                            player.getInventory().setItem(i, new ItemStack(Material.AIR));
                        }
                    }
                }
                addMoney(player.getUniqueId(), price);
                player.sendMessage(ChatColor.GREEN + "Sold " + amount + " " + mat.name() + " for " + sym + String.format("%.2f", price));
            }
        } catch (Exception ignored) {}
    }

    // ──────────────── Scheduled Tasks ────────────────

    private void startTabListTask() {
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (!getConfig().getBoolean("settings.tablist-balance", true)) return;
            for (Player player : Bukkit.getOnlinePlayers()) {
                double balance = balances.getOrDefault(player.getUniqueId(), 0.0);
                player.setPlayerListName(player.getName() + ChatColor.GREEN + " [" + getCurrencySymbol() + String.format("%.2f", balance) + "]");
            }
        }, 0L, 40L);
    }

    private void startInterestTask() {
        if (interestTaskId != -1) {
            Bukkit.getScheduler().cancelTask(interestTaskId);
        }
        interestTaskId = Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (!getConfig().getBoolean("interest.enabled", false)) return;
            double rate = getConfig().getDouble("interest.rate", 0.01);
            double minBal = getConfig().getDouble("interest.min-balance", 0.0);
            double maxBal = getConfig().getDouble("interest.max-balance", -1.0);
            String sym = getCurrencySymbol();
            for (Player player : Bukkit.getOnlinePlayers()) {
                double bal = balances.getOrDefault(player.getUniqueId(), 0.0);
                if (bal < minBal) continue;
                if (maxBal > 0 && bal > maxBal) continue;
                double interest = bal * rate;
                addMoney(player.getUniqueId(), interest);
                player.sendMessage(ChatColor.GREEN + "You earned " + sym + String.format("%.2f", interest) + " in interest.");
            }
        }, 0L, getConfig().getLong("interest.interval-seconds", 3600) * 20L).getTaskId();
    }

    private void startLotteryTask() {
        if (lotteryTaskId != -1) {
            Bukkit.getScheduler().cancelTask(lotteryTaskId);
        }
        lotteryTaskId = Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (!getConfig().getBoolean("lottery.enabled", false)) return;
            if (!getConfig().getBoolean("lottery.auto-draw", true)) return;
            long now = System.currentTimeMillis() / 1000;
            long drawInterval = getConfig().getLong("lottery.draw-interval-seconds", 86400);
            long warningSecs = getConfig().getLong("lottery.countdown-warning-seconds", 3600);

            if (nextLotteryDrawTime <= 0) {
                nextLotteryDrawTime = now + drawInterval;
                saveData();
                return;
            }

            if (warningSecs > 0 && nextLotteryDrawTime - now <= warningSecs && nextLotteryDrawTime - now > 0) {
                long mins = (nextLotteryDrawTime - now) / 60;
                if (mins > 0 && mins % 10 == 0) {
                    Bukkit.broadcastMessage(ChatColor.GOLD + "Lottery draw in " + mins + " minutes! Pot: " + getCurrencySymbol() + String.format("%.2f", lotteryPot));
                }
            }

            if (now < nextLotteryDrawTime) return;

            int totalTickets = lotteryTickets.values().stream().mapToInt(Integer::intValue).sum();
            int minTickets = getConfig().getInt("lottery.min-tickets-to-draw", 1);

            if (totalTickets < minTickets) {
                Bukkit.broadcastMessage(ChatColor.YELLOW + "Not enough tickets sold for a lottery draw. The pot carries over.");
                nextLotteryDrawTime = now + drawInterval;
                saveData();
                return;
            }

            UUID winner = drawLotteryWinner();
            if (winner == null) {
                nextLotteryDrawTime = now + drawInterval;
                saveData();
                return;
            }

            double houseCutPercent = getConfig().getDouble("lottery.house-cut-percent", 10);
            double houseCut = lotteryPot * (houseCutPercent / 100.0);
            String houseDest = getConfig().getString("lottery.house-cut-destination", "");
            double winnings = lotteryPot - houseCut;

            if (houseDest.equals("jackpot")) {
            } else if (!houseDest.isEmpty()) {
                try {
                    UUID targetUUID = UUID.fromString(houseDest);
                    addMoney(targetUUID, houseCut);
                } catch (IllegalArgumentException ignored) {}
            }

            addMoney(winner, winnings);
            String winnerName = Bukkit.getOfflinePlayer(winner).getName();
            String sym = getCurrencySymbol();

            if (getConfig().getBoolean("lottery.notify-winner", true)) {
                Player wp = Bukkit.getPlayer(winner);
                if (wp != null) wp.sendMessage(ChatColor.GOLD + "You won the lottery! " + sym + String.format("%.2f", winnings) + " has been added to your balance.");
            }
            if (getConfig().getBoolean("lottery.broadcast-winner", true)) {
                Bukkit.broadcastMessage(ChatColor.GOLD + winnerName + " won the lottery and received " + sym + String.format("%.2f", winnings) + "!");
            }

            lotteryTickets.clear();
            lotteryPot = getConfig().getDouble("lottery.starting-pot", 0.0);
            nextLotteryDrawTime = now + drawInterval;
            saveData();
        }, 0L, 20L).getTaskId();
    }

    private UUID drawLotteryWinner() {
        int totalTickets = lotteryTickets.values().stream().mapToInt(Integer::intValue).sum();
        if (totalTickets <= 0) return null;
        int roll = random.nextInt(totalTickets);
        int cumulative = 0;
        for (Map.Entry<UUID, Integer> entry : lotteryTickets.entrySet()) {
            cumulative += entry.getValue();
            if (roll < cumulative) return entry.getKey();
        }
        return null;
    }

    private void startTradeCleanupTask() {
        // No periodic cleanup needed for GUI trades; they're cleaned on close/quit.
    }

    // ──────────────── Money Helpers ────────────────

    public void setMoney(UUID uuid, double amount) {
        double old = balances.getOrDefault(uuid, 0.0);
        balances.put(uuid, Math.max(0, amount));
        logTransaction(uuid, "SET", amount - old, String.format("%.2f -> %.2f", old, amount));
    }

    public void addMoney(UUID uuid, double amount) {
        balances.put(uuid, balances.getOrDefault(uuid, 0.0) + amount);
        logTransaction(uuid, "ADD", amount, "");
    }

    public boolean removeMoney(UUID uuid, double amount) {
        double current = balances.getOrDefault(uuid, 0.0);
        if (current >= amount) {
            balances.put(uuid, current - amount);
            logTransaction(uuid, "REMOVE", amount, "");
            return true;
        }
        return false;
    }

    // ──────────────── Kit Helpers ────────────────

    public boolean checkKitCooldown(String kitName, Player player) {
        if (!kitCooldowns.containsKey(kitName)) return true;
        Map<UUID, Long> cooldowns = kitCooldowns.get(kitName);
        if (!cooldowns.containsKey(player.getUniqueId())) return true;
        long expiry = cooldowns.get(player.getUniqueId());
        if (System.currentTimeMillis() < expiry) return false;
        cooldowns.remove(player.getUniqueId());
        return true;
    }

    public long getKitCooldownRemaining(String kitName, Player player) {
        if (!kitCooldowns.containsKey(kitName)) return 0;
        Map<UUID, Long> cooldowns = kitCooldowns.get(kitName);
        if (!cooldowns.containsKey(player.getUniqueId())) return 0;
        long remaining = cooldowns.get(player.getUniqueId()) - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    public void setKitCooldown(String kitName, Player player, long cooldownSeconds) {
        if (cooldownSeconds <= 0) return;
        kitCooldowns.computeIfAbsent(kitName, k -> new HashMap<>())
                .put(player.getUniqueId(), System.currentTimeMillis() + (cooldownSeconds * 1000));
    }

    // ──────────────── Lottery Helpers ────────────────

    public boolean buyLotteryTickets(Player player, int count) {
        double price = getConfig().getDouble("lottery.ticket-price", 100.0);
        double cost = price * count;
        int maxTickets = getConfig().getInt("lottery.max-tickets-per-player", 100);
        int currentTickets = lotteryTickets.getOrDefault(player.getUniqueId(), 0);
        if (currentTickets + count > maxTickets) return false;
        if (!removeMoney(player.getUniqueId(), cost)) return false;
        lotteryTickets.put(player.getUniqueId(), currentTickets + count);
        lotteryPot += cost;
        logTransaction(player.getUniqueId(), "LOTTERY_BUY", cost, count + " tickets");
        return true;
    }

    // ──────────────── Gambling Location Check ────────────────

    public boolean isInGamblingLocation(Player player) {
        if (!getConfig().getBoolean("gambling.require-location", false)) return true;
        List<Map<?, ?>> locations = getConfig().getMapList("gambling.locations");
        if (locations.isEmpty()) return true;
        for (Map<?, ?> locMap : locations) {
            String worldName = (String) locMap.get("world");
            if (worldName == null) continue;
            World world = Bukkit.getWorld(worldName);
            if (world == null || !player.getWorld().equals(world)) continue;
            Object rawX = locMap.get("x"); double lx = rawX instanceof Number ? ((Number) rawX).doubleValue() : 0.0;
            Object rawY = locMap.get("y"); double ly = rawY instanceof Number ? ((Number) rawY).doubleValue() : 0.0;
            Object rawZ = locMap.get("z"); double lz = rawZ instanceof Number ? ((Number) rawZ).doubleValue() : 0.0;
            Object rawR = locMap.get("radius"); double radius = rawR instanceof Number ? ((Number) rawR).doubleValue() : 10.0;
            Location loc = player.getLocation();
            double dx = loc.getX() - lx;
            double dy = loc.getY() - ly;
            double dz = loc.getZ() - lz;
            if (dx * dx + dy * dy + dz * dz <= radius * radius) return true;
        }
        return false;
    }

    // ──────────────── Banknote ────────────────

    public ItemStack createBanknote(double amount) {
        String materialName = getConfig().getString("banknotes.item-material", "PAPER");
        Material mat = Material.getMaterial(materialName.toUpperCase());
        if (mat == null) mat = Material.PAPER;
        ItemStack item = new ItemStack(mat, 1);
        ItemMeta meta = item.getItemMeta();
        String formatted = getCurrencySymbol() + String.format("%.2f", amount);
        String displayName = ChatColor.translateAlternateColorCodes('&', getConfig().getString("banknotes.item-name", "&6Bank Note &7(%amount%)"));
        meta.setDisplayName(displayName.replace("%amount%", formatted));
        List<String> loreTemplate = getConfig().getStringList("banknotes.item-lore");
        List<String> lore = new ArrayList<>();
        for (String line : loreTemplate) {
            lore.add(ChatColor.translateAlternateColorCodes('&', line.replace("%amount%", formatted)));
        }
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(banknoteKey, PersistentDataType.DOUBLE, amount);
        item.setItemMeta(meta);
        return item;
    }

    // ──────────────── Config / Misc ────────────────

    public String getCurrencySymbol() {
        return getConfig().getString("settings.currency-symbol", "$");
    }

    public boolean isFeatureEnabled(String path) {
        return getConfig().getBoolean("settings.enable-command-" + path, true);
    }

    public void reloadPluginConfig() {
        reloadConfig();
        setupSellPrices();
        setupBuyPrices();
        startInterestTask();
        startLotteryTask();
        getLogger().info("Configuration reloaded.");
    }

    private void setupSellPrices() {
        sellPrices.clear();
        if (getConfig().contains("sell-prices")) {
            for (String key : getConfig().getConfigurationSection("sell-prices").getKeys(false)) {
                Material mat = Material.getMaterial(key);
                if (mat != null) {
                    sellPrices.put(mat, getConfig().getDouble("sell-prices." + key));
                }
            }
        }
        if (sellPrices.isEmpty()) {
            sellPrices.put(Material.DIAMOND, 6.0);
            sellPrices.put(Material.NETHERITE_INGOT, 120.0);
            sellPrices.put(Material.ENDER_PEARL, 3.0);
            sellPrices.put(Material.GOLDEN_APPLE, 16.0);
            sellPrices.put(Material.IRON_INGOT, 0.25);
            sellPrices.put(Material.GOLD_INGOT, 2.0);
            sellPrices.put(Material.ENCHANTED_GOLDEN_APPLE, 120.0);
            sellPrices.put(Material.TOTEM_OF_UNDYING, 144.0);
            sellPrices.put(Material.ELYTRA, 384.0);
            sellPrices.put(Material.SHULKER_BOX, 48.0);
            sellPrices.put(Material.DRIED_KELP_BLOCK, 2.0);
            sellPrices.put(Material.HONEY_BOTTLE, 4.0);
            sellPrices.put(Material.SHORT_GRASS, 1.0 / 32.0);
        }
        getLogger().info("Loaded " + sellPrices.size() + " sellable items.");
    }

    private void setupBuyPrices() {
        buyPrices.clear();
        if (getConfig().contains("buy-prices")) {
            for (String key : getConfig().getConfigurationSection("buy-prices").getKeys(false)) {
                Material mat = Material.getMaterial(key);
                if (mat != null) {
                    buyPrices.put(mat, getConfig().getDouble("buy-prices." + key));
                }
            }
        }
        getLogger().info("Loaded " + buyPrices.size() + " buyable items.");
    }

    public Random getRandom() {
        return random;
    }

    public static String chunkKeyFromBlock(int blockX, int blockZ) {
        int chunkX = blockX >> 4;
        int chunkZ = blockZ >> 4;
        int midX = chunkX * 16 + 8;
        int midZ = chunkZ * 16 + 8;
        return midX + "," + midZ;
    }

    // ──────────────── Transaction Log ────────────────

    private void logTransaction(UUID player, String type, double amount, String details) {
        if (!getConfig().getBoolean("transaction-log.enabled", false)) return;
        if (txLogFile == null) {
            String filename = getConfig().getString("transaction-log.log-file", "transactions.log");
            txLogFile = new File(getDataFolder(), filename);
        }
        try (PrintWriter pw = new PrintWriter(new FileWriter(txLogFile, true))) {
            String ts = LocalDateTime.now().format(dtf);
            String name = Bukkit.getOfflinePlayer(player).getName();
            String sym = getCurrencySymbol();
            if ("CSV".equalsIgnoreCase(getConfig().getString("transaction-log.format", "PLAIN"))) {
                pw.println(ts + "," + player + "," + name + "," + type + "," + amount + ",\"" + details + "\"");
            } else {
                pw.println("[" + ts + "] " + name + " (" + player + ") " + type + " " + sym + String.format("%.2f", amount) + " " + details);
            }
        } catch (IOException e) {
            getLogger().warning("Failed to write transaction log: " + e.getMessage());
        }
    }

    // ──────────────── Data Persistence ────────────────

    @SuppressWarnings("unchecked")
    private void loadData() {
        dataFile = new File(getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            try { dataFile.createNewFile(); } catch (IOException ignored) {}
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        if (dataConfig.contains("balances")) {
            for (String key : dataConfig.getConfigurationSection("balances").getKeys(false)) {
                balances.put(UUID.fromString(key), dataConfig.getDouble("balances." + key));
            }
        }
        if (dataConfig.contains("land")) {
            for (String key : dataConfig.getConfigurationSection("land").getKeys(false)) {
                landClaims.put(key, UUID.fromString(dataConfig.getString("land." + key)));
            }
        }
        if (dataConfig.contains("bounties")) {
            for (String key : dataConfig.getConfigurationSection("bounties").getKeys(false)) {
                bounties.put(UUID.fromString(key), dataConfig.getDouble("bounties." + key));
            }
        }
        if (dataConfig.contains("lottery-tickets")) {
            for (String key : dataConfig.getConfigurationSection("lottery-tickets").getKeys(false)) {
                lotteryTickets.put(UUID.fromString(key), dataConfig.getInt("lottery-tickets." + key));
            }
        }
        lotteryPot = dataConfig.getDouble("lottery-pot", getConfig().getDouble("lottery.starting-pot", 0.0));
        nextLotteryDrawTime = dataConfig.getLong("lottery-next-draw", 0);

        // Kit cooldowns
        if (dataConfig.contains("kit-cooldowns")) {
            for (String kitName : dataConfig.getConfigurationSection("kit-cooldowns").getKeys(false)) {
                Map<UUID, Long> cooldowns = new HashMap<>();
                String path = "kit-cooldowns." + kitName;
                for (String uuidStr : dataConfig.getConfigurationSection(path).getKeys(false)) {
                    cooldowns.put(UUID.fromString(uuidStr), dataConfig.getLong(path + "." + uuidStr));
                }
                kitCooldowns.put(kitName, cooldowns);
            }
        }

        // Warps
        if (dataConfig.contains("warps")) {
            for (String warpName : dataConfig.getConfigurationSection("warps").getKeys(false)) {
                String base = "warps." + warpName;
                Map<String, Object> warpData = new HashMap<>();
                warpData.put("world", dataConfig.getString(base + ".world"));
                warpData.put("x", dataConfig.getDouble(base + ".x"));
                warpData.put("y", dataConfig.getDouble(base + ".y"));
                warpData.put("z", dataConfig.getDouble(base + ".z"));
                warpData.put("yaw", (float) dataConfig.getDouble(base + ".yaw"));
                warpData.put("pitch", (float) dataConfig.getDouble(base + ".pitch"));
                warpData.put("cost", dataConfig.getDouble(base + ".cost"));
                warps.put(warpName, warpData);
            }
        }

        // Load default warps from config
        if (getConfig().contains("warps.defaults")) {
            for (String key : getConfig().getConfigurationSection("warps.defaults").getKeys(false)) {
                if (!warps.containsKey(key)) {
                    String base = "warps.defaults." + key;
                    Map<String, Object> warpData = new HashMap<>();
                    warpData.put("world", getConfig().getString(base + ".world"));
                    warpData.put("x", getConfig().getDouble(base + ".x"));
                    warpData.put("y", getConfig().getDouble(base + ".y"));
                    warpData.put("z", getConfig().getDouble(base + ".z"));
                    warpData.put("yaw", (float) getConfig().getDouble(base + ".yaw"));
                    warpData.put("pitch", (float) getConfig().getDouble(base + ".pitch"));
                    warpData.put("cost", getConfig().getDouble(base + ".cost"));
                    warps.put(key, warpData);
                }
            }
        }

        // Shared accounts
        if (dataConfig.contains("shared-accounts")) {
            for (String acctName : dataConfig.getConfigurationSection("shared-accounts").getKeys(false)) {
                String base = "shared-accounts." + acctName;
                UUID owner = UUID.fromString(dataConfig.getString(base + ".owner"));
                SharedAccount acct = new SharedAccount(acctName, owner);
                acct.setBalance(dataConfig.getDouble(base + ".balance"));
                acct.setVisible(dataConfig.getBoolean(base + ".visible", true));
                if (dataConfig.contains(base + ".members")) {
                    for (String uuidStr : dataConfig.getConfigurationSection(base + ".members").getKeys(false)) {
                        acct.addMember(UUID.fromString(uuidStr), dataConfig.getString(base + ".members." + uuidStr));
                    }
                }
                sharedAccounts.put(acctName.toLowerCase(), acct);
            }
        }
    }

    public void saveData() {
        dataConfig.set("balances", null);
        for (Map.Entry<UUID, Double> entry : balances.entrySet()) {
            dataConfig.set("balances." + entry.getKey().toString(), entry.getValue());
        }
        dataConfig.set("land", null);
        for (Map.Entry<String, UUID> entry : landClaims.entrySet()) {
            dataConfig.set("land." + entry.getKey(), entry.getValue().toString());
        }
        dataConfig.set("bounties", null);
        for (Map.Entry<UUID, Double> entry : bounties.entrySet()) {
            dataConfig.set("bounties." + entry.getKey().toString(), entry.getValue());
        }
        dataConfig.set("lottery-tickets", null);
        for (Map.Entry<UUID, Integer> entry : lotteryTickets.entrySet()) {
            dataConfig.set("lottery-tickets." + entry.getKey().toString(), entry.getValue());
        }
        dataConfig.set("lottery-pot", lotteryPot);
        dataConfig.set("lottery-next-draw", nextLotteryDrawTime);

        // Kit cooldowns
        dataConfig.set("kit-cooldowns", null);
        for (Map.Entry<String, Map<UUID, Long>> kitEntry : kitCooldowns.entrySet()) {
            String base = "kit-cooldowns." + kitEntry.getKey();
            for (Map.Entry<UUID, Long> entry : kitEntry.getValue().entrySet()) {
                dataConfig.set(base + "." + entry.getKey().toString(), entry.getValue());
            }
        }

        // Warps
        dataConfig.set("warps", null);
        for (Map.Entry<String, Map<String, Object>> warpEntry : warps.entrySet()) {
            String base = "warps." + warpEntry.getKey();
            Map<String, Object> wd = warpEntry.getValue();
            dataConfig.set(base + ".world", wd.get("world"));
            dataConfig.set(base + ".x", wd.get("x"));
            dataConfig.set(base + ".y", wd.get("y"));
            dataConfig.set(base + ".z", wd.get("z"));
            dataConfig.set(base + ".yaw", wd.get("yaw"));
            dataConfig.set(base + ".pitch", wd.get("pitch"));
            dataConfig.set(base + ".cost", wd.get("cost"));
        }

        // Shared accounts
        dataConfig.set("shared-accounts", null);
        for (Map.Entry<String, SharedAccount> entry : sharedAccounts.entrySet()) {
            SharedAccount acct = entry.getValue();
            String base = "shared-accounts." + entry.getKey();
            dataConfig.set(base + ".owner", acct.getOwner().toString());
            dataConfig.set(base + ".balance", acct.getBalance());
            dataConfig.set(base + ".visible", acct.isVisible());
            for (Map.Entry<UUID, String> member : acct.getMembers().entrySet()) {
                dataConfig.set(base + ".members." + member.getKey().toString(), member.getValue());
            }
        }

        try { dataConfig.save(dataFile); } catch (IOException ignored) {}
    }

    public void saveDataAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(this, this::saveData);
    }
}
