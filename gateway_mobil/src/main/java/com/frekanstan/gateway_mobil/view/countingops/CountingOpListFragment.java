package com.frekanstan.gateway_mobil.view.countingops;

import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.frekanstan.asset_management.app.webservice.AbpResult;
import com.frekanstan.asset_management.view.shared.ISearchableListFragment;
import com.frekanstan.gateway_mobil.R;
import com.frekanstan.gateway_mobil.app.connection.ServiceConnector;
import com.frekanstan.gateway_mobil.app.multitenancy.TenantRepository;
import com.frekanstan.gateway_mobil.app.tracking.CountingOpDAO;
import com.frekanstan.gateway_mobil.data.CountingOp;
import com.frekanstan.gateway_mobil.data.Storage;
import com.frekanstan.gateway_mobil.view.MainActivity;
import com.google.common.base.Strings;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Date;

import lombok.val;
import lombok.var;

public class CountingOpListFragment extends Fragment implements ISearchableListFragment {
    private MainActivity context;
    private CountingOpListAdapter adapter;
    private CountingOpListViewModel model;

    //filters
    private String query;

    public CountingOpListFragment() {
    }

    public static CountingOpListFragment newInstance(Bundle input) {
        val fragment = new CountingOpListFragment();
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
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        context.actionButton.setEnabled(false);
        setHasOptionsMenu(true);
        val view = inflater.inflate(R.layout.countingop_list_fragment, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.countingOpList);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        adapter = new CountingOpListAdapter(context, getArguments());
        recyclerView.setAdapter(adapter);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                context.hideKeyboard();
            }
        });

        //view model
        model = new ViewModelProvider(context).get(CountingOpListViewModel.class);
        model.getLiveData((result, error) -> context.runOnUiThread(() -> context.hideProgBar()))
                .observe(getViewLifecycleOwner(), countingOps -> {
                    adapter.submitList(countingOps);
                    context.setFooterText(String.format(context.getLocale(), context.getString(R.string.number_countingops), countingOps.size()));
                });
        refreshList();
        context.progDialog.setMessage("Depo listesi yükleniyor");
        context.progDialog.show();
        val conn = ServiceConnector.getInstance(context);
        conn.addToRequestQueue(conn.getAllWarehousesReq(this::onWarehousesResponse));
        return view;
    }

    ArrayList<Storage> storageList;
    private void onWarehousesResponse(AbpResult<ArrayList> response) {
        context.progDialog.hide();
        storageList = new ArrayList<>();
        val gson = new Gson();
        for (val obj : response.getResult())
            storageList.add(gson.fromJson(gson.toJsonTree(obj).getAsJsonObject(), Storage.class));
        context.actionButton.setEnabled(true);
    }

    @Override
    public void refreshList() {
        var bundle = new Bundle();
        bundle.putString("query", getQuery());
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
        context.actionButton.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_add_white));
        context.actionButton.setOnClickListener(v -> showAddCountingOpPopup());
        context.showHideFooter(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        //context.actionButton.hide();
    }

    private void showAddCountingOpPopup() {
        val ll = new LinearLayout(context);
        final Spinner warehouseSpinner = new Spinner(context);
        ArrayAdapter<Storage> warehouseAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, storageList);
        warehouseAdapter.insert(new Storage(0, context.getString(R.string.select_a_storage)), 0);
        warehouseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        warehouseSpinner.setAdapter(warehouseAdapter);
        ll.setPadding(16, 16, 16, 16);
        ll.addView(warehouseSpinner);

        new AlertDialog.Builder(context).setMessage(R.string.alert_a_new_counting_operation_will_be_created)
                .setPositiveButton(getString(R.string.yes), (dialog, id) -> {
                    if (warehouseSpinner.getSelectedItemPosition() == 0)
                    {
                        Toast.makeText(context, R.string.storage_selection_must_be_made, Toast.LENGTH_LONG).show();
                        return;
                    }
                    var op = new CountingOp();
                    op.setCreationTime(new Date());
                    op.tenant.setTargetId(TenantRepository.getCurrentTenant().getId());
                    op.setIsUpdated(true);
                    op.setTitle(((Storage)(warehouseSpinner.getSelectedItem())).getName());
                    CountingOpDAO.getDao().put(op);
                })
                .setNegativeButton(getString(R.string.no), (dialog, id) -> dialog.dismiss())
                .setView(ll)
                .show();
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
}