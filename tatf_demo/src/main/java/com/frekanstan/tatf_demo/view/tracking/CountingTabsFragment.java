package com.frekanstan.tatf_demo.view.tracking;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.frekanstan.asset_management.app.assets.AssetTypeIconFinder;
import com.frekanstan.asset_management.app.locations.LocationIconFinder;
import com.frekanstan.asset_management.app.tracking.IRfidDeviceManager;
import com.frekanstan.asset_management.data.people.EPersonType;
import com.frekanstan.asset_management.view.shared.ICanScanCode;
import com.frekanstan.asset_management.view.shared.ICanScanRfid;
import com.frekanstan.asset_management.view.shared.ISearchableFragment;
import com.frekanstan.asset_management.view.shared.ISearchableListFragment;
import com.frekanstan.tatf_demo.R;
import com.frekanstan.tatf_demo.app.assets.AssetDAO;
import com.frekanstan.tatf_demo.app.assets.AssetRepository;
import com.frekanstan.tatf_demo.app.assettypes.AssetTypeDAO;
import com.frekanstan.tatf_demo.app.locations.LocationDAO;
import com.frekanstan.tatf_demo.app.people.PersonDAO;
import com.frekanstan.tatf_demo.app.sync.CountedStateChangeDAO;
import com.frekanstan.tatf_demo.app.tracking.CountingOpDAO;
import com.frekanstan.tatf_demo.data.Asset;
import com.frekanstan.tatf_demo.data.Asset_;
import com.frekanstan.tatf_demo.data.CountedStateChange;
import com.frekanstan.tatf_demo.data.CountingOp;
import com.frekanstan.tatf_demo.databinding.CountingTabsFragmentBinding;
import com.frekanstan.tatf_demo.view.MainActivity;
import com.frekanstan.tatf_demo.view.NavigationManager;
import com.frekanstan.tatf_demo.view.assetlist.AssetListViewModel;
import com.google.android.material.tabs.TabLayout;
import com.google.common.base.Strings;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import io.objectbox.reactive.DataObserver;
import io.objectbox.reactive.DataSubscription;
import lombok.val;
import lombok.var;

import static com.frekanstan.asset_management.view.MainActivityBase.personPhotosFolder;

