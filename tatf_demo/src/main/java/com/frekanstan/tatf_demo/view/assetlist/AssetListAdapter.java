package com.frekanstan.tatf_demo.view.assetlist;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.frekanstan.asset_management.app.ISelectableList;
import com.frekanstan.asset_management.app.SelectableListAdapter;
import com.frekanstan.asset_management.app.assets.AssetTypeIconFinder;
import com.frekanstan.asset_management.app.helpers.PictureTaker;
import com.frekanstan.tatf_demo.R;
import com.frekanstan.tatf_demo.app.assets.AssetDAO;
import com.frekanstan.tatf_demo.app.assets.AssetRepository;
import com.frekanstan.tatf_demo.app.labeling.LabelPrinter;
import com.frekanstan.tatf_demo.app.tracking.CountingOpDAO;
import com.frekanstan.tatf_demo.data.Asset;
import com.frekanstan.tatf_demo.view.MainActivity;
import com.frekanstan.tatf_demo.view.labeling.LabelingTabsFragment;
import com.google.common.base.Strings;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;

import lombok.Getter;
import lombok.val;
import lombok.var;

import static com.frekanstan.asset_management.view.MainActivityBase.assetPhotosFolder;
import static com.frekanstan.asset_management.view.helpers.LayoutHelpers.setTextWithDrawable;

public class AssetListAdapter extends PagedListAdapter<Asset, AssetListAdapter.AssetListViewHolder> {

    private MainActivity context;
    private AssetListFragment fragment;
    private AssetListViewModel viewModel;
    private String listType, operation;
    private long countingOpId;
    private AssetTypeIconFinder finder;
    @Getter
    private ISelectableList selectableList;
    private AssetDAO dao;

    AssetListAdapter(MainActivity context, AssetListFragment fragment) {
        super(DIFF_CALLBACK);
        this.context = context;
        this.fragment = fragment;
        listType = fragment.requireArguments().getString("listType");
        operation = fragment.requireArguments().getString("operation");
        countingOpId = fragment.requireArguments().getLong("countingOpId");
        viewModel = new ViewModelProvider(context).get(listType, AssetListViewModel.class);
        finder = new AssetTypeIconFinder(this.context);
        dao = AssetDAO.getDao();
    }

