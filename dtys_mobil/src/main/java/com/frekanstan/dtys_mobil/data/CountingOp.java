package com.frekanstan.dtys_mobil.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.frekanstan.asset_management.data.EntityBase;
import com.frekanstan.asset_management.data.tracking.ICountingOp;

import java.util.Date;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Index;
import io.objectbox.relation.ToMany;
import io.objectbox.relation.ToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
public class CountingOp extends EntityBase implements ICountingOp {
    @Id
    @Getter @Setter
    public long id;

    @Index @Getter @Setter
    public long globalId;

    @Getter @Setter
    private long personCreatedId;
    @JsonIgnore
    public ToOne<Person> personCreated;

    @Getter
    private long personTaskedId;
    @JsonIgnore
    public ToOne<Person> personTasked;

    @Getter @Setter
    private Date creationTime;

    @Getter @Setter
    private Date deadline;

    @Getter
    private Date timeStarted;

    @Getter
    private Date timeFinished;

    @Getter
    private long relatedTypeId;
    @JsonIgnore
    public ToOne<AssetType> relatedType;

    @Getter
    private long relatedLocationId;
    @JsonIgnore
    public ToOne<Location> relatedLocation;

    @Getter
    private long relatedPersonId;
    @JsonIgnore
    public ToOne<Person> relatedPerson;

    @Getter
    private Boolean isConfirmed;

    @Getter
    private Boolean isDeleted;

    @Getter @Setter
    private Boolean isUpdated;

    @Getter
    private long tenantId;
    @JsonIgnore
    public ToOne<Tenant> tenant;

    @JsonIgnore
    public ToMany<Asset> countedAssets;

    public CountingOp() { }

    public CountingOp(long id, long globalId, long personCreatedId, long personTaskedId, Date creationTime, Date deadline, Date timeStarted, Date timeFinished, long relatedTypeId, long relatedLocationId, long relatedPersonId, Boolean isConfirmed, Boolean isDeleted, long tenantId) {
        this.id = id;
        this.globalId = globalId;
        this.personCreatedId = personCreatedId;
        if (personCreatedId != 0)
            this.personCreated.setTargetId(personCreatedId);
        this.personTaskedId = personTaskedId;
        if (personTaskedId != 0)
            this.personTasked.setTargetId(personTaskedId);
        this.creationTime = creationTime;
        this.deadline = deadline;
        this.timeStarted = timeStarted;
        this.timeFinished = timeFinished;
        this.relatedTypeId = relatedTypeId;
        if (relatedTypeId != 0)
            this.relatedType.setTargetId(relatedTypeId);
        this.relatedLocationId = relatedLocationId;
        if (relatedLocationId != 0)
            this.relatedLocation.setTargetId(relatedLocationId);
        this.relatedPersonId = relatedPersonId;
        if (relatedPersonId != 0)
            this.relatedPerson.setTargetId(relatedPersonId);
        this.isConfirmed = isConfirmed;
        this.isDeleted = isDeleted;
        this.tenantId = tenantId;
        if (tenantId != 0)
            this.tenant.setTargetId(tenantId);
    }

    @Override
    public AssetType getRelatedType() { return relatedType.getTarget(); }

    @Override
    public Person getRelatedPerson() { return relatedPerson.getTarget(); }

    @Override
    public Person getPersonTasked() { return personTasked.getTarget(); }

    @Override
    public Location getRelatedLocation() { return relatedLocation.getTarget(); }
}