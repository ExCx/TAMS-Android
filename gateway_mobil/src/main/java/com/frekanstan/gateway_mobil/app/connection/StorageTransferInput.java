package com.frekanstan.gateway_mobil.app.connection;

import java.io.Serializable;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

public class StorageTransferInput implements Serializable {
    @Getter @Setter
    private int inWarehouseId;

    @Getter @Setter
    private int outWarehouseId;

    private List<String> rfids;

    public StorageTransferInput() { }

    public StorageTransferInput(List<String> rfids, int inWarehouseId, int outWarehouseId) {
        this.rfids = rfids;
        this.inWarehouseId = inWarehouseId;
        this.outWarehouseId = outWarehouseId;
    }

    public List<String> getRfids() {
        return rfids;
    }

    public void setIds(List<String> rfids) {
        this.rfids = rfids;
    }
}