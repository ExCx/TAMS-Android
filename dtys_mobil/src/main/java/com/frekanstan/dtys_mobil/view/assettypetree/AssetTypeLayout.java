package com.frekanstan.dtys_mobil.view.assettypetree;

import com.frekanstan.dtys_mobil.R;
import com.frekanstan.dtys_mobil.data.AssetType;

import lombok.Getter;
import lombok.Setter;
import tellh.com.recyclertreeview_lib.LayoutItemType;

public class AssetTypeLayout implements LayoutItemType {
    @Getter @Setter
    private AssetType assetType;

    @Getter @Setter
    private long assetCount;

    @Getter @Setter
    private long countedCount;

    @Getter @Setter
    private long labeledCount;

    public AssetTypeLayout(AssetType assetType, long assetCount, long countedCount, long labeledCount) {
        this.assetType = assetType;
        this.assetCount = assetCount;
        this.countedCount = countedCount;
        this.labeledCount = labeledCount;
    }

    @Override
    public int getLayoutId() {
        return R.layout.asset_type_card;
    }
}