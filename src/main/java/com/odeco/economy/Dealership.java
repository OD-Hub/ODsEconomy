package com.odeco.economy;

import org.bukkit.inventory.ItemStack;

import java.util.*;

public class Dealership {

    private final String name;
    private final UUID owner;
    private final List<DealershipItem> items;
    private final Set<UUID> allowedPlayers;
    private boolean openToAll;
    private ItemStack icon;

    public Dealership(String name, UUID owner) {
        this.name = name;
        this.owner = owner;
        this.items = new ArrayList<>();
        this.allowedPlayers = new HashSet<>();
        this.openToAll = false;
        this.icon = null;
    }

    public Dealership(String name, UUID owner, List<DealershipItem> items, Set<UUID> allowedPlayers, boolean openToAll) {
        this(name, owner, items, allowedPlayers, openToAll, null);
    }

    public Dealership(String name, UUID owner, List<DealershipItem> items, Set<UUID> allowedPlayers, boolean openToAll, ItemStack icon) {
        this.name = name;
        this.owner = owner;
        this.items = items;
        this.allowedPlayers = allowedPlayers;
        this.openToAll = openToAll;
        this.icon = icon;
    }

    public String getName() { return name; }
    public UUID getOwner() { return owner; }
    public List<DealershipItem> getItems() { return items; }
    public Set<UUID> getAllowedPlayers() { return allowedPlayers; }
    public boolean isOpenToAll() { return openToAll; }
    public void setOpenToAll(boolean openToAll) { this.openToAll = openToAll; }
    public ItemStack getIcon() { return icon; }
    public void setIcon(ItemStack icon) { this.icon = icon; }

    public void addItem(DealershipItem item) { items.add(item); }
    public void removeItem(int index) {
        if (index >= 0 && index < items.size()) items.remove(index);
    }

    public boolean canAccess(UUID playerId) {
        if (openToAll) return true;
        if (owner.equals(playerId)) return true;
        return allowedPlayers.contains(playerId);
    }

    public int getAccessCount() {
        if (openToAll) return -1;
        return 1 + allowedPlayers.size();
    }
}
