package com.odeco;

import com.odeco.commands.EconomyCommands;
import com.odeco.config.ConfigManager;
import com.odeco.economy.DealershipManager;
import com.odeco.economy.EconomyManager;
import com.odeco.gui.GUIClickListener;
import com.odeco.listeners.BalanceScoreboardListener;
import com.odeco.listeners.BanknoteListener;
import com.odeco.listeners.BountyListener;
import com.odeco.listeners.ChatListener;
import com.odeco.listeners.GracePeriodListener;
import com.odeco.utils.ColorUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Objects;

public class ODEco extends JavaPlugin {

    private ConfigManager configManager;
    private EconomyManager economyManager;
    private DealershipManager dealershipManager;
    private EconomyCommands economyCommands;
    private ChatInputManager chatInputManager;
    private MiniMessage miniMessage;
    private BukkitTask interestTask;
    private BukkitTask tabListTask;
    private BukkitTask saveTask;
    private BukkitTask taxTask;
    private BalanceScoreboardListener scoreboardListener;
    private Object expansion;

    @Override
    public void onEnable() {
        this.miniMessage = MiniMessage.miniMessage();

        // Config
        this.configManager = new ConfigManager(this);

        // Economy
        this.economyManager = new EconomyManager(this);

        // Dealerships
        this.dealershipManager = new DealershipManager(this);

        // Commands
        this.economyCommands = new EconomyCommands(this);
        for (String cmd : new String[]{"eco", "pay", "sell", "worth", "baltop", "bounty", "lottery", "dice", "banknote", "auction", "interest", "ecoadmin", "taxmanager", "taxsetup", "dealership", "dealershipsetup"}) {
            Objects.requireNonNull(getCommand(cmd)).setExecutor(economyCommands);
            Objects.requireNonNull(getCommand(cmd)).setTabCompleter(economyCommands);
        }

        // Chat input
        this.chatInputManager = new ChatInputManager(this);

        // Listeners
        getServer().getPluginManager().registerEvents(new GUIClickListener(this), this);
        getServer().getPluginManager().registerEvents(new BountyListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new BanknoteListener(this), this);
        getServer().getPluginManager().registerEvents(new GracePeriodListener(this), this);

        // Balance scoreboard
        this.scoreboardListener = new BalanceScoreboardListener(this);
        scoreboardListener.start();

        // PAPI expansion
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                Class<?> expClass = Class.forName("com.odeco.ODEcoExpansion");
                Object exp = expClass.getConstructor(ODEco.class).newInstance(this);
                expClass.getMethod("register").invoke(exp);
                this.expansion = exp;
                getLogger().info("PlaceholderAPI expansion registered.");
            } catch (Exception e) {
                getLogger().info("PlaceholderAPI available but expansion not compiled.");
            }
        }

        // Interest timer
        startInterestTask();

        // Tax assessment timer
        startTaxTask();

        // Tab list
        startTabListTask();

        // Auto-save
        startSaveTask();

        getLogger().info("OD Eco v" + getPluginMeta().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        if (interestTask != null) interestTask.cancel();
        if (taxTask != null) taxTask.cancel();
        if (tabListTask != null) tabListTask.cancel();
        if (saveTask != null) saveTask.cancel();
        if (scoreboardListener != null) scoreboardListener.stop();
        try { if (expansion != null) { expansion.getClass().getMethod("unregister").invoke(expansion); } } catch (Exception ignored) {}
        if (economyManager != null) economyManager.saveData();
        if (dealershipManager != null) dealershipManager.saveData();
        getLogger().info("OD Eco disabled.");
    }

    private void startInterestTask() {
        if (!configManager.isInterestEnabled()) return;
        int interval = configManager.getInterestInterval();
        interestTask = getServer().getScheduler().runTaskTimer(this, () -> {
            economyManager.applyInterest();
        }, interval * 60 * 20L, interval * 60 * 20L);
    }

    private void startTaxTask() {
        if (!configManager.isTaxesEnabled()) return;
        if ("NONE".equals(configManager.getTaxMode())) return;
        if (!configManager.isTaxIntervalEnabled()) {
            getLogger().info("Tax assessment interval disabled. Use /taxmanager to assess manually.");
            return;
        }
        int interval = configManager.getTaxIntervalMinutes();
        taxTask = getServer().getScheduler().runTaskTimer(this, () -> {
            economyManager.assessTaxes();
        }, interval * 60 * 20L, interval * 60 * 20L);
        getLogger().info("Tax assessment task started (every " + interval + " min, mode: " + configManager.getTaxMode() + ")");
    }

    private void startTabListTask() {
        if (!configManager.isTabListEnabled()) return;
        int ticks = configManager.getTabListUpdateTicks();
        tabListTask = getServer().getScheduler().runTaskTimer(this, () -> {
            for (Player player : getServer().getOnlinePlayers()) {
                player.sendPlayerListHeaderAndFooter(Component.empty(), Component.empty());
            }
        }, 0, ticks);
    }

    private void startSaveTask() {
        saveTask = getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            economyManager.saveData();
        }, 6000L, 6000L); // Every 5 minutes
    }

    public ConfigManager getConfigManager() { return configManager; }
    public EconomyManager getEconomyManager() { return economyManager; }
    public DealershipManager getDealershipManager() { return dealershipManager; }
    public EconomyCommands getEconomyCommands() { return economyCommands; }
    public ChatInputManager getChatInputManager() { return chatInputManager; }
    public MiniMessage getMiniMessage() { return miniMessage; }
}
