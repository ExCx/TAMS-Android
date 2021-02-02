package com.frekanstan.gateway_mobil.app.connection;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

public class GetAllDemandedVariantsInput implements Serializable {
    @Getter @Setter
    private long receiptId;

    public GetAllDemandedVariantsInput(long receiptId) {
        this.receiptId = receiptId;
    }
}
