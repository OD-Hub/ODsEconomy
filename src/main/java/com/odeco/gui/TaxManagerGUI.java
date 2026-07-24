package com.odeco.gui;

import com.odeco.ODEco;
import com.odeco.economy.EconomyManager;
import com.odeco.utils.ColorUtils;
import com.odeco.utils.Heads;
import com.odeco.utils.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.*;

public class TaxManagerGUI implements InventoryHolder {

    private final ODEco plugin;
    private final Player player;
    private final Inventory inventory;
    private final boolean isAdmin;
    private TaxPage currentPage;
    private int pageOffset = 0;

    private enum TaxPage {
        PLAYER_INFO,
        ADMIN_MAIN,
        ADMIN_CONFIGURE,
        ADMIN_MANAGE_DEBTS
    }

    public TaxManagerGUI(ODEco plugin, Player player) {
        this(plugin, player, false);
    }

    public TaxManagerGUI(ODEco plugin, Player player, boolean forcePlayerView) {
        this.plugin = plugin;
        this.player = player;
        this.isAdmin = !forcePlayerView && player.hasPermission("odeco.admin");
        this.inventory = Bukkit.createInventory(this, 54, ColorUtils.color("<dark_red>Tax Manager</dark_red>"));
        this.currentPage = isAdmin ? TaxPage.ADMIN_MAIN : TaxPage.PLAYER_INFO;
        populate();
    }

    private void populate() {
        inventory.clear();
        switch (currentPage) {
            case PLAYER_INFO -> populatePlayerInfo();
            case ADMIN_MAIN -> populateAdminMain();
            case ADMIN_CONFIGURE -> populateAdminConfigure();
            case ADMIN_MANAGE_DEBTS -> populateAdminManageDebts();
        }
    }

    // ═══════════════════════════════════════════
    //  Player View — Single Page
    // ═══════════════════════════════════════════

