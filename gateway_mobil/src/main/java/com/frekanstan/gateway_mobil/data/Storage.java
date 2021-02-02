package com.frekanstan.gateway_mobil.data;

import androidx.annotation.NonNull;

import lombok.Getter;
import lombok.Setter;

public class Storage {
    @Getter @Setter
    private Integer remoteId;

    @Getter @Setter
    private String name;

    @NonNull
    @Override
    public String toString() {
        return name;
    }

    public Storage(Integer remoteId, String name) {
        this.remoteId = remoteId;
        this.name = name;
    }
}