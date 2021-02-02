package com.frekanstan.asset_management.app;

import android.os.Bundle;

import androidx.lifecycle.LiveData;
import androidx.paging.PagedList;

import com.frekanstan.asset_management.data.IEntity;

import java.util.ArrayList;
import java.util.List;

import io.objectbox.Box;
import io.objectbox.TxCallback;
import io.objectbox.query.Query;
import io.objectbox.reactive.DataObserver;
import io.objectbox.reactive.DataSubscription;

public interface IDAO<TEntity extends IEntity> {
    Box<TEntity> getBox();

    long[] getIdIndex(String key);

    int getIndexOf(String key, long id);

    TEntity get(long id);

    ArrayList<TEntity> getAll();

    ArrayList<TEntity> getAll(List<Long> ids);

    ArrayList<TEntity> getAll(Bundle input);

    long count();

    long count(Bundle bundle);

    long put(TEntity item);

    void putAll(List<TEntity> items);

    void putAllAsync(List<TEntity> items, TxCallback<Void> callback);

    void removeAll();

    void removeAll(List<TEntity> items);

    ArrayList<Long> getAllIds(Bundle input);

    LiveData<PagedList<TEntity>> getAllLiveData(Bundle bundle);

    LiveData<PagedList<TEntity>> getAllLiveData(Bundle bundle, TxCallback<Void> indexCallback);

    //LiveData<PagedList<TEntity>> getAllLiveDataWithQuery(String q);

    Query<TEntity> createFilteredQuery(Bundle input);

    DataSubscription subscribe(DataObserver<List<TEntity>> observer, Bundle input);
}
