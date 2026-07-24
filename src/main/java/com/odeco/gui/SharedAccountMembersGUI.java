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

public class SharedAccountMembersGUI implements InventoryHolder {

    private final ODEco plugin;
    private final Player player;
    private final SharedAccount account;
    private final Inventory inventory;
    private int page = 0;
    private UUID managingMember = null;

    public SharedAccountMembersGUI(ODEco plugin, Player player, SharedAccount account) {
        this.plugin = plugin;
        this.player = player;
        this.account = account;
        this.inventory = Bukkit.createInventory(this, 54, ColorUtils.color("<aqua>" + account.getName() + " Members</aqua>"));
        populate();
    }

    private void populate() {
        inventory.clear();
        boolean canManage = account.hasPermission(player.getUniqueId(), SharedAccount.PERM_MANAGE_PERMISSIONS)
                || account.isOwner(player.getUniqueId());

        if (managingMember == null) {
            // Normal member list view
            inventory.setItem(4, new ItemBuilder(Material.PLAYER_HEAD)
                    .name(ColorUtils.color("<aqua>Members</aqua>"))
                    .lore(
                        ColorUtils.color("<gray>Total: " + account.getMembers().size() + "</gray>"),
                        ColorUtils.color(canManage ? "<green>Click a member to manage permissions</green>" : "<gray>Viewing members</gray>")
                    )
                    .build());

            if (canManage && account.hasPermission(player.getUniqueId(), SharedAccount.PERM_INVITE)) {
                inventory.setItem(8, new ItemBuilder(Material.ANVIL)
                        .name(ColorUtils.color("<green>Invite Member</green>"))
                        .lore(ColorUtils.color("<gray>Click to invite a new member</gray>"))
                        .build());
            }

            List<UUID> members = new ArrayList<>(account.getMembers());
            int start = page * 36;
            int slot = 9;
            for (int i = start; i < Math.min(members.size(), start + 36); i++) {
                if (slot >= 45) break;
                UUID memberId = members.get(i);
                String memberName = Bukkit.getOfflinePlayer(memberId).getName();
                if (memberName == null) memberName = "Unknown";
                boolean isOwner = account.isOwner(memberId);
                Set<String> perms = account.getPermissions(memberId);

                List<Component> lore = new ArrayList<>();
                if (isOwner) {
                    lore.add(ColorUtils.color("<gold>Owner</gold>"));
                }
                lore.add(ColorUtils.color("<gray>Permissions:</gray>"));
                for (String perm : SharedAccount.ALL_PERMISSIONS) {
                    if (perms.contains(perm)) {
                        lore.add(ColorUtils.color("  <green>" + perm + "</green>"));
                    }
                }
                if (!isOwner && canManage) {
                    lore.add(ColorUtils.color("<dark_gray>Click to manage permissions</dark_gray>"));
                }

                inventory.setItem(slot, new ItemBuilder(isOwner ? Material.GOLDEN_HELMET : Material.PLAYER_HEAD)
                        .name(ColorUtils.color((isOwner ? "<gold>" : "<aqua>") + memberName + (isOwner ? "</gold>" : "</aqua>")))
                        .lore(lore)
                        .build());
                slot++;
            }

            if (page > 0) {
                inventory.setItem(45, new ItemBuilder(Material.ARROW)
                        .name(ColorUtils.color("<yellow>Previous Page</yellow>"))
                        .build());
            }
            if (members.size() > (page + 1) * 36) {
                inventory.setItem(53, new ItemBuilder(Material.ARROW)
                        .name(ColorUtils.color("<yellow>Next Page</yellow>"))
                        .build());
            }
        } else {
            // Permission management view for a specific member
            String memberName = Bukkit.getOfflinePlayer(managingMember).getName();
            if (memberName == null) memberName = "Unknown";
            Set<String> currentPerms = account.getPermissions(managingMember);

            inventory.setItem(4, new ItemBuilder(Material.PLAYER_HEAD)
                    .name(ColorUtils.color("<gold>" + memberName + "</gold>"))
                    .lore(ColorUtils.color("<gray>Click permissions to toggle</gray>"))
                    .build());

            int slot = 9;
            for (String perm : SharedAccount.ALL_PERMISSIONS) {
                boolean hasPerm = currentPerms.contains(perm);
                inventory.setItem(slot, new ItemBuilder(hasPerm ? Material.LIME_DYE : Material.GRAY_DYE)
                        .name(ColorUtils.color((hasPerm ? "<green>" : "<red>") + perm + (hasPerm ? "</green>" : "</red>")))
                        .lore(
                            ColorUtils.color(hasPerm ? "<gray>Click to revoke</gray>" : "<gray>Click to grant</gray>")
                        )
                        .build());
                slot++;
            }

            // Remove member button
            inventory.setItem(22, new ItemBuilder(Material.BARRIER)
                    .name(ColorUtils.color("<red>Remove Member</red>"))
                    .lore(ColorUtils.color("<gray>Kick " + memberName + " from this account</gray>"))
                    .build());

            // Back
            inventory.setItem(45, new ItemBuilder(Material.ARROW)
                    .name(ColorUtils.color("<yellow>Back to Members</yellow>"))
                    .build());
        }
    }

