package com.frekanstan.gateway_mobil.app.connection;

import java.io.Serializable;
import java.util.List;

public class GetPackagesInput implements Serializable {
    private List<String> rfids;

    public GetPackagesInput() { }

    public GetPackagesInput(List<String> rfids) {
        this.rfids = rfids;
    }

    public List<String> getRfids() {
        return rfids;
    }

    public void setIds(List<String> rfids) {
        this.rfids = rfids;
    }
}
