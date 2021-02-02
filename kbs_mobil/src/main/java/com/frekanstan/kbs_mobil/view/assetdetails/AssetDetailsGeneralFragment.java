package com.frekanstan.kbs_mobil.view.assetdetails;

import android.content.Context;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.frekanstan.asset_management.data.assets.IAsset;
import com.frekanstan.kbs_mobil.R;
import com.frekanstan.kbs_mobil.app.assets.AssetDAO;
import com.frekanstan.kbs_mobil.data.Asset;
import com.frekanstan.kbs_mobil.databinding.AssetDetailsGeneralFragmentBinding;
import com.frekanstan.kbs_mobil.view.MainActivity;

import lombok.val;

public class AssetDetailsGeneralFragment extends Fragment implements View.OnClickListener {
    private static IAsset asset;
    private AssetDetailsGeneralFragmentBinding view;
    private MainActivity context;

    public AssetDetailsGeneralFragment() { }

    public static AssetDetailsGeneralFragment newInstance(IAsset _asset) {
        asset = _asset;
        return new AssetDetailsGeneralFragment();
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
        view = AssetDetailsGeneralFragmentBinding.inflate(inflater, container, false);
        populateInfo();
        view.featuresB.setOnClickListener(this);
        return view.getRoot();
    }

    void populateInfo() {
        if (asset.getAssetTypeId() != 0)
            view.assetDetailsTypeDef.setText(asset.getAssetType().getDefinition());
        view.assetDetailsFeatures.setText(asset.getFeatures());
        view.assetDetailsTagNo.setText(asset.getRemoteId());
        view.assetDetailsRegistrationNo.setText(asset.getRegistrationCode());
        view.assetDetailsBudgetType.setText(asset.getBudgetType() != null ? asset.getBudgetType().getDefinition() : "");
        view.assetDetailsStorageBelongs.setText(asset.getStorageBelongsTo() != null ? asset.getStorageBelongsTo().getName() : "");
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.featuresB) {
            View viewInflated = LayoutInflater.from(context).inflate(R.layout.single_input_dialog, (ViewGroup)getView(), false);
            final EditText input = viewInflated.findViewById(R.id.input_text);
            input.setFilters(new InputFilter[] {new InputFilter.AllCaps()});
            input.setText(asset.getFeatures());
            new AlertDialog.Builder(context)
                    .setView(viewInflated)
                    .setTitle(R.string.features)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        dialog.dismiss();
                        val features = input.getText().toString();
                        view.assetDetailsFeatures.setText(features);
                        asset.setFeatures(features);
                        asset.setIsUpdated(true);
                        AssetDAO.getDao().put((Asset) asset);
                    })
                    .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                    .show();
        }
    }
}