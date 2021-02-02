package com.frekanstan.asset_management.data.assets;

import com.frekanstan.asset_management.data.IEntity;

public interface IBrand extends IEntity {
    String getDefinition();

    long getParentTypeId();
}
