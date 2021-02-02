package com.frekanstan.kbs_mobil.view.locations;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;
import androidx.core.content.ContextCompat;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.daimajia.numberprogressbar.NumberProgressBar;
import com.frekanstan.asset_management.app.locations.LocationIconFinder;
import com.frekanstan.asset_management.data.locations.ELocationType;
import com.frekanstan.asset_management.view.widgets.CircularTextView;
import com.frekanstan.kbs_mobil.R;
import com.frekanstan.kbs_mobil.app.assets.AssetDAO;
import com.frekanstan.kbs_mobil.app.labeling.LabelPrinter;
import com.frekanstan.kbs_mobil.app.locations.LocationDAO;
import com.frekanstan.kbs_mobil.data.Location;
import com.frekanstan.kbs_mobil.view.MainActivity;
import com.frekanstan.kbs_mobil.view.labeling.LabelingTabsFragment;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import lombok.val;
import lombok.var;

public class LocationListAdapter extends PagedListAdapter<Location, LocationListAdapter.LocationListViewHolder>
{
    private MainActivity context;
    Location lastClickedItem;
    private ActionMode actionMode;
    private ArrayList<Long> selectedIds;
    private AssetDAO assetDAO;
    private Bundle args;

    LocationListAdapter(MainActivity context, Bundle args) {
        super(DIFF_CALLBACK);
        this.context = context;
        selectedIds = new ArrayList<>();
        assetDAO = AssetDAO.getDao();
        this.args = args;
    }

    private static final DiffUtil.ItemCallback<Location> DIFF_CALLBACK = new DiffUtil.ItemCallback<Location>() {
        @Override
        public boolean areItemsTheSame(Location oldItem, Location newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Location oldItem, @NonNull Location newItem) {
            return oldItem.getName().equals(newItem.getName());
        }
    };

