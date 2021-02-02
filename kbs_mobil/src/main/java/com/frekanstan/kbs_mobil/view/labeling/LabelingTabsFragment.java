package com.frekanstan.kbs_mobil.view.labeling;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.frekanstan.asset_management.app.assets.AssetTypeIconFinder;
import com.frekanstan.asset_management.app.locations.LocationIconFinder;
import com.frekanstan.asset_management.app.tracking.IRfidDeviceManager;
import com.frekanstan.asset_management.data.people.EPersonType;
import com.frekanstan.asset_management.view.shared.ICanScanCode;
import com.frekanstan.asset_management.view.shared.ICanScanRfid;
import com.frekanstan.asset_management.view.shared.ISearchableFragment;
import com.frekanstan.asset_management.view.shared.ISearchableListFragment;
import com.frekanstan.kbs_mobil.R;
import com.frekanstan.kbs_mobil.app.assets.AssetDAO;
import com.frekanstan.kbs_mobil.app.assettypes.AssetTypeDAO;
import com.frekanstan.kbs_mobil.app.labeling.LabelPrinter;
import com.frekanstan.kbs_mobil.app.locations.LocationDAO;
import com.frekanstan.kbs_mobil.app.people.PersonDAO;
import com.frekanstan.kbs_mobil.app.sync.LabeledStateChangeDAO;
import com.frekanstan.kbs_mobil.data.LabeledStateChange;
import com.frekanstan.kbs_mobil.data.Location;
import com.frekanstan.kbs_mobil.data.Person;
import com.frekanstan.kbs_mobil.databinding.LabelingTabsFragmentBinding;
import com.frekanstan.kbs_mobil.view.MainActivity;
import com.frekanstan.kbs_mobil.view.assetlist.AssetListViewModel;
import com.frekanstan.kbs_mobil.view.locations.LocationListViewModel;
import com.frekanstan.kbs_mobil.view.people.PersonListViewModel;
import com.google.android.material.tabs.TabLayout;
import com.google.common.base.Strings;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import io.objectbox.reactive.DataObserver;
import io.objectbox.reactive.DataSubscription;
import lombok.Setter;
import lombok.val;
import lombok.var;

import static com.frekanstan.asset_management.view.MainActivityBase.personPhotosFolder;

