package com.frekanstan.asset_management.data.multitenancy;

import com.frekanstan.asset_management.data.IEntity;

public interface ITenant extends IEntity {
    String getTenancyName();

    String getName();

    Boolean getIsActive();
}
