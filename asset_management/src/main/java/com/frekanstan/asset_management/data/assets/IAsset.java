package com.frekanstan.asset_management.data.assets;

import com.frekanstan.asset_management.data.IEntity;
import com.frekanstan.asset_management.data.acquisition.EWorkingState;
import com.frekanstan.asset_management.data.acquisition.IBudget;
import com.frekanstan.asset_management.data.assettypes.IAssetType;
import com.frekanstan.asset_management.data.locations.ILocation;
import com.frekanstan.asset_management.data.people.IPerson;

import java.util.Date;
import java.util.List;

public interface IAsset extends IEntity {
    long getAssetTypeId();

    IAssetType getAssetType();

    String getAssetTypeDefinition();

    List<Long> getAllTypeIds();

    String getAssetCode();

    String getRemoteId();

    String getRegistrationCode();

    String getFeatures();

    long getBrandNameId();

    long getModelNameId();

    Date getLabelingDateTime();

    void setAsLabeled(boolean labeled);

    void setAsToBeLabeled();

    Date getLastControlTime();

    Boolean getIsDeleted();

    String getSerialNo();

    String getRfidCode();

    long getAssignedPersonId();

    IPerson getAssignedPerson();

    long getAssignedLocationId();

    ILocation getAssignedLocation();

    long getStorageBelongsToId();

    ILocation getStorageBelongsTo();

    Boolean getHasPhoto();

    Double getPrice();

    long getTenantId();

    void setRfidCode(String code);

    String getAssignedPersonNameSurname();

    String getAssignedLocationName();

    EWorkingState getWorkingState();

    void setWorkingState(EWorkingState selectedState);

    void setIsUpdated(boolean b);

    IBrand getBrandName();

    IModel getModelName();

    IBudget getBudgetType();

    void setFeatures(String features);
}
