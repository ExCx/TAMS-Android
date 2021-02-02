package com.frekanstan.gateway_mobil.app.connection;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

public class GetAllInventoryInput implements Serializable {
    @Getter @Setter
    private Integer warehouseId;

    public GetAllInventoryInput() { }

    public GetAllInventoryInput(Integer warehouseId) {
        this.warehouseId = warehouseId;
    }
}
