package com.frekanstan.kbs_mobil.view.countingops;

import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.frekanstan.asset_management.data.locations.ELocationType;
import com.frekanstan.asset_management.data.people.EPersonType;
import com.frekanstan.kbs_mobil.R;
import com.frekanstan.kbs_mobil.app.locations.LocationRepository;
import com.frekanstan.kbs_mobil.app.multitenancy.TenantRepository;
import com.frekanstan.kbs_mobil.app.people.PersonDAO;
import com.frekanstan.kbs_mobil.app.tracking.CountingOpDAO;
import com.frekanstan.kbs_mobil.data.CountingOp;
import com.frekanstan.kbs_mobil.data.Person_;
import com.frekanstan.kbs_mobil.databinding.CountingopDialogBinding;
import com.frekanstan.kbs_mobil.view.MainActivity;
import com.frekanstan.kbs_mobil.view.locations.LocationSelectorAdapter;
import com.frekanstan.kbs_mobil.view.people.PersonSelectorAdapter;

import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

import lombok.val;
import lombok.var;

import static com.frekanstan.asset_management.view.MainActivityBase.dateFormat;

public class CountingOpDialogFragment extends DialogFragment implements View.OnClickListener {
    private MainActivity mContext;
    private CountingOp op;
    private CountingopDialogBinding view;

    public CountingOpDialogFragment() { }

