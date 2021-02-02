package com.frekanstan.asset_management.data.assets;

import java.io.Serializable;
import java.util.Collection;

public class ChangeAssetDetailsInput implements Serializable {
    public Collection<IAsset> assets;

    public ChangeAssetDetailsInput(Collection<IAsset> assets) {
        this.assets = assets;
    }
}