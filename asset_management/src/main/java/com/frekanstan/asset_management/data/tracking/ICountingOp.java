package com.frekanstan.asset_management.data.tracking;

import com.frekanstan.asset_management.data.IEntity;
import com.frekanstan.asset_management.data.assettypes.IAssetType;
import com.frekanstan.asset_management.data.locations.ILocation;
import com.frekanstan.asset_management.data.people.IPerson;

public interface ICountingOp extends IEntity {
    long getPersonCreatedId();

    long getPersonTaskedId();

    java.util.Date getCreationTime();

    java.util.Date getDeadline();

    java.util.Date getTimeStarted();

    java.util.Date getTimeFinished();

    long getRelatedTypeId();

    long getRelatedLocationId();

    long getRelatedPersonId();

    Boolean getIsConfirmed();

    Boolean getIsDeleted();

    long getTenantId();

    IAssetType getRelatedType();

    IPerson getPersonTasked();

    ILocation getRelatedLocation();

    IPerson getRelatedPerson();
}