    private static final DiffUtil.ItemCallback<Asset> DIFF_CALLBACK = new DiffUtil.ItemCallback<Asset>() {
        @Override
        public boolean areItemsTheSame(Asset oldItem, Asset newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Asset oldItem, @NonNull Asset newItem) {
            return Objects.equals(oldItem.getRemoteId(), newItem.getRemoteId()) &&
                    Objects.equals(oldItem.getLastControlTime(), newItem.getLastControlTime()) &&
                    Objects.equals(oldItem.getLabelingDateTime(), newItem.getLabelingDateTime()) &&
                    oldItem.getAssetTypeId() == newItem.getAssetTypeId() &&
                    Objects.equals(oldItem.getAssignedLocationId(), newItem.getAssignedLocationId()) &&
                    Objects.equals(oldItem.getAssignedPersonId(), newItem.getAssignedPersonId());
        }
    };

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        val callback = new AssetListActionModeCallback(context, this);
        selectableList = new SelectableListAdapter<>(context, this, recyclerView, dao, callback);
    }

    @NonNull
    @Override
    public AssetListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.asset_card, parent, false);
        return new AssetListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final AssetListViewHolder holder, final int position) {
        final Asset asset = getItem(position);
        if (asset != null)
            holder.bindTo(asset);
        else
            holder.clear();
    }

    class AssetListViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        ImageView assetImage, countingStatus;
        TextView typeDefinition, assignedPerson, brand, model, assignedLocation;

        AssetListViewHolder(View itemView) {
            super(itemView);
            assetImage = itemView.findViewById(R.id.asset_image);
            typeDefinition = itemView.findViewById(R.id.asset_card_type_definition);
            assignedPerson = itemView.findViewById(R.id.asset_card_assigned_person);
            assignedLocation = itemView.findViewById(R.id.asset_card_assigned_location);
            brand = itemView.findViewById(R.id.asset_card_brand);
            model = itemView.findViewById(R.id.asset_card_model);
            countingStatus = itemView.findViewById(R.id.asset_card_status_image);

            assetImage.setOnClickListener(this);
            countingStatus.setOnClickListener(this);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        void bindTo(Asset asset) {
            //type or biotype
            typeDefinition.setText(asset.getAssetTypeDefinition());

            if (asset.getBrandNameId() != 0)
                setTextWithDrawable(context, brand, asset.getBrandNameDefinition());
            else
                setTextWithDrawable(context, brand, "");
            if (asset.getModelNameId() != 0)
                setTextWithDrawable(context, model, asset.getModelNameDefinition());
            else
                setTextWithDrawable(context, model, "");

            //assignment details
            if (asset.getAssignedLocationId() != 0)
                setTextWithDrawable(context, assignedLocation, asset.getAssignedLocationName());
            else
                setTextWithDrawable(context, assignedLocation, "");
            if (asset.getAssignedPersonId() != 0)
                setTextWithDrawable(context, assignedPerson, asset.getAssignedPersonNameSurname());
            else
                setTextWithDrawable(context, assignedPerson, "");
            var ac = dao.getAssignmentChange(asset.getId());
            if (ac != null) { //has pending assignment
                if (ac.getLocationId() != 0)
                    assignedLocation.setTextColor(Color.RED);
                if (ac.getPersonId() != 0)
                    assignedPerson.setTextColor(Color.RED);
            } else {
                assignedLocation.setTextColor(Color.BLACK);
                assignedPerson.setTextColor(Color.BLACK);
            }

            //asset photo or icon
            final File imgFile = new File(assetPhotosFolder + File.separator + asset.getAssetCode() + "-0.jpg");
            if (imgFile.exists()) {
                assetImage.setPadding(0, 0, 0, 0);
                Picasso.get().load(imgFile)
                        .resize(128, 128)
                        .centerCrop()
                        .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
                        .into(assetImage);
            } else
                assetImage.setImageResource(R.drawable.cat_icon_255); //TODO: FINDER

            switch (operation) {
                case "counting":
                    if (!Strings.isNullOrEmpty(listType) && listType.equals("foreign"))
                        countingStatus.setImageResource(R.drawable.location_update_48px_orange);
                    else if (asset.getLastControlTime() == null)
                        countingStatus.setImageResource(R.drawable.notcounted_gray_48);
                    else
                        countingStatus.setImageResource(R.drawable.tick_green_48);
                    break;
                case "labeling":
                    if (asset.getLabelingDateTime() == null)
                        countingStatus.setImageResource(R.drawable.no_tag_48px_red);
                    else {
                        var cal = Calendar.getInstance();
                        cal.setTime(asset.getLabelingDateTime());
                        if (cal.get(Calendar.YEAR) == 1986)
                            countingStatus.setImageResource(R.drawable.tag_48px_orange);
                        else
                            countingStatus.setImageResource(R.drawable.tag_48px_green);
                    }
                    break;
                case "locate":
                    countingStatus.setImageResource(R.drawable.sensor_96px);
                    if (viewModel.getLastClickedAsset().getId() == asset.getId())
                        countingStatus.setColorFilter(context.getResources().getColor(R.color.light_green_A700));
                    else
                        countingStatus.setColorFilter(context.getResources().getColor(R.color.grey_700));
                    break;
            }

            if (selectableList.getActionMode() == null)
                itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
            else {
                if (selectableList.getSelectedIds().contains(asset.getId()))
                    itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.light_green_200));
                else
                    itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
            }
        }

        void clear() {
            typeDefinition.setText("");
            assignedPerson.setText("");
            brand.setText("");
            model.setText("");
            assignedLocation.setText("");
        }

        @Override
        public void onClick(View v) {
            val asset = getItem(getAdapterPosition());
            assert asset != null;
            viewModel.setLastClickedAsset(asset);
            if (selectableList.getActionMode() != null)
                selectableList.selectItem(asset.getId(), false, listType);
            else if (v.getId() == countingStatus.getId()) {
                switch (operation) {
                    case "counting":
                        if (!Strings.isNullOrEmpty(listType) && listType.equals("foreign")) {
                            var alertDialog = new AlertDialog.Builder(context)
                                    .setNeutralButton(context.getString(R.string.remove_asset_from_foreign_list), (dialog, id) -> {
                                        if (fragment.requireArguments().getLong("locationId") != 0)
                                            asset.setLocationToAssignId(0);
                                        else if (fragment.requireArguments().getLong("personId") != 0)
                                            asset.setPersonToAssignId(0);
                                        AssetDAO.getDao().put(asset);
                                    });
                            if (asset.getPersonToAssignId() != 0 && asset.getPersonToAssignId() == fragment.requireArguments().getLong("personId")) {
                                if (asset.getAssignedPersonId() != 0) //birine aitse
                                    alertDialog.setMessage(
                                            String.format(context.getString(R.string.alert_asset_is_on_another_person),
                                                    asset.getAssignedPerson().getNameSurname()));
                                else //depodaysa
                                    alertDialog.setMessage(R.string.alert_asset_is_not_assigned_to_person);
                                alertDialog
                                        .setPositiveButton(context.getString(R.string.assign_title), (dialog, id) -> {
                                            var bundle = new Bundle();
                                            bundle.putLongArray("assetIds", new long[]{asset.getId()});
                                            bundle.putLong("personId", asset.getPersonToAssignId());
                                            context.nav.navigate(R.id.assignmentDialogFragment, bundle);
                                        })
                                        .setNegativeButton(context.getString(R.string.goto_person), (dialog, id) -> {
                                            var bundle = new Bundle();
                                            bundle.putLong("assetId", asset.getId());
                                            bundle.putLong("personId", asset.getAssignedPersonId());
                                            context.nav.navigate(R.id.countingTabsFragment, bundle);
                                        }).show();

                            } else if (asset.getLocationToAssignId() != 0 && asset.getLocationToAssignId() == fragment.requireArguments().getLong("locationId")) {
                                if (asset.getAssignedLocationId() != 0) //bir yere aitse
                                    alertDialog.setMessage(String.format(context.getString(R.string.alert_asset_is_on_another_location),
                                            asset.getAssignedLocation().getName()));
                                else //yeri belirsizse
                                    alertDialog.setMessage(R.string.alert_asset_is_not_assigned_to_location);
                                alertDialog
                                        .setPositiveButton(context.getString(R.string.assign_title), (dialog, id) -> {
                                            var bundle = new Bundle();
                                            bundle.putLongArray("assetIds", new long[]{asset.getId()});
                                            bundle.putLong("locationId", asset.getLocationToAssignId());
                                            context.nav.navigate(R.id.assignmentDialogFragment, bundle);
                                        })
                                        .setNegativeButton(context.getString(R.string.goto_unit), (dialog, id) -> {
                                            var bundle = new Bundle();
                                            bundle.putLong("assetId", asset.getId());
                                            bundle.putLong("locationId", asset.getAssignedLocationId());
                                            context.nav.navigate(R.id.countingTabsFragment, bundle);
                                        }).show();
                            }
                        } else {
                            var alertDialog = new AlertDialog.Builder(context)
                                    .setPositiveButton(context.getString(R.string.yes), (dialog, id) -> {
                                        val op = CountingOpDAO.getDao().get(countingOpId);
                                        if (asset.getTempCounted()) {
                                            dao.setCountedStateChange(asset.getId(), asset.getLastControlTime(), false, countingOpId);
                                            asset.setTempCounted(false);
                                            op.countedAssets.remove(asset);
                                            AssetRepository.scannedTags.remove(asset.getRfidCode());
                                        }
                                        else {
                                            dao.setCountedStateChange(asset.getId(), asset.getLastControlTime(), true, countingOpId);
                                            asset.setTempCounted(true);
                                            op.countedAssets.add(asset);
                                            AssetRepository.scannedTags.add(asset.getRfidCode());
                                        }
                                        dao.put(asset);
                                        CountingOpDAO.getDao().put(op);
                                    })
                                    .setNegativeButton(context.getString(R.string.no), (dialog, id) ->
                                            dialog.dismiss());
                            if (!asset.getTempCounted())
                                alertDialog.setMessage(R.string.alert_asset_will_be_marked_as_counted);
                            else
                                alertDialog.setMessage(R.string.alert_asset_will_be_marked_as_not_counted);
                            alertDialog.show();
                        }
                        break;
                    case "labeling":
                        ArrayList<Long> idList = new ArrayList<>();
                        idList.add(asset.getId());
                        if (context.rfidManager != null && context.rfidManager.isDeviceOnline()) { //rfid varsa
                            val builder = new AlertDialog.Builder(context);
                            if (asset.getLabelingDateTime() == null)
                                builder.setMessage(R.string.clicked_label_not_printed_single_rfid);
                            else
                                builder.setMessage(context.getString(R.string.clicked_label_already_printed_single_rfid, asset.getAssetCode()));
                            builder.setPositiveButton(context.getString(R.string.transfer_to_rfid_tag), (dialog, id) -> {
                                LabelingTabsFragment.setNewTag("C05E" + String.format(Locale.US, "%020d", asset.getId()));
                                Toast.makeText(context, R.string.scan_the_rfid_tag_to_overwrite, Toast.LENGTH_SHORT).show();
                            })
                                    .setNeutralButton(context.getString(R.string.print_qrcode), (dialog, id) ->
                                            new LabelPrinter(context, idList, "asset", null).print())
                                    .setNegativeButton(context.getString(R.string.mark_as_labeled), (dialog, id) ->
                                            AssetDAO.getDao().setLabeledStateChange(asset.getId(), true))
                                    .show();
                        } else {
                            val builder = new AlertDialog.Builder(context);
                            if (asset.getLabelingDateTime() == null)
                                builder.setMessage(R.string.clicked_label_not_printed_single)
                                        .setNeutralButton(context.getString(R.string.mark_as_labeled), (dialog, id) ->
                                                AssetDAO.getDao().setLabeledStateChange(asset.getId(), true));
                            else
                                builder.setMessage(R.string.clicked_label_already_printed_single)
                                        .setNeutralButton(context.getString(R.string.mark_as_not_labeled), (dialog, id) ->
                                                AssetDAO.getDao().setLabeledStateChange(asset.getId(), true));

                            builder.setPositiveButton(context.getString(R.string.print_label), (dialog, id) ->
                                    new LabelPrinter(context, idList, "asset", null).print())
                                    .setNegativeButton(context.getString(R.string.view_asset), (dialog, id) -> {
                                        var bundle = new Bundle();
                                        bundle.putLong("assetId", asset.getId());
                                        context.nav.navigate(R.id.assetDetailsFragment, bundle);
                                    })
                                    .show();
                        }
                        break;
                    case "locate":
                        notifyDataSetChanged();
                        break;
                }
            } else if (v.getId() == assetImage.getId()) {
                final File imgFile = new File(assetPhotosFolder + File.separator + asset.getAssetCode() + "-0.jpg");
                if (imgFile.exists()) {
                    var bundle = new Bundle();
                    bundle.putString("mAssetCode", asset.getAssetCode());
                    context.nav.navigate(R.id.assetImagePagerFragment, bundle);
                } else
                    PictureTaker.dispatchTakePictureIntent(imgFile, fragment);
            } else {
                var bundle = new Bundle();
                bundle.putLong("assetId", asset.getId());
                context.nav.navigate(R.id.assetDetailsFragment, bundle);
            }
        }

        @Override
        public boolean onLongClick(View v) {
            viewModel.setLastClickedAsset(getItem(getAdapterPosition()));
            selectableList.selectItem(viewModel.getLastClickedAsset().getId(), false, listType);
            return true;
        }
    }

    @Override
    public int getItemCount() {
        return super.getItemCount();
    }
}