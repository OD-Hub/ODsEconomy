package com.odeco.economy;

import java.util.*;

public class SharedAccount {

    public static final String PERM_DEPOSIT = "Deposit";
    public static final String PERM_WITHDRAW = "Withdraw";
    public static final String PERM_MANAGE_PERMISSIONS = "Manage Permissions";
    public static final String PERM_INVITE = "Invite";
    public static final Set<String> ALL_PERMISSIONS = Set.of(PERM_DEPOSIT, PERM_WITHDRAW, PERM_MANAGE_PERMISSIONS, PERM_INVITE);
    public static final Set<String> DEFAULT_PERMISSIONS = Set.of(PERM_DEPOSIT);

    private final String name;
    private final UUID ownerId;
    private final Map<UUID, Set<String>> memberPermissions;
    private double balance;

    public SharedAccount(String name, UUID ownerId) {
        this.name = name;
        this.ownerId = ownerId;
        this.memberPermissions = new HashMap<>();
        this.balance = 0;
        memberPermissions.put(ownerId, new HashSet<>(ALL_PERMISSIONS));
    }

    public SharedAccount(String name, UUID ownerId, Map<UUID, Set<String>> memberPermissions) {
        this(name, ownerId, memberPermissions, 0);
    }

    public SharedAccount(String name, UUID ownerId, Map<UUID, Set<String>> memberPermissions, double balance) {
        this.name = name;
        this.ownerId = ownerId;
        this.memberPermissions = new HashMap<>(memberPermissions);
        this.balance = balance;
        memberPermissions.put(ownerId, new HashSet<>(ALL_PERMISSIONS));
    }

    public String getName() { return name; }
    public UUID getOwnerId() { return ownerId; }
    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public Set<UUID> getMembers() { return memberPermissions.keySet(); }
    public boolean isMember(UUID playerId) { return memberPermissions.containsKey(playerId); }

    public boolean isOwner(UUID playerId) { return ownerId.equals(playerId); }

    public boolean hasPermission(UUID playerId, String permission) {
        if (ownerId.equals(playerId)) return true;
        Set<String> perms = memberPermissions.get(playerId);
        return perms != null && perms.contains(permission);
    }

    public Set<String> getPermissions(UUID playerId) {
        if (ownerId.equals(playerId)) return ALL_PERMISSIONS;
        Set<String> perms = memberPermissions.get(playerId);
        return perms != null ? Collections.unmodifiableSet(perms) : Collections.emptySet();
    }

    public boolean addMember(UUID playerId) {
        if (memberPermissions.containsKey(playerId)) return false;
        memberPermissions.put(playerId, new HashSet<>(DEFAULT_PERMISSIONS));
        return true;
    }

    public boolean removeMember(UUID playerId) {
        if (ownerId.equals(playerId)) return false;
        return memberPermissions.remove(playerId) != null;
    }

    public boolean setPermission(UUID playerId, String permission, boolean value) {
        if (ownerId.equals(playerId)) return false;
        Set<String> perms = memberPermissions.get(playerId);
        if (perms == null) return false;
        if (value) {
            perms.add(permission);
        } else {
            perms.remove(permission);
        }
        return true;
    }

    public void setPermissions(UUID playerId, Set<String> permissions) {
        if (!ownerId.equals(playerId)) {
            memberPermissions.put(playerId, new HashSet<>(permissions));
        }
    }

    public Map<UUID, Set<String>> getMemberPermissionsSnapshot() {
        Map<UUID, Set<String>> copy = new HashMap<>();
        for (Map.Entry<UUID, Set<String>> entry : memberPermissions.entrySet()) {
            copy.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        return copy;
    }

    public static SharedAccount deserialize(String name, Map<?, ?> data) {
        UUID ownerId = UUID.fromString((String) data.get("owner"));
        double balance = data.containsKey("balance") ? ((Number) data.get("balance")).doubleValue() : 0;
        Map<UUID, Set<String>> perms = new HashMap<>();
        if (data.containsKey("members")) {
            for (Map.Entry<String, ?> entry : ((Map<String, List<String>>) data.get("members")).entrySet()) {
                UUID uuid = UUID.fromString(entry.getKey());
                perms.put(uuid, new HashSet<>((List<String>) entry.getValue()));
            }
        }
        return new SharedAccount(name, ownerId, perms, balance);
    }

    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("owner", ownerId.toString());
        data.put("balance", balance);
        Map<String, List<String>> membersData = new HashMap<>();
        for (Map.Entry<UUID, Set<String>> entry : memberPermissions.entrySet()) {
            if (entry.getKey().equals(ownerId)) continue;
            membersData.put(entry.getKey().toString(), new ArrayList<>(entry.getValue()));
        }
        data.put("members", membersData);
        return data;
    }
}
