package com.odeco.gui;

import com.odeco.ODEco;
import com.odeco.economy.SharedAccount;
import com.odeco.utils.ColorUtils;
import com.odeco.utils.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class SharedAccountListGUI implements InventoryHolder {

    private final ODEco plugin;
    private final Player player;
    private final Inventory inventory;
    private int page = 0;

    public SharedAccountListGUI(ODEco plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, ColorUtils.color("<aqua>Manage Shared Accounts</aqua>"));
        populate();
    }

    private void populate() {
        inventory.clear();

        List<SharedAccount> accounts = plugin.getEconomyManager().getAccountsForPlayer(player.getUniqueId());

        // Title bar
        inventory.setItem(4, new ItemBuilder(Material.PLAYER_HEAD)
                .name(ColorUtils.color("<aqua>Your Shared Accounts</aqua>"))
                .lore(ColorUtils.color("<gray>Click an account to manage it</gray>"))
                .build());

        // Create new account
        inventory.setItem(8, new ItemBuilder(Material.ANVIL)
                .name(ColorUtils.color("<green>Create Account</green>"))
                .lore(ColorUtils.color("<gray>Click to create a new shared account</gray>"))
                .build());

        int start = page * 36;
        int slot = 9;
        for (int i = start; i < Math.min(accounts.size(), start + 36); i++) {
            if (slot >= 45) break;
            SharedAccount account = accounts.get(i);
            Set<String> perms = account.getPermissions(player.getUniqueId());

            List<Component> lore = new ArrayList<>();
            lore.add(ColorUtils.color("<gray>Owner: " + Bukkit.getOfflinePlayer(account.getOwnerId()).getName() + "</gray>"));
            lore.add(ColorUtils.color("<gray>Members: " + account.getMembers().size() + "</gray>"));
            lore.add(ColorUtils.color("<gray>Your permissions:</gray>"));
            for (String perm : SharedAccount.ALL_PERMISSIONS) {
                if (perms.contains(perm)) {
                    lore.add(ColorUtils.color("  <green>" + perm + "</green>"));
                } else {
                    lore.add(ColorUtils.color("  <red>" + perm + "</red>"));
                }
            }
            lore.add(ColorUtils.color("<dark_gray>Click to manage</dark_gray>"));

            inventory.setItem(slot, new ItemBuilder(Material.BOOK)
                    .name(ColorUtils.color("<gold>" + account.getName() + "</gold>"))
                    .lore(lore)
                    .build());
            slot++;
        }

        // Pagination
        if (page > 0) {
            inventory.setItem(45, new ItemBuilder(Material.ARROW)
                    .name(ColorUtils.color("<yellow>Previous Page</yellow>"))
                    .build());
        }
        if (accounts.size() > (page + 1) * 36) {
            inventory.setItem(53, new ItemBuilder(Material.ARROW)
                    .name(ColorUtils.color("<yellow>Next Page</yellow>"))
                    .build());
        }
    }

    @Override
    public Inventory getInventory() { return inventory; }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getSlot();
        Player clicker = (Player) event.getWhoClicked();

        if (slot == 8) {
            plugin.getChatInputManager().requestInput(clicker,
                "<gold>Enter a name for the new shared account:</gold>",
                name -> {
                    if (name.isEmpty() || name.length() > 32) {
                        clicker.sendMessage(ColorUtils.color("<red>Account name must be 1-32 characters.</red>"));
                        return;
                    }
                    if (plugin.getEconomyManager().createSharedAccount(name, clicker.getUniqueId())) {
                        clicker.sendMessage(ColorUtils.color("<green>Created shared account '" + name + "'.</green>"));
                        clicker.openInventory(new SharedAccountListGUI(plugin, clicker).getInventory());
                    } else {
                        clicker.sendMessage(ColorUtils.color("<red>Account '" + name + "' already exists.</red>"));
                    }
                });
            return;
        }

        if (slot == 45 && page > 0) {
            page--;
            populate();
            return;
        }
        if (slot == 53) {
            page++;
            populate();
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName()) return;

        List<SharedAccount> accounts = plugin.getEconomyManager().getAccountsForPlayer(clicker.getUniqueId());
        int start = page * 36;
        int index = slot - 9 + start;
        if (index >= 0 && index < accounts.size()) {
            SharedAccount account = accounts.get(index);
            clicker.closeInventory();
            clicker.openInventory(new SharedAccountGUI(plugin, clicker, account).getInventory());
        }
    }

}
