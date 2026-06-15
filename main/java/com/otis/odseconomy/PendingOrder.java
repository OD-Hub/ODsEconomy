package com.otis.odseconomy;

import org.bukkit.Material;

public class PendingOrder {
    public String department;
    public Material material;
    public int amount;
    public double totalCost;

    public PendingOrder(String department, Material material, int amount, double totalCost) {
        this.department = department;
        this.material = material;
        this.amount = amount;
        this.totalCost = totalCost;
    }
}
