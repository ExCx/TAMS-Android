package com.frekanstan.gateway_mobil.view.personlist;

import android.os.Bundle;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import androidx.paging.PagedList;

import com.frekanstan.gateway_mobil.app.people.PersonDAO;
import com.frekanstan.gateway_mobil.data.Person;

import io.objectbox.TxCallback;

public class PersonListViewModel extends ViewModel {
    private MutableLiveData<Bundle> getAllInput = new MutableLiveData<>();

    public LiveData<PagedList<Person>> getLiveData(TxCallback<Void> callback) {
        return Transformations.switchMap(
                getAllInput,
                (input) -> PersonDAO.getDao().getAllLiveData(input, callback));
    }

    void setInput(Bundle input) {
        this.getAllInput.setValue(input);
    }
}