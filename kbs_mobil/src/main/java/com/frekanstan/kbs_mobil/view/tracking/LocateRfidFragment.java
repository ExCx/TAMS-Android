package com.frekanstan.kbs_mobil.view.tracking;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.frekanstan.asset_management.app.tracking.IRfidDeviceManager;
import com.frekanstan.asset_management.view.shared.ICanScanCode;
import com.frekanstan.asset_management.view.shared.ISearchableFragment;
import com.frekanstan.asset_management.view.shared.ISearchableListFragment;
import com.frekanstan.asset_management.view.tracking.ICanScanRange;
import com.frekanstan.kbs_mobil.R;
import com.frekanstan.kbs_mobil.app.assets.AssetDAO;
import com.frekanstan.kbs_mobil.data.Asset;
import com.frekanstan.kbs_mobil.databinding.LocateRfidFragmentBinding;
import com.frekanstan.kbs_mobil.view.MainActivity;
import com.frekanstan.kbs_mobil.view.assetlist.AssetListFragment;
import com.frekanstan.kbs_mobil.view.assetlist.AssetListViewModel;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;

import java.io.File;

import io.reactivex.disposables.Disposable;
import lombok.val;

import static com.frekanstan.asset_management.view.MainActivityBase.assetPhotosFolder;

public class LocateRfidFragment extends Fragment implements ISearchableFragment, ICanScanCode, ICanScanRange {
    private MainActivity context;
    private LocateRfidFragmentBinding view;
    private Fragment listFragment;
    private Disposable sub;
    private AssetListViewModel model;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity)
            this.context = (MainActivity) context;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = LocateRfidFragmentBinding.inflate(inflater, container, false);
        val input = new Bundle();
        input.putString("operation", "locate");
        input.putString("listType", "nofilter");
        listFragment = AssetListFragment.newInstance(input);
        getChildFragmentManager().beginTransaction()
                .add(R.id.locate_rfid_list_fragment, listFragment)
                .commit();
        model = new ViewModelProvider(context).get("nofilter", AssetListViewModel.class);
        sub = model.subscribeToLastClickedAsset(this::refreshCurrent);
        return view.getRoot();
    }

    private void refreshCurrent(Asset asset) {
        context.runOnUiThread(() -> { //cihazdan okutunca başka threadde çalışıp hata veriyor
            view.locateRfidCardBodyLayout.setVisibility(View.VISIBLE);
            view.locateRfidCardId.setText(asset.getRfidCode());
            final File imgFile = new File(assetPhotosFolder + File.separator + asset.getAssetCode() + "-0.jpg");
            if (imgFile.exists()) {
                view.locateRfidCardImage.setPadding(0, 0, 0, 0);
                Picasso.get().load(imgFile)
                        .resize(128, 128)
                        .centerCrop()
                        .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
                        .into(view.locateRfidCardImage);
            } else
                view.locateRfidCardImage.setImageResource(R.drawable.cat_icon_255);
            view.locateRfidCardTitle.setText(asset.getAssetType().getDefinition());
            ((AssetListFragment)listFragment).getAdapter().notifyDataSetChanged();
        });

        context.rfidManager.setTargetTag(asset.getRfidCode());
        context.rfidManager.onResume(IRfidDeviceManager.OperationType.TagFinder);
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
        if (code.contains(getString(R.string.qrcode_contains))) //if code is asset
        {
            val splittedData = code.split("=");
            int assetId = Integer.parseInt(splittedData[splittedData.length - 1]); //extract remote id
            val asset = AssetDAO.getDao().get(assetId);
            if (asset == null) //if asset is not found
                Toast.makeText(context, R.string.foreign_asset_qrcode_read, Toast.LENGTH_SHORT).show();
            else
                model.setLastClickedAsset(asset);
        }
    }

    @Override
    public void onRangeScanned(int range) {
        context.runOnUiThread(() -> {
            //view.locateRfidProgressBar.setBottomText(String.format("%%%s", range));
            view.locateRfidProgressBar.setProgress(range);
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (context.barcodeManager != null)
            context.barcodeManager.onPause();
        if (context.rfidManager != null && context.rfidManager.isDeviceOnline())
            context.rfidManager.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sub.dispose();
        model.setLastClickedAsset(null);
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
            context.rfidManager.onResume(IRfidDeviceManager.OperationType.TagFinder);
        context.actionButton.show();
        context.showHideFooter(true);
    }
}