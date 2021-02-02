package com.frekanstan.dtys_mobil.data;

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
    private Short acquisitionYear;

    @Getter
    private String lotNo;

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
    private String reasonOfFailure;

    @Getter
    private Short warrantyPeriodInYears;

    @Getter
    private String biomedicalType;

    @Getter
    private String biomedicalDefinition;

    @Getter
    private String biomedicalBranch;

    @Getter @Setter
    private Date lastControlTime;

    @Getter
    private Date labelingDateTime;

    @Getter
    private String placeOfUse;

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

    public Asset(long id, long assetTypeId, long tenantId, String remoteId, long budgetTypeId, long brandNameId, long modelNameId, String registrationCode, String serialNo, String rfidCode, Double price, String features, String brand, String model, String biomedicalType, String biomedicalDefinition, String biomedicalBranch, Short acquisitionYear, String lotNo, long assignedPersonId, long assignedLocationId, long storageBelongsToId, Date lastControlTime, EWorkingState workingState, String reasonOfFailure, Short warrantyPeriodInYears, String placeOfUse, Date labelingDateTime, Boolean isDeleted, Boolean hasPhoto) {
        this.id = id;
        this.assetTypeId = assetTypeId;
        this.assetType.setTargetId(assetTypeId);
        this.budgetTypeId = budgetTypeId;
        if (budgetTypeId != 0)
            this.budgetType.setTargetId(budgetTypeId);
        this.reasonOfFailure = reasonOfFailure;
        this.placeOfUse = placeOfUse;
        this.remoteId = remoteId;
        this.registrationCode = registrationCode;
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
        this.biomedicalType = biomedicalType;
        this.biomedicalDefinition = biomedicalDefinition;
        this.biomedicalBranch = biomedicalBranch;
        this.acquisitionYear = acquisitionYear;
        this.lotNo = lotNo;
        this.assignedPersonId = assignedPersonId;
        if (assignedPersonId != 0)
            this.assignedPerson.setTargetId(assignedPersonId);
        this.assignedLocationId = assignedLocationId;
        if (assignedLocationId != 0)
            this.assignedLocation.setTargetId(assignedLocationId);
        this.storageBelongsToId = storageBelongsToId;
        this.storageBelongsTo.setTargetId(storageBelongsToId);
        this.lastControlTime = lastControlTime;
        this.workingState = workingState;
        this.warrantyPeriodInYears = warrantyPeriodInYears;
        this.labelingDateTime = labelingDateTime;
        this.isDeleted = isDeleted;
        this.hasPhoto = hasPhoto;
        this.tenantId = tenantId;
        this.tenant.setTargetId(tenantId);
    }

    @Override
    public AssetType getAssetType() { return assetType.getTarget(); }

    @Override
    public Brand getBrandName() { return brandName.getTarget(); }

    @Override
    public Model getModelName() { return modelName.getTarget(); }

    @Override
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