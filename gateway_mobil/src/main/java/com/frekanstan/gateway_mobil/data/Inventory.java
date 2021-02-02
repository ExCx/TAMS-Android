package com.frekanstan.gateway_mobil.data;

import lombok.Getter;
import lombok.Setter;

public class Inventory {
    @Getter @Setter
    private String productName;

    @Getter @Setter
    private Integer quantity;

    @Getter @Setter
    private String typeRemoteId;
}