package com.frekanstan.kbs_mobil.view.assetlist;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.frekanstan.asset_management.app.helpers.PictureTaker;
import com.frekanstan.asset_management.data.ObjectBox;
import com.frekanstan.asset_management.view.shared.ISearchableListFragment;
import com.frekanstan.kbs_mobil.R;
import com.frekanstan.kbs_mobil.data.ImageToUpload;
import com.frekanstan.kbs_mobil.databinding.AssetListFragmentBinding;
import com.frekanstan.kbs_mobil.view.MainActivity;
import com.google.common.base.Strings;

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
    private SearchView searchView;
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
        setHasOptionsMenu(true);
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

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.assetlist_menu, menu);

        //arama
        val searchManager = (SearchManager) context.getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) menu.findItem(R.id.search).getActionView();
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
                if (newText.length() == 0)
                    setQuery(newText);
                else if (!searchView.getQuery().toString().equals(newText.toUpperCase()))
                    searchView.setQuery(newText.toUpperCase(), false);
                return false;
            }
        });
        //mSearchView.setSearchableInfo();
        searchView.setIconifiedByDefault(true);
        if (!Strings.isNullOrEmpty(getQuery())) {
            searchView.setIconified(false);
            searchView.setQuery(getQuery(), false);
            searchView.clearFocus();
        }
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
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.add_filter_is_labeled) {
            filterManager.showFilterLabelDialog();
            return true;
        } else if (itemId == R.id.add_filter_assignment) {
            filterManager.showFilterAssignmentDialog();
            return true;
        } else if (itemId == R.id.add_filter_deployment) {
            filterManager.showFilterDeploymentDialog();
            return true;
        } else if (itemId == R.id.add_filter_counting_status) {
            filterManager.showFilterCountingDialog();
            return true;
        } else if (itemId == R.id.add_filter_budget_type) {
            filterManager.showFilterBudgetTypeDialog();
            return true;
        } else if (itemId == R.id.add_filter_working_status) {
            filterManager.showFilterWSDialog();
            return true;
        } else if (itemId == R.id.add_filter_storage) {
            filterManager.showFilterStoragesDialog();
            return true;
        } else if (itemId == R.id.add_filter_price) {
            filterManager.showFilterPriceDialog();
            return true;
        } else if (itemId == R.id.remove_filters) {
            view.chipLayout.removeAllViews();
            var lp = (RelativeLayout.LayoutParams) view.chipLayout.getLayoutParams();
            lp.setMargins(0, 0, 0, 0);
            view.chipLayout.setLayoutParams(lp);
            filterManager.removeFilters();
            searchView.setQuery("", false);
            refreshList();
            return true;
        } else if (itemId == R.id.sort_by_definition) {
            sortBy = "definition";
            refreshList();
            return true;
        } else if (itemId == R.id.sort_by_assigned_person) {
            sortBy = "person";
            refreshList();
            return true;
        } else if (itemId == R.id.sort_by_assigned_location) {
            sortBy = "location";
            refreshList();
            return true;
        } else if (itemId == R.id.sort_by_last_control_date) {
            sortBy = "last_control";
            refreshList();
            return true;
        } else if (itemId == R.id.sort_by_labeling_date) {
            sortBy = "labeling";
            refreshList();
            return true;
        } /*else if (itemId == R.id.select_all) {
            //adapter.getCurrentList();
            List<Long> idList = (List<Long>) TransformerUtils.collect(objectList,
                    new BeanToPropertyValueTransformer("id"));
            adapter.getCurrentList()
            adapter.getSelectableList().selectAll();
            return true;
        }*/
        return super.onOptionsItemSelected(item);
    }

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