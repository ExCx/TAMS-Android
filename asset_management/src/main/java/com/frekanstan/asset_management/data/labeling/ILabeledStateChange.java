package com.frekanstan.asset_management.data.labeling;

import com.frekanstan.asset_management.data.IEntity;

import java.util.Date;

public interface ILabeledStateChange extends IEntity {
    long getEntityId();

    Date getLabelingDateTime();

    String getType();

    void setLabelingDateTime(Date labelingDateTime);
}
