package com.frekanstan.asset_management.data.assettypes;

import com.frekanstan.asset_management.data.IEntity;

public interface IAssetType extends IEntity {
    String getDefinition();

    long getParentTypeId();

    IAssetType getParentType();

    String getAssetCode();

    int getDepth();

    void setDepth(int depth);
}
