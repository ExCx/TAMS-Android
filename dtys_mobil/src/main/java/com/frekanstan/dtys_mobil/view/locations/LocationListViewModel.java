package com.frekanstan.dtys_mobil.view.locations;

import android.os.Bundle;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import androidx.paging.PagedList;

import com.frekanstan.dtys_mobil.app.locations.LocationDAO;
import com.frekanstan.dtys_mobil.data.Location;

import io.objectbox.TxCallback;

public class LocationListViewModel extends ViewModel {
    private MutableLiveData<Bundle> getAllInput = new MutableLiveData<>();

    public LiveData<PagedList<Location>> getLiveData(TxCallback<Void> callback) {
        return Transformations.switchMap(
                    getAllInput,
                (input) -> LocationDAO.getDao().getAllLiveData(input, callback));
    }

    void setInput(Bundle input) {
        this.getAllInput.setValue(input);
    }
}