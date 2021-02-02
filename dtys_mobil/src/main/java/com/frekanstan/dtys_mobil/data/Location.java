package com.frekanstan.dtys_mobil.data;

import android.annotation.SuppressLint;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.frekanstan.asset_management.data.EntityBase;
import com.frekanstan.asset_management.data.locations.ELocationType;
import com.frekanstan.asset_management.data.locations.ILocation;
import com.frekanstan.asset_management.view.MainActivityBase;

import java.util.Date;

import io.objectbox.annotation.Backlink;
import io.objectbox.annotation.Convert;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToMany;
import io.objectbox.relation.ToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
public class Location extends EntityBase implements ILocation {
    @Id(assignable = true)
    @Getter @Setter
    public long id;

    @Getter
    private String name;

    @Getter
    private String shortName;

    @Getter
    private String remoteId;

    @Convert(converter = ELocationType.LocationTypeTypeConverter.class, dbType = Integer.class)
    @Getter
    private ELocationType locationType;

    @Getter
    private Date labelingDateTime;

    @Getter
    private long parentLocationId;
    @JsonIgnore
    ToOne<Location> parentLocation;

    @Getter
    private long tenantBoundId;
    @JsonIgnore
    ToOne<Tenant> tenantBound;

    @Getter
    private long tenantId;
    @JsonIgnore
    public ToOne<Tenant> tenant;

    @Backlink(to = "relatedLocation")
    @JsonIgnore
    public ToMany<CountingOp> countingOps;

    public Location() { }

    public Location(long id, String name, String shortName, String remoteId, long parentLocationId, long tenantId, long tenantBoundId, ELocationType locationType, Date labelingDateTime) {
        this.id = id;
        this.name = name;
        this.shortName = shortName;
        this.remoteId = remoteId;
        this.locationType = locationType;
        this.labelingDateTime = labelingDateTime;
        this.parentLocationId = parentLocationId;
        if (parentLocationId != 0)
            this.parentLocation.setTargetId(parentLocationId);
        this.tenantBoundId = tenantBoundId;
        if (tenantBoundId != 0)
            this.tenantBound.setTargetId(tenantBoundId);
        this.tenantId = tenantId;
        if (tenantId != 0)
            this.tenant.setTargetId(tenantId);
    }

    @Override
    @SuppressLint("DefaultLocale")
    public String getLocationCode() {
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
}