    private void populatePlayerInfo() {
        var config = plugin.getConfigManager();
        EconomyManager economy = plugin.getEconomyManager();
        String mode = config.getTaxMode();
        double debt = economy.getTaxDebt(player.getUniqueId());
        boolean graceActive = economy.isGracePeriodActive(player.getUniqueId());

        List<Component> lore = new ArrayList<>();
        lore.add(ColorUtils.color("<gray>Mode: <white>" + mode + "</white></gray>"));
        if ("INCOME_TAX".equals(mode)) {
            lore.add(ColorUtils.color("<gray>Income Tax Rate: " + config.getIncomeTaxRate() + "%</gray>"));
        } else if ("SET_TAX".equals(mode)) {
            lore.add(ColorUtils.color("<gray>Set Amount: " + economy.format(config.getSetTaxAmount()) + "</gray>"));
        } else if ("PERCENTAGE_BALANCE".equals(mode)) {
            lore.add(ColorUtils.color("<gray>Balance Tax Rate: " + config.getBalanceTaxRate() + "%</gray>"));
        }
        lore.add(ColorUtils.color("<gray>Payment Method: " + config.getTaxPaymentMethod() + "</gray>"));
        if (config.isTaxIntervalEnabled()) {
            lore.add(ColorUtils.color("<gray>Assessment Interval: " + config.getTaxIntervalMinutes() + " min</gray>"));
        } else {
            lore.add(ColorUtils.color("<gray>Assessment: <red>Disabled</red></gray>"));
        }
        if (config.getGracePeriodMinutes() > 0) {
            lore.add(ColorUtils.color("<gray>Grace Period: " + config.getGracePeriodMinutes() + " min</gray>"));
        }
        if ("BANKNOTE".equals(config.getTaxPaymentMethod())) {
            List<String> targets = config.getBanknoteDeliveryTargets();
            String targetStr = targets.isEmpty() ? "an admin" : String.join(", ", targets);
            lore.add(ColorUtils.color("<gray>Deliver banknote to: <white>" + targetStr + "</white></gray>"));
        }
        if ("BANKNOTE".equals(config.getTaxPaymentMethod()) && graceActive) {
            double assessed = economy.getAssessedTaxAmount(player.getUniqueId());
            if (assessed > 0) {
                lore.add(ColorUtils.color("<yellow>Tax Amount Due: " + economy.format(assessed) + "</yellow>"));
            }
        }
        lore.add(Component.empty());
        if (debt > 0) {
            lore.add(ColorUtils.color("<red>Your Debt: " + economy.format(debt) + "</red>"));
            if (graceActive) {
                lore.add(ColorUtils.color("<yellow>Grace Period: " + economy.getRemainingGracePeriodFormatted(player.getUniqueId()) + " left</yellow>"));
            }
        } else {
            lore.add(ColorUtils.color("<green>No tax debt</green>"));
        }

        inventory.setItem(4, new ItemBuilder(Material.PLAYER_HEAD)
                .name(ColorUtils.color("<dark_red>Tax Information</dark_red>"))
                .skull(Heads.TAXES)
                .lore(lore)
                .build());

        if (debt > 0 && graceActive && "MANUAL".equals(config.getTaxPaymentMethod())) {
            inventory.setItem(20, new ItemBuilder(Material.EMERALD)
                    .name(ColorUtils.color("<green>Pay Taxes</green>"))
                    .lore(
                            ColorUtils.color("<gray>Left-click: Pay from balance</gray>"),
                            ColorUtils.color("<gray>Right-click: Pay to shared account</gray>"),
                            ColorUtils.color("<yellow>Debt: " + economy.format(debt) + "</yellow>")
                    )
                    .build());
        } else if (debt > 0 && graceActive && "BANKNOTE".equals(config.getTaxPaymentMethod())) {
            inventory.setItem(20, new ItemBuilder(Material.PAPER)
                    .name(ColorUtils.color("<gold>Banknote Payment</gold>"))
                    .lore(
                            ColorUtils.color("<gray>Create a banknote for " + economy.format(debt) + "</gray>"),
                            ColorUtils.color("<gray>using /banknote " + String.format("%.2f", debt) + "</gray>"),
                            ColorUtils.color("<gray>Then deliver it to the designated collector.</gray>")
                    )
                    .build());
        }

        inventory.setItem(49, new ItemBuilder(Material.ARROW)
                .name(ColorUtils.color("<red>Back</red>"))
                .build());
    }

    // ═══════════════════════════════════════════
    //  Admin View — 3 Pages
    // ═══════════════════════════════════════════

