package com.frekanstan.tatf_demo.data;

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

    @Setter
    private boolean sent;

    public AssignmentChange() {
    }

    public AssignmentChange(long assetId, long personId, long locationId) {
        this.assetId = assetId;
        this.personId = personId;
        this.locationId = locationId;
        this.sent = false;
    }

    public boolean getSent() {
        return sent;
    }
}