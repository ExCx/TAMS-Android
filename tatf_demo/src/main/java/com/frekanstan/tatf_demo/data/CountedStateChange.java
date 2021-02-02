package com.frekanstan.tatf_demo.data;

import com.frekanstan.asset_management.data.EntityBase;
import com.frekanstan.asset_management.data.tracking.ICountedStateChange;

import java.util.Date;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
public class CountedStateChange extends EntityBase implements ICountedStateChange {
    @Id
    @Getter @Setter
    public long id;

    @Getter
    private long assetId;

    @Getter @Setter
    private Date lastControlTime;

    @Getter @Setter
    private long countingOpId;

    @Getter @Setter
    private long globalId;

    public CountedStateChange() {
    }

    public CountedStateChange(long assetId, Date lastControlTime, long countingOpId, long globalId) {
        this.assetId = assetId;
        this.lastControlTime = lastControlTime;
        this.countingOpId = countingOpId;
        this.globalId = globalId;
    }
}