    private void populateAdminMain() {
        var config = plugin.getConfigManager();
        EconomyManager economy = plugin.getEconomyManager();
        boolean enabled = config.isTaxesEnabled();
        String mode = config.getTaxMode();
        String statusColor = enabled ? "<green>" : "<red>";
        String statusText = enabled ? "ENABLED" : "DISABLED";

        inventory.setItem(4, new ItemBuilder(Material.PLAYER_HEAD)
                .name(ColorUtils.color("<dark_red>Tax Overview</dark_red>"))
                .skull(Heads.TAXES)
                .lore(
                        ColorUtils.color("<gray>Status: " + statusColor + statusText + "</gray>"),
                        ColorUtils.color("<gray>Mode: <white>" + mode + "</white></gray>"),
                        ColorUtils.color("<gray>Interval: " + (config.isTaxIntervalEnabled() ? config.getTaxIntervalMinutes() + " min" : "Disabled") + "</gray>"),
                        ColorUtils.color("<gray>Grace Period: " + config.getGracePeriodMinutes() + " min</gray>"),
                        ColorUtils.color("<gray>Payment: " + config.getTaxPaymentMethod() + "</gray>"),
                        ColorUtils.color("<gray>Total Debt: " + economy.format(getTotalDebt()) + "</gray>")
                )
                .build());

        inventory.setItem(20, new ItemBuilder(Material.PLAYER_HEAD)
                .name(ColorUtils.color("<yellow>Configure Taxes</yellow>"))
                .skull(Heads.BANKNOTE)
                .lore(ColorUtils.color("<gray>Set tax mode, rates, intervals</gray>"))
                .build());

        inventory.setItem(22, new ItemBuilder(Material.PLAYER_HEAD)
                .name(ColorUtils.color("<red>Manage Debts</red>"))
                .skull(Heads.TRANSACTIONS)
                .lore(
                        ColorUtils.color("<gray>View and clear player tax debts</gray>"),
                        ColorUtils.color("<gray>Unpaid: " + economy.getAllTaxDebts().size() + " players</gray>")
                )
                .build());

        inventory.setItem(24, new ItemBuilder(Material.PLAYER_HEAD)
                .name(ColorUtils.color("<green>Assess All Taxes Now</green>"))
                .skull(Heads.PAY)
                .lore(ColorUtils.color("<gray>Force tax assessment for all players</gray>"))
                .build());

        inventory.setItem(40, new ItemBuilder(Material.PLAYER_HEAD)
                .name(ColorUtils.color("<aqua>Setup Wizard</aqua>"))
                .skull(Heads.WIZARD_HAT)
                .lore(
                        ColorUtils.color("<gray>Walk through tax setup step by step</gray>"),
                        ColorUtils.color("<gray>with friendly explanations</gray>")
                )
                .build());
    }

