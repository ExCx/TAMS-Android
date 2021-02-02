package com.frekanstan.kbs_mobil.data;

import com.frekanstan.asset_management.data.EntityBase;
import com.frekanstan.asset_management.data.labeling.ILabeledStateChange;

import java.util.Date;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
public class LabeledStateChange extends EntityBase implements ILabeledStateChange {
    @Id
    @Getter @Setter
    public long id;

    @Getter
    private String type;

    @Getter
    private long entityId;

    @Getter @Setter
    private Date labelingDateTime;

    public LabeledStateChange() {
    }

    public LabeledStateChange(String type, long entityId, Date labelingDateTime) {
        this.type = type;
        this.entityId = entityId;
        this.labelingDateTime = labelingDateTime;
    }
}