package com.frekanstan.gateway_mobil.view.gateway;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.frekanstan.asset_management.app.webservice.AbpResult;
import com.frekanstan.gateway_mobil.R;
import com.frekanstan.gateway_mobil.app.assets.AssetDAO;
import com.frekanstan.gateway_mobil.app.connection.GetAllDemandedVariantsInput;
import com.frekanstan.gateway_mobil.app.connection.ReceivePackagesInput;
import com.frekanstan.gateway_mobil.app.connection.ServiceConnector;
import com.frekanstan.gateway_mobil.app.connection.StorageTransferInput;
import com.frekanstan.gateway_mobil.data.AssetType;
import com.frekanstan.gateway_mobil.data.Asset_;
import com.frekanstan.gateway_mobil.data.Inventory;
import com.frekanstan.gateway_mobil.data.Storage;
import com.frekanstan.gateway_mobil.view.MainActivity;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lombok.val;
import lombok.var;

public class ReceiveReportFragment extends Fragment {
    private MainActivity context;
    private ReceiveReportAdapter adapter;
    private ServiceConnector conn;
    private ReceiveReportViewModel model;
    private RecyclerView recyclerView;
    private Integer selectedWarehouseId;
    private String[] rfids;
    private List<AssetType> types;
    private List<Inventory> inventoryList;

    public ReceiveReportFragment() { }

