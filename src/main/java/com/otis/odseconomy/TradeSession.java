package com.otis.odseconomy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class TradeSession implements org.bukkit.inventory.InventoryHolder {

    static final int A_START = 0, A_END = 17;
    static final int DIV_START = 18, DIV_END = 26;
    static final int B_START = 27, B_END = 44;
    static final int A_MONEY = 45;
    static final int A_ACCEPT = 47;
    static final int CENTER = 49;
    static final int B_ACCEPT = 51;
    static final int B_MONEY = 53;

    public final UUID playerA;
    public final UUID playerB;
    private final OdsEconomy plugin;
    private final Inventory inv;
    public double moneyA, moneyB;
    public boolean acceptedA, acceptedB;
    public boolean active = true;

    public TradeSession(Player a, Player b, OdsEconomy plugin) {
        this.playerA = a.getUniqueId();
        this.playerB = b.getUniqueId();
        this.plugin = plugin;
        this.inv = Bukkit.createInventory(this, 54, ChatColor.DARK_GRAY + "\u2694 Trade");
        renderStatic();
        updateMoneyA();
        updateMoneyB();
        updateAcceptButtons();
        updateCenter();
        a.openInventory(inv);
        b.openInventory(inv);
    }

    // ────────── RENDERING ──────────

    private void renderStatic() {
        for (int i = DIV_START; i <= DIV_END; i++) {
            inv.setItem(i, item(Material.GRAY_STAINED_GLASS_PANE, " "));
        }
        inv.setItem(46, item(Material.GRAY_STAINED_GLASS_PANE, " "));
        inv.setItem(48, item(Material.GRAY_STAINED_GLASS_PANE, " "));
        inv.setItem(50, item(Material.GRAY_STAINED_GLASS_PANE, " "));
        inv.setItem(52, item(Material.GRAY_STAINED_GLASS_PANE, " "));
    }

    void updateMoneyA() {
        inv.setItem(A_MONEY, moneyItem(playerA, moneyA, "Your Offer"));
    }

    void updateMoneyB() {
        inv.setItem(B_MONEY, moneyItem(playerB, moneyB, "Their Offer"));
    }

    private ItemStack moneyItem(UUID who, double amount, String label) {
        Material mat = amount > 0 ? Material.EMERALD : Material.EMERALD;
        ItemMeta meta = Bukkit.getItemFactory().getItemMeta(mat);
        String sym = plugin.getCurrencySymbol();
        String amt = sym + String.format("%.2f", amount);
        meta.setDisplayName(ChatColor.GOLD + label);
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Amount: " + ChatColor.GREEN + amt,
                "",
                ChatColor.YELLOW + "Left-click +" + sym + "10",
                ChatColor.YELLOW + "Shift-left +" + sym + "100",
                ChatColor.YELLOW + "Right-click -" + sym + "10",
                ChatColor.YELLOW + "Shift-right -" + sym + "100"
        ));
        ItemStack item = new ItemStack(mat);
        item.setItemMeta(meta);
        return item;
    }

    void updateAcceptButtons() {
        inv.setItem(A_ACCEPT, acceptItem(playerA, acceptedA));
        inv.setItem(B_ACCEPT, acceptItem(playerB, acceptedB));
    }

    private ItemStack acceptItem(UUID who, boolean accepted) {
        Material mat = accepted ? Material.LIME_WOOL : Material.RED_WOOL;
        String name = accepted ? ChatColor.GREEN + "\u2714 Accepted" : ChatColor.RED + "\u2718 Not Accepted";
        List<String> lore = accepted
                ? Arrays.asList(ChatColor.GRAY + "Click to un-accept")
                : Arrays.asList(ChatColor.GRAY + "Click to accept the trade");
        ItemMeta meta = Bukkit.getItemFactory().getItemMeta(mat);
        meta.setDisplayName(name);
        meta.setLore(lore);
        ItemStack item = new ItemStack(mat);
        item.setItemMeta(meta);
        return item;
    }

    void updateCenter() {
        String status;
        if (acceptedA && acceptedB) {
            status = ChatColor.GREEN + "\u2714 Both Ready!";
        } else if (acceptedA) {
            Player b = Bukkit.getPlayer(playerB);
            status = ChatColor.YELLOW + "Waiting for " + (b != null ? b.getName() : "partner") + "...";
        } else if (acceptedB) {
            Player a = Bukkit.getPlayer(playerA);
            status = ChatColor.YELLOW + "Waiting for " + (a != null ? a.getName() : "partner") + "...";
        } else {
            status = ChatColor.GRAY + "Click accept when ready";
        }
        ItemMeta meta = Bukkit.getItemFactory().getItemMeta(Material.BLACK_STAINED_GLASS_PANE);
        meta.setDisplayName(status);
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        item.setItemMeta(meta);
        inv.setItem(CENTER, item);
    }

    // ────────── ITEM HELPERS ──────────

    private static ItemStack item(Material mat, String name) {
        ItemMeta meta = Bukkit.getItemFactory().getItemMeta(mat);
        meta.setDisplayName(name);
        ItemStack item = new ItemStack(mat);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isTradeInv(Inventory inv) {
        return inv != null && inv.getHolder() instanceof TradeSession;
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

    // ────────── CLICK HANDLER ──────────

    public boolean handleClick(Player who, int slot) {
        if (!active) return true;
        boolean isA = who.getUniqueId().equals(playerA);

        // Divider / glass — always cancel
        if ((slot >= DIV_START && slot <= DIV_END)
                || slot == 46 || slot == 48 || slot == 50 || slot == 52) {
            return false;
        }

        // Money slot
        if (slot == A_MONEY && isA) return false; // cancel, we handle it
        if (slot == B_MONEY && !isA) return false;

        // Accept button
        if (slot == A_ACCEPT) {
            if (isA) { toggleAcceptA(); return false; }
            return false;
        }
        if (slot == B_ACCEPT) {
            if (!isA) { toggleAcceptB(); return false; }
            return false;
        }

        // Item area — allow for the right player
        if (slot >= A_START && slot <= A_END) return !isA;
        if (slot >= B_START && slot <= B_END) return isA;

        return true;
    }

    public boolean handleMoneyClick(Player who, int slot, boolean shift, boolean right) {
        if (!active) return true;
        boolean isA = who.getUniqueId().equals(playerA);
        if (slot == A_MONEY && !isA) return true;
        if (slot == B_MONEY && isA) return true;

        double delta;
        if (right) {
            delta = shift ? -100 : -10;
        } else {
            delta = shift ? 100 : 10;
        }

        if (isA) {
            double newAmount = moneyA + delta;
            double bal = plugin.balances.getOrDefault(playerA, 0.0);
            if (newAmount < 0) newAmount = 0;
            if (newAmount > bal) newAmount = Math.floor(bal * 100) / 100;
            moneyA = newAmount;
            updateMoneyA();
        } else {
            double newAmount = moneyB + delta;
            double bal = plugin.balances.getOrDefault(playerB, 0.0);
            if (newAmount < 0) newAmount = 0;
            if (newAmount > bal) newAmount = Math.floor(bal * 100) / 100;
            moneyB = newAmount;
            updateMoneyB();
        }

        // Un-accept when offer changes
        if (isA && acceptedA) { acceptedA = false; updateAcceptButtons(); updateCenter(); }
        if (!isA && acceptedB) { acceptedB = false; updateAcceptButtons(); updateCenter(); }

        return false;
    }

    private void toggleAcceptA() {
        acceptedA = !acceptedA;
        updateAcceptButtons();
        updateCenter();
        if (acceptedA && acceptedB) execute();
    }

    private void toggleAcceptB() {
        acceptedB = !acceptedB;
        updateAcceptButtons();
        updateCenter();
        if (acceptedA && acceptedB) execute();
    }

    // ────────── EXECUTION ──────────

    private void execute() {
        if (!active) return;
        active = false;

        Player a = Bukkit.getPlayer(playerA);
        Player b = Bukkit.getPlayer(playerB);
        if (a == null || b == null) {
            cancel();
            return;
        }

        // Validate balances
        if (!plugin.removeMoney(playerA, moneyA)) {
            a.sendMessage(ChatColor.RED + "Trade failed: insufficient funds.");
            b.sendMessage(ChatColor.RED + "Trade failed: " + a.getName() + " lacks sufficient funds.");
            cancel();
            return;
        }
        if (!plugin.removeMoney(playerB, moneyB)) {
            // Refund A
            plugin.addMoney(playerA, moneyA);
            a.sendMessage(ChatColor.RED + "Trade failed: insufficient funds.");
            b.sendMessage(ChatColor.RED + "Trade failed: " + b.getName() + " lacks sufficient funds.");
            cancel();
            return;
        }

        // Transfer money
        plugin.addMoney(playerB, moneyA);
        plugin.addMoney(playerA, moneyB);

        // Transfer items A -> B
        for (int i = A_START; i <= A_END; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                Map<Integer, ItemStack> left = b.getInventory().addItem(item.clone());
                for (ItemStack drop : left.values()) {
                    b.getWorld().dropItem(b.getLocation(), drop);
                }
                inv.setItem(i, null);
            }
        }

        // Transfer items B -> A
        for (int i = B_START; i <= B_END; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                Map<Integer, ItemStack> left = a.getInventory().addItem(item.clone());
                for (ItemStack drop : left.values()) {
                    a.getWorld().dropItem(a.getLocation(), drop);
                }
                inv.setItem(i, null);
            }
        }

        String sym = plugin.getCurrencySymbol();
        a.sendMessage(ChatColor.GREEN + "Trade completed with " + b.getName() + "!");
        if (moneyA > 0) a.sendMessage(ChatColor.GRAY + "  Sent: " + sym + String.format("%.2f", moneyA));
        if (moneyB > 0) a.sendMessage(ChatColor.GRAY + "  Received: " + sym + String.format("%.2f", moneyB));
        b.sendMessage(ChatColor.GREEN + "Trade completed with " + a.getName() + "!");
        if (moneyB > 0) b.sendMessage(ChatColor.GRAY + "  Sent: " + sym + String.format("%.2f", moneyB));
        if (moneyA > 0) b.sendMessage(ChatColor.GRAY + "  Received: " + sym + String.format("%.2f", moneyA));

        a.closeInventory();
        b.closeInventory();
        plugin.activeTrades.remove(playerA);
        plugin.activeTrades.remove(playerB);
    }

    public void cancel() {
        if (!active) return;
        active = false;

        Player a = Bukkit.getPlayer(playerA);
        Player b = Bukkit.getPlayer(playerB);

        // Return items to A
        if (a != null) {
            for (int i = A_START; i <= A_END; i++) {
                ItemStack item = inv.getItem(i);
                if (item != null && item.getType() != Material.AIR) {
                    Map<Integer, ItemStack> left = a.getInventory().addItem(item.clone());
                    for (ItemStack drop : left.values()) a.getWorld().dropItem(a.getLocation(), drop);
                    inv.setItem(i, null);
                }
            }
        }
        // Return items to B
        if (b != null) {
            for (int i = B_START; i <= B_END; i++) {
                ItemStack item = inv.getItem(i);
                if (item != null && item.getType() != Material.AIR) {
                    Map<Integer, ItemStack> left = b.getInventory().addItem(item.clone());
                    for (ItemStack drop : left.values()) b.getWorld().dropItem(b.getLocation(), drop);
                    inv.setItem(i, null);
                }
            }
        }

        if (a != null) a.closeInventory();
        if (b != null) b.closeInventory();

        plugin.activeTrades.remove(playerA);
        plugin.activeTrades.remove(playerB);
    }
}
