package com.frekanstan.asset_management.data.assettypes;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

public class AssetTypeGetAllInput implements Serializable {
    @Getter
    @Setter
    private String assetCode;

    public AssetTypeGetAllInput(String assetCode) {
        setAssetCode(assetCode);
    }
}