    public static CountingOpDialogFragment newInstance() {
        return new CountingOpDialogFragment();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity){
            mContext = (MainActivity) context;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        op = new CountingOp();
        if (getArguments() != null) {
            val countingOpId = getArguments().getLong("countingOpId");
            if (countingOpId != 0)
                op = CountingOpDAO.getDao().get(countingOpId);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = CountingopDialogBinding.inflate(inflater, container, false);
        Objects.requireNonNull(getDialog()).setTitle(getString(R.string.add_counting_op_title));

        view.opTypeSelect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                if (position == 0) { //general
                    view.typeLabel.setVisibility(View.GONE);
                    view.personLabel.setVisibility(View.GONE);
                    view.locationLabel.setVisibility(View.GONE);
                    view.storageLabel.setVisibility(View.GONE);
                    view.countingopTypeSelector.setVisibility(View.GONE);
                    view.countingopPersonSelector.setVisibility(View.GONE);
                    view.countingopLocationSelector.setVisibility(View.GONE);
                    view.countingopStorageSelector.setVisibility(View.GONE);
                }
                else if (position == 1) { //location
                    view.typeLabel.setVisibility(View.GONE);
                    view.personLabel.setVisibility(View.GONE);
                    view.locationLabel.setVisibility(View.VISIBLE);
                    view.storageLabel.setVisibility(View.GONE);
                    view.countingopTypeSelector.setVisibility(View.GONE);
                    view.countingopPersonSelector.setVisibility(View.GONE);
                    view.countingopLocationSelector.setVisibility(View.VISIBLE);
                    view.countingopStorageSelector.setVisibility(View.GONE);
                }
                else if (position == 2) { //storage
                    view.typeLabel.setVisibility(View.GONE);
                    view.personLabel.setVisibility(View.GONE);
                    view.locationLabel.setVisibility(View.GONE);
                    view.storageLabel.setVisibility(View.VISIBLE);
                    view.countingopTypeSelector.setVisibility(View.GONE);
                    view.countingopPersonSelector.setVisibility(View.GONE);
                    view.countingopLocationSelector.setVisibility(View.GONE);
                    view.countingopStorageSelector.setVisibility(View.VISIBLE);
                }
                else if (position == 3) { //person
                    view.typeLabel.setVisibility(View.GONE);
                    view.personLabel.setVisibility(View.VISIBLE);
                    view.locationLabel.setVisibility(View.GONE);
                    view.storageLabel.setVisibility(View.GONE);
                    view.countingopTypeSelector.setVisibility(View.GONE);
                    view.countingopPersonSelector.setVisibility(View.VISIBLE);
                    view.countingopLocationSelector.setVisibility(View.GONE);
                    view.countingopStorageSelector.setVisibility(View.GONE);
                }
                else if (position == 4) { //type
                    view.typeLabel.setVisibility(View.VISIBLE);
                    view.personLabel.setVisibility(View.GONE);
                    view.locationLabel.setVisibility(View.GONE);
                    view.storageLabel.setVisibility(View.GONE);
                    view.countingopTypeSelector.setVisibility(View.VISIBLE);
                    view.countingopPersonSelector.setVisibility(View.GONE);
                    view.countingopLocationSelector.setVisibility(View.GONE);
                    view.countingopStorageSelector.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        //person selector
        view.countingopPersonSelector.setSelectAllOnFocus(true);
        if (op.getRelatedPersonId() != 0) {
            if (op.getRelatedPerson().getIdentityNo() == null)
                view.countingopPersonSelector.setText(op.getRelatedPerson().getNameSurname());
            else
                view.countingopPersonSelector.setText(String.format("%s #%s", op.getRelatedPerson().getNameSurname(), op.getRelatedPerson().getIdentityNo()));
        }
        var personAdapter = new PersonSelectorAdapter(mContext, R.id.countingop_person_selector, PersonDAO.getDao().getAll());
        view.countingopPersonSelector.setAdapter(personAdapter);
        view.countingopPersonSelector.setOnItemClickListener((parent, view1, position, id) ->
                op.relatedPerson.setTargetId(Objects.requireNonNull(personAdapter.getItem(position)).getId()));

        //person tasked selector
        view.countingopPersonTaskedSelector.setSelectAllOnFocus(true);
        if (op.getPersonTaskedId() != 0) {
            if (op.getPersonTasked().getIdentityNo() == null)
                view.countingopPersonTaskedSelector.setText(op.getPersonTasked().getNameSurname());
            else
                view.countingopPersonTaskedSelector.setText(String.format("%s #%s", op.getPersonTasked().getNameSurname(), op.getPersonTasked().getIdentityNo()));
        }
        var personTaskedAdapter = new PersonSelectorAdapter(mContext, R.id.countingop_person_tasked_selector, PersonDAO.getDao().getBox().query().equal(Person_.personType, EPersonType.Recorder.id).build().find());
        view.countingopPersonTaskedSelector.setAdapter(personTaskedAdapter);
        view.countingopPersonTaskedSelector.setOnItemClickListener((parent, view1, position, id) ->
                op.personTasked.setTargetId(Objects.requireNonNull(personTaskedAdapter.getItem(position)).getId()));

        //location selector
        var locationAdapter = new LocationSelectorAdapter(mContext, R.id.countingop_location_selector, LocationRepository.getAllUnits());
        view.countingopLocationSelector.setAdapter(locationAdapter);
        view.countingopLocationSelector.setSelectAllOnFocus(true);
        if (op.getRelatedLocationId() != 0)
            if (op.getRelatedLocation().getLocationType() != ELocationType.Warehouse)
                view.countingopLocationSelector.setText(op.getRelatedLocation().getName());
        view.countingopLocationSelector.setOnItemClickListener((parent, view1, position, id) ->
                op.relatedLocation.setTargetId(Objects.requireNonNull(locationAdapter.getItem(position)).getId()));

        //storage selector
        var storageAdapter = new LocationSelectorAdapter(mContext, R.id.countingop_storage_selector, LocationRepository.getAllStorages());
        view.countingopStorageSelector.setAdapter(storageAdapter);
        view.countingopStorageSelector.setSelectAllOnFocus(true);
        if (op.getRelatedLocationId() != 0)
            if (op.getRelatedLocation().getLocationType() == ELocationType.Warehouse)
                view.countingopStorageSelector.setText(op.getRelatedLocation().getName());
        view.countingopStorageSelector.setOnItemClickListener((parent, view1, position, id) ->
                op.relatedLocation.setTargetId(Objects.requireNonNull(storageAdapter.getItem(position)).getId()));

        view.countingopDeadlineSelector.setOnClickListener(v -> {
            final Calendar cldr = Calendar.getInstance();
            int day = cldr.get(Calendar.DAY_OF_MONTH);
            int month = cldr.get(Calendar.MONTH);
            int year = cldr.get(Calendar.YEAR);
            new DatePickerDialog(mContext,
                    (vi, year1, monthOfYear, dayOfMonth) -> {
                        Calendar calendar = Calendar.getInstance();
                        calendar.set(year1, monthOfYear, dayOfMonth);
                        op.setDeadline(calendar.getTime());
                        view.countingopDeadlineSelector.setText(dateFormat.format(calendar.getTime()));
                        }, year, month, day
                    )
                    .show();
        });

        view.confirmButton.setOnClickListener(this);
        view.cancelButton.setOnClickListener(this);

        setCancelable(false);
        return view.getRoot();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.confirm_button) {
            op.setCreationTime(new Date());
            op.tenant.setTargetId(TenantRepository.getCurrentTenant().getId());
            op.setIsUpdated(true);
            CountingOpDAO.getDao().put(op);
        }
        else if (v.getId() == R.id.cancel_button)
            dismiss();
        dismiss();
    }
}