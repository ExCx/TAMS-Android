package com.frekanstan.asset_management.data.locations;

import android.annotation.SuppressLint;

import com.frekanstan.asset_management.data.IEntity;

import java.util.Date;

public interface ILocation extends IEntity {
    String getName();

    Date getLabelingDateTime();

    long getParentLocationId();

    long getTenantId();

    long getTenantBoundId();

    ELocationType getLocationType();

    @SuppressLint("DefaultLocale")
    String getLocationCode();

    void setAsLabeled(boolean labeled);

    void setAsToBeLabeled();
}
