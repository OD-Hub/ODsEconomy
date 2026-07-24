package com.odeco.gui;

import com.odeco.ODEco;
import com.odeco.economy.BountyEntry;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class BountyGUI implements InventoryHolder {

    private final ODEco plugin;
    private final Player player;
    private final Inventory inventory;
    private int page = 0;

    public BountyGUI(ODEco plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, ColorUtils.color("<red>Bounties</red>"));
        populate();
    }

    private void populate() {
        inventory.clear();
        EconomyManager economy = plugin.getEconomyManager();
        Map<UUID, BountyEntry> bountyEntries = economy.getAllBountyEntries();
        List<Map.Entry<UUID, BountyEntry>> entries = new ArrayList<>(bountyEntries.entrySet());

        int totalPages = Math.max(1, (int) Math.ceil((double) entries.size() / 36));
        if (page >= totalPages) page = totalPages - 1;

        int start = page * 36;
        int slot = 9;
        for (int i = start; i < Math.min(entries.size(), start + 36); i++) {
            if (slot >= 45) break;
            Map.Entry<UUID, BountyEntry> mapEntry = entries.get(i);
            UUID targetId = mapEntry.getKey();
            BountyEntry bounty = mapEntry.getValue();
            OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetId);
            String targetName = offlineTarget.getName() != null ? offlineTarget.getName() : "Unknown";

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
            var profile = Bukkit.createProfile(targetId, null);
            skullMeta.setPlayerProfile(profile);
            head.setItemMeta(skullMeta);

            List<Component> lore = new ArrayList<>();
            lore.add(ColorUtils.color("<yellow>Bounty: " + economy.format(bounty.getAmount()) + "</yellow>"));
            if (!bounty.isAnonymous()) {
                OfflinePlayer placer = Bukkit.getOfflinePlayer(bounty.getPlacerId());
                String placerName = placer.getName() != null ? placer.getName() : "Unknown";
                lore.add(ColorUtils.color("<gray>Placed by: " + placerName + "</gray>"));
            } else {
                lore.add(ColorUtils.color("<gray>Placed by: Anonymous</gray>"));
            }
            lore.add(Component.empty());
            lore.add(ColorUtils.color("<dark_gray>Click to claim (kill to collect)</dark_gray>"));
            head = new ItemBuilder(head)
                    .name(ColorUtils.color("<gold>" + targetName + "</gold>"))
                    .lore(lore)
                    .build();
            inventory.setItem(slot, head);
            slot++;
        }

        if (page > 0) {
            inventory.setItem(45, new ItemBuilder(Material.ARROW)
                    .name(ColorUtils.color("<yellow>Previous Page</yellow>"))
                    .lore(ColorUtils.color("<gray>Page " + page + " of " + totalPages + "</gray>"))
                    .build());
        } else {
            inventory.setItem(45, null);
        }

        if (entries.size() > (page + 1) * 36) {
            inventory.setItem(53, new ItemBuilder(Material.ARROW)
                    .name(ColorUtils.color("<yellow>Next Page</yellow>"))
                    .lore(ColorUtils.color("<gray>Page " + (page + 2) + " of " + totalPages + "</gray>"))
                    .build());
        } else {
            inventory.setItem(53, null);
        }

        inventory.setItem(48, new ItemBuilder(Material.PLAYER_HEAD)
                .name(ColorUtils.color("<green>Place Bounty</green>"))
                .lore(ColorUtils.color("<gray>Click to place a bounty on a player</gray>"))
                .skull(Heads.BOUNTY)
                .build());

        inventory.setItem(49, new ItemBuilder(Material.ARROW)
                .name(ColorUtils.color("<red>Back</red>"))
                .build());

        inventory.setItem(50, new ItemBuilder(Material.PLAYER_HEAD)
                .name(ColorUtils.color("<gold>Check Bounty</gold>"))
                .lore(ColorUtils.color("<gray>Click to check a player's bounty</gray>"))
                .skull(Heads.BALTOP)
                .build());

        if (player.hasPermission("odeco.admin")) {
            inventory.setItem(47, new ItemBuilder(Material.BARRIER)
                    .name(ColorUtils.color("<red>Bounty Manager</red>"))
                    .lore(
                            ColorUtils.color("<gray>Cancel bounties and issue refunds</gray>"),
                            ColorUtils.color("<dark_gray>Requires admin permission</dark_gray>")
                    )
                    .build());
        }
    }

    @Override
    public Inventory getInventory() { return inventory; }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getSlot();
        Player clicker = (Player) event.getWhoClicked();

        switch (slot) {
            case 45 -> {
                if (page > 0) { page--; populate(); }
            }
            case 53 -> {
                Map<UUID, BountyEntry> bounties = plugin.getEconomyManager().getAllBountyEntries();
                if (bounties.size() > (page + 1) * 36) { page++; populate(); }
            }
            case 48 -> promptPlaceBounty(clicker);
            case 49 -> {
                clicker.closeInventory();
                clicker.openInventory(new EcoPanelGUI(plugin, clicker).getInventory());
            }
            case 50 -> promptCheckBounty(clicker);
            case 47 -> openBountyManager(clicker);
        }
    }

    private void promptPlaceBounty(Player clicker) {
        plugin.getChatInputManager().requestInput(clicker,
            "<gold>Enter the player name to place a bounty on:</gold>",
            targetName -> {
                Player target = Bukkit.getPlayerExact(targetName);
                if (target == null) {
                    clicker.sendMessage(ColorUtils.color("<red>Player not found (must be online).</red>"));
                    return;
                }
                plugin.getChatInputManager().requestInput(clicker,
                    "<gold>Enter the bounty amount for " + target.getName() + ":</gold>",
                    amountStr -> {
                        try {
                            double amount = Double.parseDouble(amountStr);
                            if (amount <= 0) {
                                clicker.sendMessage(ColorUtils.color("<red>Amount must be positive.</red>"));
                                return;
                            }
                            plugin.getChatInputManager().requestInput(clicker,
                                "<gold>Should this bounty be anonymous? (yes/no):</gold>",
                                anonStr -> {
                                    boolean anonymous = anonStr.toLowerCase().startsWith("yes") || anonStr.toLowerCase().startsWith("y");
                                    EconomyManager economy = plugin.getEconomyManager();
                                    if (economy.setBounty(target.getUniqueId(), clicker.getUniqueId(), amount, anonymous)) {
                                        clicker.sendMessage(ColorUtils.color("<green>Bounty of " + economy.format(amount) + " placed on " + target.getName() + (anonymous ? " (anonymous)" : "") + "!</green>"));
                                        if (!anonymous) {
                                            target.sendMessage(ColorUtils.color("<red>A bounty of " + economy.format(amount) + " has been placed on you by " + clicker.getName() + "!</red>"));
                                        } else {
                                            target.sendMessage(ColorUtils.color("<red>A bounty of " + economy.format(amount) + " has been placed on you!</red>"));
                                        }
                                        populate();
                                    } else {
                                        clicker.sendMessage(ColorUtils.color("<red>Could not place bounty. Check balance and minimum amount.</red>"));
                                    }
                                });
                        } catch (NumberFormatException e) {
                            clicker.sendMessage(ColorUtils.color("<red>Invalid amount.</red>"));
                        }
                    });
            });
    }

    private void promptCheckBounty(Player clicker) {
        plugin.getChatInputManager().requestInput(clicker,
            "<gold>Enter the player name to check bounty:</gold>",
            targetName -> {
                Player target = Bukkit.getPlayerExact(targetName);
                if (target == null) {
                    clicker.sendMessage(ColorUtils.color("<red>Player not found.</red>"));
                    return;
                }
                EconomyManager economy = plugin.getEconomyManager();
                BountyEntry entry = economy.getBountyEntry(target.getUniqueId());
                if (entry != null && entry.getAmount() > 0) {
                    clicker.sendMessage(ColorUtils.color("<gold>" + target.getName() + " has a bounty of " + economy.format(entry.getAmount()) + "</gold>"));
                } else {
                    clicker.sendMessage(ColorUtils.color("<gray>" + target.getName() + " has no bounty.</gray>"));
                }
            });
    }

    private void openBountyManager(Player clicker) {
        clicker.closeInventory();
        clicker.openInventory(new BountyManagerGUI(plugin, clicker).getInventory());
    }
}
