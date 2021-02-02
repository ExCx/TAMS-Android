package com.frekanstan.tatf_demo.view.assetdetails;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.frekanstan.asset_management.data.acquisition.EWorkingState;
import com.frekanstan.asset_management.data.assets.IAsset;
import com.frekanstan.tatf_demo.R;
import com.frekanstan.tatf_demo.app.assets.AssetDAO;
import com.frekanstan.tatf_demo.app.labeling.LabelPrinter;
import com.frekanstan.tatf_demo.app.locations.LocationDAO;
import com.frekanstan.tatf_demo.app.people.PersonDAO;
import com.frekanstan.tatf_demo.data.Asset;
import com.frekanstan.tatf_demo.databinding.AssetDetailsAssignmentFragmentBinding;
import com.frekanstan.tatf_demo.view.MainActivity;

import java.util.ArrayList;

import lombok.val;
import lombok.var;

import static com.frekanstan.tatf_demo.view.MainActivity.dateFormat;

public class AssetDetailsAssignmentFragment extends Fragment implements View.OnClickListener {
    private static IAsset asset;
    private AssetDetailsAssignmentFragmentBinding view;
    private MainActivity context;

    public AssetDetailsAssignmentFragment() { }

    public static AssetDetailsAssignmentFragment newInstance(IAsset _asset) {
        asset = _asset;
        return new AssetDetailsAssignmentFragment();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity){
            this.context = (MainActivity) context;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null)
            asset = (Asset)getArguments().getSerializable("mAsset");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = AssetDetailsAssignmentFragmentBinding.inflate(inflater, container, false);
        populateInfo();
        view.assignedPersonB.setOnClickListener(this);
        view.assignedLocationB.setOnClickListener(this);
        view.lastControlDateB.setOnClickListener(this);
        view.labelingDateB.setOnClickListener(this);
        view.workingStateB.setOnClickListener(this);
        return view.getRoot();
    }

    void populateInfo() {
        if (asset.getAssignedPersonId() != 0) {
            view.assetDetailsAssignedEmployeeName.setTextColor(Color.BLACK);
            view.assetDetailsAssignedEmployeeName.setText(asset.getAssignedPersonNameSurname());
        }
        else {
            view.assetDetailsAssignedEmployeeName.setTextColor(Color.RED);
            view.assetDetailsAssignedEmployeeName.setText(getString(R.string.not_assigned));
        }
        if (asset.getAssignedLocationId() != 0) {
            view.assetDetailsAssignedLocationName.setTextColor(Color.BLACK);
            view.assetDetailsAssignedLocationName.setText(asset.getAssignedLocationName());
        }
        else {
            view.assetDetailsAssignedLocationName.setTextColor(Color.RED);
            view.assetDetailsAssignedLocationName.setText(getString(R.string.not_deployed));
        }
        var ac = AssetDAO.getDao().getAssignmentChange(asset.getId());
        if (ac != null) {
            if (ac.getPersonId() != 0) {
                ((LinearLayout)view.assetDetailsEmployeeToAssignName.getParent()).setVisibility(View.VISIBLE);
                view.assetDetailsEmployeeToAssignName.setText(PersonDAO.getDao().get(ac.getPersonId()).getNameSurname());
            }
            if (ac.getLocationId() != 0) {
                ((LinearLayout) view.assetDetailsRoomToAssignName.getParent()).setVisibility(View.VISIBLE);
                view.assetDetailsRoomToAssignName.setText(LocationDAO.getDao().get(ac.getLocationId()).getName());
            }
        }
        if (asset.getLastControlTime() != null) {
            view.assetDetailsLastControlDate.setTextColor(Color.BLACK);
            view.assetDetailsLastControlDate.setText(dateFormat.format(asset.getLastControlTime()));
        }
        else {
            view.assetDetailsLastControlDate.setTextColor(Color.RED);
            view.assetDetailsLastControlDate.setText(getString(R.string.not_seen));
        }
        if (asset.getLabelingDateTime() != null) {
            view.assetDetailsLabelingDate.setTextColor(Color.BLACK);
            view.assetDetailsLabelingDate.setText(dateFormat.format(asset.getLabelingDateTime()));
        }
        else {
            view.assetDetailsLabelingDate.setTextColor(Color.RED);
            view.assetDetailsLabelingDate.setText(getString(R.string.not_labeled));
        }
        switch (asset.getWorkingState()) {
            case Unknown:
                view.assetDetailsWorkingState.setTextColor(Color.GRAY);
                view.assetDetailsWorkingState.setText(getString(R.string.unknown));
                break;
            case Active:
                view.assetDetailsWorkingState.setTextColor(Color.BLACK);
                view.assetDetailsWorkingState.setText(getString(R.string.active));
                break;
            case Passive:
                view.assetDetailsWorkingState.setTextColor(Color.MAGENTA);
                view.assetDetailsWorkingState.setText(getString(R.string.passive));
                break;
            case Broken:
                view.assetDetailsWorkingState.setTextColor(Color.RED);
                view.assetDetailsWorkingState.setText(getString(R.string.broken));
                break;
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.assignedPersonB || v.getId() == R.id.assignedLocationB) {
            var bundle = new Bundle();
            bundle.putLongArray("assetIds", new long[]{asset.getId()});
            bundle.putLong("personId", asset.getAssignedPersonId());
            bundle.putLong("locationId", asset.getAssignedLocationId());
            context.nav.navigate(R.id.action_assetDetailsFragment_to_assignmentDialogFragment, bundle);
        }
        else if (v.getId() == R.id.labelingDateB) {
            ArrayList<Long> idList = new ArrayList<>();
            idList.add(asset.getId());
            if (asset.getLabelingDateTime() != null) {
                new AlertDialog.Builder(context).setMessage(R.string.scanned_label_already_printed_single)
                        .setPositiveButton(getString(R.string.confirm), (dialog, id) -> {
                            new LabelPrinter(context, idList, "asset", null).print();
                            populateInfo();
                        })
                        .setNegativeButton(getString(R.string.cancel_title), (dialog, id) -> dialog.dismiss())
                        .show();
            } else {
                new LabelPrinter(context, idList, "asset", null).print();
                populateInfo();
            }
        }
        else if (v.getId() == R.id.workingStateB) {
            View viewInflated = LayoutInflater.from(context).inflate(R.layout.single_dropdown_dialog, (ViewGroup) getView(), false);
            final Spinner dropdown = viewInflated.findViewById(R.id.dropdown);
            val ddAdapter = new ArrayAdapter<String>(
                    context,
                    android.R.layout.simple_spinner_item,
                    getResources().getStringArray(R.array.working_status));
            //adapter.insert(new Brand(0, null, mContext.getString(R.string.institution_name), true), 0);
            ddAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            dropdown.setAdapter(ddAdapter);
            dropdown.setSelection(asset.getWorkingState().ordinal());
            new AlertDialog.Builder(context)
                    .setTitle(R.string.working_state).setView(viewInflated)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        val selectedState = EWorkingState.values()[dropdown.getSelectedItemPosition()];
                        if (!asset.getWorkingState().equals(selectedState)) {
                            asset.setWorkingState(selectedState);
                            asset.setIsUpdated(true);
                            AssetDAO.getDao().put((Asset)asset);
                            //dialog.dismiss();
                            populateInfo();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                    .show();
        }
    }
}