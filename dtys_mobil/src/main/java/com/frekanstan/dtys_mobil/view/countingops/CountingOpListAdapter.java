package com.frekanstan.dtys_mobil.view.countingops;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.daimajia.numberprogressbar.NumberProgressBar;
import com.frekanstan.asset_management.app.assets.AssetTypeIconFinder;
import com.frekanstan.asset_management.app.locations.LocationIconFinder;
import com.frekanstan.asset_management.data.locations.ELocationType;
import com.frekanstan.asset_management.data.people.EPersonType;
import com.frekanstan.asset_management.view.widgets.CircularTextView;
import com.frekanstan.dtys_mobil.R;
import com.frekanstan.dtys_mobil.app.assets.AssetDAO;
import com.frekanstan.dtys_mobil.data.CountingOp;
import com.frekanstan.dtys_mobil.view.MainActivity;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;

import java.io.File;

import lombok.val;
import lombok.var;

import static com.frekanstan.asset_management.view.MainActivityBase.dateTimeFormat;
import static com.frekanstan.asset_management.view.MainActivityBase.personPhotosFolder;

public class CountingOpListAdapter extends PagedListAdapter<CountingOp, CountingOpListAdapter.CountingOpListViewHolder>
{
    private MainActivity context;
    CountingOp lastClickedItem;
    private AssetDAO assetDAO;
    private Bundle args;
    private AssetTypeIconFinder finder;

    CountingOpListAdapter(MainActivity context, Bundle args) {
        super(DIFF_CALLBACK);
        this.context = context;
        assetDAO = AssetDAO.getDao();
        this.args = args;
        finder = new AssetTypeIconFinder(context);
    }

    private static final DiffUtil.ItemCallback<CountingOp> DIFF_CALLBACK = new DiffUtil.ItemCallback<CountingOp>() {
        @Override
        public boolean areItemsTheSame(CountingOp oldItem, CountingOp newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull CountingOp oldItem, @NonNull CountingOp newItem) {
            return oldItem.countedAssets.size() == newItem.countedAssets.size();
        }
    };

    @NonNull
    @Override
    public CountingOpListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        var view = LayoutInflater.from(parent.getContext()).inflate(R.layout.countingop_card, parent, false);
        return new CountingOpListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final CountingOpListViewHolder holder, final int position) {
        final CountingOp countingOp = getItem(position);
        if (countingOp != null)
            holder.bindTo(countingOp);
        else
            holder.clear();
    }

    class CountingOpListViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView countingOpImage;
        TextView definition, creationTime;
        NumberProgressBar completionP;
        CircularTextView assetCount;

        CountingOpListViewHolder(View itemView) {
            super(itemView);
            countingOpImage = itemView.findViewById(R.id.countingop_card_image);
            definition = itemView.findViewById(R.id.countingop_card_definition);
            creationTime = itemView.findViewById(R.id.countingop_card_creation_time);
            completionP = itemView.findViewById(R.id.countingop_card_counting_progress);
            assetCount = itemView.findViewById(R.id.countingop_card_asset_count);

            itemView.setOnClickListener(this);
        }

        void bindTo(CountingOp countingOp) {
            var bundle = new Bundle();
            if (countingOp.getRelatedTypeId() != 0) {
                definition.setText(countingOp.getRelatedType().getDefinition());
                countingOpImage.setImageResource(finder.findIconId(countingOp.getRelatedType().getAssetCode()));
                bundle.putLong("assetTypeId", countingOp.getRelatedType().getId());
            }
            else if (countingOp.getRelatedPersonId() != 0) {
                definition.setText(countingOp.getRelatedPerson().getNameSurname());
                final File imgFile = new File(personPhotosFolder + File.separator + countingOp.getRelatedPerson().getId() + ".jpg");
                if (imgFile.exists()) {
                    countingOpImage.setPadding(0, 0, 0, 0);
                    Picasso.get().load(imgFile)
                            .resize(128, 128)
                            .centerCrop()
                            .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
                            .into(countingOpImage);
                } else {
                    if (countingOp.getRelatedPerson().getPersonType() == EPersonType.Recorder)
                        countingOpImage.setImageResource(R.drawable.recorder_gray_96px);
                    else
                        countingOpImage.setImageResource(R.drawable.person_gray_96px);
                }
                bundle.putLong("personId", countingOp.getRelatedPerson().getId());
            }
            else if (countingOp.getRelatedLocationId() != 0) {
                definition.setText(countingOp.getRelatedLocation().getName());
                if (countingOp.getRelatedLocation().getLocationType() == ELocationType.Warehouse) {
                    countingOpImage.setImageResource(R.drawable.database_gray_96px);
                    bundle.putLong("storageId", countingOp.getRelatedLocation().getId());
                }
                else {
                    countingOpImage.setImageResource(LocationIconFinder.findLocationIconId(countingOp.getRelatedLocation().getName()));
                    bundle.putLong("locationId", countingOp.getRelatedLocation().getId());
                }
            }
            else {
                definition.setText(context.getString(R.string.general_counting));
                countingOpImage.setImageResource(R.drawable.globe_gray_96px);
            }
            creationTime.setText(dateTimeFormat.format(countingOp.getCreationTime()));
            val count = assetDAO.count(bundle);
            assetCount.setSolidColor("#2962FF");
            assetCount.setText(String.valueOf(countingOp.countedAssets.size()));
            //bundle.putBooleanArray("filterCountingSelected", new boolean[]{false, true});
            int rPercent = Math.round((float) countingOp.countedAssets.size() / (float) count * 100F);
            completionP.setProgress(rPercent);
            if (rPercent == 100)
                itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.green_200));
            else if (rPercent > 50)
                itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.green_100));
            else if (rPercent > 0)
                itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.yellow_100));
            else if (rPercent == 0)
                itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
        }

        void clear() {
            definition.setText("");
            creationTime.setText("");
        }

        @Override
        public void onClick(View v) {
            lastClickedItem = getItem(getAdapterPosition());
            val bundle = new Bundle();
            bundle.putLong("countingOpId", lastClickedItem.getId());
            bundle.putLong("assetTypeId", lastClickedItem.getRelatedTypeId());
            bundle.putLong("personId", lastClickedItem.getRelatedPersonId());
            if (lastClickedItem.getRelatedLocationId() != 0) {
                if (lastClickedItem.getRelatedLocation().getLocationType() == ELocationType.Warehouse)
                    bundle.putLong("storageId", lastClickedItem.getRelatedLocationId());
                else
                    bundle.putLong("locationId", lastClickedItem.getRelatedLocationId());
            }
            context.nav.navigate(R.id.action_countingOpListFragment_to_countingTabsFragment, bundle);
        }
    }

    @Override
    public int getItemCount() {
        return super.getItemCount();
    }
}