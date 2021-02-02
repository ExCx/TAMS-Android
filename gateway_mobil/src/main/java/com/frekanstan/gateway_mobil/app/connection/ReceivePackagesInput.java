package com.frekanstan.gateway_mobil.app.connection;

import java.io.Serializable;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

public class ReceivePackagesInput implements Serializable {
    @Getter @Setter
    public long currentAccountId;

    @Getter @Setter
    private int warehouseId;

    private List<String> rfids;

    public ReceivePackagesInput() { }

    public ReceivePackagesInput(List<String> rfids, int warehouseId, long currentAccountId) {
        this.rfids = rfids;
        this.warehouseId = warehouseId;
        this.currentAccountId = currentAccountId;
    }

    public List<String> getRfids() {
        return rfids;
    }

    public void setIds(List<String> rfids) {
        this.rfids = rfids;
    }
}
