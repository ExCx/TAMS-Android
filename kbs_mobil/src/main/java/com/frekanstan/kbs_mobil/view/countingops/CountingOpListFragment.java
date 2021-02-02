package com.frekanstan.kbs_mobil.view.countingops;

import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.frekanstan.asset_management.view.shared.ISearchableListFragment;
import com.frekanstan.kbs_mobil.R;
import com.frekanstan.kbs_mobil.view.MainActivity;
import com.google.common.base.Strings;

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
        return view;
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
        context.nav.navigate(R.id.countingOpDialogFragment);

        /*new AlertDialog.Builder(context).setMessage(R.string.alert_a_new_counting_operation_will_be_created)
                .setPositiveButton(getString(R.string.yes), (dialog, id) -> {
                    var op = new CountingOp();
                    op.setCreationTime(new Date());
                    op.tenant.setTargetId(TenantRepository.getCurrentTenant().getId());
                    op.setIsUpdated(true);
                    CountingOpDAO.getDao().put(op);
                })
                .setNegativeButton(getString(R.string.no), (dialog, id) -> dialog.dismiss())
                .show();*/
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