    @NonNull
    @Override
    public LocationListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (args.getString("listType", "").isEmpty())
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.location_card, parent, false);
        else
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.location_labeling_card, parent, false);
        return new LocationListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final LocationListViewHolder holder, final int position) {
        final Location location = getItem(position);
        if (location != null)
            holder.bindTo(location);
        else
            holder.clear();
    }

    class LocationListViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        ImageView locationIcon, labelingState;
        TextView name, remoteId;
        NumberProgressBar completionP;
        CircularTextView assetCount;

        LocationListViewHolder(View itemView) {
            super(itemView);
            locationIcon = itemView.findViewById(R.id.location_card_icon);
            name = itemView.findViewById(R.id.location_card_name);
            remoteId = itemView.findViewById(R.id.location_card_remote_id);
            if (args.getString("listType", "").isEmpty()) {
                completionP = itemView.findViewById(R.id.location_card_counting_progress);
                assetCount = itemView.findViewById(R.id.location_card_asset_count);
            }
            else {
                labelingState = itemView.findViewById(R.id.location_card_status_image);
                labelingState.setOnClickListener(this);
            }

            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        void bindTo(Location location) {
            name.setText(location.getName());
            remoteId.setText(location.getLocationCode());
            var bundle = new Bundle();
            if (location.getLocationType() == ELocationType.Warehouse) {
                locationIcon.setImageResource(R.drawable.database_gray_96px);
                bundle.putLong("storageId", location.getId());
            }
            else {
                locationIcon.setImageResource(LocationIconFinder.findLocationIconId(location.getName()));
                bundle.putLong("locationId", location.getId());
            }

            if (args.getString("listType", "").isEmpty()) {
                val count = assetDAO.count(bundle);
                assetCount.setSolidColor("#2962FF");
                assetCount.setText(String.valueOf(count));
                if (args.getString("operation").equals("counting"))
                    bundle.putBooleanArray("filterCountingSelected", new boolean[]{false, true});
                else if (args.getString("operation").equals("labeling"))
                    bundle.putBooleanArray("filterLabelSelected", new boolean[]{false, true});
                int rPercent = Math.round((float) assetDAO.count(bundle) / (float) count * 100F);
                completionP.setProgress(rPercent);
                if (actionMode != null) {
                    if (selectedIds.contains(location.getId()))
                        itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.blue_100));
                    else
                        itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
                } else if (rPercent == 100)
                    itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.green_200));
                else if (rPercent > 50)
                    itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.green_100));
                else if (rPercent > 0)
                    itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.yellow_100));
                else if (rPercent == 0)
                    itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
            }
            else {
                if (location.getLabelingDateTime() == null)
                    labelingState.setImageResource(R.drawable.no_tag_48px_red);
                else {
                    var cal = Calendar.getInstance();
                    cal.setTime(location.getLabelingDateTime());
                    if (cal.get(Calendar.YEAR) == 1986)
                        labelingState.setImageResource(R.drawable.tag_48px_orange);
                    else
                        labelingState.setImageResource(R.drawable.tag_48px_green);
                }
                if (actionMode != null) {
                    if (selectedIds.contains(location.getId()))
                        itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.light_green_200));
                    else
                        itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
                }
            }
        }

        void clear() {
            name.setText("");
            remoteId.setText("");
        }

        @Override
        public void onClick(View v) {
            lastClickedItem = getItem(getAdapterPosition());
            if (actionMode == null) {
                if (!args.getString("listType", "").isEmpty() && v.getId() == labelingState.getId()) //location labeling and clicked icon
                {
                    val location = LocationDAO.getDao().get(lastClickedItem.getId());
                    ArrayList<Long> idList = new ArrayList<>();
                    idList.add(location.getId());
                    if (context.rfidManager != null && context.rfidManager.isDeviceOnline()) { //rfid varsa
                        val builder = new AlertDialog.Builder(context);
                        if (location.getLabelingDateTime() == null)
                            builder.setMessage(R.string.clicked_label_not_printed_single_rfid);
                        else
                            builder.setMessage(context.getString(R.string.clicked_label_already_printed_single_rfid, String.valueOf(location.getLocationCode())));
                        builder.setPositiveButton(context.getString(R.string.transfer_to_rfid_tag), (dialog, id) -> {
                            LabelingTabsFragment.setNewTag("C05E1" + String.format(Locale.US, "%019d", location.getId()));
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
                            builder.setMessage(R.string.clicked_label_not_printed_single)
                                    .setNeutralButton(context.getString(R.string.mark_as_labeled), (dialog, id) -> {
                                        location.setAsLabeled(true);
                                        LocationDAO.getDao().put((Location) location);
                                        LocationDAO.getDao().setLabeledStateChange(location.getId(), location.getLabelingDateTime());
                                    });
                        else
                            builder.setMessage(R.string.clicked_label_already_printed_single)
                                    .setNeutralButton(context.getString(R.string.mark_as_not_labeled), (dialog, id) -> {
                                        location.setAsLabeled(false);
                                        LocationDAO.getDao().put((Location) location);
                                        LocationDAO.getDao().setLabeledStateChange(location.getId(), null);
                                    });

                        builder.setPositiveButton(context.getString(R.string.print_label), (dialog, id) ->
                                new LabelPrinter(context, idList, "location", null).print())
                                .setNegativeButton(context.getString(R.string.cancel_title), (dialog, id) -> dialog.dismiss())
                                .show();
                    }
                }
                else {
                    val bundle = new Bundle();
                    if (lastClickedItem.getLocationType() == ELocationType.Warehouse)
                        bundle.putLong("storageId", lastClickedItem.getId());
                    else
                        bundle.putLong("locationId", lastClickedItem.getId());
                    val op = args.getString("operation");
                    assert op != null;
                    switch (op) {
                        case "counting":
                            context.nav.navigate(R.id.action_locationListFragment_to_countingTabsFragment, bundle);
                            break;
                        case "labeling":
                            context.nav.navigate(R.id.action_locationListFragment_to_labelingTabsFragment, bundle);
                            break;
                    }
                }
            }
            else
                selectItem(lastClickedItem.getId());
        }

        @Override
        public boolean onLongClick(View v) {
            lastClickedItem = getItem(getAdapterPosition());
            if (lastClickedItem != null)
                selectItem(lastClickedItem.getId());
            return true;
        }
    }

    private void selectItem(Long id) {
        if (actionMode == null) {
            selectedIds.add(id);
            actionMode = context.startSupportActionMode(actionModeCallback);
        }
        else {
            if (selectedIds.contains(id)) {
                selectedIds.remove(id);
                if (selectedIds.size() == 0)
                    actionMode.finish();
            }
            else
                selectedIds.add(id);
        }
        if (actionMode != null)
            actionMode.setTitle(context.getString(R.string.selected_location_count, selectedIds.size()));
        notifyDataSetChanged();
    }

    private ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.asset_actions, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.print) {
                new LabelPrinter(context, selectedIds, "location", null).print();
                mode.finish();
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            actionMode = null;
            selectedIds = new ArrayList<>();
            notifyDataSetChanged();
        }
    };

    @Override
    public int getItemCount() {
        return super.getItemCount();
    }
}