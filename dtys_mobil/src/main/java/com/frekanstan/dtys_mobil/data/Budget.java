package com.frekanstan.dtys_mobil.data;

import androidx.annotation.NonNull;

import com.frekanstan.asset_management.data.EntityBase;
import com.frekanstan.asset_management.data.acquisition.IBudget;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
public class Budget extends EntityBase implements IBudget {
    @Id(assignable = true)
    @Getter @Setter
    public long id;

    @Getter
    private String definition;

    public Budget() {
    }

    public Budget(long id, String definition) {
        this.id = id;
        this.definition = definition;
    }

    @NonNull
    @Override
    public String toString() {
        return definition;
    }
}