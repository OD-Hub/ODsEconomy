package com.odeco.economy;

import com.odeco.ODEco;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DealershipManager {

    private final ODEco plugin;
    private final Map<String, Dealership> dealerships = new ConcurrentHashMap<>();
    private File dataFile;
    private FileConfiguration data;

    public DealershipManager(ODEco plugin) {
        this.plugin = plugin;
        loadData();
    }

    public void loadData() {
        dataFile = new File(plugin.getDataFolder(), "dealerships.yml");
        if (!dataFile.exists()) {
            try { dataFile.createNewFile(); } catch (IOException e) { plugin.getLogger().severe("Could not create dealerships.yml: " + e.getMessage()); }
        }
        data = YamlConfiguration.loadConfiguration(dataFile);
        dealerships.clear();
        if (data.contains("dealerships")) {
            for (String key : data.getConfigurationSection("dealerships").getKeys(false)) {
                try {
                    Dealership d = deserializeDealership(data.getConfigurationSection("dealerships." + key));
                    dealerships.put(key, d);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to load dealership '" + key + "': " + e.getMessage());
                }
            }
        }
    }

    public void saveData() {
        data.set("dealerships", null);
        for (Map.Entry<String, Dealership> entry : dealerships.entrySet()) {
            serializeDealership(data, "dealerships." + entry.getKey(), entry.getValue());
        }
        try { data.save(dataFile); } catch (IOException e) { plugin.getLogger().severe("Could not save dealerships.yml: " + e.getMessage()); }
    }

    private void serializeDealership(FileConfiguration config, String path, Dealership d) {
        config.set(path + ".name", d.getName());
        config.set(path + ".owner", d.getOwner().toString());
        config.set(path + ".open-to-all", d.isOpenToAll());

        List<String> allowed = new ArrayList<>();
        for (UUID id : d.getAllowedPlayers()) allowed.add(id.toString());
        config.set(path + ".allowed-players", allowed);

        if (d.getIcon() != null) {
            config.set(path + ".icon", d.getIcon());
        }

        int i = 0;
        for (DealershipItem item : d.getItems()) {
            String ip = path + ".items." + i++;
            config.set(ip + ".item", item.getItem());
            config.set(ip + ".price", item.getPrice());
            config.set(ip + ".stock", item.getStock());
        }
    }

    private Dealership deserializeDealership(org.bukkit.configuration.ConfigurationSection section) {
        String name = section.getString("name");
        UUID owner = UUID.fromString(section.getString("owner"));
        boolean openToAll = section.getBoolean("open-to-all", false);

        Set<UUID> allowed = new HashSet<>();
        if (section.contains("allowed-players")) {
            for (String s : section.getStringList("allowed-players")) {
                allowed.add(UUID.fromString(s));
            }
        }

        ItemStack icon = null;
        if (section.contains("icon")) {
            try {
                icon = section.getItemStack("icon");
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load dealership icon: " + e.getMessage());
            }
        }

        List<DealershipItem> items = new ArrayList<>();
        if (section.contains("items")) {
            for (String key : section.getConfigurationSection("items").getKeys(false)) {
                try {
                    org.bukkit.inventory.ItemStack itemStack = section.getItemStack("items." + key + ".item");
                    double price = section.getDouble("items." + key + ".price");
                    int stock = section.getInt("items." + key + ".stock", -1);
                    items.add(new DealershipItem(itemStack, price, stock));
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to load dealership item: " + e.getMessage());
                }
            }
        }

        return new Dealership(name, owner, items, allowed, openToAll, icon);
    }

    public boolean createDealership(String name, UUID owner) {
        if (dealerships.containsKey(name)) return false;
        dealerships.put(name, new Dealership(name, owner));
        saveData();
        return true;
    }

    public boolean deleteDealership(String name) {
        boolean removed = dealerships.remove(name) != null;
        if (removed) saveData();
        return removed;
    }

    public Dealership getDealership(String name) {
        return dealerships.get(name);
    }

    public Collection<Dealership> getAllDealerships() {
        return dealerships.values();
    }

    public List<Dealership> getDealershipsForPlayer(UUID playerId) {
        List<Dealership> result = new ArrayList<>();
        for (Dealership d : dealerships.values()) {
            if (d.canAccess(playerId)) {
                result.add(d);
            }
        }
        return result;
    }

    public void addItemToDealership(String dealershipName, org.bukkit.inventory.ItemStack item, double price, int stock) {
        Dealership d = dealerships.get(dealershipName);
        if (d == null) return;
        d.addItem(new DealershipItem(item, price, stock));
        saveData();
    }

    public boolean buyItem(String dealershipName, int itemIndex, UUID buyerId) {
        return buyItemQuantity(dealershipName, itemIndex, buyerId, 1);
    }

    public boolean buyItemQuantity(String dealershipName, int itemIndex, UUID buyerId, int quantity) {
        Dealership d = dealerships.get(dealershipName);
        if (d == null || itemIndex < 0 || itemIndex >= d.getItems().size()) return false;
        if (quantity <= 0) return false;

        DealershipItem di = d.getItems().get(itemIndex);
        if (!di.hasStock()) return false;
        if (di.getStock() > 0 && di.getStock() < quantity) return false;

        double totalCost = di.getPrice() * quantity;
        EconomyManager eco = plugin.getEconomyManager();
        if (!eco.withdraw(buyerId, totalCost)) return false;

        org.bukkit.entity.Player player = plugin.getServer().getPlayer(buyerId);
        if (player != null) {
            org.bukkit.inventory.ItemStack giveItem = di.getItem().clone();
            giveItem.setAmount(quantity);
            player.getInventory().addItem(giveItem);
        }

        if (di.getStock() > 0) {
            di.setStock(di.getStock() - quantity);
        }

        eco.logTransaction(buyerId, "dealership_buy", totalCost, "Bought " + quantity + "x " + di.getItem().getType().name() + " from " + dealershipName);
        eco.deposit(d.getOwner(), totalCost);
        saveData();
        return true;
    }
}