public class LabelingTabsFragment extends Fragment implements ISearchableFragment, ICanScanCode, ICanScanRfid, DataObserver<List<LabeledStateChange>> {
    private MainActivity context;
    private LabelingTabPagerAdapter adapter;
    private LabelingTabsFragmentBinding view;
    private long locationId, storageId, assetTypeId, personId;
    private DataSubscription labelingSub;
    @Setter
    private static String newTag;
    private Bundle args;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity)
            this.context = (MainActivity) context;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = LabelingTabsFragmentBinding.inflate(inflater, container, false);
        args = getArguments();
        adapter = new LabelingTabPagerAdapter(getChildFragmentManager(), args);
        view.pager.setAdapter(adapter);
        view.pager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(view.tabLayout));
        view.tabLayout.setupWithViewPagerAndKeepIcons(view.pager);

        switch (args.getString("operation")) {
            case "labeling": { //asset labeling
                locationId = args.getLong("locationId");
                storageId = args.getLong("storageId");
                assetTypeId = args.getLong("assetTypeId");
                personId = args.getLong("personId");
                if (locationId != 0) {
                    val location = LocationDAO.getDao().get(locationId);
                    view.labelingCardTitle.setText(location.getName());
                    view.labelingCardId.setText(String.valueOf(location.getId()));
                    view.labelingCardIcon.setImageResource(LocationIconFinder.findLocationIconId(location.getName()));
                    refreshLabelingProgress(locationId, "location");
                } else if (storageId != 0) {
                    val storage = LocationDAO.getDao().get(storageId);
                    view.labelingCardTitle.setText(storage.getName());
                    view.labelingCardId.setText(String.valueOf(storage.getId()));
                    view.labelingCardIcon.setImageResource(R.drawable.database_gray_96px);
                    refreshLabelingProgress(storageId, "storage");
                } else if (personId != 0) {
                    val person = PersonDAO.getDao().get(personId);
                    view.labelingCardTitle.setText(person.getNameSurname());
                    if (Strings.isNullOrEmpty(person.getIdentityNo()))
                        view.labelingCardId.setText(String.valueOf(person.getId()));
                    else
                        view.labelingCardId.setText(person.getIdentityNo());
                    final File imgFile = new File(personPhotosFolder + File.separator + person.getId() + ".jpg");
                    if (imgFile.exists()) {
                        view.labelingCardIcon.setPadding(0, 0, 0, 0);
                        Picasso.get().load(imgFile)
                                .resize(128, 128)
                                .centerCrop()
                                .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
                                .into(view.labelingCardIcon);
                    } else {
                        if (person.getPersonType() == EPersonType.Recorder)
                            view.labelingCardIcon.setImageResource(R.drawable.recorder_gray_96px);
                        else
                            view.labelingCardIcon.setImageResource(R.drawable.person_gray_96px);
                    }
                    refreshLabelingProgress(personId, "person");
                } else if (assetTypeId != 0) {
                    val assetType = AssetTypeDAO.getDao().get(assetTypeId);
                    view.labelingCardTitle.setText(assetType.getDefinition());
                    view.labelingCardId.setText(assetType.getAssetCode());
                    view.labelingCardIcon.setImageResource(new AssetTypeIconFinder(context).findIconId(assetType.getAssetCode()));
                    refreshLabelingProgress(assetTypeId, "assetType");
                } else
                    view.labelingCardBodyLayout.setVisibility(View.GONE);

                labelingSub = LabeledStateChangeDAO.getDao().subscribe(this, new Bundle());

                val model = new ViewModelProvider(context).get("labeled", AssetListViewModel.class);
                model.getLiveData((result, error) -> {
                }).observe(getViewLifecycleOwner(), assets -> {
                    val badge = view.tabLayout.getTabAt(0).getOrCreateBadge();
                    badge.setVisible(true);
                    badge.setNumber(assets.size());
                });
                val model2 = new ViewModelProvider(context).get("notlabeled", AssetListViewModel.class);
                model2.getLiveData((result, error) -> {
                }).observe(getViewLifecycleOwner(), assets -> {
                    val badge = view.tabLayout.getTabAt(1).getOrCreateBadge();
                    badge.setVisible(true);
                    badge.setNumber(assets.size());
                });
                break;
            }
            case "location_labeling": { //location labeling
                view.labelingCardBodyLayout.setVisibility(View.GONE);
                val model = new ViewModelProvider(context).get("labeled", LocationListViewModel.class);
                model.getLiveData((result, error) -> {
                }).observe(getViewLifecycleOwner(), locations -> {
                    val badge = view.tabLayout.getTabAt(0).getOrCreateBadge();
                    badge.setVisible(true);
                    badge.setNumber(locations.size());
                });
                val model2 = new ViewModelProvider(context).get("notlabeled", LocationListViewModel.class);
                model2.getLiveData((result, error) -> {
                }).observe(getViewLifecycleOwner(), locations -> {
                    val badge = view.tabLayout.getTabAt(1).getOrCreateBadge();
                    badge.setVisible(true);
                    badge.setNumber(locations.size());
                });
                break;
            }
            case "person_labeling": { //person labeling
                view.labelingCardBodyLayout.setVisibility(View.GONE);
                val model = new ViewModelProvider(context).get("labeled", PersonListViewModel.class);
                model.getLiveData((result, error) -> {
                }).observe(getViewLifecycleOwner(), persons -> {
                    val badge = view.tabLayout.getTabAt(0).getOrCreateBadge();
                    badge.setVisible(true);
                    badge.setNumber(persons.size());
                });
                val model2 = new ViewModelProvider(context).get("notlabeled", PersonListViewModel.class);
                model2.getLiveData((result, error) -> {
                }).observe(getViewLifecycleOwner(), persons -> {
                    val badge = view.tabLayout.getTabAt(1).getOrCreateBadge();
                    badge.setVisible(true);
                    badge.setNumber(persons.size());
                });
                break;
            }
        }

        return view.getRoot();
    }

    @Override
    public void onCodeScanned(String code) {
        switch (Objects.requireNonNull(args.getString("operation"))) {
            case "labeling":  //asset labeling
                if (code.contains(getString(R.string.qrcode_contains))) //if code is asset
                {
                    val splittedData = code.split("=");
                    int assetId = Integer.parseInt(splittedData[splittedData.length - 1]); //extract remote id
                    val asset = AssetDAO.getDao().get(assetId);
                    if (asset == null) { //if asset is not found
                        Toast.makeText(context, R.string.foreign_asset_qrcode_read, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    ArrayList<Long> idList = new ArrayList<>();
                    idList.add(asset.getId());
                    if (context.rfidManager != null && context.rfidManager.isDeviceOnline()) { //rfid varsa
                        val builder = new AlertDialog.Builder(context);
                        if (asset.getLabelingDateTime() == null)
                            builder.setMessage(R.string.scanned_label_not_printed_single_rfid)
                                    .setNegativeButton(context.getString(R.string.mark_as_labeled), (dialog, id) ->
                                            AssetDAO.getDao().setLabeledStateChange(asset.getId(), true));
                        else
                            builder.setMessage(getString(R.string.scanned_label_already_printed_single_rfid, String.valueOf(asset.getAssetCode())))
                                    .setNegativeButton(context.getString(R.string.mark_as_not_labeled), (dialog, id) ->
                                            AssetDAO.getDao().setLabeledStateChange(asset.getId(), false));
                        builder.setPositiveButton(context.getString(R.string.transfer_to_rfid_tag), (dialog, id) -> {
                            newTag = "C05E" + String.format(Locale.US, "%020d", assetId);
                            Toast.makeText(context, R.string.scan_the_rfid_tag_to_overwrite, Toast.LENGTH_SHORT).show();
                        })
                                .setNeutralButton(context.getString(R.string.print_qrcode), (dialog, id) ->
                                        new LabelPrinter(context, idList, "asset", null).print())
                                .show();
                    } else {
                        val builder = new AlertDialog.Builder(context);
                        if (asset.getLabelingDateTime() == null)
                            builder.setMessage(R.string.scanned_label_not_printed_single)
                                    .setNeutralButton(context.getString(R.string.mark_as_labeled), (dialog, id) ->
                                            AssetDAO.getDao().setLabeledStateChange(asset.getId(), true));
                        else
                            builder.setMessage(R.string.scanned_label_already_printed_single)
                                    .setNeutralButton(context.getString(R.string.mark_as_not_labeled), (dialog, id) ->
                                            AssetDAO.getDao().setLabeledStateChange(asset.getId(), false));
                        builder.setPositiveButton(context.getString(R.string.print_label), (dialog, id) ->
                                new LabelPrinter(context, idList, "asset", null).print())
                                .setNegativeButton(context.getString(R.string.view_asset), (dialog, id) -> {
                                    var bundle = new Bundle();
                                    bundle.putLong("assetId", asset.getId());
                                    context.nav.navigate(R.id.assetDetailsFragment, bundle);
                                })
                                .show();
                    }
                } else
                    Toast.makeText(context, R.string.invalid_qrcode_read, Toast.LENGTH_SHORT).show();
                break;
            case "location_labeling":  //location labeling
                if (code.contains(getString(R.string.location_qr_prefix))) { //if code is location
                    val locationCode = code.substring(getString(R.string.location_qr_prefix).length());
                    int locationId = Integer.parseInt(locationCode); //extract id
                    val location = LocationDAO.getDao().get(locationId);
                    if (location == null) { //if location is not found
                        Toast.makeText(context, R.string.foreign_location_qrcode_read, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    ArrayList<Long> idList = new ArrayList<>();
                    idList.add(location.getId());
                    if (context.rfidManager != null && context.rfidManager.isDeviceOnline()) { //rfid varsa
                        val builder = new AlertDialog.Builder(context);
                        if (location.getLabelingDateTime() == null)
                            builder.setMessage(R.string.scanned_label_not_printed_single_rfid);
                        else
                            builder.setMessage(getString(R.string.scanned_label_already_printed_single_rfid, String.valueOf(location.getLocationCode())));
                        builder.setPositiveButton(context.getString(R.string.transfer_to_rfid_tag), (dialog, id) -> {
                            newTag = "C05E1" + String.format(Locale.US, "%019d", locationId);
                            Toast.makeText(context, R.string.scan_the_rfid_tag_to_overwrite, Toast.LENGTH_SHORT).show();
                        })
                                .setNeutralButton(context.getString(R.string.print_qrcode), (dialog, id) ->
                                        new LabelPrinter(context, idList, "location", null).print())
                                .setNegativeButton(context.getString(R.string.mark_as_labeled), (dialog, id) -> {
                                    location.setAsLabeled(true);
                                    LocationDAO.getDao().put((Location) location);
                                    LocationDAO.getDao().setLabeledStateChange(location.getId(), location.getLabelingDateTime());
                                })
                                .show();
                    } else {
                        val builder = new AlertDialog.Builder(context);
                        if (location.getLabelingDateTime() == null)
                            builder.setMessage(R.string.scanned_label_not_printed_single);
                        else
                            builder.setMessage(R.string.scanned_label_already_printed_single);
                        builder.setPositiveButton(context.getString(R.string.print_label), (dialog, id) ->
                                new LabelPrinter(context, idList, "location", null).print())
                                .setNeutralButton(context.getString(R.string.mark_as_labeled), (dialog, id) -> {
                                    location.setAsLabeled(true);
                                    LocationDAO.getDao().put((Location) location);
                                    LocationDAO.getDao().setLabeledStateChange(location.getId(), location.getLabelingDateTime());
                                })
                                .setNegativeButton(context.getString(R.string.view_assets_of_location), (dialog, id) -> {
                                    var bundle = requireArguments();
                                    bundle.putLong("locationId", location.getId());
                                    context.nav.navigate(R.id.labelingTabsFragment, bundle);
                                })
                                .show();
                    }
                } else
                    Toast.makeText(context, R.string.invalid_qrcode_read, Toast.LENGTH_SHORT).show();
                break;
            case "person_labeling":  //person labeling
                if (code.contains(getString(R.string.person_qr_prefix))) { //if code is person
                    val personCode = code.substring(getString(R.string.person_qr_prefix).length());
                    int personId = Integer.parseInt(personCode); //extract id
                    val person = PersonDAO.getDao().get(personId);
                    if (person == null) { //if person is not found
                        Toast.makeText(context, R.string.foreign_person_qrcode_read, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    ArrayList<Long> idList = new ArrayList<>();
                    idList.add(person.getId());
                    if (context.rfidManager != null && context.rfidManager.isDeviceOnline()) { //rfid varsa
                        val builder = new AlertDialog.Builder(context);
                        if (person.getLabelingDateTime() == null)
                            builder.setMessage(R.string.scanned_label_not_printed_single_rfid);
                        else
                            builder.setMessage(getString(R.string.scanned_label_already_printed_single_rfid, String.valueOf(person.getPersonCode())));
                        builder.setPositiveButton(context.getString(R.string.transfer_to_rfid_tag), (dialog, id) -> {
                            newTag = "C05E2" + String.format(Locale.US, "%019d", personId);
                            Toast.makeText(context, R.string.scan_the_rfid_tag_to_overwrite, Toast.LENGTH_SHORT).show();
                        })
                                .setNeutralButton(context.getString(R.string.print_qrcode), (dialog, id) ->
                                        new LabelPrinter(context, idList, "person", null).print())
                                .setNegativeButton(context.getString(R.string.mark_as_labeled), (dialog, id) -> {
                                    person.setAsLabeled(true);
                                    PersonDAO.getDao().put((Person) person);
                                    PersonDAO.getDao().setLabeledStateChange(person.getId(), person.getLabelingDateTime());
                                })
                                .show();
                    } else {
                        val builder = new AlertDialog.Builder(context);
                        if (person.getLabelingDateTime() == null)
                            builder.setMessage(R.string.scanned_label_not_printed_single);
                        else
                            builder.setMessage(R.string.scanned_label_already_printed_single);
                        builder.setPositiveButton(context.getString(R.string.print_label), (dialog, id) ->
                                new LabelPrinter(context, idList, "person", null).print())
                                .setNeutralButton(context.getString(R.string.mark_as_labeled), (dialog, id) -> {
                                    person.setAsLabeled(true);
                                    PersonDAO.getDao().put((Person) person);
                                    PersonDAO.getDao().setLabeledStateChange(person.getId(), person.getLabelingDateTime());
                                })
                                .setNegativeButton(context.getString(R.string.view_assets_of_person), (dialog, id) -> {
                                    var bundle = requireArguments();
                                    bundle.putLong("personId", person.getId());
                                    context.nav.navigate(R.id.labelingTabsFragment, bundle);
                                })
                                .show();
                    }
                } else
                    Toast.makeText(context, R.string.invalid_qrcode_read, Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    public void onRfidScanned(String code) {
        if (newTag == null) {
            Toast.makeText(context, R.string.first_scan_the_qrcode_to_transfer, Toast.LENGTH_LONG).show();
            return;
        }

        context.rfidManager.writeTag(code, newTag);

        if (Objects.equals(args.getString("operation"), "labeling")) { //asset labeling
            val asset = AssetDAO.getDao().get(Long.parseLong(newTag.substring(12)));
            asset.setRfidCode(newTag);
            AssetDAO.getDao().put(asset);
        }
        newTag = null;
        Toast.makeText(context, R.string.code_is_transferred_successfully, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onReaderStopped() {

    }

    private void refreshLabelingProgress(long id, String type) {
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
        bundle.putBooleanArray("filterLabelSelected", new boolean[]{false, true});
        val countedCount = AssetDAO.getDao().count(bundle);
        float percent = (float) countedCount / (float) count * 100F;
        int rPercent = Math.round(percent);
        view.labelingCardProgress.setProgress(rPercent);
    }

    @Override
    public void onData(@NonNull List<LabeledStateChange> data) {
        if (locationId != 0)
            refreshLabelingProgress(locationId, "location");
        else if (storageId != 0)
            refreshLabelingProgress(storageId, "storage");
        else if (personId != 0)
            refreshLabelingProgress(personId, "person");
        else if (assetTypeId != 0)
            refreshLabelingProgress(assetTypeId, "assetType");
    }

    @Override
    public String getQuery() {
        return ((ISearchableListFragment) adapter.getItem(view.tabLayout.getSelectedTabPosition())).getQuery();
    }

    @Override
    public void setQuery(String query) {
        ((ISearchableListFragment) adapter.getItem(view.tabLayout.getSelectedTabPosition())).setQuery(query);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (context.barcodeManager != null)
            context.barcodeManager.onPause();
        if (context.rfidManager != null && context.rfidManager.isDeviceOnline())
            context.rfidManager.onPause();
        if (labelingSub != null)
            labelingSub.cancel();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (context.barcodeManager != null)
            context.barcodeManager.onResume();
        if (context.rfidManager != null && context.rfidManager.isDeviceOnline())
            context.rfidManager.onResume(IRfidDeviceManager.OperationType.ReadWrite);
        context.actionButton.show();
        context.showHideFooter(false);
    }
}