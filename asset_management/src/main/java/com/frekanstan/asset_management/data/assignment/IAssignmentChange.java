package com.frekanstan.asset_management.data.assignment;

import com.frekanstan.asset_management.data.IEntity;

public interface IAssignmentChange extends IEntity {
    long getAssetId();

    long getPersonId();

    void setPersonId(long personId);

    long getLocationId();

    void setLocationId(long locationId);

    boolean getSent();

    void setSent(boolean sent);
}
