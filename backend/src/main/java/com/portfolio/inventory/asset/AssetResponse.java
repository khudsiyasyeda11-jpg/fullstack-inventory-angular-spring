package com.portfolio.inventory.asset;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record AssetResponse(
        Long id,
        String assetTag,
        String name,
        AssetCategory category,
        AssetStatus status,
        String location,
        String assignedTo,
        LocalDate purchaseDate,
        BigDecimal purchaseCost,
        String notes,
        Instant createdAt,
        Instant updatedAt) {

    public static AssetResponse from(Asset a) {
        return new AssetResponse(a.getId(), a.getAssetTag(), a.getName(), a.getCategory(), a.getStatus(),
                a.getLocation(), a.getAssignedTo(), a.getPurchaseDate(), a.getPurchaseCost(), a.getNotes(),
                a.getCreatedAt(), a.getUpdatedAt());
    }
}
