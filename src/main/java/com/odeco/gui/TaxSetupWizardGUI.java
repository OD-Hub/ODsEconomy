package com.odeco.gui;

import com.odeco.ODEco;
import com.odeco.config.ConfigManager;
import com.odeco.utils.ColorUtils;
import com.odeco.utils.Heads;
import com.odeco.utils.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TaxSetupWizardGUI implements InventoryHolder {

    private final ODEco plugin;
    private final Player player;
    private final Inventory inventory;
    private WizardPage currentPage;

    private enum WizardPage {
        WELCOME,
        ENABLE,
        MODE,
        RATE,
        INTERVAL,
        GRACE_PERIOD,
        PAYMENT_METHOD,
        BANKNOTE_COLLECTORS,
        PENALTY,
        SUMMARY
    }

    public TaxSetupWizardGUI(ODEco plugin, Player player) {
        this(plugin, player, WizardPage.WELCOME);
    }

    public TaxSetupWizardGUI(ODEco plugin, Player player, WizardPage startPage) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, ColorUtils.color("<dark_green>Tax Setup Wizard</dark_green>"));
        this.currentPage = startPage;
        populate();
    }

    private Runnable reopenSelf() {
        return () -> player.openInventory(new TaxSetupWizardGUI(plugin, player, currentPage).getInventory());
    }

    private void populate() {
        inventory.clear();
        switch (currentPage) {
            case WELCOME -> populateWelcome();
            case ENABLE -> populateEnable();
            case MODE -> populateMode();
            case RATE -> populateRate();
            case INTERVAL -> populateInterval();
            case GRACE_PERIOD -> populateGracePeriod();
            case PAYMENT_METHOD -> populatePaymentMethod();
            case BANKNOTE_COLLECTORS -> populateBanknoteCollectors();
            case PENALTY -> populatePenalty();
            case SUMMARY -> populateSummary();
        }
    }

    // ═══════════════════════════════════════════
    //  Page 1: Welcome
    // ═══════════════════════════════════════════

    private void populateWelcome() {
        inventory.setItem(4, new ItemBuilder(Material.PLAYER_HEAD)
                .name(ColorUtils.color("<dark_green>Tax Setup Wizard</dark_green>"))
                .skull(Heads.WIZARD_HAT)
                .lore(
                        ColorUtils.color("<gray>This wizard will walk you through</gray>"),
                        ColorUtils.color("<gray>setting up the tax system step by step.</gray>"),
                        ColorUtils.color(""),
                        ColorUtils.color("<white>What you'll configure:</white>"),
                        ColorUtils.color("<gray>  - Turn taxes on or off</gray>"),
                        ColorUtils.color("<gray>  - How taxes are calculated</gray>"),
                        ColorUtils.color("<gray>  - How often players are taxed</gray>"),
                        ColorUtils.color("<gray>  - How players pay their taxes</gray>"),
                        ColorUtils.color("<gray>  - Penalties for late payment</gray>")
                )
                .build());

        inventory.setItem(22, new ItemBuilder(Material.EMERALD_BLOCK)
                .name(ColorUtils.color("<green>Start Setup</green>"))
                .lore(ColorUtils.color("<gray>Click to begin configuring taxes</gray>"))
                .build());

        inventory.setItem(49, new ItemBuilder(Material.BARRIER)
                .name(ColorUtils.color("<red>Cancel</red>"))
                .lore(ColorUtils.color("<gray>Close without saving</gray>"))
                .build());
    }

    // ═══════════════════════════════════════════
    //  Page 2: Enable Taxes
    // ═══════════════════════════════════════════

    private void populateEnable() {
        ConfigManager config = plugin.getConfigManager();
        boolean enabled = config.isTaxesEnabled();

        inventory.setItem(4, new ItemBuilder(Material.PLAYER_HEAD)
                .name(ColorUtils.color("<dark_green>Step 1: Enable Taxes</dark_green>"))
                .skull(Heads.WIZARD_HAT)
                .lore(
                        ColorUtils.color("<gray>This is the master switch.</gray>"),
                        ColorUtils.color("<gray>When disabled, no taxes are collected</gray>"),
                        ColorUtils.color("<gray>and players won't see any tax info.</gray>")
                )
                .build());

        inventory.setItem(22, new ItemBuilder(enabled ? Material.LIME_DYE : Material.RED_DYE)
                .name(ColorUtils.color(enabled ? "<green>Taxes: ENABLED</green>" : "<red>Taxes: DISABLED</red>"))
                .lore(
                        ColorUtils.color("<gray>Click to " + (enabled ? "disable" : "enable") + " taxes</gray>"),
                        ColorUtils.color(""),
                        enabled
                                ? ColorUtils.color("<green>Taxes are currently active on this server.</green>")
                                : ColorUtils.color("<red>Taxes are off. Players won't be taxed.</red>")
                )
                .build());

        addNavigation(true, true);
    }

    // ═══════════════════════════════════════════
    //  Page 3: Tax Mode
    // ═══════════════════════════════════════════

    private void populateMode() {
        ConfigManager config = plugin.getConfigManager();
        String mode = config.getTaxMode();

        inventory.setItem(4, new ItemBuilder(Material.PLAYER_HEAD)
                .name(ColorUtils.color("<dark_green>Step 2: Tax Calculation Method</dark_green>"))
                .skull(Heads.WIZARD_HAT)
                .lore(
                        ColorUtils.color("<gray>Choose how taxes are calculated.</gray>"),
                        ColorUtils.color("<gray>This determines what players are taxed on.</gray>")
                )
                .build());

        inventory.setItem(20, new ItemBuilder(Material.GOLD_INGOT)
                .name(ColorUtils.color("<gold>Income Tax</gold>"))
                .lore(
                        ColorUtils.color("<gray>Tax on money players earn</gray>"),
                        ColorUtils.color("<gray>(from jobs, trading, etc.)</gray>"),
                        ColorUtils.color(""),
                        ColorUtils.color("<dark_gray>Example: 10% income tax means</dark_gray>"),
                        ColorUtils.color("<dark_gray>if a player earns $100, they keep $90</dark_gray>"),
                        ColorUtils.color(""),
                        "INCOME_TAX".equals(mode)
                                ? ColorUtils.color("<green><bold>SELECTED</bold></green>")
                                : ColorUtils.color("<gray>Click to select</gray>")
                )
                .build());

        inventory.setItem(22, new ItemBuilder(Material.IRON_INGOT)
                .name(ColorUtils.color("<gold>Fixed Tax</gold>"))
                .lore(
                        ColorUtils.color("<gray>A flat amount taken each cycle</gray>"),
                        ColorUtils.color("<gray>(same for everyone)</gray>"),
                        ColorUtils.color(""),
                        ColorUtils.color("<dark_gray>Example: $100 fixed tax means</dark_gray>"),
                        ColorUtils.color("<dark_gray>every player owes $100 per cycle</dark_gray>"),
                        ColorUtils.color(""),
                        "SET_TAX".equals(mode)
                                ? ColorUtils.color("<green><bold>SELECTED</bold></green>")
                                : ColorUtils.color("<gray>Click to select</gray>")
                )
                .build());

        inventory.setItem(24, new ItemBuilder(Material.NETHERITE_INGOT)
                .name(ColorUtils.color("<gold>Balance Tax</gold>"))
                .lore(
                        ColorUtils.color("<gray>A percentage of total balance</gray>"),
                        ColorUtils.color("<gray>(rich players pay more)</gray>"),
                        ColorUtils.color(""),
                        ColorUtils.color("<dark_gray>Example: 1% balance tax means</dark_gray>"),
                        ColorUtils.color("<dark_gray>a player with $10,000 pays $100</dark_gray>"),
                        ColorUtils.color(""),
                        "PERCENTAGE_BALANCE".equals(mode)
                                ? ColorUtils.color("<green><bold>SELECTED</bold></green>")
                                : ColorUtils.color("<gray>Click to select</gray>")
                )
                .build());

        addNavigation(true, true);
    }

    // ═══════════════════════════════════════════
    //  Page 4: Rate / Amount
    // ═══════════════════════════════════════════

    private void populateRate() {
        ConfigManager config = plugin.getConfigManager();
        String mode = config.getTaxMode();
        var economy = plugin.getEconomyManager();

        String title;
        List<Component> desc = new ArrayList<>();

        if ("INCOME_TAX".equals(mode)) {
            title = "Income Tax Rate";
            desc.add(ColorUtils.color("<gray>This is the percentage taken from money</gray>"));
            desc.add(ColorUtils.color("<gray>players earn. Set it to something fair</gray>"));
            desc.add(ColorUtils.color("<gray>for your server's economy.</gray>"));
            desc.add(Component.empty());
            desc.add(ColorUtils.color("<white>Current rate: <gold>" + config.getIncomeTaxRate() + "%</gold></white>"));
            desc.add(Component.empty());
            desc.add(ColorUtils.color("<dark_gray>Suggested: 5-15% for most servers</dark_gray>"));
            desc.add(ColorUtils.color("<dark_gray>Click the ingot below to change it</dark_gray>"));
        } else if ("SET_TAX".equals(mode)) {
            title = "Fixed Tax Amount";
            desc.add(ColorUtils.color("<gray>This is the flat amount every player</gray>"));
            desc.add(ColorUtils.color("<gray>owes each assessment cycle, regardless</gray>"));
            desc.add(ColorUtils.color("<gray>of how much they have.</gray>"));
            desc.add(Component.empty());
            desc.add(ColorUtils.color("<white>Current amount: <gold>" + economy.format(config.getSetTaxAmount()) + "</gold></white>"));
            desc.add(Component.empty());
            desc.add(ColorUtils.color("<dark_gray>Consider your server's average balance</dark_gray>"));
            desc.add(ColorUtils.color("<dark_gray>when picking this amount</dark_gray>"));
        } else if ("PERCENTAGE_BALANCE".equals(mode)) {
            title = "Balance Tax Rate";
            desc.add(ColorUtils.color("<gray>This is the percentage taken from each</gray>"));
            desc.add(ColorUtils.color("<gray>player's total balance. Wealthier players</gray>"));
            desc.add(ColorUtils.color("<gray>automatically pay more.</gray>"));
            desc.add(Component.empty());
            desc.add(ColorUtils.color("<white>Current rate: <gold>" + config.getBalanceTaxRate() + "%</gold></white>"));
            desc.add(Component.empty());
            desc.add(ColorUtils.color("<dark_gray>Suggested: 0.5-3% for most servers</dark_gray>"));
            desc.add(ColorUtils.color("<dark_gray>Click the ingot below to change it</dark_gray>"));
        } else {
            title = "Configure Rate";
            desc.add(ColorUtils.color("<gray>No mode selected. Go back and choose one.</gray>"));
        }

        inventory.setItem(4, new ItemBuilder(Material.PLAYER_HEAD)
                .name(ColorUtils.color("<dark_green>Step 3: " + title + "</dark_green>"))
                .skull(Heads.WIZARD_HAT)
                .lore(desc)
                .build());

        if ("INCOME_TAX".equals(mode)) {
            inventory.setItem(22, new ItemBuilder(Material.GOLD_INGOT)
                    .name(ColorUtils.color("<gold>Set Income Tax Rate</gold>"))
                    .lore(
                            ColorUtils.color("<gray>Current: " + config.getIncomeTaxRate() + "%</gray>"),
                            ColorUtils.color("<dark_gray>Click to type a new value in chat</dark_gray>")
                    )
                    .build());
        } else if ("SET_TAX".equals(mode)) {
            inventory.setItem(22, new ItemBuilder(Material.IRON_INGOT)
                    .name(ColorUtils.color("<gold>Set Fixed Tax Amount</gold>"))
                    .lore(
                            ColorUtils.color("<gray>Current: " + economy.format(config.getSetTaxAmount()) + "</gray>"),
                            ColorUtils.color("<dark_gray>Click to type a new value in chat</dark_gray>")
                    )
                    .build());
        } else if ("PERCENTAGE_BALANCE".equals(mode)) {
            inventory.setItem(22, new ItemBuilder(Material.NETHERITE_INGOT)
                    .name(ColorUtils.color("<gold>Set Balance Tax Rate</gold>"))
                    .lore(
                            ColorUtils.color("<gray>Current: " + config.getBalanceTaxRate() + "%</gray>"),
                            ColorUtils.color("<dark_gray>Click to type a new value in chat</dark_gray>")
                    )
                    .build());
        }

        addNavigation(true, true);
    }

    // ═══════════════════════════════════════════
    //  Page 5: Assessment Interval
    // ═══════════════════════════════════════════

    private void populateInterval() {
        ConfigManager config = plugin.getConfigManager();
        int interval = config.getTaxIntervalMinutes();
        boolean enabled = config.isTaxIntervalEnabled();

        inventory.setItem(4, new ItemBuilder(Material.PLAYER_HEAD)
                .name(ColorUtils.color("<dark_green>Step 4: How Often Taxes Are Checked</dark_green>"))
                .skull(Heads.WIZARD_HAT)
                .lore(
                        ColorUtils.color("<gray>This controls how often the server</gray>"),
                        ColorUtils.color("<gray>automatically calculates what players owe.</gray>"),
                        ColorUtils.color(""),
                        ColorUtils.color(enabled
                                ? "<white>Current: <gold>" + interval + " minutes</gold></white>"
                                : "<white>Current: <red>Disabled</red></white>"),
                        ColorUtils.color(""),
                        ColorUtils.color("<dark_gray>Set to 0 to disable automatic checking.</dark_gray>"),
                        ColorUtils.color("<dark_gray>You can still assess manually with /taxmanager assess</dark_gray>"),
                        ColorUtils.color(""),
                        ColorUtils.color("<gray>Suggested: 60 min (1 hour) or 120 min (2 hours)</gray>")
                )
                .build());

        inventory.setItem(22, new ItemBuilder(Material.CLOCK)
                .name(ColorUtils.color("<gold>Set Assessment Interval</gold>"))
                .lore(
                        ColorUtils.color("<gray>Current: " + (enabled ? interval + " minutes" : "Disabled (0)") + "</gray>"),
                        ColorUtils.color("<dark_gray>Click to type a new value in chat</dark_gray>"),
                        ColorUtils.color("<dark_gray>0 = disable automatic assessment</dark_gray>")
                )
                .build());

        addNavigation(true, true);
    }

    // ═══════════════════════════════════════════
    //  Page 6: Grace Period
    // ═══════════════════════════════════════════

    private void populateGracePeriod() {
        ConfigManager config = plugin.getConfigManager();
        int grace = config.getGracePeriodMinutes();

        inventory.setItem(4, new ItemBuilder(Material.PLAYER_HEAD)
                .name(ColorUtils.color("<dark_green>Step 5: Grace Period</dark_green>"))
                .skull(Heads.WIZARD_HAT)
                .lore(
                        ColorUtils.color("<gray>After taxes are assessed, players get</gray>"),
                        ColorUtils.color("<gray>this much time to pay before penalties kick in.</gray>"),
                        ColorUtils.color(""),
                        grace > 0
                                ? ColorUtils.color("<white>Current: <gold>" + grace + " minutes</gold></white>")
                                : ColorUtils.color("<white>Current: <red>None (must pay immediately)</red></white>"),
                        ColorUtils.color(""),
                        ColorUtils.color("<dark_gray>Set to 0 if you want taxes taken right away.</dark_gray>"),
                        ColorUtils.color("<dark_gray>The timer only counts while the player is</dark_gray>"),
                        ColorUtils.color("<dark_gray>online, so offline players aren't penalized.</dark_gray>"),
                        ColorUtils.color(""),
                        ColorUtils.color("<gray>Suggested: 30-60 min for most servers</gray>")
                )
                .build());

        inventory.setItem(22, new ItemBuilder(Material.RED_BED)
                .name(ColorUtils.color("<gold>Set Grace Period</gold>"))
                .lore(
                        ColorUtils.color("<gray>Current: " + (grace > 0 ? grace + " min" : "None") + "</gray>"),
                        ColorUtils.color("<dark_gray>Click to type a new value in chat</dark_gray>"),
                        ColorUtils.color("<dark_gray>0 = no grace period</dark_gray>")
                )
                .build());

        addNavigation(true, true);
    }

    // ═══════════════════════════════════════════
    //  Page 7: Payment Method
    // ═══════════════════════════════════════════

    private void populatePaymentMethod() {
        ConfigManager config = plugin.getConfigManager();
        String method = config.getTaxPaymentMethod();

        inventory.setItem(4, new ItemBuilder(Material.PLAYER_HEAD)
                .name(ColorUtils.color("<dark_green>Step 6: How Players Pay</dark_green>"))
                .skull(Heads.WIZARD_HAT)
                .lore(
                        ColorUtils.color("<gray>Choose how players settle their tax debt.</gray>")
                )
                .build());

        inventory.setItem(20, new ItemBuilder(Material.HOPPER)
                .name(ColorUtils.color("<gold>Auto Deduct</gold>"))
                .lore(
                        ColorUtils.color("<gray>Money is taken automatically from</gray>"),
                        ColorUtils.color("<gray>the player's balance when taxes are due.</gray>"),
                        ColorUtils.color(""),
                        ColorUtils.color("<green>Simplest option. No player action needed.</green>"),
                        ColorUtils.color("<dark_gray>Players with enough balance are auto-charged.</dark_gray>"),
                        ColorUtils.color("<dark_gray>Players without enough balance get a debt.</dark_gray>"),
                        ColorUtils.color(""),
                        "AUTO".equals(method)
                                ? ColorUtils.color("<green><bold>SELECTED</bold></green>")
                                : ColorUtils.color("<gray>Click to select</gray>")
                )
                .build());

        inventory.setItem(22, new ItemBuilder(Material.EMERALD)
                .name(ColorUtils.color("<gold>Manual Payment</gold>"))
                .lore(
                        ColorUtils.color("<gray>Players must go to the Tax Manager</gray>"),
                        ColorUtils.color("<gray>and click a button to pay their taxes.</gray>"),
                        ColorUtils.color(""),
                        ColorUtils.color("<yellow>Gives players more control and awareness.</yellow>"),
                        ColorUtils.color("<dark_gray>They can also pay via /taxmanager pay in chat.</dark_gray>"),
                        ColorUtils.color(""),
                        "MANUAL".equals(method)
                                ? ColorUtils.color("<green><bold>SELECTED</bold></green>")
                                : ColorUtils.color("<gray>Click to select</gray>")
                )
                .build());

        inventory.setItem(24, new ItemBuilder(Material.PAPER)
                .name(ColorUtils.color("<gold>Banknote Delivery</gold>"))
                .lore(
                        ColorUtils.color("<gray>Players create a physical banknote</gray>"),
                        ColorUtils.color("<gray>and deliver it to a designated collector.</gray>"),
                        ColorUtils.color(""),
                        ColorUtils.color("<red>Most immersive but hardest to manage.</red>"),
                        ColorUtils.color("<dark_gray>Good for roleplay servers. You'll need to</dark_gray>"),
                        ColorUtils.color("<dark_gray>specify who collects the banknotes.</dark_gray>"),
                        ColorUtils.color(""),
                        "BANKNOTE".equals(method)
                                ? ColorUtils.color("<green><bold>SELECTED</bold></green>")
                                : ColorUtils.color("<gray>Click to select</gray>")
                )
                .build());

        addNavigation(true, true);
    }

    // ═══════════════════════════════════════════
    //  Page 7b: Banknote Collectors
    // ═══════════════════════════════════════════

    private void populateBanknoteCollectors() {
        ConfigManager config = plugin.getConfigManager();
        List<String> targets = config.getBanknoteDeliveryTargets();
        String targetStr = targets.isEmpty() ? "None set" : String.join(", ", targets);

        inventory.setItem(4, new ItemBuilder(Material.PLAYER_HEAD)
                .name(ColorUtils.color("<dark_green>Step 6b: Tax Collectors</dark_green>"))
                .skull(Heads.WIZARD_HAT)
                .lore(
                        ColorUtils.color("<gray>These players will receive banknotes</gray>"),
                        ColorUtils.color("<gray>from players paying their taxes.</gray>"),
                        ColorUtils.color(""),
                        ColorUtils.color("<white>Current collectors: <gold>" + targetStr + "</gold></white>"),
                        ColorUtils.color(""),
                        ColorUtils.color("<dark_gray>Enter comma-separated player names</dark_gray>"),
                        ColorUtils.color("<dark_gray>when prompted. These players will</dark_gray>"),
                        ColorUtils.color("<dark_gray>collect banknote tax payments.</dark_gray>")
                )
                .build());

        inventory.setItem(22, new ItemBuilder(Material.PLAYER_HEAD)
                .name(ColorUtils.color("<gold>Set Tax Collectors</gold>"))
                .skull(Heads.BOUNTY)
                .lore(
                        ColorUtils.color("<gray>Current: " + targetStr + "</gray>"),
                        ColorUtils.color("<dark_gray>Click to set collector names in chat</dark_gray>"),
                        ColorUtils.color("<dark_gray>Comma-separated: Player1, Player2</dark_gray>")
                )
                .build());

        addNavigation(true, true);
    }

    // ═══════════════════════════════════════════
    //  Page 8: Overdue Penalty
    // ═══════════════════════════════════════════

    private void populatePenalty() {
        ConfigManager config = plugin.getConfigManager();
        double penalty = config.getOverduePenaltyPercent();

        inventory.setItem(4, new ItemBuilder(Material.PLAYER_HEAD)
                .name(ColorUtils.color("<dark_green>Step 7: Late Payment Penalty</dark_green>"))
                .skull(Heads.WIZARD_HAT)
                .lore(
                        ColorUtils.color("<gray>After the grace period runs out,</gray>"),
                        ColorUtils.color("<gray>an extra percentage is added to the debt.</gray>"),
                        ColorUtils.color(""),
                        ColorUtils.color("<white>Current: <gold>" + penalty + "%</gold> extra on overdue taxes</white>"),
                        ColorUtils.color(""),
                        ColorUtils.color("<dark_gray>This encourages players to pay on time.</dark_gray>"),
                        ColorUtils.color("<dark_gray>Set to 0 for no penalty.</dark_gray>"),
                        ColorUtils.color(""),
                        ColorUtils.color("<gray>Suggested: 5-20% for most servers</gray>")
                )
                .build());

        inventory.setItem(22, new ItemBuilder(Material.TRIPWIRE_HOOK)
                .name(ColorUtils.color("<gold>Set Overdue Penalty</gold>"))
                .lore(
                        ColorUtils.color("<gray>Current: " + penalty + "%</gray>"),
                        ColorUtils.color("<dark_gray>Click to type a new value in chat</dark_gray>"),
                        ColorUtils.color("<dark_gray>0 = no penalty for late payment</dark_gray>")
                )
                .build());

        addNavigation(true, true);
    }

    // ═══════════════════════════════════════════
    //  Page 9: Summary
    // ═══════════════════════════════════════════

    private void populateSummary() {
        ConfigManager config = plugin.getConfigManager();
        var economy = plugin.getEconomyManager();
        boolean enabled = config.isTaxesEnabled();
        String mode = config.getTaxMode();
        String method = config.getTaxPaymentMethod();

        List<Component> summary = new ArrayList<>();
        summary.add(ColorUtils.color("<gray>───────────────────────</gray>"));
        summary.add(enabled
                ? ColorUtils.color("<green>Status: ENABLED</green>")
                : ColorUtils.color("<red>Status: DISABLED</red>"));

        if (!"NONE".equals(mode)) {
            summary.add(ColorUtils.color("<white>Mode: </white><gold>" + formatMode(mode) + "</gold>"));

            if ("INCOME_TAX".equals(mode)) {
                summary.add(ColorUtils.color("<white>Rate: </white><gold>" + config.getIncomeTaxRate() + "% of income</gold>"));
            } else if ("SET_TAX".equals(mode)) {
                summary.add(ColorUtils.color("<white>Amount: </white><gold>" + economy.format(config.getSetTaxAmount()) + " per cycle</gold>"));
            } else if ("PERCENTAGE_BALANCE".equals(mode)) {
                summary.add(ColorUtils.color("<white>Rate: </white><gold>" + config.getBalanceTaxRate() + "% of balance</gold>"));
            }

            summary.add(ColorUtils.color("<white>Checks every: </white><gold>" + config.getTaxIntervalMinutes() + " min</gold>"));
            summary.add(ColorUtils.color("<white>Grace period: </white><gold>" + config.getGracePeriodMinutes() + " min</gold>"));
            summary.add(ColorUtils.color("<white>Pay method: </white><gold>" + formatMethod(method) + "</gold>"));
            if ("BANKNOTE".equals(method)) {
                List<String> targets = config.getBanknoteDeliveryTargets();
                summary.add(ColorUtils.color("<white>Collectors: </white><gold>" + (targets.isEmpty() ? "None" : String.join(", ", targets)) + "</gold>"));
            }
            summary.add(ColorUtils.color("<white>Late penalty: </white><gold>" + config.getOverduePenaltyPercent() + "%</gold>"));
        }
        summary.add(ColorUtils.color("<gray>───────────────────────</gray>"));

        inventory.setItem(4, new ItemBuilder(Material.PLAYER_HEAD)
                .name(ColorUtils.color("<dark_green>Review Your Settings</dark_green>"))
                .skull(Heads.WIZARD_HAT)
                .lore(summary)
                .build());

        inventory.setItem(20, new ItemBuilder(Material.EMERALD_BLOCK)
                .name(ColorUtils.color("<green>Apply & Finish</green>"))
                .lore(
                        ColorUtils.color("<gray>Save these settings and enable taxes.</gray>"),
                        ColorUtils.color(""),
                        ColorUtils.color("<green>You can change any of these later</green>"),
                        ColorUtils.color("<green>with /taxmanager</green>")
                )
                .build());

        inventory.setItem(24, new ItemBuilder(Material.BARRIER)
                .name(ColorUtils.color("<red>Discard & Start Over</red>"))
                .lore(ColorUtils.color("<gray>Go back to the beginning</gray>"))
                .build());

        addNavigation(true, false);
    }

    // ═══════════════════════════════════════════
    //  Helpers
    // ═══════════════════════════════════════════

    private void addNavigation(boolean showBack, boolean showNext) {
        if (showBack) {
            inventory.setItem(45, new ItemBuilder(Material.ARROW)
                    .name(ColorUtils.color("<red>Back</red>"))
                    .build());
        }
        if (showNext) {
            inventory.setItem(53, new ItemBuilder(Material.ARROW)
                    .name(ColorUtils.color("<green>Next</green>"))
                    .build());
        }
    }

    private String formatMode(String mode) {
        return switch (mode) {
            case "INCOME_TAX" -> "Income Tax";
            case "SET_TAX" -> "Fixed Amount";
            case "PERCENTAGE_BALANCE" -> "Balance Percentage";
            default -> "None";
        };
    }

    private String formatMethod(String method) {
        return switch (method) {
            case "AUTO" -> "Auto Deduct";
            case "MANUAL" -> "Manual Payment";
            case "BANKNOTE" -> "Banknote Delivery";
            default -> method;
        };
    }

    private void nextPage() {
        WizardPage[] pages = WizardPage.values();
        int idx = currentPage.ordinal();
        if (idx < pages.length - 1) {
            currentPage = pages[idx + 1];
            populate();
        }
    }

    private void prevPage() {
        WizardPage[] pages = WizardPage.values();
        int idx = currentPage.ordinal();
        if (idx > 0) {
            currentPage = pages[idx - 1];
            populate();
        }
    }

    @Override
    public Inventory getInventory() { return inventory; }

    // ═══════════════════════════════════════════
    //  Click Handling
    // ═══════════════════════════════════════════

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getSlot();
        Player clicker = (Player) event.getWhoClicked();
        ConfigManager config = plugin.getConfigManager();

        if (slot == 45) { prevPage(); return; }
        if (slot == 53 && currentPage != WizardPage.SUMMARY) { nextPage(); return; }
        if (slot == 49) { clicker.closeInventory(); return; }

        switch (currentPage) {
            case WELCOME -> {
                if (slot == 22) nextPage();
            }
            case ENABLE -> {
                if (slot == 22) {
                    config.setTaxesEnabled(!config.isTaxesEnabled());
                    clicker.sendMessage(ColorUtils.color(config.isTaxesEnabled()
                            ? "<green>Taxes enabled!</green>"
                            : "<red>Taxes disabled.</red>"));
                    populate();
                }
            }
            case MODE -> {
                if (slot == 20) {
                    config.setTaxMode("INCOME_TAX");
                    clicker.sendMessage(ColorUtils.color("<green>Tax mode: Income Tax</green>"));
                    populate();
                } else if (slot == 22) {
                    config.setTaxMode("SET_TAX");
                    clicker.sendMessage(ColorUtils.color("<green>Tax mode: Fixed Amount</green>"));
                    populate();
                } else if (slot == 24) {
                    config.setTaxMode("PERCENTAGE_BALANCE");
                    clicker.sendMessage(ColorUtils.color("<green>Tax mode: Balance Percentage</green>"));
                    populate();
                }
            }
            case RATE -> {
                if (slot == 22) {
                    String mode = config.getTaxMode();
                    if ("INCOME_TAX".equals(mode)) {
                        plugin.getChatInputManager().requestInput(clicker,
                                "<gold>Enter income tax rate (%):</gold>",
                                input -> {
                                    try {
                                        double val = Double.parseDouble(input);
                                        if (val < 0 || val > 100) {
                                            clicker.sendMessage(ColorUtils.color("<red>Must be 0-100.</red>"));
                                            return;
                                        }
                                        config.setIncomeTaxRate(val);
                                        clicker.sendMessage(ColorUtils.color("<green>Income tax rate set to " + val + "%</green>"));
                                    } catch (NumberFormatException e) {
                                        clicker.sendMessage(ColorUtils.color("<red>Invalid number.</red>"));
                                    }
                                }, reopenSelf());
                    } else if ("SET_TAX".equals(mode)) {
                        plugin.getChatInputManager().requestInput(clicker,
                                "<gold>Enter fixed tax amount:</gold>",
                                input -> {
                                    try {
                                        double val = Double.parseDouble(input);
                                        if (val < 0) {
                                            clicker.sendMessage(ColorUtils.color("<red>Must be positive.</red>"));
                                            return;
                                        }
                                        config.setSetTaxAmount(val);
                                        clicker.sendMessage(ColorUtils.color("<green>Fixed tax set to " + plugin.getEconomyManager().format(val) + "</green>"));
                                    } catch (NumberFormatException e) {
                                        clicker.sendMessage(ColorUtils.color("<red>Invalid number.</red>"));
                                    }
                                }, reopenSelf());
                    } else if ("PERCENTAGE_BALANCE".equals(mode)) {
                        plugin.getChatInputManager().requestInput(clicker,
                                "<gold>Enter balance tax rate (%):</gold>",
                                input -> {
                                    try {
                                        double val = Double.parseDouble(input);
                                        if (val < 0 || val > 100) {
                                            clicker.sendMessage(ColorUtils.color("<red>Must be 0-100.</red>"));
                                            return;
                                        }
                                        config.setBalanceTaxRate(val);
                                        clicker.sendMessage(ColorUtils.color("<green>Balance tax rate set to " + val + "%</green>"));
                                    } catch (NumberFormatException e) {
                                        clicker.sendMessage(ColorUtils.color("<red>Invalid number.</red>"));
                                    }
                                }, reopenSelf());
                    }
                }
            }
            case INTERVAL -> {
                if (slot == 22) {
                    plugin.getChatInputManager().requestInput(clicker,
                            "<gold>Enter assessment interval (minutes, 0 = disabled):</gold>",
                            input -> {
                                try {
                                    int val = Integer.parseInt(input);
                                    if (val < 0) {
                                        clicker.sendMessage(ColorUtils.color("<red>Must be 0 or more.</red>"));
                                        return;
                                    }
                                    config.setTaxIntervalMinutes(val);
                                    clicker.sendMessage(ColorUtils.color("<green>Interval set to " + (val == 0 ? "disabled" : val + " minutes") + "</green>"));
                                } catch (NumberFormatException e) {
                                    clicker.sendMessage(ColorUtils.color("<red>Invalid number.</red>"));
                                }
                            }, reopenSelf());
                }
            }
            case GRACE_PERIOD -> {
                if (slot == 22) {
                    plugin.getChatInputManager().requestInput(clicker,
                            "<gold>Enter grace period (minutes, 0 = none):</gold>",
                            input -> {
                                try {
                                    int val = Integer.parseInt(input);
                                    if (val < 0) {
                                        clicker.sendMessage(ColorUtils.color("<red>Must be 0 or more.</red>"));
                                        return;
                                    }
                                    config.setGracePeriodMinutes(val);
                                    clicker.sendMessage(ColorUtils.color("<green>Grace period set to " + val + " minutes</green>"));
                                } catch (NumberFormatException e) {
                                    clicker.sendMessage(ColorUtils.color("<red>Invalid number.</red>"));
                                }
                            }, reopenSelf());
                }
            }
            case PAYMENT_METHOD -> {
                if (slot == 20) {
                    config.setTaxPaymentMethod("AUTO");
                    clicker.sendMessage(ColorUtils.color("<green>Payment method: Auto Deduct</green>"));
                    populate();
                } else if (slot == 22) {
                    config.setTaxPaymentMethod("MANUAL");
                    clicker.sendMessage(ColorUtils.color("<green>Payment method: Manual Payment</green>"));
                    populate();
                } else if (slot == 24) {
                    config.setTaxPaymentMethod("BANKNOTE");
                    clicker.sendMessage(ColorUtils.color("<green>Payment method: Banknote Delivery</green>"));
                    populate();
                }
            }
            case BANKNOTE_COLLECTORS -> {
                if (slot == 22) {
                    plugin.getChatInputManager().requestInput(clicker,
                            "<gold>Enter tax collector names (comma-separated):</gold>",
                            input -> {
                                List<String> targets = Arrays.stream(input.split(","))
                                        .map(String::trim)
                                        .filter(s -> !s.isEmpty())
                                        .collect(Collectors.toList());
                                config.setBanknoteDeliveryTargets(targets);
                                if (targets.isEmpty()) {
                                    clicker.sendMessage(ColorUtils.color("<yellow>Tax collectors cleared.</yellow>"));
                                } else {
                                    clicker.sendMessage(ColorUtils.color("<green>Tax collectors set to: " + String.join(", ", targets) + "</green>"));
                                }
                            }, reopenSelf());
                }
            }
            case PENALTY -> {
                if (slot == 22) {
                    plugin.getChatInputManager().requestInput(clicker,
                            "<gold>Enter overdue penalty percent:</gold>",
                            input -> {
                                try {
                                    double val = Double.parseDouble(input);
                                    if (val < 0 || val > 100) {
                                        clicker.sendMessage(ColorUtils.color("<red>Must be 0-100.</red>"));
                                        return;
                                    }
                                    config.setOverduePenaltyPercent(val);
                                    clicker.sendMessage(ColorUtils.color("<green>Overdue penalty set to " + val + "%</green>"));
                                } catch (NumberFormatException e) {
                                    clicker.sendMessage(ColorUtils.color("<red>Invalid number.</red>"));
                                }
                            }, reopenSelf());
                }
            }
            case SUMMARY -> {
                if (slot == 20) {
                    config.setTaxesEnabled(true);
                    clicker.sendMessage("Tax setup complete! Your settings have been applied.");
                    clicker.closeInventory();
                } else if (slot == 24) {
                    currentPage = WizardPage.WELCOME;
                    populate();
                }
            }
        }
    }
}
