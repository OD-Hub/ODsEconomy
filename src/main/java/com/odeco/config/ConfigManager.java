package com.odeco.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {

    private final JavaPlugin plugin;
    private FileConfiguration config;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        saveDefault();
    }

    public void saveDefault() {
        plugin.saveDefaultConfig();
        this.config = plugin.getConfig();
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    // ── Currency ──────────────────────────────────────

    public String getCurrencySingular() {
        return config.getString("currency-singular", "Dollar");
    }

    public String getCurrencyPlural() {
        return config.getString("currency-plural", "Dollars");
    }

    public String getCurrencySymbol() {
        return config.getString("currency-symbol", "$");
    }

    public double getStartingBalance() {
        return config.getDouble("starting-balance", 100);
    }

    // ── Interest ──────────────────────────────────────

    public boolean isInterestEnabled() {
        return config.getBoolean("interest.enabled", true);
    }

    public double getInterestRate() {
        return config.getDouble("interest.rate", 2.5);
    }

    public int getInterestInterval() {
        return config.getInt("interest.interval-minutes", 60);
    }

    public void setInterestEnabled(boolean enabled) {
        config.set("interest.enabled", enabled);
        plugin.saveConfig();
    }

    public void setInterestRate(double rate) {
        config.set("interest.rate", rate);
        plugin.saveConfig();
    }

    public void setInterestInterval(int minutes) {
        config.set("interest.interval-minutes", minutes);
        plugin.saveConfig();
    }

    // ── Sell ──────────────────────────────────────────

    public boolean isSellEnabled() {
        return config.getBoolean("sell.enabled", true);
    }

    public double getSellMultiplier() {
        return config.getDouble("sell.multiplier", 1.0);
    }

    // ── Worth ─────────────────────────────────────────

    public Map<String, Double> getWorth() {
        Map<String, Double> worth = new HashMap<>();
        if (config.contains("worth")) {
            for (String key : config.getConfigurationSection("worth").getKeys(false)) {
                worth.put(key, config.getDouble("worth." + key));
            }
        }
        return worth;
    }

    public double getWorth(String material) {
        return config.getDouble("worth." + material, 0);
    }

    public void setWorth(String material, double price) {
        config.set("worth." + material, price);
        plugin.saveConfig();
    }

    public void removeWorth(String material) {
        config.set("worth." + material, null);
        plugin.saveConfig();
    }

    // ── Lottery ───────────────────────────────────────

    public boolean isLotteryEnabled() {
        return config.getBoolean("lottery.enabled", true);
    }

    public double getLotteryTicketPrice() {
        return config.getDouble("lottery.ticket-price", 100);
    }

    public double getLotteryPayoutPercent() {
        return config.getDouble("lottery.payout-percentage", 80);
    }

    public void setLotteryTicketPrice(double price) {
        config.set("lottery.ticket-price", price);
        plugin.saveConfig();
    }

    // ── Dice ──────────────────────────────────────────

    public boolean isDiceEnabled() {
        return config.getBoolean("dice.enabled", true);
    }

    public double getDiceMaxBet() {
        return config.getDouble("dice.max-bet", 50000);
    }

    // ── Banknotes ─────────────────────────────────────

    public boolean isBanknotesEnabled() {
        return config.getBoolean("banknotes.enabled", true);
    }

    public double getBanknoteFeePercent() {
        return config.getDouble("banknotes.fee-percent", 2);
    }

    // ── Auctions ──────────────────────────────────────

    public boolean isAuctionsEnabled() {
        return config.getBoolean("auctions.enabled", true);
    }

    public double getAuctionListingFee() {
        return config.getDouble("auctions.listing-fee", 0);
    }

    public double getAuctionTaxPercent() {
        return config.getDouble("auctions.tax-percent", 5);
    }

    public int getAuctionDurationHours() {
        return config.getInt("auctions.duration-hours", 24);
    }

    // ── Bounties ──────────────────────────────────────

    public boolean isBountiesEnabled() {
        return config.getBoolean("bounties.enabled", true);
    }

    public double getBountyMinimum() {
        return config.getDouble("bounties.minimum", 100);
    }

    public double getBountyTaxPercent() {
        return config.getDouble("bounties.tax-percent", 5);
    }

    // ── Shared Accounts ──────────────────────────────

    public boolean isSharedAccountsEnabled() {
        return config.getBoolean("shared-accounts.enabled", true);
    }

    // ── Dealerships ────────────────────────────────

    public boolean isDealershipsEnabled() {
        return config.getBoolean("dealerships.enabled", true);
    }

    // ── Transaction Logging ──────────────────────────

    public boolean isTransactionLoggingEnabled() {
        return config.getBoolean("transaction-logging.enabled", true);
    }

    public int getTransactionLogKeepDays() {
        return config.getInt("transaction-logging.keep-days", 30);
    }

    // ── Tab List ──────────────────────────────────────

    public boolean isTabListEnabled() {
        return config.getBoolean("tab-list.enabled", true);
    }

    public int getTabListUpdateTicks() {
        return config.getInt("tab-list.update-interval-ticks", 100);
    }

    // ── Taxes ─────────────────────────────────────────

    public boolean isTaxesEnabled() {
        return config.getBoolean("taxes.enabled", false);
    }

    public void setTaxesEnabled(boolean enabled) {
        config.set("taxes.enabled", enabled);
        plugin.saveConfig();
    }

    public String getTaxMode() {
        return config.getString("taxes.mode", "NONE");
    }

    public void setTaxMode(String mode) {
        config.set("taxes.mode", mode);
        plugin.saveConfig();
    }

    public double getIncomeTaxRate() {
        return config.getDouble("taxes.income-tax-rate", 10);
    }

    public void setIncomeTaxRate(double rate) {
        config.set("taxes.income-tax-rate", rate);
        plugin.saveConfig();
    }

    public double getSetTaxAmount() {
        return config.getDouble("taxes.set-tax-amount", 100);
    }

    public void setSetTaxAmount(double amount) {
        config.set("taxes.set-tax-amount", amount);
        plugin.saveConfig();
    }

    public double getBalanceTaxRate() {
        return config.getDouble("taxes.balance-tax-rate", 1);
    }

    public void setBalanceTaxRate(double rate) {
        config.set("taxes.balance-tax-rate", rate);
        plugin.saveConfig();
    }

    public int getTaxIntervalMinutes() {
        return config.getInt("taxes.interval-minutes", 60);
    }

    public void setTaxIntervalMinutes(int minutes) {
        config.set("taxes.interval-minutes", minutes);
        plugin.saveConfig();
    }

    public boolean isTaxIntervalEnabled() {
        return getTaxIntervalMinutes() > 0;
    }

    public int getGracePeriodMinutes() {
        return config.getInt("taxes.grace-period-minutes", 30);
    }

    public void setGracePeriodMinutes(int minutes) {
        config.set("taxes.grace-period-minutes", minutes);
        plugin.saveConfig();
    }

    public String getTaxPaymentMethod() {
        return config.getString("taxes.payment-method", "AUTO");
    }

    public void setTaxPaymentMethod(String method) {
        config.set("taxes.payment-method", method);
        plugin.saveConfig();
    }

    public String getTaxPayTo() {
        return config.getString("taxes.auto-pay-to", "SERVER");
    }

    public void setTaxPayTo(String payTo) {
        config.set("taxes.auto-pay-to", payTo);
        plugin.saveConfig();
    }

    public String getTaxSharedAccount() {
        return config.getString("taxes.tax-shared-account", "Taxes");
    }

    public void setTaxSharedAccount(String name) {
        config.set("taxes.tax-shared-account", name);
        plugin.saveConfig();
    }

    public List<String> getBanknoteDeliveryTargets() {
        return config.getStringList("taxes.banknote-delivery-targets");
    }

    public void setBanknoteDeliveryTargets(List<String> targets) {
        config.set("taxes.banknote-delivery-targets", targets);
        plugin.saveConfig();
    }

    public boolean isShowInfoButton() {
        return config.getBoolean("taxes.show-info-button", true);
    }

    public void setShowInfoButton(boolean show) {
        config.set("taxes.show-info-button", show);
        plugin.saveConfig();
    }

    public double getOverduePenaltyPercent() {
        return config.getDouble("taxes.overdue-penalty-percent", 10);
    }

    public void setOverduePenaltyPercent(double percent) {
        config.set("taxes.overdue-penalty-percent", percent);
        plugin.saveConfig();
    }

    // ── Admin ─────────────────────────────────────────

    public String getServerAccount() {
        return config.getString("admin.server-account", "Server");
    }
}