    private void populateAdminConfigure() {
        var config = plugin.getConfigManager();
        EconomyManager economy = plugin.getEconomyManager();

        inventory.setItem(0, new ItemBuilder(Material.ARROW)
                .name(ColorUtils.color("<red>Back</red>"))
                .build());

        inventory.setItem(4, new ItemBuilder(Material.PLAYER_HEAD)
                .name(ColorUtils.color("<yellow>Configure Taxes</yellow>"))
                .skull(Heads.TAXES)
                .lore(ColorUtils.color("<gray>Click options below to configure</gray>"))
                .build());

        boolean enabled = config.isTaxesEnabled();
        inventory.setItem(10, new ItemBuilder(enabled ? Material.LIME_DYE : Material.GRAY_DYE)
                .name(ColorUtils.color(enabled ? "<green>Taxes: ENABLED</green>" : "<red>Taxes: DISABLED</red>"))
                .lore(ColorUtils.color("<gray>Click to toggle</gray>"))
                .build());

        String mode = config.getTaxMode();
        inventory.setItem(11, new ItemBuilder(Material.COMPARATOR)
                .name(ColorUtils.color("<gold>Tax Mode: " + mode + "</gold>"))
                .lore(
                        ColorUtils.color("<gray>Click to cycle modes</gray>"),
                        ColorUtils.color("<dark_gray>Options: NONE, INCOME_TAX, SET_TAX, PERCENTAGE_BALANCE</dark_gray>")
                )
                .build());

        inventory.setItem(12, new ItemBuilder(Material.GOLD_INGOT)
                .name(ColorUtils.color("<gold>Income Tax Rate</gold>"))
                .lore(
                        ColorUtils.color("<gray>Current: " + config.getIncomeTaxRate() + "%</gray>"),
                        ColorUtils.color("<dark_gray>Click to set (input in chat)</dark_gray>")
                )
                .build());

        inventory.setItem(13, new ItemBuilder(Material.IRON_INGOT)
                .name(ColorUtils.color("<gold>Set Tax Amount</gold>"))
                .lore(
                        ColorUtils.color("<gray>Current: " + economy.format(config.getSetTaxAmount()) + "</gray>"),
                        ColorUtils.color("<dark_gray>Click to set (input in chat)</dark_gray>")
                )
                .build());

        inventory.setItem(14, new ItemBuilder(Material.NETHERITE_INGOT)
                .name(ColorUtils.color("<gold>Balance Tax Rate</gold>"))
                .lore(
                        ColorUtils.color("<gray>Current: " + config.getBalanceTaxRate() + "%</gray>"),
                        ColorUtils.color("<dark_gray>Click to set (input in chat)</dark_gray>")
                )
                .build());

        inventory.setItem(15, new ItemBuilder(Material.CLOCK)
                .name(ColorUtils.color("<gold>Assessment Interval</gold>"))
                .lore(
                        ColorUtils.color("<gray>Current: " + (config.isTaxIntervalEnabled() ? config.getTaxIntervalMinutes() + " min" : "Disabled (0)") + "</gray>"),
                        ColorUtils.color("<dark_gray>Click to set (0 = disabled)</dark_gray>")
                )
                .build());

        inventory.setItem(16, new ItemBuilder(Material.RED_BED)
                .name(ColorUtils.color("<gold>Grace Period</gold>"))
                .lore(
                        ColorUtils.color("<gray>Current: " + config.getGracePeriodMinutes() + " min</gray>"),
                        ColorUtils.color("<dark_gray>Click to set (0 = no grace)</dark_gray>")
                )
                .build());

        inventory.setItem(17, new ItemBuilder(Material.HOPPER)
                .name(ColorUtils.color("<gold>Payment Method</gold>"))
                .lore(
                        ColorUtils.color("<gray>Current: " + config.getTaxPaymentMethod() + "</gray>"),
                        ColorUtils.color("<dark_gray>Click to cycle: AUTO, MANUAL, BANKNOTE</dark_gray>")
                )
                .build());

        inventory.setItem(19, new ItemBuilder(Material.BARREL)
                .name(ColorUtils.color("<gold>Pay To</gold>"))
                .lore(
                        ColorUtils.color("<gray>Current: " + config.getTaxPayTo() + "</gray>"),
                        ColorUtils.color("<dark_gray>Click to cycle: SERVER, SHARED_ACCOUNT</dark_gray>")
                )
                .build());

        inventory.setItem(20, new ItemBuilder(Material.CHEST)
                .name(ColorUtils.color("<gold>Shared Account Name</gold>"))
                .lore(
                        ColorUtils.color("<gray>Current: " + config.getTaxSharedAccount() + "</gray>"),
                        ColorUtils.color("<dark_gray>Click to set (input in chat)</dark_gray>")
                )
                .build());

        inventory.setItem(21, new ItemBuilder(Material.PAPER)
                .name(ColorUtils.color("<gold>Show Info Button</gold>"))
                .lore(
                        ColorUtils.color("<gray>Current: " + (config.isShowInfoButton() ? "Yes" : "No") + "</gray>"),
                        ColorUtils.color("<dark_gray>Show Tax Info on eco panel when no payment due</dark_gray>"),
                        ColorUtils.color("<dark_gray>Click to toggle</dark_gray>")
                )
                .build());

        inventory.setItem(22, new ItemBuilder(Material.TRIPWIRE_HOOK)
                .name(ColorUtils.color("<gold>Overdue Penalty %</gold>"))
                .lore(
                        ColorUtils.color("<gray>Current: " + config.getOverduePenaltyPercent() + "%</gray>"),
                        ColorUtils.color("<dark_gray>Click to set (input in chat)</dark_gray>")
                )
                .build());

        inventory.setItem(23, new ItemBuilder(Material.PLAYER_HEAD)
                .name(ColorUtils.color("<gold>Banknote Delivery Targets</gold>"))
                .skull(Heads.BOUNTY)
                .lore(
                        ColorUtils.color("<gray>Current: " + config.getBanknoteDeliveryTargets() + "</gray>"),
                        ColorUtils.color("<dark_gray>Player names who collect banknote taxes</dark_gray>"),
                        ColorUtils.color("<dark_gray>Click to set (comma-separated in chat)</dark_gray>")
                )
                .build());

        inventory.setItem(31, new ItemBuilder(Material.EMERALD_BLOCK)
                .name(ColorUtils.color("<green>Save Configuration</green>"))
                .lore(ColorUtils.color("<gray>Click to apply settings</gray>"))
                .build());
    }

