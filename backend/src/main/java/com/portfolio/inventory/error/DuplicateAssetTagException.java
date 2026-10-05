package com.portfolio.inventory.error;

public class DuplicateAssetTagException extends RuntimeException {

    public DuplicateAssetTagException(String assetTag) {
        super("Asset tag " + assetTag + " is already in use");
    }
}
