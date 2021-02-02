package com.frekanstan.asset_management.app;

import android.os.Bundle;

import androidx.lifecycle.LiveData;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;

import com.frekanstan.asset_management.data.IEntity;
import com.google.common.primitives.Longs;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import io.objectbox.Box;
import io.objectbox.TxCallback;
import io.objectbox.android.AndroidScheduler;
import io.objectbox.android.ObjectBoxDataSource;
import io.objectbox.query.Query;
import io.objectbox.reactive.DataObserver;
import io.objectbox.reactive.DataSubscription;
import lombok.val;

public abstract class DAO<TEntity extends IEntity> implements IDAO<TEntity>
{
    @Override
    public abstract Box<TEntity> getBox();

    private final HashMap<String, long[]> idIndex = new HashMap<>();

    @Override
    public long[] getIdIndex(String key) {
        return idIndex.get(key);
    }

    @Override
    public int getIndexOf(String key, long id) {
        return Longs.indexOf(getIdIndex(key), id);
    }

    @Override
    public TEntity get(long id) {
        return getBox().get(id);
    }

    @Override
    public ArrayList<TEntity> getAll() {
        return new ArrayList<>(getBox().getAll());
    }

    @Override
    public ArrayList<TEntity> getAll(Bundle input) {
        return new ArrayList<>(createFilteredQuery(input).find());
    }

    @Override
    public ArrayList<TEntity> getAll(List<Long> ids) {
        return new ArrayList<>(getBox().get(ids));
    }

    @Override
    public long count() {
        return getBox().count();
    }

    @Override
    public long count(Bundle input) {
        return createFilteredQuery(input).count();
    }

    @Override
    public long put(TEntity item) {
        return getBox().put(item);
    }

    @Override
    public void putAll(List<TEntity> items) {
        getBox().put(items);
    }

    @Override
    public void putAllAsync(List<TEntity> items, TxCallback<Void> callback) {
        getBox().getStore().runInTxAsync(() -> DAO.this.getBox().put(items), callback);
    }

    @Override
    public void removeAll() {
        getBox().removeAll();
    }

    @Override
    public void removeAll(List<TEntity> items) {
        getBox().remove(items);
    }

    @Override
    public ArrayList<Long> getAllIds(Bundle input) {
        val ids = ArrayUtils.toObject(createFilteredQuery(input).findIds());
        return new ArrayList<>(Arrays.asList(ids));
    }

    @Override
    public LiveData<PagedList<TEntity>> getAllLiveData(Bundle bundle) {
        val query = createFilteredQuery(bundle);
        //idIndex.put(bundle.getString("listType", "default"), query.findIds());
        val dataSourceFactory = new ObjectBoxDataSource.Factory<>(query);
        return new LivePagedListBuilder<>(dataSourceFactory, 20).build();
    }

    @Override
    public LiveData<PagedList<TEntity>> getAllLiveData(Bundle bundle, TxCallback<Void> indexCallback) {
        val query = createFilteredQuery(bundle);
        //getBox().getStore().runInTxAsync(() -> idIndex.put(bundle.getString("listType", "default"), query.findIds()), indexCallback);
        val dataSourceFactory = new ObjectBoxDataSource.Factory<>(query);
        //dataSourceFactory.map((x) -> x);
        return new LivePagedListBuilder<>(dataSourceFactory, 20).build();
    }

    @Override
    public Query<TEntity> createFilteredQuery(Bundle input) {
        return getBox().query().build();
    }

    @Override
    public DataSubscription subscribe(DataObserver<List<TEntity>> observer, Bundle input) {
        return createFilteredQuery(input).subscribe().on(AndroidScheduler.mainThread()).observer(observer);
    }
}
