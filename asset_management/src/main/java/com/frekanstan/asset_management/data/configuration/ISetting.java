package com.frekanstan.asset_management.data.configuration;

import com.frekanstan.asset_management.data.IEntity;

public interface ISetting extends IEntity {
    String getName();

    String getValue();
}
