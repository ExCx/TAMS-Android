package com.frekanstan.tatf_demo.data;

import androidx.annotation.NonNull;

import com.frekanstan.asset_management.data.EntityBase;
import com.frekanstan.asset_management.data.assets.IBrand;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
public class Brand extends EntityBase implements IBrand {
    @Id(assignable = true)
    @Getter @Setter
    public long id;

    @Getter
    private String definition;

    @Getter
    private long parentTypeId;
    public ToOne<AssetType> parentType;

    public Brand() {
    }

    public Brand(long id, String definition, long parentTypeId) {
        this.id = id;
        this.definition = definition;
        this.parentTypeId = parentTypeId;
        if (parentTypeId != 0)
            this.parentType.setTargetId(parentTypeId);
    }

    @NonNull
    @Override
    public String toString() {
        return definition;
    }
}