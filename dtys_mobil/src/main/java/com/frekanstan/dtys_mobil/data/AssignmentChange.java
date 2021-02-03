package com.frekanstan.dtys_mobil.data;

import com.frekanstan.asset_management.data.EntityBase;
import com.frekanstan.asset_management.data.assignment.IAssignmentChange;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
public class AssignmentChange extends EntityBase implements IAssignmentChange {
    @Id
    @Getter @Setter
    public long id;

    @Getter
    private long assetId;

    @Getter @Setter
    private long personId;

    @Getter @Setter
    private long locationId;

    @Getter @Setter
    private Boolean isRequest;

    @Setter
    private boolean sent;

    public AssignmentChange() {
    }

    public AssignmentChange(long assetId, long personId, long locationId, Boolean isRequest) {
        this.assetId = assetId;
        this.personId = personId;
        this.locationId = locationId;
        this.isRequest = isRequest;
        this.sent = false;
    }

    public boolean getSent() {
        return sent;
    }
}