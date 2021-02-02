package com.frekanstan.gateway_mobil.view.assetlist;

import android.os.Bundle;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import androidx.paging.PagedList;

import com.frekanstan.gateway_mobil.app.assets.AssetDAO;
import com.frekanstan.gateway_mobil.data.Asset;

import io.objectbox.TxCallback;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import lombok.Getter;

public class AssetListViewModel extends ViewModel {
    private LiveData<PagedList<Asset>> liveData;
    private MutableLiveData<Bundle> getAllInput = new MutableLiveData<>();

    @Getter
    private Asset lastClickedAsset = new Asset();
    private Subject<Asset> lcaObservable;

    public Disposable subscribeToLastClickedAsset(Consumer<Asset> o) {
        if (lcaObservable == null)
            lcaObservable = PublishSubject.create();
        return lcaObservable.subscribe(o);
    }

    public void setLastClickedAsset(Asset asset) {
        lastClickedAsset = asset;
        if (lcaObservable != null && lastClickedAsset != null)
            lcaObservable.onNext(lastClickedAsset);
    }

    public LiveData<PagedList<Asset>> getLiveData(TxCallback<Void> callback) {
        liveData = Transformations.switchMap(
                getAllInput,
                (input) -> {
                    return AssetDAO.getDao().getAllLiveData(input, callback);
                    /*val q = input.getString("query");
                    if (Strings.isNullOrEmpty(q))
                        return AssetDAO.getInstance().getAllLiveData(input, callback);
                    else {
                        return AssetDAO.getInstance().getAllLiveDataWithQuery(q);
                    }*/
                });
        return liveData;
    }

    void setInput(Bundle input) {
        this.getAllInput.setValue(input);
    }
}