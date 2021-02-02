package com.frekanstan.kbs_mobil.view.locations;

import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.frekanstan.asset_management.data.locations.ELocationType;
import com.frekanstan.asset_management.view.shared.ICanScanCode;
import com.frekanstan.asset_management.view.shared.ISearchableListFragment;
import com.frekanstan.kbs_mobil.R;
import com.frekanstan.kbs_mobil.app.assets.AssetDAO;
import com.frekanstan.kbs_mobil.app.locations.LocationDAO;
import com.frekanstan.kbs_mobil.view.MainActivity;
import com.google.common.base.Strings;

import lombok.val;
import lombok.var;

public class LocationListFragment extends Fragment implements ISearchableListFragment, ICanScanCode {
    private MainActivity context;
    private LocationListAdapter adapter;
    private LocationListViewModel model;

    //filters
    private String query, sortBy, listType;
    private ELocationType locationTypeFilter;
    private byte hasAssetsFilter;

    public LocationListFragment() {
    }

    public static LocationListFragment newInstance(Bundle input) {
        val fragment = new LocationListFragment();
        fragment.setArguments(input);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity)
            this.context = (MainActivity) context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null)
            listType = getArguments().getString("listType");
        //init filters
        sortBy = "name";
        locationTypeFilter = ELocationType.NotSet;
        hasAssetsFilter = 1;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        val view = inflater.inflate(R.layout.location_list_fragment, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.locationList);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        adapter = new LocationListAdapter(context, getArguments());
        recyclerView.setAdapter(adapter);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                context.hideKeyboard();
            }
        });

        //view model
        model = new ViewModelProvider(context).get(listType, LocationListViewModel.class);
        model.getLiveData((result, error) -> context.runOnUiThread(() -> context.hideProgBar()))
                .observe(getViewLifecycleOwner(), locations -> {
                    adapter.submitList(locations);
                    context.setFooterText(String.format(context.getLocale(), context.getString(R.string.number_location), locations.size()));
                });
        refreshList();
        return view;
    }

    @Override
    public void refreshList() {
        var bundle = new Bundle();
        bundle.putString("query", getQuery());
        if (locationTypeFilter != null)
            bundle.putInt("locationType", locationTypeFilter.id);
        bundle.putByte("hasAssets", hasAssetsFilter);
        bundle.putString("sortBy", sortBy);
        bundle.putString("listType", listType);
        model.setInput(bundle);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_search_only, menu);

        //arama
        val searchManager = (SearchManager) context.getSystemService(Context.SEARCH_SERVICE);
        val searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setInputType(InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        if (searchManager != null)
            searchView.setSearchableInfo(searchManager.getSearchableInfo(context.getComponentName()));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                setQuery(newText);
                return false;
            }
        });
        searchView.setIconifiedByDefault(true);
        if (!Strings.isNullOrEmpty(getQuery())) {
            searchView.setIconified(false);
            searchView.setQuery(getQuery(), false);
            searchView.clearFocus();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        context.actionButton.show();
        if (Strings.isNullOrEmpty(listType))
            context.showHideFooter(true);
    }

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
    public void onCodeScanned(String code) {
        if (code.contains(getString(R.string.qrcode_contains))) //if code is asset
        {
            val splittedData = code.split("=");
            int assetId = Integer.parseInt(splittedData[splittedData.length - 1]); //extract remote id
            val asset = AssetDAO.getDao().get(assetId);
            if (asset == null) { //if asset is not found
                Toast.makeText(getContext(), R.string.foreign_asset_qrcode_read, Toast.LENGTH_SHORT).show();
                return;
            }
            var bundle = new Bundle();
            bundle.putLong("assetId", asset.getId());
            if (asset.getAssignedLocationId() == 0) {
                new AlertDialog.Builder(context).setMessage(R.string.alert_asset_is_not_assigned_to_location)
                        .setPositiveButton(getString(R.string.yes), (dialog, id) -> {
                            var bundle2 = new Bundle();
                            bundle2.putLongArray("assetIds", new long[]{assetId});
                            bundle2.putLong("locationId", asset.getAssignedLocationId());
                            context.nav.navigate(R.id.assignmentDialogFragment, bundle2);
                        })
                        .setNegativeButton(getString(R.string.no), (dialog, id) -> dialog.dismiss())
                        .show();
            } else
                goToLocation(asset.getAssignedLocationId(), assetId);
        }
        else if (code.contains(getString(R.string.location_qr_prefix))) { //if code is location
            long locationId = Long.parseLong(code.substring(getString(R.string.location_qr_prefix).length()));
            if (LocationDAO.getDao().get(locationId) == null)
                Toast.makeText(context, R.string.foreign_location_qrcode_read, Toast.LENGTH_SHORT).show();
            else
                goToLocation(locationId, 0);
        }
        else
            setQuery(code);
    }

    private void goToLocation(long id, long assetId) {
        var bundle = requireArguments();
        bundle.putLong("locationId", id);
        if (assetId != 0)
            bundle.putLong("assetId", assetId);
        val op = requireArguments().getString("operation");
        assert op != null;
        switch (op) {
            case "counting":
                context.nav.navigate(R.id.action_locationListFragment_to_countingTabsFragment, bundle);
                break;
            case "labeling":
                context.nav.navigate(R.id.action_locationListFragment_to_labelingTabsFragment, bundle);
                break;
        }
    }
}