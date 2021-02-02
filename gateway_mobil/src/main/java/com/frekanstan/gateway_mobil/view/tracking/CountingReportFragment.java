package com.frekanstan.gateway_mobil.view.tracking;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.frekanstan.asset_management.app.webservice.AbpResult;
import com.frekanstan.gateway_mobil.R;
import com.frekanstan.gateway_mobil.app.connection.GetAllInventoryInput;
import com.frekanstan.gateway_mobil.app.connection.ServiceConnector;
import com.frekanstan.gateway_mobil.app.tracking.CountingOpDAO;
import com.frekanstan.gateway_mobil.data.Inventory;
import com.frekanstan.gateway_mobil.data.Storage;
import com.frekanstan.gateway_mobil.view.MainActivity;
import com.google.gson.Gson;

import java.util.ArrayList;

import lombok.val;
import lombok.var;

public class CountingReportFragment extends Fragment/* implements ICanScanRfid, DataObserver<List<CountedStateChange>> */{
    private MainActivity context;
    private CountingReportAdapter adapter;
    private RecyclerView recyclerView;
    private ServiceConnector conn;

    //sadfasdf
    /*private long locationId;
    private long storageId;
    private long assetTypeId;
    private long personId;
    private long countingOpId;
    private CountingOp op;
    private List<String> assetRfids;
    private List<Asset> assetsRead;
    private DataSubscription countingSub;*/
    //asdfasdf

    public CountingReportFragment() {
    }

