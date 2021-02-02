package com.frekanstan.gateway_mobil.view.assetlist;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.frekanstan.asset_management.app.helpers.PictureTaker;
import com.frekanstan.asset_management.data.ObjectBox;
import com.frekanstan.asset_management.view.shared.ISearchableListFragment;
import com.frekanstan.gateway_mobil.data.ImageToUpload;
import com.frekanstan.gateway_mobil.databinding.AssetListFragmentBinding;
import com.frekanstan.gateway_mobil.view.MainActivity;

import java.io.File;

import lombok.Getter;
import lombok.val;
import lombok.var;

import static android.app.Activity.RESULT_OK;
import static com.frekanstan.asset_management.view.MainActivityBase.assetPhotosFolder;

public class AssetListFragment extends Fragment implements ISearchableListFragment {
    private MainActivity context;
    private AssetListFragmentBinding view;
    @Getter
    private AssetListAdapter adapter;
    private AssetListViewModel model;
    private AssetListFilterManager filterManager;
    private boolean init = true;
    private String query, sortBy;
    private long assetTypeId, personId, locationId, storageId, countingOpId;
    private String listType = "nofilter";
    private long[] ids;

    public AssetListFragment() {
    }

    public static AssetListFragment newInstance(Bundle input) {
        val fragment = new AssetListFragment();
        fragment.setArguments(input);
        return fragment;
    }

    //----------------------------------------------------------------------------------------------
    // Initialization
    //----------------------------------------------------------------------------------------------

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity)
            this.context = (MainActivity) context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) { //pre-filter
            assetTypeId = getArguments().getLong("assetTypeId");
            personId = getArguments().getLong("personId");
            locationId = getArguments().getLong("locationId");
            storageId = getArguments().getLong("storageId");
            listType = getArguments().getString("listType");
            countingOpId = getArguments().getLong("countingOpId");
            ids = getArguments().getLongArray("ids");
        }
        filterManager = new AssetListFilterManager(context, this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //init view
        view = AssetListFragmentBinding.inflate(inflater, container, false);
        view.assetList.setLayoutManager(new LinearLayoutManager(context)); //set recyclerview as linear rows list
        adapter = new AssetListAdapter(context, this);
        view.assetList.setAdapter(adapter);
        view.assetList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                context.hideKeyboard();
            }
        });
        filterManager.addFilterChips(view.chipLayout); //add chips if already filtered

        //view model to populate asset list
        model = new ViewModelProvider(context).get(listType, AssetListViewModel.class);
        model.getLiveData((result, error) -> context.runOnUiThread(() -> context.hideProgBar()))
                .observe(getViewLifecycleOwner(), assets -> { //every time live data changes
                    adapter.submitList(assets);
                    context.runOnUiThread(() -> context.hideProgBar());
                });
        if (init) {
            refreshList();
            init = false;
        }
        return view.getRoot();
    }

    //----------------------------------------------------------------------------------------------
    // Public Functions
    //----------------------------------------------------------------------------------------------

    @Override
    public String getQuery() {
        return query;
    }

    @Override
    public void setQuery(String query) {
        this.query = query;
        refreshList();
    }

    @Override
    public void refreshList() {
        context.showProgBar();
        var bundle = new Bundle();
        bundle.putString("query", getQuery());
        bundle.putBooleanArray("filterTagNoSelected", filterManager.mFilterTagNoSelected);
        bundle.putBooleanArray("filterLabelSelected", filterManager.mFilterLabelSelected);
        bundle.putBooleanArray("filterAssignmentSelected", filterManager.mFilterAssignmentSelected);
        bundle.putBooleanArray("filterDeploymentSelected", filterManager.mFilterDeploymentSelected);
        bundle.putBooleanArray("filterCountingSelected", filterManager.mFilterCountingSelected);
        bundle.putBooleanArray("filterBudgetSelected", filterManager.mFilterBudgetSelected);
        bundle.putBooleanArray("filterWSSelected", filterManager.mFilterWSSelected);
        bundle.putBooleanArray("filterStoragesSelected", filterManager.mFilterStoragesSelected);
        bundle.putSerializable("filterPrice", filterManager.mFilterPrice);
        bundle.putInt("filterPriceType", filterManager.mFilterPriceType);
        bundle.putString("listType", listType);
        bundle.putLong("assetTypeId", assetTypeId);
        bundle.putLong("personId", personId);
        bundle.putLong("locationId", locationId);
        bundle.putLong("countingOpId", countingOpId);
        bundle.putLong("storageId", storageId);
        bundle.putString("sortBy", sortBy);
        bundle.putLongArray("ids", ids);
        model.setInput(bundle);
    }

    //----------------------------------------------------------------------------------------------
    // Triggered Events
    //----------------------------------------------------------------------------------------------

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == 1) {
                val mAssetImagePath = assetPhotosFolder + File.separator +
                        model.getLastClickedAsset().getAssetCode() + "-0.jpg";
                PictureTaker.saveBitmapToFile(new File(mAssetImagePath));
                ObjectBox.get().boxFor(ImageToUpload.class)
                        .put(new ImageToUpload(mAssetImagePath, "asset", false, false));
                adapter.notifyDataSetChanged();
            }
        }
    }
}