public class CountingTabsFragment extends Fragment implements ISearchableFragment, ICanScanCode, ICanScanRfid, DataObserver<List<CountedStateChange>> {
    private MainActivity context;
    private CountingTabPagerAdapter adapter;
    private CountingTabsFragmentBinding view;
    private long locationId;
    private long storageId;
    private long assetTypeId;
    private long personId;
    private long countingOpId;
    private DataSubscription countingSub;
    private CountingOp op;
    private List<String> assetRfids;
    private List<Asset> assetsRead;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity)
            this.context = (MainActivity) context;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        countingOpId = requireArguments().getLong("countingOpId");
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

        assetsRead = new ArrayList<>();

        view = CountingTabsFragmentBinding.inflate(inflater, container, false);
        adapter = new CountingTabPagerAdapter(getChildFragmentManager(), requireArguments());
        view.pager.setOffscreenPageLimit(adapter.getCount() - 1);
        view.pager.setAdapter(adapter);
        view.pager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(view.tabLayout));
        view.tabLayout.setupWithViewPagerAndKeepIcons(view.pager);

        locationId = requireArguments().getLong("locationId");
        storageId = requireArguments().getLong("storageId");
        assetTypeId = requireArguments().getLong("assetTypeId");
        personId = requireArguments().getLong("personId");

        if (countingOpId != 0)
        {
            view.countingCardTitle.setText(context.getString(R.string.general_counting));
            view.countingCardId.setText(String.valueOf(countingOpId));
            view.countingCardIcon.setImageResource(R.drawable.globe_gray_96px);
            //refreshCountingProgress(locationId, "location");
        }

        if (locationId != 0)
        {
            val location = LocationDAO.getDao().get(locationId);
            view.countingCardTitle.setText(location.getName());
            view.countingCardId.setText(String.valueOf(location.getId()));
            view.countingCardIcon.setImageResource(LocationIconFinder.findLocationIconId(location.getName()));
            refreshCountingProgress(locationId, "location");
        }
        else if (storageId != 0)
        {
            val storage = LocationDAO.getDao().get(storageId);
            view.countingCardTitle.setText(storage.getName());
            view.countingCardId.setText(String.valueOf(storage.getId()));
            view.countingCardIcon.setImageResource(R.drawable.database_gray_96px);
            refreshCountingProgress(storageId, "storage");
        }
        else if (personId != 0)
        {
            val person = PersonDAO.getDao().get(personId);
            view.countingCardTitle.setText(person.getNameSurname());
            if (Strings.isNullOrEmpty(person.getIdentityNo()))
                view.countingCardId.setText(String.valueOf(person.getId()));
            else
                view.countingCardId.setText(person.getIdentityNo());
            final File imgFile = new File(personPhotosFolder + File.separator + person.getId() + ".jpg");
            if (imgFile.exists()) {
                view.countingCardIcon.setPadding(0, 0, 0, 0);
                Picasso.get().load(imgFile)
                        .resize(128, 128)
                        .centerCrop()
                        .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
                        .into(view.countingCardIcon);
            } else {
                if (person.getPersonType() == EPersonType.Recorder)
                    view.countingCardIcon.setImageResource(R.drawable.recorder_gray_96px);
                else
                    view.countingCardIcon.setImageResource(R.drawable.person_gray_96px);
            }
            refreshCountingProgress(personId, "person");
        }
        else if (assetTypeId != 0)
        {
            val assetType = AssetTypeDAO.getDao().get(assetTypeId);
            view.countingCardTitle.setText(assetType.getDefinition());
            view.countingCardId.setText(assetType.getAssetCode());
            view.countingCardIcon.setImageResource(new AssetTypeIconFinder(context).findIconId(assetType.getAssetCode()));
            refreshCountingProgress(assetTypeId, "assetType");
        }

        countingSub = CountedStateChangeDAO.getDao().subscribe(this, new Bundle()); //observe for lists

        //observe for badges
        val model = new ViewModelProvider(context).get("counted", AssetListViewModel.class);
        model.getLiveData((result, error) -> {
        }).observe(getViewLifecycleOwner(), assets -> {
            val badge = view.tabLayout.getTabAt(0).getOrCreateBadge();
            badge.setVisible(true);
            badge.setNumber(assets.size());

            /*val assetId = requireArguments().getLong("assetId");
            if (assetId != 0) { //if there is an asset to count, count it and reset id
                ((AssetListFragment)adapter.getItem(0)).countAsset(AssetDAO.getDao().get(assetId));
                requireArguments().putLong("assetId", 0);
            }*/
        });
        val model2 = new ViewModelProvider(context).get("notcounted", AssetListViewModel.class);
        model2.getLiveData((result, error) -> {
        }).observe(getViewLifecycleOwner(), assets -> {
            val badge = view.tabLayout.getTabAt(1).getOrCreateBadge();
            badge.setVisible(true);
            badge.setNumber(assets.size());
        });
        if (locationId != 0 || personId != 0) {
            val model3 = new ViewModelProvider(context).get("foreign", AssetListViewModel.class);
            model3.getLiveData((result, error) -> {
            }).observe(getViewLifecycleOwner(), assets -> {
                val badge = view.tabLayout.getTabAt(2).getOrCreateBadge();
                badge.setVisible(true);
                badge.setNumber(assets.size());
            });
        }

        return view.getRoot();
    }

    @Override
    public String getQuery() {
        return ((ISearchableListFragment)adapter.getItem(view.tabLayout.getSelectedTabPosition())).getQuery();
    }

    @Override
    public void setQuery(String query) {
        ((ISearchableListFragment)adapter.getItem(view.tabLayout.getSelectedTabPosition())).setQuery(query);
    }

    @Override
    public void onCodeScanned(String code) {
        if (code.contains(getString(R.string.qrcode_contains))) //if code is asset
        {
            val splittedData = code.split("=");
            int assetId = Integer.parseInt(splittedData[splittedData.length - 1]); //extract remote id
            val asset = AssetDAO.getDao().get(assetId);
            if (asset == null) { //if asset is not found
                Toast.makeText(context, R.string.foreign_asset_qrcode_read, Toast.LENGTH_SHORT).show();
                return;
            }
            val typeDao = AssetTypeDAO.getDao();
            var alertDialog = new AlertDialog.Builder(context)
                    .setNeutralButton(context.getString(R.string.view_asset), (dialog, id) ->
                            NavigationManager.goToAssetDetails(context, assetId, R.id.assetDetailsFragment))
                    .setPositiveButton(context.getString(R.string.assign_title), (dialog, id) -> {
                        var bundle = new Bundle();
                        bundle.putLongArray("assetIds", new long[]{assetId});
                        bundle.putLong("personId", personId);
                        bundle.putLong("locationId", locationId);
                        context.nav.navigate(R.id.assignmentDialogFragment, bundle);
                    });
            if (personId != 0 && asset.getAssignedPersonId() != personId) { //bu kişiye ait değilse
                alertDialog.setNegativeButton(context.getString(R.string.goto_person), (dialog, id) -> {
                    var bundle = new Bundle();
                    bundle.putLong("assetId", asset.getId());
                    bundle.putLong("personId", asset.getAssignedPersonId());
                    context.nav.navigate(R.id.countingTabsFragment, bundle);
                });
                if (asset.getAssignedPersonId() != 0) //başka kişiye aitse
                    alertDialog.setMessage(
                            String.format(context.getString(R.string.alert_asset_is_on_another_person),
                                    asset.getAssignedPerson().getNameSurname()))
                            .show();
                else //depodaysa
                    alertDialog.setMessage(R.string.alert_asset_is_not_assigned_to_person)
                            .show();
            } else if (locationId != 0 && asset.getAssignedLocationId() != locationId) { //bu yere ait değilse
                alertDialog.setNegativeButton(context.getString(R.string.goto_unit), (dialog, id) -> {
                    var bundle = new Bundle();
                    bundle.putLong("assetId", asset.getId());
                    bundle.putLong("locationId", asset.getAssignedLocationId());
                    context.nav.navigate(R.id.countingTabsFragment, bundle);
                });
                if (asset.getAssignedLocationId() != 0) //başka yere aitse
                    alertDialog.setMessage(
                            String.format(context.getString(R.string.alert_asset_is_on_another_location),
                                    asset.getAssignedLocation().getName()))
                            .show();
                else //yeri belirsizse
                    alertDialog.setMessage(R.string.alert_asset_is_not_assigned_to_location)
                            .show();
            } else if (assetTypeId != 0 && !asset.getRegistrationCode().startsWith(typeDao.get(assetTypeId).getAssetCode() + "-")) { //bu kategoriye ait değilse
                /*val depth = typeDao.get(assetTypeId).getDepth();
                val type = typeDao.findTypeOfDepth(asset.getAssetType(), depth);
                new AlertDialog.Builder(context).setMessage(
                        String.format(context.getString(R.string.alert_asset_is_of_another_type),
                                type.getDefinition()))
                        .setNeutralButton(context.getString(R.string.view_asset), (dialog, id) ->
                                NavigationManager.goToAssetDetails(context, asset.getId(), R.id.assetDetailsFragment))
                        .setPositiveButton(context.getString(R.string.mark), (dialog, id) ->
                                AssetDAO.getDao().setCountedStateChange(asset.getId(), true))
                        .setNegativeButton(context.getString(R.string.mark_and_goto_type), (dialog, id) -> {
                            var bundle = new Bundle();
                            bundle.putLong("assetId", asset.getId());
                            bundle.putLong("assetTypeId", type.getId());
                            context.nav.navigate(R.id.countingTabsFragment, bundle);
                        })
                        .show();*/
            } else { //listede varsa
                asset.setTempCounted(true);
                asset.setLastControlTime(new Date());
                op.countedAssets.add(asset);
                AssetDAO.getDao().setCountedStateChange(asset.getId(), asset.getLastControlTime(), true, countingOpId);
                AssetDAO.getDao().put(asset);
                CountingOpDAO.getDao().put(op);
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
        /*if ((personId == 0 && locationId == 0 && assetTypeId == 0) ||
                (personId != 0 && asset.getAssignedPersonId() == personId) ||
                (locationId != 0 && asset.getAssignedLocationId() == locationId) ||
                (assetTypeId != 0 && asset.getRegistrationCode().startsWith(AssetTypeDAO.getDao().get(assetTypeId).getAssetCode() + "-"))) {
            AssetDAO.getDao().setCountedStateChange(asset.getId(), true);
        } else if (personId != 0 && asset.getAssignedPersonId() != personId) {
            asset.setPersonToAssignId(personId);
            AssetDAO.getDao().put(asset);
        } else if (locationId != 0 && asset.getAssignedLocationId() != locationId) {
            asset.setLocationToAssignId(locationId);
            AssetDAO.getDao().put(asset);
        }*/
    }

    @Override
    public void onReaderStopped() {
        var assetsToUpdate = new ArrayList<Asset>();
        val now = new Date();
        for (val asset : assetsRead) {
            asset.setTempCounted(true);
            asset.setLastControlTime(now);
            assetsToUpdate.add(asset);
            AssetDAO.getDao().setCountedStateChange(asset.getId(), now, true, countingOpId);
        }
        op.countedAssets.addAll(assetsToUpdate);
        AssetDAO.getDao().putAll(assetsToUpdate);
        CountingOpDAO.getDao().put(op);
        assetsRead.clear();
    }

    private void refreshCountingProgress(long id, String type) {
        var bundle = new Bundle();
        switch (type) {
            case "location":
                bundle.putLong("locationId", id);
                break;
            case "storage":
                bundle.putLong("storageId", id);
                break;
            case "person":
                bundle.putLong("personId", id);
                break;
            case "assetType":
                bundle.putLong("assetTypeId", id);
                break;
        }
        val count = AssetDAO.getDao().count(bundle);
        //bundle.putString("listType", "counted");
        //val countedCount = AssetDAO.getDao().count(bundle);
        float percent = (float)op.countedAssets.size() / (float)count * 100F;
        int rPercent = Math.round(percent);
        view.countingCardProgress.setProgress(rPercent);
    }

    @Override
    public void onData(List<CountedStateChange> data) {
        if (locationId != 0)
            refreshCountingProgress(locationId, "location");
        else if (storageId != 0)
            refreshCountingProgress(storageId, "storage");
        else if (personId != 0)
            refreshCountingProgress(personId, "person");
        else if (assetTypeId != 0)
            refreshCountingProgress(assetTypeId, "assetType");
    }

    @Override
    public void onPause() {
        super.onPause();
        if (context.barcodeManager != null)
            context.barcodeManager.onPause();
        if (context.rfidManager != null && context.rfidManager.isDeviceOnline())
            context.rfidManager.onPause();
        countingSub.cancel();
        AssetRepository.scannedTags.clear();
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
        /*context.actionButton.setOnClickListener(v -> {
            mRunnableCode.run();
        });*/
    }

    Handler handler = new Handler();
    int counter = 0;
    Runnable mRunnableCode = new Runnable() {
        @Override
        public void run() {
            //Log.d("asdf", "fdas");
            onRfidScanned(AssetDAO.getDao().getRandom().getRfidCode());
            handler.postDelayed(mRunnableCode, 50);
            counter++;
            if (counter == 50)
                handler.removeCallbacks(mRunnableCode);
        }
    };
}