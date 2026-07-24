package com.odeco.gui;

import com.odeco.ODEco;
import com.odeco.economy.SharedAccount;
import com.odeco.utils.ColorUtils;
import com.odeco.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.*;

public class SharedAccountGUI implements InventoryHolder {

    private final ODEco plugin;
    private final Player player;
    private final SharedAccount account;
    private final Inventory inventory;

    public SharedAccountGUI(ODEco plugin, Player player, SharedAccount account) {
        this.plugin = plugin;
        this.player = player;
        this.account = account;
        this.inventory = Bukkit.createInventory(this, 27, ColorUtils.color("<gold>" + account.getName() + "</gold>"));
        populate();
    }

    private void populate() {
        inventory.clear();

        // Account info
        String ownerName = Bukkit.getOfflinePlayer(account.getOwnerId()).getName();
        inventory.setItem(4, new ItemBuilder(Material.BOOK)
                .name(ColorUtils.color("<gold>" + account.getName() + "</gold>"))
                .lore(
                    ColorUtils.color("<gray>Owner: " + (ownerName != null ? ownerName : "Unknown") + "</gray>"),
                    ColorUtils.color("<gray>Members: " + account.getMembers().size() + "</gray>"),
                    ColorUtils.color("<gray>Balance: " + plugin.getEconomyManager().format(account.getBalance()) + "</gray>")
                )
                .build());

        // Members
        inventory.setItem(11, new ItemBuilder(Material.PLAYER_HEAD)
                .name(ColorUtils.color("<aqua>Members</aqua>"))
                .lore(ColorUtils.color("<gray>View and manage members</gray>"))
                .build());

        // Deposit
        if (account.hasPermission(player.getUniqueId(), SharedAccount.PERM_DEPOSIT)) {
            inventory.setItem(13, new ItemBuilder(Material.GREEN_DYE)
                    .name(ColorUtils.color("<green>Deposit</green>"))
                    .lore(ColorUtils.color("<gray>Deposit money into this account</gray>"))
                    .build());
        }

        // Withdraw
        if (account.hasPermission(player.getUniqueId(), SharedAccount.PERM_WITHDRAW)) {
            inventory.setItem(15, new ItemBuilder(Material.RED_DYE)
                    .name(ColorUtils.color("<red>Withdraw</red>"))
                    .lore(ColorUtils.color("<gray>Withdraw money from this account</gray>"))
                    .build());
        }

        // Back
        inventory.setItem(22, new ItemBuilder(Material.ARROW)
                .name(ColorUtils.color("<yellow>Back to Accounts</yellow>"))
                .build());
    }

    @Override
    public Inventory getInventory() { return inventory; }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getSlot();
        Player clicker = (Player) event.getWhoClicked();

        switch (slot) {
            case 11 -> {
                clicker.closeInventory();
                clicker.openInventory(new SharedAccountMembersGUI(plugin, clicker, account).getInventory());
            }
            case 13 -> promptDeposit(clicker);
            case 15 -> promptWithdraw(clicker);
            case 22 -> {
                clicker.closeInventory();
                clicker.openInventory(new SharedAccountListGUI(plugin, clicker).getInventory());
            }
        }
    }

    private void promptDeposit(Player clicker) {
        plugin.getChatInputManager().requestInput(clicker,
            "<gold>Enter the amount to deposit into '" + account.getName() + "':</gold>",
            amountStr -> {
                try {
                    double amount = Double.parseDouble(amountStr);
                    if (amount <= 0) {
                        clicker.sendMessage(ColorUtils.color("<red>Amount must be positive.</red>"));
                        return;
                    }
                    if (!plugin.getEconomyManager().withdraw(clicker.getUniqueId(), amount)) {
                        clicker.sendMessage(ColorUtils.color("<red>Insufficient balance.</red>"));
                        return;
                    }
                    account.setBalance(account.getBalance() + amount);
                    plugin.getEconomyManager().saveData();
                    plugin.getEconomyManager().logTransaction(clicker.getUniqueId(), "shared_deposit", amount, "Deposit to " + account.getName());
                    clicker.sendMessage(ColorUtils.color("<green>Deposited " + plugin.getEconomyManager().format(amount) + " into '" + account.getName() + "'.</green>"));
                } catch (NumberFormatException e) {
                    clicker.sendMessage(ColorUtils.color("<red>Invalid amount.</red>"));
                }
            });
    }

    private void promptWithdraw(Player clicker) {
        plugin.getChatInputManager().requestInput(clicker,
            "<gold>Enter the amount to withdraw from '" + account.getName() + "':</gold>",
            amountStr -> {
                try {
                    double amount = Double.parseDouble(amountStr);
                    if (amount <= 0) {
                        clicker.sendMessage(ColorUtils.color("<red>Amount must be positive.</red>"));
                        return;
                    }
                    if (account.getBalance() < amount) {
                        clicker.sendMessage(ColorUtils.color("<red>Insufficient shared account balance.</red>"));
                        return;
                    }
                    account.setBalance(account.getBalance() - amount);
                    plugin.getEconomyManager().deposit(clicker.getUniqueId(), amount);
                    plugin.getEconomyManager().saveData();
                    plugin.getEconomyManager().logTransaction(clicker.getUniqueId(), "shared_withdraw", amount, "Withdraw from " + account.getName());
                    clicker.sendMessage(ColorUtils.color("<green>Withdrew " + plugin.getEconomyManager().format(amount) + " from '" + account.getName() + "'.</green>"));
                } catch (NumberFormatException e) {
                    clicker.sendMessage(ColorUtils.color("<red>Invalid amount.</red>"));
                }
            });
    }
}
