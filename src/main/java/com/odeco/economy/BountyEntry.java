package com.odeco.economy;

import java.util.UUID;

public class BountyEntry {

    private final UUID targetId;
    private final UUID placerId;
    private final double amount;
    private final boolean anonymous;
    private long placedAt;

    public BountyEntry(UUID targetId, UUID placerId, double amount, boolean anonymous, long placedAt) {
        this.targetId = targetId;
        this.placerId = placerId;
        this.amount = amount;
        this.anonymous = anonymous;
        this.placedAt = placedAt;
    }

    public UUID getTargetId() { return targetId; }
    public UUID getPlacerId() { return placerId; }
    public double getAmount() { return amount; }
    public boolean isAnonymous() { return anonymous; }
    public long getPlacedAt() { return placedAt; }

    public BountyEntry withAmount(double newAmount) {
        return new BountyEntry(targetId, placerId, newAmount, anonymous, placedAt);
    }
}
