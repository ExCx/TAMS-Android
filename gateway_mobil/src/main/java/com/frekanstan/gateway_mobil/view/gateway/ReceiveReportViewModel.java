package com.frekanstan.gateway_mobil.view.gateway;

import android.os.Bundle;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import androidx.paging.PagedList;

import com.frekanstan.gateway_mobil.app.assettypes.AssetTypeDAO;
import com.frekanstan.gateway_mobil.data.AssetType;

import io.objectbox.TxCallback;

public class ReceiveReportViewModel extends ViewModel {
    private MutableLiveData<Bundle> getAllInput = new MutableLiveData<>();

    public LiveData<PagedList<AssetType>> getLiveData(TxCallback<Void> callback) {
        return Transformations.switchMap(
                getAllInput,
                (input) -> AssetTypeDAO.getDao().getAllLiveData(input, callback));
    }

    void setInput(Bundle input) {
        this.getAllInput.setValue(input);
    }

    public Bundle getInput() {
        return this.getAllInput.getValue();
    }
}