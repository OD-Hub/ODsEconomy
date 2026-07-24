package com.odeco.economy;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class DealershipItem {

    private final ItemStack item;
    private double price;
    private int stock;

    public DealershipItem(ItemStack item, double price, int stock) {
        this.item = item.clone();
        this.price = price;
        this.stock = stock;
    }

    public ItemStack getItem() { return item.clone(); }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public boolean hasStock() { return stock > 0 || stock == -1; }
}