    private void populateAdminManageDebts() {
        EconomyManager economy = plugin.getEconomyManager();
        Map<UUID, Double> debts = economy.getAllTaxDebts();

        inventory.setItem(0, new ItemBuilder(Material.ARROW)
                .name(ColorUtils.color("<red>Back</red>"))
                .build());

        inventory.setItem(4, new ItemBuilder(Material.PLAYER_HEAD)
                .name(ColorUtils.color("<red>Tax Debts</red>"))
                .skull(Heads.TRANSACTIONS)
                .lore(ColorUtils.color("<gray>Total owed: " + economy.format(getTotalDebt()) + "</gray>"))
                .build());

        List<Map.Entry<UUID, Double>> entries = new ArrayList<>(debts.entrySet());
        int start = pageOffset * 36;
        int slot = 9;
        for (int i = start; i < Math.min(entries.size(), start + 36); i++) {
            if (slot >= 45) break;
            Map.Entry<UUID, Double> entry = entries.get(i);
            UUID playerId = entry.getKey();
            double amount = entry.getValue();
            OfflinePlayer off = Bukkit.getOfflinePlayer(playerId);
            String name = off.getName() != null ? off.getName() : "Unknown";

            inventory.setItem(slot, new ItemBuilder(Material.PLAYER_HEAD)
                    .name(ColorUtils.color("<red>" + name + "</red>"))
                    .lore(
                            ColorUtils.color("<yellow>Debt: " + economy.format(amount) + "</yellow>"),
                            ColorUtils.color("<green>Left-click: Clear debt</green>"),
                            ColorUtils.color("<dark_gray>Right-click: Send reminder</dark_gray>")
                    )
                    .build());
            slot++;
        }

        if (entries.isEmpty()) {
            inventory.setItem(22, new ItemBuilder(Material.LIME_DYE)
                    .name(ColorUtils.color("<green>No Unpaid Taxes</green>"))
                    .lore(ColorUtils.color("<gray>All players are up to date!</gray>"))
                    .build());
        }

        if (pageOffset > 0) {
            inventory.setItem(45, new ItemBuilder(Material.ARROW)
                    .name(ColorUtils.color("<yellow>Previous Page</yellow>"))
                    .build());
        }
        if (entries.size() > (pageOffset + 1) * 36) {
            inventory.setItem(53, new ItemBuilder(Material.ARROW)
                    .name(ColorUtils.color("<yellow>Next Page</yellow>"))
                    .build());
        }

        inventory.setItem(48, new ItemBuilder(Material.BARRIER)
                .name(ColorUtils.color("<red>Clear All Debts</red>"))
                .lore(ColorUtils.color("<gray>Click to forgive all tax debts</gray>"))
                .build());
    }

    private double getTotalDebt() {
        return plugin.getEconomyManager().getAllTaxDebts().values().stream().mapToDouble(Double::doubleValue).sum();
    }

    @Override
    public Inventory getInventory() { return inventory; }

    private Runnable reopenSelf() {
        return () -> player.openInventory(new TaxManagerGUI(plugin, player).getInventory());
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getSlot();
        Player clicker = (Player) event.getWhoClicked();
        com.odeco.config.ConfigManager config = plugin.getConfigManager();
        EconomyManager economy = plugin.getEconomyManager();

        if (!isAdmin) {
            handlePlayerClick(slot, clicker, config, economy, event);
        } else {
            handleAdminClick(slot, event, clicker, config, economy);
        }
    }

