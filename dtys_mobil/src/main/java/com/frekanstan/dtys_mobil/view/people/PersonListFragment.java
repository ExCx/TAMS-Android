package com.frekanstan.dtys_mobil.view.people;

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.frekanstan.asset_management.app.helpers.PictureTaker;
import com.frekanstan.asset_management.data.ObjectBox;
import com.frekanstan.asset_management.data.people.EPersonType;
import com.frekanstan.asset_management.view.shared.ICanScanCode;
import com.frekanstan.asset_management.view.shared.ICanShootPhoto;
import com.frekanstan.asset_management.view.shared.ISearchableListFragment;
import com.frekanstan.dtys_mobil.R;
import com.frekanstan.dtys_mobil.app.assets.AssetDAO;
import com.frekanstan.dtys_mobil.app.people.PersonDAO;
import com.frekanstan.dtys_mobil.data.ImageToUpload;
import com.frekanstan.dtys_mobil.view.MainActivity;
import com.google.common.base.Strings;

import java.io.File;

import lombok.val;
import lombok.var;

import static android.app.Activity.RESULT_OK;
import static com.frekanstan.dtys_mobil.view.MainActivity.personPhotosFolder;

public class PersonListFragment extends Fragment implements ISearchableListFragment, ICanShootPhoto, ICanScanCode {
    private MainActivity context;
    private PersonListAdapter adapter;
    private PersonListViewModel model;

    //filters
    private String query, sortBy, listType;
    private EPersonType personTypeFilter;
    private byte hasAssetsFilter;

    public PersonListFragment() {
    }

    public static PersonListFragment newInstance(Bundle input) {
        val fragment = new PersonListFragment();
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
        sortBy = "nameSurname";
        personTypeFilter = EPersonType.NotSet;
        hasAssetsFilter = 1;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        val view = inflater.inflate(R.layout.person_list_fragment, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.personList);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        adapter = new PersonListAdapter(context, this, getArguments());
        recyclerView.setAdapter(adapter);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                context.hideKeyboard();
            }
        });

        //view model
        model = new ViewModelProvider(context).get(listType, PersonListViewModel.class);
        model.getLiveData((result, error) -> context.runOnUiThread(() -> context.hideProgBar()))
                .observe(getViewLifecycleOwner(), persons -> {
                    adapter.submitList(persons);
                    context.setFooterText(String.format(context.getLocale(), context.getString(R.string.number_person), persons.size()));
        });
        refreshList();
        return view;
    }

    @Override
    public void refreshList() {
        var bundle = new Bundle();
        bundle.putString("query", getQuery());
        if (personTypeFilter != null)
            bundle.putInt("personType", personTypeFilter.id);
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

    public void shootPhoto() {
        PictureTaker.dispatchTakePictureIntent(new File(personPhotosFolder + File.separator + adapter.lastClickedItem.getId() + ".jpg"), this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1 && resultCode == RESULT_OK) {
            val imagePath = personPhotosFolder + File.separator +
                    adapter.lastClickedItem.getId() + ".jpg";
            PictureTaker.saveBitmapToFile(new File(imagePath));
            ObjectBox.get().boxFor(ImageToUpload.class)
                    .put(new ImageToUpload(imagePath, "person", false, false));
            adapter.notifyDataSetChanged();
        }
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
            if (asset.getAssignedPersonId() == 0) {
                new AlertDialog.Builder(context).setMessage(R.string.alert_asset_is_not_assigned_to_person)
                        .setPositiveButton(getString(R.string.yes), (dialog, id) -> {
                            var bundle2 = new Bundle();
                            bundle2.putLongArray("assetIds", new long[]{asset.getId()});
                            bundle2.putLong("personId", asset.getAssignedPersonId());
                            context.nav.navigate(R.id.assignmentDialogFragment, bundle2);
                        })
                        .setNegativeButton(getString(R.string.no), (dialog, id) -> dialog.dismiss())
                        .show();
            } else
                goToPerson(asset.getAssignedPersonId(), asset.getId());
        }
        else if (code.contains(getString(R.string.person_qr_prefix))) { //if code is person
            long personId = Long.parseLong(code.substring(getString(R.string.person_qr_prefix).length()));
            if (PersonDAO.getDao().get(personId) == null)
                Toast.makeText(context, R.string.foreign_person_qrcode_read, Toast.LENGTH_SHORT).show();
            else
                goToPerson(personId, 0);
        }
        else
            setQuery(code);
    }

    private void goToPerson(long id, long assetId) {
        var bundle = new Bundle();
        bundle.putLong("personId", id);
        if (assetId != 0)
            bundle.putLong("assetId", assetId);
        val op = getArguments().getString("operation");
        assert op != null;
        switch (op) {
            case "counting":
                context.nav.navigate(R.id.countingTabsFragment, bundle);
                break;
            case "labeling":
                context.nav.navigate(R.id.action_personListFragment_to_labelingTabsFragment, bundle);
                break;
        }
    }
}