    @Override
    public Inventory getInventory() { return inventory; }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getSlot();
        Player clicker = (Player) event.getWhoClicked();
        boolean canManage = account.hasPermission(clicker.getUniqueId(), SharedAccount.PERM_MANAGE_PERMISSIONS)
                || account.isOwner(clicker.getUniqueId());

        if (managingMember == null) {
            handleMemberListView(slot, clicker, canManage, event);
        } else {
            handlePermissionView(slot, clicker);
        }
    }

    private void handleMemberListView(int slot, Player clicker, boolean canManage, InventoryClickEvent event) {
        if (slot == 8 && canManage && account.hasPermission(clicker.getUniqueId(), SharedAccount.PERM_INVITE)) {
            plugin.getChatInputManager().requestInput(clicker,
                "<gold>Enter the player name to invite to '" + account.getName() + "':</gold>",
                name -> {
                    Player target = Bukkit.getPlayerExact(name);
                    if (target == null) {
                        clicker.sendMessage(ColorUtils.color("<red>Player not found.</red>"));
                        return;
                    }
                    if (account.addMember(target.getUniqueId())) {
                        clicker.sendMessage(ColorUtils.color("<green>Invited " + target.getName() + " to '" + account.getName() + "'.</green>"));
                        target.sendMessage(ColorUtils.color("<green>You were invited to the shared account '" + account.getName() + "'.</green>"));
                        clicker.openInventory(new SharedAccountMembersGUI(plugin, clicker, account).getInventory());
                    } else {
                        clicker.sendMessage(ColorUtils.color("<red>" + target.getName() + " is already a member.</red>"));
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

        if (!canManage) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName()) return;

        List<UUID> members = new ArrayList<>(account.getMembers());
        int start = page * 36;
        int index = slot - 9 + start;
        if (index >= 0 && index < members.size()) {
            UUID target = members.get(index);
            if (!account.isOwner(target)) {
                managingMember = target;
                populate();
            }
        }
    }

    private void handlePermissionView(int slot, Player clicker) {
        if (slot == 45) {
            managingMember = null;
            populate();
            return;
        }

        if (slot >= 9 && slot < 9 + SharedAccount.ALL_PERMISSIONS.size()) {
            String[] permArray = SharedAccount.ALL_PERMISSIONS.toArray(new String[0]);
            int permIndex = slot - 9;
            if (permIndex < permArray.length) {
                String perm = permArray[permIndex];
                boolean hasPerm = account.getPermissions(managingMember).contains(perm);
                account.setPermission(managingMember, perm, !hasPerm);
                populate();
            }
            return;
        }

        if (slot == 22) {
            UUID removed = managingMember;
            managingMember = null;
            account.removeMember(removed);
            populate();
            String name = Bukkit.getOfflinePlayer(removed).getName();
            clicker.sendMessage(ColorUtils.color("<green>Removed " + (name != null ? name : "member") + " from account.</green>"));
            return;
        }
    }
}
