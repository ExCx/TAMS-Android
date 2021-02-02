package com.frekanstan.gateway_mobil.data;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

public class Receipt implements Comparable<Receipt> {
    @Getter @Setter
    private long remoteId;

    @Getter @Setter
    private String receiptNo;

    @Getter @Setter
    private Date receiptDate;

    @Getter @Setter
    private Integer warehouseId;

    @Getter @Setter
    public long currentAccountId;

    @Getter @Setter
    private String currentAccountName;

    @Override
    public int compareTo(Receipt o) {
        return this.getReceiptNo().compareTo(o.getReceiptNo());
    }
}