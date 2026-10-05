package com.portfolio.inventory.asset;

/** Optional list filters; null fields are ignored. */
public record AssetFilter(String q, AssetCategory category, AssetStatus status) {

    public static AssetFilter none() {
        return new AssetFilter(null, null, null);
    }
}
