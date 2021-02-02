package com.frekanstan.gateway_mobil.view.assignmentdialog;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.frekanstan.asset_management.app.connection.NetManager;
import com.frekanstan.gateway_mobil.R;
import com.frekanstan.gateway_mobil.app.assets.AssetDAO;
import com.frekanstan.gateway_mobil.app.locations.LocationDAO;
import com.frekanstan.gateway_mobil.app.locations.LocationRepository;
import com.frekanstan.gateway_mobil.app.people.PersonDAO;
import com.frekanstan.gateway_mobil.view.MainActivity;

import java.util.Objects;

import lombok.val;
import lombok.var;

public class AssignmentDialogFragment extends DialogFragment implements View.OnClickListener {
    private MainActivity mContext;
    private long personId , locationId;
    private long[] assetIds;

    public AssignmentDialogFragment() { }

    public static AssignmentDialogFragment newInstance() {
        return new AssignmentDialogFragment();
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
        if (getArguments() != null) {
            assetIds = getArguments().getLongArray("assetIds");
            personId = getArguments().getLong("personId");
            locationId = getArguments().getLong("locationId");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mContext.showHideFooter(false);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.assignment_dialog, container);

        Objects.requireNonNull(getDialog()).setTitle(getString(R.string.assignment_dialog_title));

        AutoCompleteTextView personSelector = view.findViewById(R.id.assignment_person_selector);
        personSelector.setSelectAllOnFocus(true);
        if (personId != 0) {
            val person = PersonDAO.getDao().get(personId);
            if (person != null) {
                if (person.getIdentityNo() == null)
                    personSelector.setText(person.getNameSurname());
                else
                    personSelector.setText(String.format("%s #%s", person.getNameSurname(), person.getIdentityNo()));
            }
        }
        var personAdapter = new PersonSelectorAdapter(mContext, R.id.assignment_person_selector, PersonDAO.getDao().getAll());
        personSelector.setAdapter(personAdapter);
        personSelector.setOnItemClickListener((parent, view1, position, id) ->
                personId = Objects.requireNonNull(personAdapter.getItem(position)).getId());

        AutoCompleteTextView locationSelector = view.findViewById(R.id.assignment_location_selector);
        var locationAdapter = new LocationSelectorAdapter(mContext, R.id.assignment_location_selector, LocationRepository.getAllUnits());
        locationSelector.setAdapter(locationAdapter);
        locationSelector.setSelectAllOnFocus(true);
        if (locationId != 0) {
            val location = LocationDAO.getDao().get(locationId);
            if (location != null)
                locationSelector.setText(location.getName());
        }
        locationSelector.setOnItemClickListener((parent, view1, position, id) ->
                locationId = Objects.requireNonNull(locationAdapter.getItem(position)).getId());

        Button confirm = view.findViewById(R.id.confirm_button);
        Button cancel = view.findViewById(R.id.cancel_button);
        confirm.setOnClickListener(this);
        cancel.setOnClickListener(this);

        setCancelable(false);
        return view;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.confirm_button) {
            for (val assetId : assetIds)
                AssetDAO.getDao().setAssignmentChange(assetId, personId, locationId);
        }
        else if (v.getId() == R.id.return_to_storage) {
            for (val assetId : assetIds)
                AssetDAO.getDao().setAssignmentChange(assetId, 0, 0);
        }
        else
            dismiss();
        if (NetManager.isOnline)
            Toast.makeText(getContext(), R.string.assignment_operation_underway, Toast.LENGTH_LONG).show();
        else
            Toast.makeText(getContext(), R.string.assignment_request_created, Toast.LENGTH_LONG).show();
        dismiss();
    }
}