package com.portfolio.inventory.error;

public class AssetNotFoundException extends RuntimeException {

    public AssetNotFoundException(Long id) {
        super("Asset " + id + " does not exist");
    }
}
