package com.frekanstan.asset_management.data.tracking;

import com.frekanstan.asset_management.data.IEntity;

import java.util.Date;

public interface ICountedStateChange extends IEntity {
    long getAssetId();

    Date getLastControlTime();

    void setLastControlTime(Date lastControlTime);

    long getCountingOpId();
}
