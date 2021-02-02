package com.frekanstan.gateway_mobil.data;

import com.frekanstan.asset_management.data.EntityBase;
import com.frekanstan.asset_management.data.configuration.ISetting;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Unique;
import lombok.Getter;
import lombok.Setter;

@Entity
public class Setting extends EntityBase implements ISetting {
    @Id
    @Getter @Setter
    public long id;

    @Unique
    @Getter
    private String name;

    @Getter
    private String value;

    public Setting() {
    }

    public Setting(String name, String value) {
        this.name = name;
        this.value = value;
    }
}