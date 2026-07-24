package com.odeco.economy;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class PendingOrder {

    private final UUID sellerId;
    private final String sellerName;
    private final ItemStack item;
    private final double price;
    private final long expiry;

    public PendingOrder(UUID sellerId, String sellerName, ItemStack item, double price, long expiry) {
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.item = item;
        this.price = price;
        this.expiry = expiry;
    }

    public UUID getSellerId() { return sellerId; }
    public String getSellerName() { return sellerName; }
    public ItemStack getItem() { return item; }
    public double getPrice() { return price; }
    public long getExpiry() { return expiry; }
    public boolean isExpired() { return System.currentTimeMillis() > expiry; }
}
