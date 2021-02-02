package com.frekanstan.kbs_mobil.data;

import android.annotation.SuppressLint;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.frekanstan.asset_management.data.EntityBase;
import com.frekanstan.asset_management.data.acquisition.EWorkingState;
import com.frekanstan.asset_management.data.assets.IAsset;
import com.frekanstan.asset_management.view.MainActivityBase;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.objectbox.annotation.Backlink;
import io.objectbox.annotation.Convert;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Index;
import io.objectbox.annotation.IndexType;
import io.objectbox.annotation.Unique;
import io.objectbox.relation.ToMany;
import io.objectbox.relation.ToOne;
import lombok.Getter;
import lombok.Setter;
import lombok.var;

@Entity
public class Asset extends EntityBase implements IAsset {
    @Id(assignable = true)
    @Getter @Setter
    public long id;

    @Getter
    private long assetTypeId;
    @JsonIgnore
    public ToOne<AssetType> assetType;

    @Getter @Setter
    private String assetTypeDefinition;

    @Index
    @Getter
    private String remoteId;
    @Getter
    private String remoteId2;

    @Getter
    private String registrationCode;

    @Getter @Setter
    private String features;

    @Getter @Setter
    private long brandNameId;
    @JsonIgnore
    public ToOne<Brand> brandName;
    @Getter @Setter
    private String brandNameDefinition;

    @Getter @Setter
    private long modelNameId;
    @JsonIgnore
    public ToOne<Model> modelName;
    @Getter @Setter
    private String modelNameDefinition;

    @Getter @Setter
    private long budgetTypeId;
    @JsonIgnore
    public ToOne<Budget> budgetType;

    @Getter @Setter
    private String serialNo;

    @Getter @Setter @Index(type = IndexType.VALUE) @Unique
    private String rfidCode;

    @Getter
    private Double price;

    @Getter @Setter
    private long assignedPersonId;
    @JsonIgnore
    public ToOne<Person> assignedPerson;

    @Getter @Setter
    private String assignedPersonNameSurname;

    @Getter @Setter
    private long personToAssignId;
    @JsonIgnore
    public ToOne<Person> personToAssign;

    @Getter @Setter
    private long assignedLocationId;
    @JsonIgnore
    public ToOne<Location> assignedLocation;

    @Getter @Setter
    private long locationToAssignId;
    @JsonIgnore
    public ToOne<Location> locationToAssign;

    @Getter @Setter
    private String assignedLocationName;

    @Getter
    private long storageBelongsToId;
    @JsonIgnore
    public ToOne<Location> storageBelongsTo;

    @Convert(converter = EWorkingState.WorkingStateTypeConverter.class, dbType = Integer.class)
    @Getter @Setter
    private EWorkingState workingState;

    @Getter
    private Integer quantity;

    @Getter
    private Integer lastQuantity;

    @Getter @Setter
    private Date lastControlTime;

    @Getter
    private Date labelingDateTime;

    @Getter
    private Boolean isDeleted;

    @Getter
    private Boolean hasPhoto;

    @Getter
    private Boolean isUpdated;

    @Getter @Setter
    private Boolean tempCounted = false;

    @Getter
    private long tenantId;
    @JsonIgnore
    public ToOne<Tenant> tenant;

    @Backlink(to = "countedAssets")
    @JsonIgnore
    public ToMany<CountingOp> countingOps;

    public Asset() {
    }

    public Asset(long id, long assetTypeId, String registrationCode, long brandNameId, long tenantId, String serialNo, String rfidCode, Double price, String features, long modelNameId, long budgetTypeId, long assignedPersonId, long assignedLocationId, long storageBelongsToId, EWorkingState workingState, Integer quantity, Integer lastQuantity, String remoteId, String remoteId2, Date lastControlTime, Date labelingDateTime, Boolean isDeleted, Boolean hasPhoto) {
        this.id = id;
        this.assetTypeId = assetTypeId;
        this.assetType.setTargetId(assetTypeId);
        this.registrationCode = registrationCode;
        this.budgetTypeId = budgetTypeId;
        if (budgetTypeId != 0)
            this.budgetType.setTargetId(budgetTypeId);
        this.remoteId = remoteId;
        this.remoteId2 = remoteId2;
        this.brandNameId = brandNameId;
        if (brandNameId != 0)
            this.brandName.setTargetId(brandNameId);
        this.modelNameId = modelNameId;
        if (modelNameId != 0)
            this.modelName.setTargetId(modelNameId);
        this.serialNo = serialNo;
        this.rfidCode = rfidCode;
        this.price = price;
        this.features = features;
        this.assignedPersonId = assignedPersonId;
        if (assignedPersonId != 0)
            this.assignedPerson.setTargetId(assignedPersonId);
        this.assignedLocationId = assignedLocationId;
        if (assignedLocationId != 0)
            this.assignedLocation.setTargetId(assignedLocationId);
        this.storageBelongsToId = storageBelongsToId;
        this.storageBelongsTo.setTargetId(storageBelongsToId);
        this.workingState = workingState;
        this.quantity = quantity;
        this.lastQuantity = lastQuantity;
        this.lastControlTime = lastControlTime;
        this.labelingDateTime = labelingDateTime;
        this.isDeleted = isDeleted;
        this.hasPhoto = hasPhoto;
        this.tenantId = tenantId;
        this.tenant.setTargetId(tenantId);
    }

    @Override
    public AssetType getAssetType() { return assetType.getTarget(); }

    public Brand getBrandName() { return brandName.getTarget(); }

    public Model getModelName() { return modelName.getTarget(); }

    public Budget getBudgetType() { return budgetType.getTarget(); }

    @Override
    public List<Long> getAllTypeIds() {
        var typeIds = new ArrayList<Long>();
        typeIds.add(getAssetTypeId());
        var type = getAssetType();
        if (type == null)
            return typeIds;
        while (type.getParentTypeId() != 0)
        {
            typeIds.add(type.getParentTypeId());
            type = type.getParentType();
        }
        return typeIds;
    }

    @Override
    @SuppressLint("DefaultLocale")
    public String getAssetCode() {
        return String.format("%010d", id);
    }

    @Override
    public void setAsLabeled(boolean labeled) {
        if (labeled)
            this.labelingDateTime = new Date();
        else
            this.labelingDateTime = null;
    }

    @Override
    public void setAsToBeLabeled() {
        this.labelingDateTime = MainActivityBase.nullDate.getTime();
    }

    @Override
    public Person getAssignedPerson() { return assignedPerson.getTarget(); }

    @Override
    public Location getAssignedLocation() { return assignedLocation.getTarget(); }

    @Override
    public Location getStorageBelongsTo() { return storageBelongsTo.getTarget(); }

    @Override
    public void setIsUpdated(boolean b) {
        this.isUpdated = b;
    }
}