    public static CountingReportFragment newInstance(Bundle input) {
        val fragment = new CountingReportFragment();
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
        //sadfasdf
        /*countingOpId = requireArguments().getLong("countingOpId");
        locationId = requireArguments().getLong("locationId");
        storageId = requireArguments().getLong("storageId");
        assetTypeId = requireArguments().getLong("assetTypeId");
        personId = requireArguments().getLong("personId");

        op = CountingOpDAO.getDao().get(countingOpId);
        var updatedAssets = new ArrayList<Asset>();
        for (val asset : op.countedAssets) {
            asset.setTempCounted(true);
            updatedAssets.add(asset);
        }
        AssetDAO.getDao().putAll(updatedAssets);

        assetRfids = Arrays.asList(AssetDAO.getDao().getBox().query()
                .equal(Asset_.tempCounted, false).build()
                .property(Asset_.rfidCode)
                .findStrings());

        assetsRead = new ArrayList<>();*/
        //asdfasdf

        val view = inflater.inflate(R.layout.counting_report_fragment, container, false);
        recyclerView = view.findViewById(R.id.countingReport);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));

        context.progDialog.setMessage("Depo listesi yükleniyor");
        context.progDialog.show();
        conn = ServiceConnector.getInstance(context);
        conn.addToRequestQueue(conn.getAllWarehousesReq(this::onWarehousesResponse));

        return view;
    }

    private void onWarehousesResponse(AbpResult<ArrayList> response) {
        context.progDialog.setMessage("Stok miktar bilgisi yükleniyor");
        val op = CountingOpDAO.getDao().get(requireArguments().getLong("countingOpId"));
        val gson = new Gson();
        for (val obj : response.getResult()) {
            val storage = gson.fromJson(gson.toJsonTree(obj).getAsJsonObject(), Storage.class);
            if (storage.getName().equals(op.getTitle())) {
                conn.addToRequestQueue(conn.getAllInventoryReq(new GetAllInventoryInput(storage.getRemoteId()), this::onInventoryResponse));
                return;
            }
        }
    }

    CountingReportViewModel model;
    private void onInventoryResponse(AbpResult<ArrayList> response) {
        context.progDialog.hide();
        ArrayList<Inventory> inventoryList = new ArrayList<>();
        val gson = new Gson();
        for (val obj : response.getResult())
            inventoryList.add(gson.fromJson(gson.toJsonTree(obj).getAsJsonObject(), Inventory.class));
        var remoteIds = new ArrayList<String>();
        for (val inv : inventoryList)
            remoteIds.add(inv.getTypeRemoteId());

        adapter = new CountingReportAdapter(context, getArguments(), inventoryList);
        recyclerView.setAdapter(adapter);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                context.hideKeyboard();
            }
        });
        model = new ViewModelProvider(context).get(CountingReportViewModel.class);
        var bundle = new Bundle();
        bundle.putLong("countingOpId", requireArguments().getLong("countingOpId"));
        bundle.putStringArray("remoteIds", remoteIds.toArray(new String[0]));
        model.setInput(bundle);
        //refreshCountingProgress();
        model.getLiveData((result, error) -> context.runOnUiThread(() -> context.hideProgBar()))
                .observe(getViewLifecycleOwner(), types -> {
                    adapter.submitList(types);
                    //refreshCountingProgress();
                    context.setFooterText(String.format(context.getLocale(), context.getString(R.string.number_asset_types), types.size()));
                });
        //asdfasdfas
    }

   /* @Override
    public void onRfidScanned(String code) {
        if (!code.startsWith(getString(R.string.rfid_prefix)))
            return;
        if (AssetRepository.scannedTags.contains(code))
            return;
        AssetRepository.scannedTags.add(code);
        if (!assetRfids.contains(code))
            return;
        val asset = AssetDAO.getDao().getBox().query().equal(Asset_.rfidCode, code).build().findFirst();
        assetsRead.add(asset);
    }

    @Override
    public void onReaderStopped() {
        var assetsToUpdate = new ArrayList<Asset>();
        var assetsToAddOp = new ArrayList<Asset>();
        val now = new Date();
        for (val asset : assetsRead) {
            asset.setTempCounted(true);
            if ((personId == 0 && locationId == 0 && assetTypeId == 0) ||
                    (personId != 0 && asset.getAssignedPersonId() == personId) ||
                    (locationId != 0 && asset.getAssignedLocationId() == locationId) ||
                    (assetTypeId != 0 && asset.getRegistrationCode().startsWith(AssetTypeDAO.getDao().get(assetTypeId).getAssetCode() + "-"))) {
                asset.setLastControlTime(now);
                AssetDAO.getDao().setCountedStateChange(asset.getId(), now, true, countingOpId);
                assetsToAddOp.add(asset);
            }
            assetsToUpdate.add(asset);
        }
        op.countedAssets.addAll(assetsToAddOp);
        AssetDAO.getDao().putAll(assetsToUpdate);
        CountingOpDAO.getDao().put(op);
        assetsRead.clear();
    }

    private void refreshCountingProgress() {
        var bundle = new Bundle();
        if (locationId != 0)
            bundle.putLong("locationId", locationId);
        else if (storageId != 0)
            bundle.putLong("storageId", storageId);
        else if (personId != 0)
            bundle.putLong("personId", personId);
        else if (assetTypeId != 0)
            bundle.putLong("assetTypeId", assetTypeId);
        bundle.putString("listType", "counted");
        val countedCount = AssetDAO.getDao().count(bundle);
        bundle.putString("listType", "notcounted");
        val notCountedCount = AssetDAO.getDao().count(bundle);

        //float percent = (float)op.countedAssets.size() / (float)count * 100F;
        //int rPercent = Math.round(percent);
        //view.countingCardProgress.setProgress(rPercent);

        if (locationId == 0 && personId == 0)
            context.setFooterText(countedCount + "/" + notCountedCount);
        else {
            bundle.putString("listType", "foreign");
            val foreignCount = AssetDAO.getDao().count(bundle);
            context.setFooterText(countedCount + "/" + notCountedCount + "/" + foreignCount);
        }
    }*/

    /*@Override
    public void onPause() {
        super.onPause();
        if (context.barcodeManager != null)
            context.barcodeManager.onPause();
        if (context.rfidManager != null && context.rfidManager.isDeviceOnline())
            context.rfidManager.onPause();
        AssetRepository.scannedTags.clear();
        var listToUpdate = new ArrayList<Asset>();
        for (val asset : AssetDAO.getDao().getBox().query().equal(Asset_.tempCounted, true).build().find()) {
            asset.setTempCounted(false);
            listToUpdate.add(asset);
        }
        AssetDAO.getDao().putAll(listToUpdate);
        countingSub.cancel();
    }*/

    /*@Override
    public void onResume() {
        super.onResume();
        val barcodeModel = PreferenceManager.getDefaultSharedPreferences(context).getString("barcode_device_model", "");
        if (!barcodeModel.equals(""))
            context.initializeBarcode(barcodeModel);
        if (context.barcodeManager != null)
            context.barcodeManager.onResume();
        if (context.rfidManager != null && context.rfidManager.isDeviceOnline())
            context.rfidManager.onResume(IRfidDeviceManager.OperationType.Inventory);
        context.showHideFooter(true);
        context.actionButton.show();
        countingSub = CountedStateChangeDAO.getDao().subscribe(this, new Bundle());
        //context.actionButton.setOnClickListener(v -> mRunnableCode.run());
        /*context.actionButton.setOnClickListener(v -> {
            val assets = AssetDAO.getDao().getBox().query().notNull(Asset_.lastQuantity).build().find();
            val assettypeids = new ArrayList<Long>();
            for (val asset :
                    assets) {
                if (assettypeids.contains(asset.getAssetTypeId()))
                    continue;
                assettypeids.add(asset.getAssetTypeId());
                onRfidScanned(asset.getRfidCode());
            }
            onReaderStopped();
        });*/
    /*}
    Handler handler = new Handler();
    int counter = 0;
    Runnable mRunnableCode = new Runnable() {
        @Override
        public void run() {
            val asset = AssetDAO.getDao().getRandom();
            if (asset.getLastQuantity() != null)
                onRfidScanned(AssetDAO.getDao().getRandom().getRfidCode());
            handler.postDelayed(mRunnableCode, 10);
            counter++;
            if (counter == 300) {
                handler.removeCallbacks(mRunnableCode);
                onReaderStopped();
            }
        }
    };

    @Override
    public void onData(@NonNull List<CountedStateChange> data) {
        if (data.size() == 0)
            return;
        var bundle = new Bundle();
        bundle.putLong("countingOpId", requireArguments().getLong("countingOpId"));
        model.setInput(bundle);
        //adapter.notifyDataSetChanged();
        refreshCountingProgress();
    }*/
}