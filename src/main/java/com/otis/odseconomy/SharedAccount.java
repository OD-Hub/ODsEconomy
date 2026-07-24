package com.otis.odseconomy;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SharedAccount {
    private String name;
    private UUID owner;
    private Map<UUID, String> members; // UUID -> donate_only / spend_only / view_only
    private double balance;
    private boolean visible;

    public SharedAccount(String name, UUID owner) {
        this.name = name;
        this.owner = owner;
        this.members = new HashMap<>();
        this.balance = 0.0;
        this.visible = true;
    }

    public String getName() { return name; }
    public UUID getOwner() { return owner; }
    public Map<UUID, String> getMembers() { return members; }
    public double getBalance() { return balance; }
    public boolean isVisible() { return visible; }

    public void setName(String name) { this.name = name; }
    public void setOwner(UUID owner) { this.owner = owner; }
    public void setBalance(double balance) { this.balance = Math.max(0, balance); }
    public void setVisible(boolean visible) { this.visible = visible; }

    public void addMember(UUID uuid, String perm) {
        members.put(uuid, perm);
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
    }

    public String getMemberPerm(UUID uuid) {
        return members.get(uuid);
    }

    public boolean hasAccess(UUID uuid) {
        return owner.equals(uuid) || members.containsKey(uuid);
    }

    public boolean canDonate(UUID uuid) {
        if (owner.equals(uuid)) return true;
        String perm = members.get(uuid);
        return "donate_only".equals(perm) || "spend_only".equals(perm);
    }

    public boolean canSpend(UUID uuid) {
        if (owner.equals(uuid)) return true;
        String perm = members.get(uuid);
        return "spend_only".equals(perm);
    }

    public boolean canView(UUID uuid) {
        if (visible) return true;
        return owner.equals(uuid) || members.containsKey(uuid);
    }

    public void deposit(double amount) {
        balance += amount;
    }

    public boolean withdraw(double amount) {
        if (balance >= amount) {
            balance -= amount;
            return true;
        }
        return false;
    }
}
