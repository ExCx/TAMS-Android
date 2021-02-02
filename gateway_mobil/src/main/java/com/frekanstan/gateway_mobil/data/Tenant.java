package com.frekanstan.gateway_mobil.data;

import androidx.annotation.NonNull;

import com.frekanstan.asset_management.data.EntityBase;
import com.frekanstan.asset_management.data.multitenancy.ITenant;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
public class Tenant extends EntityBase implements ITenant {
    @Id(assignable = true)
    @Getter @Setter
    public long id;

    @Getter
    private String tenancyName;

    @Getter
    private String name;

    @Getter
    private Boolean isActive;

    public Tenant() {
    }

    public Tenant(long id, String tenancyName, String name, Boolean isActive) {
        this.id = id;
        this.tenancyName = tenancyName;
        this.name = name;
        this.isActive = isActive;
    }

    @NonNull
    @Override
    public String toString() {
        return name;
    }
}