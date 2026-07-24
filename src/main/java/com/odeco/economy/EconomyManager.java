package com.odeco.economy;

import com.odeco.ODEco;
import com.odeco.config.ConfigManager;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class EconomyManager {

    private final ODEco plugin;
    private final ConfigManager config;

    private final Map<UUID, Double> balances = new ConcurrentHashMap<>();
    private final Map<UUID, BountyEntry> bounties = new ConcurrentHashMap<>();
    private final Map<String, SharedAccount> sharedAccounts = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastInterest = new ConcurrentHashMap<>();
    private final List<TransactionEntry> transactionLog = new ArrayList<>();
    private final List<PendingOrder> pendingOrders = new ArrayList<>();

    private double lotteryPot = 0;
    private final Set<UUID> lotteryEntries = new HashSet<>();
    private UUID lastLotteryWinner = null;
    private double lastLotteryPrize = 0;

    private final Map<UUID, DiceGame> activeDiceGames = new ConcurrentHashMap<>();

    // Tax system
    private final Map<UUID, Double> taxDebt = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastTaxAssessment = new ConcurrentHashMap<>();
    private final Map<UUID, Double> incomeSinceLastTax = new ConcurrentHashMap<>();
    private final Map<UUID, Double> assessedTaxAmount = new ConcurrentHashMap<>();
    private final Map<UUID, Long> graceOfflineTime = new ConcurrentHashMap<>();
    private final Map<UUID, Long> graceOfflineSince = new ConcurrentHashMap<>();

    private File dataFile;
    private FileConfiguration data;

    public EconomyManager(ODEco plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        loadData();
    }

    // ═══════════════════════════════════════════
    //  Data Persistence
    // ═══════════════════════════════════════════

    public void loadData() {
        dataFile = new File(plugin.getDataFolder(), "economy-data.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create economy-data.yml: " + e.getMessage());
            }
        }
        data = YamlConfiguration.loadConfiguration(dataFile);

        balances.clear();
        if (data.contains("balances")) {
            for (String key : data.getConfigurationSection("balances").getKeys(false)) {
                try {
                    balances.put(UUID.fromString(key), data.getDouble("balances." + key));
                } catch (IllegalArgumentException ignored) {}
            }
        }

        bounties.clear();
        if (data.contains("bounties")) {
            for (String key : data.getConfigurationSection("bounties").getKeys(false)) {
                try {
                    UUID targetId = UUID.fromString(key);
                    double amount = data.getDouble("bounties." + key + ".amount");
                    UUID placerId = UUID.fromString(data.getString("bounties." + key + ".placer", key));
                    boolean anonymous = data.getBoolean("bounties." + key + ".anonymous", false);
                    long placedAt = data.getLong("bounties." + key + ".placed-at", System.currentTimeMillis());
                    bounties.put(targetId, new BountyEntry(targetId, placerId, amount, anonymous, placedAt));
                } catch (Exception ignored) {}
            }
        }

        sharedAccounts.clear();
        if (data.contains("shared-accounts")) {
            for (String name : data.getConfigurationSection("shared-accounts").getKeys(false)) {
                try {
                    sharedAccounts.put(name, SharedAccount.deserialize(name, data.getConfigurationSection("shared-accounts." + name).getValues(false)));
                } catch (Exception ignored) {}
            }
        }

        lastInterest.clear();
        if (data.contains("last-interest")) {
            for (String key : data.getConfigurationSection("last-interest").getKeys(false)) {
                try {
                    lastInterest.put(UUID.fromString(key), data.getLong("last-interest." + key));
                } catch (IllegalArgumentException ignored) {}
            }
        }

        lotteryPot = data.getDouble("lottery.pot", 0);
        lotteryEntries.clear();
        if (data.contains("lottery.entries")) {
            for (String uuidStr : data.getStringList("lottery.entries")) {
                try {
                    lotteryEntries.add(UUID.fromString(uuidStr));
                } catch (IllegalArgumentException ignored) {}
            }
        }
        if (data.contains("lottery.last-winner")) {
            try {
                lastLotteryWinner = UUID.fromString(data.getString("lottery.last-winner"));
            } catch (IllegalArgumentException ignored) {}
        }
        lastLotteryPrize = data.getDouble("lottery.last-prize", 0);

        pendingOrders.clear();
        if (data.contains("auctions")) {
            for (String key : data.getConfigurationSection("auctions").getKeys(false)) {
                try {
                    UUID sellerId = UUID.fromString(data.getString("auctions." + key + ".seller-id"));
                    String sellerName = data.getString("auctions." + key + ".seller-name");
                    ItemStack item = data.getItemStack("auctions." + key + ".item");
                    double price = data.getDouble("auctions." + key + ".price");
                    long expiry = data.getLong("auctions." + key + ".expiry");
                    pendingOrders.add(new PendingOrder(sellerId, sellerName, item, price, expiry));
                } catch (Exception ignored) {}
            }
        }

        transactionLog.clear();
        if (data.contains("transaction-log")) {
            for (String key : data.getConfigurationSection("transaction-log").getKeys(false)) {
                try {
                    UUID playerId = UUID.fromString(data.getString("transaction-log." + key + ".player"));
                    String type = data.getString("transaction-log." + key + ".type");
                    double amount = data.getDouble("transaction-log." + key + ".amount");
                    long timestamp = data.getLong("transaction-log." + key + ".timestamp");
                    String details = data.getString("transaction-log." + key + ".details", "");
                    transactionLog.add(new TransactionEntry(playerId, type, amount, timestamp, details));
                } catch (Exception ignored) {}
            }
        }

        taxDebt.clear();
        if (data.contains("tax-debt")) {
            for (String key : data.getConfigurationSection("tax-debt").getKeys(false)) {
                try {
                    taxDebt.put(UUID.fromString(key), data.getDouble("tax-debt." + key));
                } catch (IllegalArgumentException ignored) {}
            }
        }

        lastTaxAssessment.clear();
        if (data.contains("last-tax-assessment")) {
            for (String key : data.getConfigurationSection("last-tax-assessment").getKeys(false)) {
                try {
                    lastTaxAssessment.put(UUID.fromString(key), data.getLong("last-tax-assessment." + key));
                } catch (IllegalArgumentException ignored) {}
            }
        }

        incomeSinceLastTax.clear();
        if (data.contains("income-since-last-tax")) {
            for (String key : data.getConfigurationSection("income-since-last-tax").getKeys(false)) {
                try {
                    incomeSinceLastTax.put(UUID.fromString(key), data.getDouble("income-since-last-tax." + key));
                } catch (IllegalArgumentException ignored) {}
            }
        }

        assessedTaxAmount.clear();
        if (data.contains("assessed-tax-amount")) {
            for (String key : data.getConfigurationSection("assessed-tax-amount").getKeys(false)) {
                try {
                    assessedTaxAmount.put(UUID.fromString(key), data.getDouble("assessed-tax-amount." + key));
                } catch (IllegalArgumentException ignored) {}
            }
        }

        graceOfflineTime.clear();
        if (data.contains("grace-offline-time")) {
            for (String key : data.getConfigurationSection("grace-offline-time").getKeys(false)) {
                try {
                    graceOfflineTime.put(UUID.fromString(key), data.getLong("grace-offline-time." + key));
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public void saveData() {
        data.set("balances", null);
        for (Map.Entry<UUID, Double> entry : balances.entrySet()) {
            data.set("balances." + entry.getKey().toString(), entry.getValue());
        }

        data.set("bounties", null);
        for (Map.Entry<UUID, BountyEntry> entry : bounties.entrySet()) {
            BountyEntry b = entry.getValue();
            String path = "bounties." + entry.getKey().toString();
            data.set(path + ".amount", b.getAmount());
            data.set(path + ".placer", b.getPlacerId().toString());
            data.set(path + ".anonymous", b.isAnonymous());
            data.set(path + ".placed-at", b.getPlacedAt());
        }

        data.set("shared-accounts", null);
        for (Map.Entry<String, SharedAccount> entry : sharedAccounts.entrySet()) {
            data.set("shared-accounts." + entry.getKey(), entry.getValue().serialize());
        }

        data.set("last-interest", null);
        for (Map.Entry<UUID, Long> entry : lastInterest.entrySet()) {
            data.set("last-interest." + entry.getKey().toString(), entry.getValue());
        }

        data.set("lottery.pot", lotteryPot);
        data.set("lottery.entries", lotteryEntries.stream().map(UUID::toString).collect(Collectors.toList()));
        data.set("lottery.last-winner", lastLotteryWinner != null ? lastLotteryWinner.toString() : null);
        data.set("lottery.last-prize", lastLotteryPrize);

        data.set("auctions", null);
        int i = 0;
        for (PendingOrder order : pendingOrders) {
            if (order.isExpired()) continue;
            String path = "auctions." + i++;
            data.set(path + ".seller-id", order.getSellerId().toString());
            data.set(path + ".seller-name", order.getSellerName());
            data.set(path + ".item", order.getItem());
            data.set(path + ".price", order.getPrice());
            data.set(path + ".expiry", order.getExpiry());
        }

        data.set("transaction-log", null);
        int j = 0;
        long cutoff = config.isTransactionLoggingEnabled() && config.getTransactionLogKeepDays() > 0
                ? System.currentTimeMillis() - (config.getTransactionLogKeepDays() * 86400000L) : 0;
        for (TransactionEntry entry : transactionLog) {
            if (cutoff > 0 && entry.timestamp() < cutoff) continue;
            String path = "transaction-log." + j++;
            data.set(path + ".player", entry.playerId().toString());
            data.set(path + ".type", entry.type());
            data.set(path + ".amount", entry.amount());
            data.set(path + ".timestamp", entry.timestamp());
            data.set(path + ".details", entry.details());
        }

        data.set("tax-debt", null);
        for (Map.Entry<UUID, Double> entry : taxDebt.entrySet()) {
            if (entry.getValue() > 0) {
                data.set("tax-debt." + entry.getKey().toString(), entry.getValue());
            }
        }

        data.set("last-tax-assessment", null);
        for (Map.Entry<UUID, Long> entry : lastTaxAssessment.entrySet()) {
            data.set("last-tax-assessment." + entry.getKey().toString(), entry.getValue());
        }

        data.set("income-since-last-tax", null);
        for (Map.Entry<UUID, Double> entry : incomeSinceLastTax.entrySet()) {
            data.set("income-since-last-tax." + entry.getKey().toString(), entry.getValue());
        }

        data.set("assessed-tax-amount", null);
        for (Map.Entry<UUID, Double> entry : assessedTaxAmount.entrySet()) {
            data.set("assessed-tax-amount." + entry.getKey().toString(), entry.getValue());
        }

        data.set("grace-offline-time", null);
        for (Map.Entry<UUID, Long> entry : graceOfflineTime.entrySet()) {
            data.set("grace-offline-time." + entry.getKey().toString(), entry.getValue());
        }

        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save economy-data.yml: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════
    //  Balance Operations
    // ═══════════════════════════════════════════

    public double getBalance(UUID playerId) {
        return balances.getOrDefault(playerId, config.getStartingBalance());
    }

    public boolean hasBalance(UUID playerId, double amount) {
        return getBalance(playerId) >= amount;
    }

    public void setBalance(UUID playerId, double amount) {
        balances.put(playerId, amount);
    }

    public boolean deposit(UUID playerId, double amount) {
        if (amount < 0) return false;
        balances.put(playerId, getBalance(playerId) + amount);
        return true;
    }

    public boolean withdraw(UUID playerId, double amount) {
        if (amount < 0) return false;
        double current = getBalance(playerId);
        if (current < amount) return false;
        balances.put(playerId, current - amount);
        return true;
    }

    public boolean transfer(UUID from, UUID to, double amount) {
        if (amount < 0) return false;
        if (!withdraw(from, amount)) return false;
        deposit(to, amount);
        return true;
    }

    public String format(double amount) {
        String symbol = config.getCurrencySymbol();
        if (amount == 1 || amount == -1) {
            return symbol + String.format("%.2f", amount) + " " + config.getCurrencySingular();
        }
        return symbol + String.format("%.2f", amount) + " " + config.getCurrencyPlural();
    }

    public String formatCompact(double amount) {
        return config.getCurrencySymbol() + String.format("%.2f", amount);
    }

    public List<Map.Entry<UUID, Double>> getTopBalances() {
        return balances.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toList());
    }

    public int getBalancePosition(UUID playerId) {
        List<Map.Entry<UUID, Double>> sorted = balances.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .collect(Collectors.toList());
        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).getKey().equals(playerId)) return i + 1;
        }
        return -1;
    }

    // ═══════════════════════════════════════════
    //  Shared Accounts
    // ═══════════════════════════════════════════

    public Set<String> getSharedAccountNames() {
        return sharedAccounts.keySet();
    }

    public SharedAccount getSharedAccount(String name) {
        return sharedAccounts.get(name);
    }

    public List<SharedAccount> getAccountsForPlayer(UUID playerId) {
        return sharedAccounts.values().stream()
                .filter(a -> a.isMember(playerId))
                .collect(Collectors.toList());
    }

    public boolean createSharedAccount(String name, UUID owner) {
        if (sharedAccounts.containsKey(name)) return false;
        sharedAccounts.put(name, new SharedAccount(name, owner));
        return true;
    }

    public boolean deleteSharedAccount(String name) {
        return sharedAccounts.remove(name) != null;
    }

    public double getSharedAccountBalance(String name) {
        SharedAccount account = sharedAccounts.get(name);
        if (account == null) return 0;
        return account.getMembers().stream().mapToDouble(this::getBalance).sum();
    }

    // ═══════════════════════════════════════════
    //  Bounties
    // ═══════════════════════════════════════════

    public boolean setBounty(UUID target, UUID setter, double amount, boolean anonymous) {
        if (!config.isBountiesEnabled()) return false;
        if (amount < config.getBountyMinimum()) return false;
        double tax = amount * (config.getBountyTaxPercent() / 100.0);
        double total = amount + tax;
        if (!withdraw(setter, total)) return false;
        if (tax > 0) deposit(getServerAccountId(), tax);
        BountyEntry existing = bounties.get(target);
        double newAmount = (existing != null ? existing.getAmount() : 0) + amount;
        bounties.put(target, new BountyEntry(target, setter, newAmount, anonymous, System.currentTimeMillis()));
        logTransaction(setter, "bounty_place", total, "Bounty on " + target);
        return true;
    }

    public double getBounty(UUID target) {
        BountyEntry entry = bounties.get(target);
        return entry != null ? entry.getAmount() : 0;
    }

    public BountyEntry getBountyEntry(UUID target) {
        return bounties.get(target);
    }

    public boolean redeemBounty(UUID target, UUID killer) {
        BountyEntry entry = bounties.remove(target);
        if (entry == null || entry.getAmount() <= 0) return false;
        deposit(killer, entry.getAmount());
        logTransaction(killer, "bounty_redeem", entry.getAmount(), "Killed " + target);
        return true;
    }

    public boolean cancelBounty(UUID target, UUID adminOrPlacer) {
        BountyEntry entry = bounties.remove(target);
        if (entry == null) return false;
        double refund = entry.getAmount();
        deposit(entry.getPlacerId(), refund);
        logTransaction(entry.getPlacerId(), "bounty_refund", refund, "Bounty cancelled on " + target);
        return true;
    }

    public Map<UUID, BountyEntry> getAllBountyEntries() {
        return Collections.unmodifiableMap(bounties);
    }

    public Map<UUID, Double> getAllBounties() {
        Map<UUID, Double> result = new HashMap<>();
        for (Map.Entry<UUID, BountyEntry> entry : bounties.entrySet()) {
            result.put(entry.getKey(), entry.getValue().getAmount());
        }
        return result;
    }

    // ═══════════════════════════════════════════
    //  Lottery
    // ═══════════════════════════════════════════

    public boolean buyLotteryTicket(UUID playerId) {
        if (!config.isLotteryEnabled()) return false;
        double price = config.getLotteryTicketPrice();
        if (!withdraw(playerId, price)) return false;
        lotteryPot += price;
        lotteryEntries.add(playerId);
        logTransaction(playerId, "lottery_ticket", price, "Lottery ticket");
        return true;
    }

    public boolean drawLottery() {
        if (lotteryEntries.size() < 2) return false;
        List<UUID> entries = new ArrayList<>(lotteryEntries);
        UUID winner = entries.get(new Random().nextInt(entries.size()));
        double prize = lotteryPot * (config.getLotteryPayoutPercent() / 100.0);
        deposit(winner, prize);
        lastLotteryWinner = winner;
        lastLotteryPrize = prize;
        logTransaction(winner, "lottery_win", prize, "Lottery win");
        lotteryPot = 0;
        lotteryEntries.clear();
        return true;
    }

    public double getLotteryPot() { return lotteryPot; }
    public int getLotteryEntryCount() { return lotteryEntries.size(); }
    public UUID getLastLotteryWinner() { return lastLotteryWinner; }
    public double getLastLotteryPrize() { return lastLotteryPrize; }

    public void resetLottery() {
        lotteryPot = 0;
        lotteryEntries.clear();
    }

    // ═══════════════════════════════════════════
    //  Dice Game
    // ═══════════════════════════════════════════

    public static class DiceGame {
        private final UUID playerId;
        private final double bet;
        private final int roll;
        private final boolean won;
        private final double prize;

        public DiceGame(UUID playerId, double bet, int roll, boolean won, double prize) {
            this.playerId = playerId;
            this.bet = bet;
            this.roll = roll;
            this.won = won;
            this.prize = prize;
        }

        public UUID getPlayerId() { return playerId; }
        public double getBet() { return bet; }
        public int getRoll() { return roll; }
        public boolean isWon() { return won; }
        public double getPrize() { return prize; }
    }

    public DiceGame playDice(UUID playerId, double bet) {
        if (!config.isDiceEnabled()) return null;
        if (bet > config.getDiceMaxBet()) return null;
        if (!withdraw(playerId, bet)) return null;

        Random rand = new Random();
        int roll = rand.nextInt(6) + 1;
        boolean won = roll >= 4;
        double prize = won ? bet * 2.0 : 0;

        if (won) {
            deposit(playerId, prize);
        }

        DiceGame game = new DiceGame(playerId, bet, roll, won, prize);
        activeDiceGames.put(playerId, game);
        logTransaction(playerId, won ? "dice_win" : "dice_lose", bet, "Dice roll: " + roll);
        return game;
    }

    // ═══════════════════════════════════════════
    //  Banknotes
    // ═══════════════════════════════════════════

    public ItemStack createBanknote(double amount) {
        if (!config.isBanknotesEnabled()) return null;

        org.bukkit.inventory.ItemStack banknote = new org.bukkit.inventory.ItemStack(org.bukkit.Material.PAPER);
        ItemMeta meta = banknote.getItemMeta();
        meta.displayName(com.odeco.utils.ColorUtils.color("<gold>Banknote</gold>"));
        meta.lore(java.util.List.of(
            com.odeco.utils.ColorUtils.color("<gray>Value: " + format(amount) + "</gray>"),
            com.odeco.utils.ColorUtils.color("<dark_gray>Right-click to redeem</dark_gray>")
        ));
        meta.setCustomModelData(1001);
        banknote.setItemMeta(meta);

        return banknote;
    }

    public double getBanknoteValue(ItemStack item) {
        if (item == null || item.getType() != org.bukkit.Material.PAPER) return 0;
        if (!item.hasItemMeta()) return 0;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return 0;
        for (var line : meta.lore()) {
            String text = PlainTextComponentSerializer.plainText().serialize(line);
            if (text.contains("Value:")) {
                try {
                    String valueStr = text.replaceAll("[^0-9.]", "");
                    return Double.parseDouble(valueStr);
                } catch (NumberFormatException ignored) {}
            }
        }
        return 0;
    }

    // ═══════════════════════════════════════════
    //  Auctions
    // ═══════════════════════════════════════════

    public boolean createAuction(UUID sellerId, String sellerName, ItemStack item, double price) {
        if (!config.isAuctionsEnabled()) return false;
        double fee = config.getAuctionListingFee();
        if (fee > 0) {
            if (!withdraw(sellerId, fee)) return false;
            deposit(getServerAccountId(), fee);
        }

        long duration = config.getAuctionDurationHours() * 3600000L;
        pendingOrders.add(new PendingOrder(sellerId, sellerName, item, price, System.currentTimeMillis() + duration));
        logTransaction(sellerId, "auction_list", 0, "Listed item for " + format(price));
        return true;
    }

    public boolean buyAuction(PendingOrder order, UUID buyerId) {
        if (order.isExpired()) return false;
        if (!withdraw(buyerId, order.getPrice())) return false;

        double tax = order.getPrice() * (config.getAuctionTaxPercent() / 100.0);
        double sellerPayout = order.getPrice() - tax;

        deposit(order.getSellerId(), sellerPayout);
        deposit(getServerAccountId(), tax);
        pendingOrders.remove(order);

        logTransaction(buyerId, "auction_buy", order.getPrice(), "Bought from " + order.getSellerName());
        logTransaction(order.getSellerId(), "auction_sell", sellerPayout, "Sold to " + buyerId);
        return true;
    }

    public boolean cancelAuction(PendingOrder order) {
        if (!pendingOrders.remove(order)) return false;
        logTransaction(order.getSellerId(), "auction_cancel", 0, "Auction cancelled");
        return true;
    }

    public List<PendingOrder> getActiveAuctions() {
        pendingOrders.removeIf(PendingOrder::isExpired);
        return Collections.unmodifiableList(pendingOrders);
    }

    // ═══════════════════════════════════════════
    //  Interest
    // ═══════════════════════════════════════════

    public void applyInterest() {
        if (!config.isInterestEnabled()) return;
        long now = System.currentTimeMillis();
        long interval = config.getInterestInterval() * 60000L;
        double rate = config.getInterestRate() / 100.0;

        for (Map.Entry<UUID, Double> entry : new HashMap<>(balances).entrySet()) {
            UUID playerId = entry.getKey();
            long last = lastInterest.getOrDefault(playerId, 0L);
            if (now - last < interval) continue;

            double balance = entry.getValue();
            double interest = balance * rate;
            deposit(playerId, interest);
            lastInterest.put(playerId, now);
            logTransaction(playerId, "interest", interest, "Interest payment");
        }
    }

    // ═══════════════════════════════════════════
    //  Tax System
    // ═══════════════════════════════════════════

    public void trackIncome(UUID playerId, double amount) {
        if (amount <= 0) return;
        incomeSinceLastTax.merge(playerId, amount, Double::sum);
    }

    public boolean isGracePeriodActive(UUID playerId) {
        if (!config.isTaxesEnabled()) return false;
        int graceMinutes = config.getGracePeriodMinutes();
        if (graceMinutes <= 0) return false;
        long last = lastTaxAssessment.getOrDefault(playerId, 0L);
        if (last == 0) return false;
        long elapsedOnlineMs = getOnlineGraceElapsedMs(playerId);
        return elapsedOnlineMs < (long) graceMinutes * 60000L;
    }

    public long getRemainingGracePeriodMs(UUID playerId) {
        int graceMinutes = config.getGracePeriodMinutes();
        if (graceMinutes <= 0) return 0;
        long last = lastTaxAssessment.getOrDefault(playerId, 0L);
        if (last == 0) return 0;
        long elapsedOnlineMs = getOnlineGraceElapsedMs(playerId);
        long totalMs = (long) graceMinutes * 60000L;
        return Math.max(0, totalMs - elapsedOnlineMs);
    }

    private long getOnlineGraceElapsedMs(UUID playerId) {
        long now = System.currentTimeMillis();
        long assessmentTime = lastTaxAssessment.getOrDefault(playerId, 0L);
        if (assessmentTime == 0) return 0;
        long realElapsed = now - assessmentTime;
        long offlineMs = graceOfflineTime.getOrDefault(playerId, 0L);
        if (graceOfflineSince.containsKey(playerId)) {
            offlineMs += (now - graceOfflineSince.get(playerId));
        }
        return Math.max(0, realElapsed - offlineMs);
    }

    public void onPlayerJoin(UUID playerId) {
        Long offlineSince = graceOfflineSince.remove(playerId);
        if (offlineSince != null) {
            long now = System.currentTimeMillis();
            long addedOffline = now - offlineSince;
            graceOfflineTime.merge(playerId, addedOffline, Long::sum);
        }
    }

    public void onPlayerQuit(UUID playerId) {
        if (isGracePeriodActive(playerId)) {
            graceOfflineSince.put(playerId, System.currentTimeMillis());
        }
    }

    private void resetGracePeriod(UUID playerId) {
        graceOfflineTime.remove(playerId);
        graceOfflineSince.remove(playerId);
    }

    public String getRemainingGracePeriodFormatted(UUID playerId) {
        long remainingMs = getRemainingGracePeriodMs(playerId);
        if (remainingMs <= 0) return "Expired";
        long minutes = remainingMs / 60000;
        long seconds = (remainingMs % 60000) / 1000;
        if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        }
        return seconds + "s";
    }

    public double getAssessedTaxAmount(UUID playerId) {
        return assessedTaxAmount.getOrDefault(playerId, 0.0);
    }

    public void assessTaxes() {
        assessTaxes(false);
    }

    public void assessTaxes(boolean force) {
        if (!config.isTaxesEnabled()) return;
        String mode = config.getTaxMode();
        if ("NONE".equals(mode)) return;

        long now = System.currentTimeMillis();
        long interval = config.getTaxIntervalMinutes() * 60000L;
        int graceMinutes = config.getGracePeriodMinutes();
        long graceMs = (long) graceMinutes * 60000L;

        for (UUID playerId : balances.keySet()) {
            long last = lastTaxAssessment.getOrDefault(playerId, 0L);

            // Apply overdue penalty if grace period has expired on previous assessment
            if (last > 0 && graceMinutes > 0) {
                long graceExpiryMs = graceMs + graceOfflineTime.getOrDefault(playerId, 0L);
                long elapsedOnline = now - last;
                // Check if player was offline during grace - they don't get penalized
                if (elapsedOnline > graceExpiryMs) {
                    double existingDebt = taxDebt.getOrDefault(playerId, 0.0);
                    if (existingDebt > 0) {
                        double penalty = existingDebt * (config.getOverduePenaltyPercent() / 100.0);
                        if (penalty > 0) {
                            taxDebt.merge(playerId, penalty, Double::sum);
                            logTransaction(playerId, "tax_owed", penalty, "Overdue penalty (" + config.getOverduePenaltyPercent() + "%)");
                        }
                    }
                }
            }

            // Check if it's time for a new assessment (interval-based)
            if (!force && interval > 0 && (now - last) < interval) continue;

            double taxAmount = 0;
            switch (mode) {
                case "INCOME_TAX" -> {
                    double income = incomeSinceLastTax.getOrDefault(playerId, 0.0);
                    taxAmount = income * (config.getIncomeTaxRate() / 100.0);
                    incomeSinceLastTax.put(playerId, 0.0);
                }
                case "SET_TAX" -> taxAmount = config.getSetTaxAmount();
                case "PERCENTAGE_BALANCE" -> {
                    double balance = getBalance(playerId);
                    taxAmount = balance * (config.getBalanceTaxRate() / 100.0);
                }
            }

            if (taxAmount <= 0) {
                lastTaxAssessment.put(playerId, now);
                assessedTaxAmount.put(playerId, 0.0);
                resetGracePeriod(playerId);
                continue;
            }

            // Lock the assessed amount and reset grace period
            assessedTaxAmount.put(playerId, taxAmount);
            resetGracePeriod(playerId);

            String method = config.getTaxPaymentMethod();
            switch (method) {
                case "AUTO" -> {
                    if (withdraw(playerId, taxAmount)) {
                        String payTo = config.getTaxPayTo();
                        if ("SERVER".equals(payTo)) {
                            deposit(getServerAccountId(), taxAmount);
                        } else if ("SHARED_ACCOUNT".equals(payTo)) {
                            String target = config.getTaxSharedAccount();
                            if (target != null && !target.isEmpty()) {
                                SharedAccount sa = sharedAccounts.get(target);
                                if (sa != null) {
                                    for (UUID memberId : sa.getMembers()) {
                                        deposit(memberId, taxAmount / sa.getMembers().size());
                                    }
                                } else {
                                    deposit(getServerAccountId(), taxAmount);
                                }
                            } else {
                                deposit(getServerAccountId(), taxAmount);
                            }
                        }
                        logTransaction(playerId, "tax_paid", taxAmount, "Auto tax payment (" + mode + ")");
                        org.bukkit.entity.Player online = plugin.getServer().getPlayer(playerId);
                        if (online != null && online.isOnline()) {
                            online.sendMessage(com.odeco.utils.ColorUtils.color(
                                "<yellow>Taxes of " + format(taxAmount) + " have been automatically deducted from your balance.</yellow>"));
                        }
                    } else {
                        taxDebt.merge(playerId, taxAmount, Double::sum);
                        logTransaction(playerId, "tax_owed", taxAmount, "Insufficient funds for auto tax");
                        org.bukkit.entity.Player online = plugin.getServer().getPlayer(playerId);
                        if (online != null && online.isOnline()) {
                            online.sendMessage(com.odeco.utils.ColorUtils.color(
                                "<red>You owe " + format(taxAmount) + " in taxes but have insufficient funds. Use /taxmanager to pay.</red>"));
                        }
                    }
                }
                case "MANUAL" -> {
                    taxDebt.merge(playerId, taxAmount, Double::sum);
                    logTransaction(playerId, "tax_owed", taxAmount, "Tax assessment (" + mode + ")");
                    org.bukkit.entity.Player online = plugin.getServer().getPlayer(playerId);
                    if (online != null && online.isOnline()) {
                        online.sendMessage(com.odeco.utils.ColorUtils.color(
                            "<red>You owe " + format(taxAmount) + " in taxes. Use /taxmanager to pay.</red>"));
                    }
                }
                case "BANKNOTE" -> {
                    taxDebt.merge(playerId, taxAmount, Double::sum);
                    logTransaction(playerId, "tax_owed", taxAmount, "Tax assessment (" + mode + ")");
                    org.bukkit.entity.Player online = plugin.getServer().getPlayer(playerId);
                    if (online != null && online.isOnline()) {
                        List<String> targets = config.getBanknoteDeliveryTargets();
                        String targetStr = targets.isEmpty() ? "an admin" : String.join(", ", targets);
                        online.sendMessage(com.odeco.utils.ColorUtils.color(
                            "<red>You owe " + format(taxAmount) + " in taxes. Create a banknote and deliver it to " + targetStr + ".</red>"));
                    }
                }
            }

            lastTaxAssessment.put(playerId, now);
        }
    }

    public boolean payTaxDebt(UUID playerId, double amount) {
        double debt = taxDebt.getOrDefault(playerId, 0.0);
        if (debt <= 0) return false;
        double toPay = Math.min(amount, debt);
        if (!withdraw(playerId, toPay)) return false;

        String payTo = config.getTaxPayTo();
        if ("SERVER".equals(payTo)) {
            deposit(getServerAccountId(), toPay);
        } else if ("SHARED_ACCOUNT".equals(payTo)) {
            String target = config.getTaxSharedAccount();
            if (target != null && !target.isEmpty()) {
                SharedAccount sa = sharedAccounts.get(target);
                if (sa != null) {
                    for (UUID memberId : sa.getMembers()) {
                        deposit(memberId, toPay / sa.getMembers().size());
                    }
                } else {
                    deposit(getServerAccountId(), toPay);
                }
            } else {
                deposit(getServerAccountId(), toPay);
            }
        }

        double remaining = debt - toPay;
        if (remaining <= 0.01) {
            taxDebt.remove(playerId);
        } else {
            taxDebt.put(playerId, remaining);
        }
        logTransaction(playerId, "tax_paid", toPay, "Manual tax payment");
        return true;
    }

    public boolean payTaxDebtToSharedAccount(UUID playerId, double amount) {
        double debt = taxDebt.getOrDefault(playerId, 0.0);
        if (debt <= 0) return false;
        double toPay = Math.min(amount, debt);
        if (!withdraw(playerId, toPay)) return false;

        String sharedAccountName = config.getTaxSharedAccount();
        SharedAccount sa = sharedAccounts.get(sharedAccountName);
        if (sa != null) {
            for (UUID memberId : sa.getMembers()) {
                deposit(memberId, toPay / sa.getMembers().size());
            }
        } else {
            deposit(getServerAccountId(), toPay);
        }

        double remaining = debt - toPay;
        if (remaining <= 0.01) {
            taxDebt.remove(playerId);
        } else {
            taxDebt.put(playerId, remaining);
        }
        logTransaction(playerId, "tax_paid", toPay, "Tax payment to shared account '" + sharedAccountName + "'");
        return true;
    }

    public void clearPlayerTaxDebt(UUID playerId) {
        taxDebt.remove(playerId);
        assessedTaxAmount.remove(playerId);
    }

    public double getTaxDebt(UUID playerId) {
        return taxDebt.getOrDefault(playerId, 0.0);
    }

    public Map<UUID, Double> getAllTaxDebts() {
        return Collections.unmodifiableMap(taxDebt);
    }

    public void clearTaxDebt(UUID playerId) {
        taxDebt.remove(playerId);
    }

    public void clearAllTaxDebts() {
        taxDebt.clear();
        assessedTaxAmount.clear();
    }

    public long getLastTaxAssessment(UUID playerId) {
        return lastTaxAssessment.getOrDefault(playerId, 0L);
    }

    // ═══════════════════════════════════════════
    //  Transaction Logging
    // ═══════════════════════════════════════════

    public record TransactionEntry(UUID playerId, String type, double amount, long timestamp, String details) {}

    public void logTransaction(UUID playerId, String type, double amount, String details) {
        if (!config.isTransactionLoggingEnabled()) return;
        transactionLog.add(new TransactionEntry(playerId, type, amount, System.currentTimeMillis(), details));

        // Track income for income tax
        if ("INCOME_TAX".equals(config.getTaxMode()) && config.isTaxesEnabled()) {
            switch (type) {
                case "deposit", "pay_received", "bounty_redeem", "auction_sell",
                     "lottery_win", "interest", "shared_deposit", "banknote_redeem" -> {
                    trackIncome(playerId, amount);
                }
            }
        }
    }

    public List<TransactionEntry> getTransactionLog(UUID playerId) {
        return transactionLog.stream()
                .filter(e -> e.playerId().equals(playerId))
                .sorted(Comparator.comparingLong(TransactionEntry::timestamp).reversed())
                .limit(50)
                .collect(Collectors.toList());
    }

    public List<TransactionEntry> getRecentTransactions(int count) {
        return transactionLog.stream()
                .sorted(Comparator.comparingLong(TransactionEntry::timestamp).reversed())
                .limit(count)
                .collect(Collectors.toList());
    }

    public List<TransactionEntry> getAllTransactions() {
        return transactionLog.stream()
                .sorted(Comparator.comparingLong(TransactionEntry::timestamp).reversed())
                .collect(Collectors.toList());
    }

    public void clearTransactionLog() {
        transactionLog.clear();
    }

    // ═══════════════════════════════════════════
    //  Utility
    // ═══════════════════════════════════════════

    public UUID getServerAccountId() {
        return UUID.nameUUIDFromBytes("Server".getBytes());
    }

    public String getCurrencySingular() { return config.getCurrencySingular(); }
    public String getCurrencyPlural() { return config.getCurrencyPlural(); }
    public String getCurrencySymbol() { return config.getCurrencySymbol(); }
}