    private void handlePlayerClick(int slot, Player clicker, com.odeco.config.ConfigManager config, EconomyManager economy, InventoryClickEvent event) {
        switch (slot) {
            case 20 -> {
                double debt = economy.getTaxDebt(clicker.getUniqueId());
                if (debt <= 0) return;

                if ("MANUAL".equals(config.getTaxPaymentMethod())) {
                    if (event.isRightClick()) {
                        if (economy.payTaxDebtToSharedAccount(clicker.getUniqueId(), debt)) {
                            clicker.sendMessage(ColorUtils.color("<green>Paid " + economy.format(debt) + " taxes to shared account '" + config.getTaxSharedAccount() + "'.</green>"));
                        } else {
                            clicker.sendMessage(ColorUtils.color("<red>Could not pay tax. Insufficient balance.</red>"));
                        }
                    } else {
                        if (economy.payTaxDebt(clicker.getUniqueId(), debt)) {
                            clicker.sendMessage(ColorUtils.color("<green>Paid " + economy.format(debt) + " in taxes!</green>"));
                        } else {
                            clicker.sendMessage(ColorUtils.color("<red>Could not pay tax. Insufficient balance.</red>"));
                        }
                    }
                    populate();
                }
            }
            case 49 -> {
                clicker.closeInventory();
                clicker.openInventory(new EcoPanelGUI(plugin, clicker).getInventory());
            }
        }
    }

