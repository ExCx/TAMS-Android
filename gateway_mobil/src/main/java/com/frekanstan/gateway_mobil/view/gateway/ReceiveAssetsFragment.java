package com.frekanstan.gateway_mobil.view.gateway;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.frekanstan.asset_management.app.maintenance.WorkingStateDeserializer;
import com.frekanstan.asset_management.app.sync.DateDeserializer;
import com.frekanstan.asset_management.app.tracking.IRfidDeviceManager;
import com.frekanstan.asset_management.app.webservice.AbpResult;
import com.frekanstan.asset_management.data.acquisition.EWorkingState;
import com.frekanstan.asset_management.view.shared.ICanScanCode;
import com.frekanstan.asset_management.view.shared.ICanScanRfid;
import com.frekanstan.asset_management.view.shared.ISearchableFragment;
import com.frekanstan.asset_management.view.shared.ISearchableListFragment;
import com.frekanstan.gateway_mobil.R;
import com.frekanstan.gateway_mobil.app.assets.AssetDAO;
import com.frekanstan.gateway_mobil.app.assets.AssetRepository;
import com.frekanstan.gateway_mobil.app.connection.GetPackagesInput;
import com.frekanstan.gateway_mobil.app.connection.ServiceConnector;
import com.frekanstan.gateway_mobil.app.multitenancy.TenantRepository;
import com.frekanstan.gateway_mobil.data.Asset;
import com.frekanstan.gateway_mobil.data.Asset_;
import com.frekanstan.gateway_mobil.databinding.ReceiveAssetsFragmentBinding;
import com.frekanstan.gateway_mobil.view.MainActivity;
import com.frekanstan.gateway_mobil.view.assetlist.AssetListFragment;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import lombok.val;
import lombok.var;

public class ReceiveAssetsFragment extends Fragment implements ISearchableFragment, ICanScanCode, ICanScanRfid {
    private MainActivity context;
    private Fragment listFragment;
    private ReceiveAssetsFragmentBinding view;
    private List<String> codesRead;
    private ServiceConnector conn;
    private List<String> assetRfids;
    private List<Asset> assetsRead;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity)
            this.context = (MainActivity) context;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        if (requireArguments().getBoolean("reset", false)) {
            var listToUpdate = new ArrayList<Asset>();
            for (val asset : AssetDAO.getDao().getBox().query().equal(Asset_.tempCounted, true).build().find()) {
                asset.setTempCounted(false);
                listToUpdate.add(asset);
            }
            AssetDAO.getDao().putAll(listToUpdate);
            requireArguments().putBoolean("reset", false);
        }

        assetRfids = Arrays.asList(AssetDAO.getDao().getBox().query()
                .equal(Asset_.tempCounted, false).build()
                .property(Asset_.rfidCode)
                .findStrings());

        assetsRead = new ArrayList<>();

        codesRead = new ArrayList<>();
        conn = ServiceConnector.getInstance(context);
        view = ReceiveAssetsFragmentBinding.inflate(inflater, container, false);
        val bundle = new Bundle();
        bundle.putString("operation", "receive_assets");
        bundle.putString("listType", "counted");
        listFragment = AssetListFragment.newInstance(bundle);
        getChildFragmentManager().beginTransaction()
                .add(R.id.receive_assets_list_fragment, listFragment)
                .commit();
        return view.getRoot();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.counting_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.report_counting) {
            val bundle = requireArguments();
            bundle.putLongArray("assetIds", AssetDAO.getDao().getBox().query().equal(Asset_.tempCounted, true).build().findIds());
            context.nav.navigate(R.id.action_receiveAssetsFragment_to_receiveReportFragment, bundle);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public String getQuery() {
        return ((ISearchableListFragment) listFragment).getQuery();
    }

    @Override
    public void setQuery(String query) {
        ((ISearchableListFragment) listFragment).setQuery(query);
    }

    @Override
    public void onCodeScanned(String code) {
        if (code.startsWith(getString(R.string.rfid_prefix))) //if code is asset
        {
            val asset = AssetDAO.getDao().getBox().query().equal(Asset_.rfidCode, code).build().findFirst();
            if (asset == null) //if asset is not found
                Toast.makeText(context, R.string.foreign_asset_qrcode_read, Toast.LENGTH_SHORT).show();
            else {
                var list = new ArrayList<String>();
                list.add(code);
                context.progDialog.setMessage("Ürün bilgisi yükleniyor");
                context.progDialog.show();
                conn.addToRequestQueue(conn.getPackagesReq(new GetPackagesInput(list), this::onPackagesGet));
            }
        }
    }

    @Override
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
        codesRead.add(code);
    }

    boolean isBusy = false;
    @Override
    public void onReaderStopped() {
        var assetsToUpdate = new ArrayList<Asset>();
        for (val asset : assetsRead) {
            asset.setTempCounted(true);
            assetsToUpdate.add(asset);
        }
        AssetDAO.getDao().putAll(assetsToUpdate);
        assetsRead.clear();

        /*if (isBusy)
            Toast.makeText(context, "Okutuğunuz ürünler kontrol ediliyor. Lütfen bekleyiniz.", Toast.LENGTH_LONG).show();
        else {
            isBusy = true;
            conn.addToRequestQueue(conn.getPackagesReq(new GetPackagesInput(codesRead), this::onPackagesGet));
        }*/
    }

    private void onPackagesGet(AbpResult<ArrayList> packageList) {
        context.progDialog.hide();
        if (packageList.getResult().size() == 0)
            return;
        val gson = new GsonBuilder()
                .registerTypeAdapter(Date.class, new DateDeserializer())
                .registerTypeAdapter(EWorkingState.class, new WorkingStateDeserializer())
                .create();
        var list = new ArrayList<Asset>();
        for (val obj : packageList.getResult()) {
            var asset = gson.fromJson(gson.toJsonTree(obj).getAsJsonObject(), Asset.class);
            asset.setTempCounted(true);
            asset.tenant.setTargetId(TenantRepository.getCurrentTenant().getId());
            asset.assetType.setTargetId(asset.getAssetTypeId());
            AssetDAO.getDao().getBox().attach(asset);
            asset.setAssetTypeDefinition(asset.getAssetType().getDefinition());
            list.add(asset);
        }
        AssetDAO.getDao().putAll(list);
        codesRead.clear();
        isBusy = false;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (context.barcodeManager != null)
            context.barcodeManager.onPause();
        if (context.rfidManager != null && context.rfidManager.isDeviceOnline())
            context.rfidManager.onPause();
        AssetRepository.scannedTags.clear();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        var listToUpdate = new ArrayList<Asset>();
        for (val asset : AssetDAO.getDao().getBox().query().equal(Asset_.tempCounted, true).build().find()) {
            asset.setTempCounted(false);
            listToUpdate.add(asset);
        }
        AssetDAO.getDao().putAll(listToUpdate);
    }

    @Override
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
        //context.actionButton.setOnClickListener(v -> mRunnableCode.run());
    }

    Runnable mRunnableCode = () -> {
        onRfidScanned("C05E0001FFFFFFFFFFFFFFFF");
        onReaderStopped();
    };
}