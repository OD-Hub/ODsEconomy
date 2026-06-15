package com.otis.odseconomy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
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

    public Map<UUID, Double> bounties = new HashMap<>();
    public Map<UUID, Integer> lotteryTickets = new HashMap<>();
    public double lotteryPot;
    public long nextLotteryDrawTime;

    private NamespacedKey banknoteKey;
    private File txLogFile;
    private boolean txLogEnabled;
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

        getServer().getPluginManager().registerEvents(this, this);

        CommandManager cmdManager = new CommandManager(this);
        String[] commands = {"mgive", "mtransfer", "sell", "mregister", "mcertification",
                "dgive", "dorder", "dinquire", "mreload", "ecohelp",
                "balance", "baltop", "mset", "mtake", "worth",
                "dice", "lottery", "bounty", "mnote"};
        for (String cmd : commands) {
            getCommand(cmd).setExecutor(cmdManager);
            getCommand(cmd).setTabCompleter(cmdManager);
        }

        startTabListTask();
        startInterestTask();
        startLotteryTask();
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

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!getConfig().getBoolean("banknotes.enabled", false)) return;
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) return;
        if (!item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        if (!meta.getPersistentDataContainer().has(banknoteKey, PersistentDataType.DOUBLE)) return;
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
                // keep in pot for next draw
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

    public boolean isBanknote(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(banknoteKey, PersistentDataType.DOUBLE);
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
        try { dataConfig.save(dataFile); } catch (IOException ignored) {}
    }

    public void saveDataAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(this, this::saveData);
    }
}
