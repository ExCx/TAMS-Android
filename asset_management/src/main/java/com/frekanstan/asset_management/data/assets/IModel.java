package com.frekanstan.asset_management.data.assets;

import com.frekanstan.asset_management.data.IEntity;

public interface IModel extends IEntity {
    String getDefinition();

    long getParentBrandId();
}