    private void handleAdminClick(int slot, InventoryClickEvent event, Player clicker, com.odeco.config.ConfigManager config, EconomyManager economy) {
        switch (currentPage) {
            case ADMIN_MAIN -> {
                switch (slot) {
                    case 20 -> { currentPage = TaxPage.ADMIN_CONFIGURE; populate(); }
                    case 22 -> { currentPage = TaxPage.ADMIN_MANAGE_DEBTS; pageOffset = 0; populate(); }
                    case 24 -> {
                        economy.assessTaxes(true);
                        clicker.sendMessage(ColorUtils.color("<green>Taxes assessed for all eligible players.</green>"));
                        populate();
                    }
                    case 40 -> {
                        clicker.closeInventory();
                        clicker.openInventory(new TaxSetupWizardGUI(plugin, clicker).getInventory());
                    }
                }
            }
            case ADMIN_CONFIGURE -> {
                switch (slot) {
                    case 0 -> { currentPage = TaxPage.ADMIN_MAIN; populate(); }
                    case 10 -> {
                        config.setTaxesEnabled(!config.isTaxesEnabled());
                        clicker.sendMessage(ColorUtils.color("<green>Taxes " + (config.isTaxesEnabled() ? "enabled" : "disabled") + ".</green>"));
                        populate();
                    }
                    case 11 -> {
                        String[] modes = {"NONE", "INCOME_TAX", "SET_TAX", "PERCENTAGE_BALANCE"};
                        String current = config.getTaxMode();
                        int idx = 0;
                        for (int i = 0; i < modes.length; i++) {
                            if (modes[i].equals(current)) { idx = (i + 1) % modes.length; break; }
                        }
                        config.setTaxMode(modes[idx]);
                        clicker.sendMessage(ColorUtils.color("<green>Tax mode set to " + modes[idx] + "</green>"));
                        populate();
                    }
                    case 12 -> {
                        plugin.getChatInputManager().requestInput(clicker,
                                "<gold>Enter income tax rate (%):</gold>",
                                amountStr -> {
                                    try {
                                        double val = Double.parseDouble(amountStr);
                                        if (val < 0 || val > 100) { clicker.sendMessage(ColorUtils.color("<red>Must be 0-100.</red>")); return; }
                                        config.setIncomeTaxRate(val);
                                        clicker.sendMessage(ColorUtils.color("<green>Income tax rate set to " + val + "%</green>"));
                                    } catch (NumberFormatException e) { clicker.sendMessage(ColorUtils.color("<red>Invalid number.</red>")); }
                                }, reopenSelf());
                    }
                    case 13 -> {
                        plugin.getChatInputManager().requestInput(clicker,
                                "<gold>Enter set tax amount:</gold>",
                                amountStr -> {
                                    try {
                                        double val = Double.parseDouble(amountStr);
                                        if (val < 0) { clicker.sendMessage(ColorUtils.color("<red>Must be positive.</red>")); return; }
                                        config.setSetTaxAmount(val);
                                        clicker.sendMessage(ColorUtils.color("<green>Set tax amount: " + economy.format(val) + "</green>"));
                                    } catch (NumberFormatException e) { clicker.sendMessage(ColorUtils.color("<red>Invalid number.</red>")); }
                                }, reopenSelf());
                    }
                    case 14 -> {
                        plugin.getChatInputManager().requestInput(clicker,
                                "<gold>Enter balance tax rate (%):</gold>",
                                amountStr -> {
                                    try {
                                        double val = Double.parseDouble(amountStr);
                                        if (val < 0 || val > 100) { clicker.sendMessage(ColorUtils.color("<red>Must be 0-100.</red>")); return; }
                                        config.setBalanceTaxRate(val);
                                        clicker.sendMessage(ColorUtils.color("<green>Balance tax rate set to " + val + "%</green>"));
                                    } catch (NumberFormatException e) { clicker.sendMessage(ColorUtils.color("<red>Invalid number.</red>")); }
                                }, reopenSelf());
                    }
                    case 15 -> {
                        plugin.getChatInputManager().requestInput(clicker,
                                "<gold>Enter assessment interval (minutes, 0 = disabled):</gold>",
                                amountStr -> {
                                    try {
                                        int val = Integer.parseInt(amountStr);
                                        if (val < 0) { clicker.sendMessage(ColorUtils.color("<red>Must be 0 or more.</red>")); return; }
                                        config.setTaxIntervalMinutes(val);
                                        clicker.sendMessage(ColorUtils.color("<green>Tax interval set to " + (val == 0 ? "disabled" : val + " minutes") + "</green>"));
                                    } catch (NumberFormatException e) { clicker.sendMessage(ColorUtils.color("<red>Invalid number.</red>")); }
                                }, reopenSelf());
                    }
                    case 16 -> {
                        plugin.getChatInputManager().requestInput(clicker,
                                "<gold>Enter grace period (minutes, 0 = none):</gold>",
                                amountStr -> {
                                    try {
                                        int val = Integer.parseInt(amountStr);
                                        if (val < 0) { clicker.sendMessage(ColorUtils.color("<red>Must be 0 or more.</red>")); return; }
                                        config.setGracePeriodMinutes(val);
                                        clicker.sendMessage(ColorUtils.color("<green>Grace period set to " + val + " minutes</green>"));
                                    } catch (NumberFormatException e) { clicker.sendMessage(ColorUtils.color("<red>Invalid number.</red>")); }
                                }, reopenSelf());
                    }
                    case 17 -> {
                        String[] methods = {"AUTO", "MANUAL", "BANKNOTE"};
                        String current = config.getTaxPaymentMethod();
                        int idx = 0;
                        for (int i = 0; i < methods.length; i++) {
                            if (methods[i].equals(current)) { idx = (i + 1) % methods.length; break; }
                        }
                        config.setTaxPaymentMethod(methods[idx]);
                        clicker.sendMessage(ColorUtils.color("<green>Payment method: " + methods[idx] + "</green>"));
                        populate();
                    }
                    case 19 -> {
                        String[] payTos = {"SERVER", "SHARED_ACCOUNT"};
                        String current = config.getTaxPayTo();
                        int idx = 0;
                        for (int i = 0; i < payTos.length; i++) {
                            if (payTos[i].equals(current)) { idx = (i + 1) % payTos.length; break; }
                        }
                        config.setTaxPayTo(payTos[idx]);
                        clicker.sendMessage(ColorUtils.color("<green>Pay to: " + payTos[idx] + "</green>"));
                        populate();
                    }
                    case 20 -> {
                        plugin.getChatInputManager().requestInput(clicker,
                                "<gold>Enter shared account name for tax payments:</gold>",
                                name -> {
                                    config.setTaxSharedAccount(name);
                                    clicker.sendMessage(ColorUtils.color("<green>Tax shared account set to '" + name + "'</green>"));
                                }, reopenSelf());
                    }
                    case 21 -> {
                        config.setShowInfoButton(!config.isShowInfoButton());
                        clicker.sendMessage(ColorUtils.color("<green>Show Info Button: " + (config.isShowInfoButton() ? "Yes" : "No") + "</green>"));
                        populate();
                    }
                    case 22 -> {
                        plugin.getChatInputManager().requestInput(clicker,
                                "<gold>Enter overdue penalty percent:</gold>",
                                amountStr -> {
                                    try {
                                        double val = Double.parseDouble(amountStr);
                                        if (val < 0 || val > 100) { clicker.sendMessage(ColorUtils.color("<red>Must be 0-100.</red>")); return; }
                                        config.setOverduePenaltyPercent(val);
                                        clicker.sendMessage(ColorUtils.color("<green>Overdue penalty set to " + val + "%</green>"));
                                    } catch (NumberFormatException e) { clicker.sendMessage(ColorUtils.color("<red>Invalid number.</red>")); }
                                }, reopenSelf());
                    }
                    case 23 -> {
                        plugin.getChatInputManager().requestInput(clicker,
                                "<gold>Enter banknote delivery targets (comma-separated player names):</gold>",
                                input -> {
                                    List<String> targets = Arrays.asList(input.split(","));
                                    targets = targets.stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
                                    config.setBanknoteDeliveryTargets(targets);
                                    clicker.sendMessage(ColorUtils.color("<green>Delivery targets set to: " + targets + "</green>"));
                                }, reopenSelf());
                    }
                    case 31 -> {
                        clicker.sendMessage(ColorUtils.color("<green>Tax configuration saved.</green>"));
                        currentPage = TaxPage.ADMIN_MAIN;
                        populate();
                    }
                }
            }
            case ADMIN_MANAGE_DEBTS -> {
                switch (slot) {
                    case 0 -> { currentPage = TaxPage.ADMIN_MAIN; populate(); }
                    case 48 -> {
                        economy.clearAllTaxDebts();
                        clicker.sendMessage(ColorUtils.color("<green>All tax debts cleared.</green>"));
                        populate();
                    }
                }

                if (slot >= 9 && slot <= 44) {
                    List<Map.Entry<UUID, Double>> entries = new ArrayList<>(economy.getAllTaxDebts().entrySet());
                    int index = pageOffset * 36 + (slot - 9);
                    if (index >= 0 && index < entries.size()) {
                        Map.Entry<UUID, Double> entry = entries.get(index);
                        UUID targetId = entry.getKey();
                        OfflinePlayer off = Bukkit.getOfflinePlayer(targetId);

                        if (event.isLeftClick()) {
                            economy.clearPlayerTaxDebt(targetId);
                            clicker.sendMessage(ColorUtils.color("<green>Cleared tax debt for " + (off.getName() != null ? off.getName() : "Unknown") + "</green>"));
                            populate();
                        } else if (event.isRightClick()) {
                            Player online = Bukkit.getPlayer(targetId);
                            if (online != null && online.isOnline()) {
                                online.sendMessage(com.odeco.utils.ColorUtils.color(
                                        "<red>Reminder: You owe " + economy.format(entry.getValue()) + " in taxes. Press T and type /taxmanager to pay.</red>"));
                                clicker.sendMessage(ColorUtils.color("<green>Reminder sent to " + off.getName() + "</green>"));
                            } else {
                                clicker.sendMessage(ColorUtils.color("<red>Player is not online.</red>"));
                            }
                        }
                    }
                }

                if (slot == 45 && pageOffset > 0) { pageOffset--; populate(); }
                if (slot == 53) { pageOffset++; populate(); }
            }
        }
    }
}
