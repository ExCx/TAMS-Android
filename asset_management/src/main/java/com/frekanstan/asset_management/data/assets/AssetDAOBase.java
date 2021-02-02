package com.frekanstan.asset_management.data.assets;

import android.os.Bundle;

import com.frekanstan.asset_management.app.DAO;
import com.frekanstan.asset_management.data.assignment.IAssignmentChange;
import com.frekanstan.asset_management.data.labeling.ILabeledStateChange;
import com.frekanstan.asset_management.data.tracking.ICountedStateChange;
import com.google.common.primitives.Longs;

import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

import io.objectbox.query.Query;
import io.objectbox.query.QueryBuilder;
import lombok.val;
import lombok.var;

public abstract class AssetDAOBase<TAsset extends IAsset> extends DAO<TAsset> {
    protected abstract QueryBuilder<TAsset> buildRootQuery();

    @Override
    public TAsset get(long id) {
        return super.get(id);
    }

    @Override
    public ArrayList<TAsset> getAll() {
        return new ArrayList<>(buildRootQuery().build().find());
    }

    @Override
    public long count() {
        return buildRootQuery().build().count();
    }

    public TAsset getRandom() {
        return buildRootQuery().build().find().get(new Random().nextInt((int)count()));
    }

    //public abstract TAsset getByRemoteId(long id);

    public abstract long[] getAllAssignedPersonIds();

    public abstract long[] getAllAssignedLocationIds();

    public long[] getAllAvailableTypeIds() {
        var typeIds = new ArrayList<Long>();
        for (val item : getAll())
        {
            for (val typeId : item.getAllTypeIds()) {
                if (!typeIds.contains(typeId))
                    typeIds.add(typeId);
            }
        }
        return Longs.toArray(typeIds);
    }

    public abstract long countByAssetCode(String assetCode, boolean onlyCounted, boolean onlyLabeled);

    protected abstract ICountedStateChange getCountedStateChange(long assetId, long countingOpId);

    public abstract void setCountedStateChange(long assetId, Date lastControlTime, boolean isCounted, long countingOpId);

    protected abstract ILabeledStateChange getLabeledStateChange(long assetId);

    public abstract void setLabeledStateChange(long assetId, boolean isLabeled);

    public abstract IAssignmentChange getAssignmentChange(long assetId);

    public abstract void setAssignmentChange(long assetId, long personId, long locationId);

    @Override
    public abstract Query<TAsset> createFilteredQuery(Bundle bundle);
}