    public static ReceiveReportFragment newInstance(Bundle input) {
        val fragment = new ReceiveReportFragment();
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
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        conn = ServiceConnector.getInstance(context);
        val view = inflater.inflate(R.layout.receive_report_fragment, container, false);
        recyclerView = view.findViewById(R.id.receiveReport);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));

        rfids = AssetDAO.getDao().getBox().query().in(Asset_.id, requireArguments().getLongArray("assetIds")).build().property(Asset_.rfidCode).findStrings();

        context.progDialog.setMessage("Fiş içeriği yükleniyor");
        context.progDialog.show();
        val receiptId = requireArguments().getLong("receiptId");
        if (requireArguments().getString("operation").equals("storage_transfer"))
            conn.addToRequestQueue(conn.getAllDemandedVariantsReq(new GetAllDemandedVariantsInput(receiptId), this::onVariantsResponse));
        else if (requireArguments().getString("operation").equals("receive_assets"))
            conn.addToRequestQueue(conn.getAllOrderedVariantsReq(new GetAllDemandedVariantsInput(receiptId), this::onVariantsResponse));
        return view;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.receive_report_menu, menu);
    }

    private void onVariantsResponse(AbpResult<ArrayList> response) {
        context.progDialog.hide();
        ArrayList<Inventory> invList = new ArrayList<>();
        val gson = new Gson();
        for (val obj : response.getResult())
            invList.add(gson.fromJson(gson.toJsonTree(obj).getAsJsonObject(), Inventory.class));
        inventoryList = invList;

        adapter = new ReceiveReportAdapter(context, requireArguments(), invList);
        recyclerView.setAdapter(adapter);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                context.hideKeyboard();
            }
        });
        model = new ViewModelProvider(context).get(ReceiveReportViewModel.class);
        model.getLiveData((result, error) -> context.runOnUiThread(() -> context.hideProgBar()))
                .observe(getViewLifecycleOwner(), types -> {
                    this.types = types;
                    adapter.submitList(types);
                    context.setFooterText(String.format(context.getLocale(), context.getString(R.string.number_asset_types), types.size()));
                });

        var remoteIds = new ArrayList<String>();
        for (val inv : invList)
            remoteIds.add(inv.getTypeRemoteId());

        var bundle = new Bundle();
        bundle.putLongArray("assetIds", requireArguments().getLongArray("assetIds"));
        bundle.putStringArray("remoteIds", remoteIds.toArray(new String[0]));
        model.setInput(bundle);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.send_received_assets) {
            var isGoodToSend = true;
            for (val type : this.types) {
                val query = AssetDAO.getDao().getBox().query().equal(Asset_.assetTypeId, type.getId()).and().in(Asset_.id, requireArguments().getLongArray("assetIds")).build();
                val qSum = query.property(Asset_.quantity).sum();
                var invQ = 0;
                if (inventoryList != null) {
                    for (val inv : inventoryList) {
                        if (inv.getTypeRemoteId().equals(type.getRemoteId())) {
                            invQ = inv.getQuantity();
                            break;
                        }
                    }
                }
                if (invQ == 0 || qSum > invQ)
                    isGoodToSend = false;
            }
            if (!isGoodToSend) {
                new AlertDialog.Builder(context)
                        .setMessage("Listede yabancı ya da olması gerekenden yüksek sayıda ürün var. İlgili ürünleri tespit edip alandan uzaklaştırarak işlemi tekrarlayın.")
                        .setPositiveButton("İşlemi Tekrarla", (dialog, which) -> {
                            val bundle = requireArguments();
                            bundle.putBoolean("reset", true);
                            context.nav.navigate(R.id.action_receiveReportFragment_to_receiveAssetsFragment, bundle);
                        })
                        .setNegativeButton(R.string.cancel_title, (dialog, which) -> dialog.dismiss())
                        .create().show();
                return true;
            }
            if (requireArguments().getString("operation").equals("receive_assets")) {
                context.progDialog.setMessage("İşlem gönderiliyor");
                context.progDialog.show();
                conn.addToRequestQueue(conn.receivePackagesReq(new ReceivePackagesInput(Arrays.asList(rfids), requireArguments().getInt("warehouseId"), requireArguments().getLong("currentAccountId")), this::onReceiveResponse));
            }
            else if (requireArguments().getString("operation").equals("storage_transfer")) {
                context.progDialog.setMessage("Depo listesi yükleniyor");
                context.progDialog.show();
                conn.addToRequestQueue(conn.getAllWarehousesReq(this::onWarehousesResponse));
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onWarehousesResponse(AbpResult<ArrayList> response) {
        context.progDialog.hide();
        ArrayList<Storage> storageList = new ArrayList<>();
        val gson = new Gson();
        for (val obj : response.getResult())
            storageList.add(gson.fromJson(gson.toJsonTree(obj).getAsJsonObject(), Storage.class));

        val ll = new LinearLayout(context);

        //storage in
        final Spinner warehouseSpinner = new Spinner(context);
        ArrayAdapter<Storage> warehouseAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, storageList);
        warehouseAdapter.insert(new Storage(0, context.getString(R.string.select_storage_in)), 0);
        warehouseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        warehouseSpinner.setAdapter(warehouseAdapter);
        ll.addView(warehouseSpinner);
        new AlertDialog.Builder(context)
                .setMessage(R.string.storage_selection_must_be_made)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    if (warehouseSpinner.getSelectedItemPosition() == 0)
                    {
                        Toast.makeText(context, R.string.you_must_select_a_warehouse, Toast.LENGTH_LONG).show();
                        return;
                    }
                    selectedWarehouseId = ((Storage)(warehouseSpinner.getSelectedItem())).getRemoteId();
                    context.showProgBar();
                    conn.addToRequestQueue(conn.storageTransferReq(new StorageTransferInput(Arrays.asList(rfids), selectedWarehouseId, requireArguments().getInt("warehouseId")), this::onReceiveResponse));
                })
                .setNegativeButton(R.string.cancel_title, (dialog, which) -> dialog.dismiss())
                .setView(ll)
                .create().show();
    }

    private void onReceiveResponse(AbpResult<String> receiveResult) {
        context.progDialog.hide();
        Toast.makeText(context,receiveResult.getResult(), Toast.LENGTH_LONG).show();
        context.nav.navigate(R.id.mainMenuFragment);
    }

    @Override
    public void onResume() {
        super.onResume();
        context.actionButton.hide();
        context.showHideFooter(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        context.showHideFooter(false);
    }
}