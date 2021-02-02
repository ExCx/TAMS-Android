package com.frekanstan.kbs_mobil.data;

import androidx.annotation.NonNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.frekanstan.asset_management.data.EntityBase;
import com.frekanstan.asset_management.data.assets.IModel;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
public class Model extends EntityBase implements IModel {
    @Id(assignable = true)
    @Getter @Setter
    public long id;

    @Getter
    private String definition;

    @Getter
    private long parentBrandId;
    @JsonIgnore
    public ToOne<Brand> parentBrand;

    public Model() {
    }

    public Model(long id, String definition, long parentBrandId) {
        this.id = id;
        this.definition = definition;
        this.parentBrandId = parentBrandId;
        if (parentBrandId != 0)
            this.parentBrand.setTargetId(parentBrandId);
    }

    @NonNull
    @Override
    public String toString() {
        return definition;
    }
}