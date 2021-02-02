package com.frekanstan.gateway_mobil.view.gateway;

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
import com.frekanstan.asset_management.view.widgets.CircularTextView;
import com.frekanstan.gateway_mobil.R;
import com.frekanstan.gateway_mobil.app.assets.AssetDAO;
import com.frekanstan.gateway_mobil.data.AssetType;
import com.frekanstan.gateway_mobil.data.Asset_;
import com.frekanstan.gateway_mobil.data.Inventory;
import com.frekanstan.gateway_mobil.view.MainActivity;

import java.util.ArrayList;
import java.util.Locale;

import lombok.val;
import lombok.var;

public class ReceiveReportAdapter extends PagedListAdapter<AssetType, ReceiveReportAdapter.ReceiveReportViewHolder>
{
    private final MainActivity context;
    private final AssetTypeIconFinder finder;
    private final long[] assetIds;
    private final ArrayList<Inventory> inventoryList;

    ReceiveReportAdapter(MainActivity context, Bundle args, ArrayList<Inventory> inventoryList) {
        super(DIFF_CALLBACK);
        this.context = context;
        this.inventoryList = inventoryList;
        assetIds = args.getLongArray("assetIds");
        finder = new AssetTypeIconFinder(context);
    }

    private static final DiffUtil.ItemCallback<AssetType> DIFF_CALLBACK = new DiffUtil.ItemCallback<AssetType>() {
        @Override
        public boolean areItemsTheSame(AssetType oldItem, AssetType newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull AssetType oldItem, @NonNull AssetType newItem) {
            return oldItem.getId() == newItem.getId();
        }
    };

    @NonNull
    @Override
    public ReceiveReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        var view = LayoutInflater.from(parent.getContext()).inflate(R.layout.counting_report_type_card, parent, false);
        return new ReceiveReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ReceiveReportViewHolder holder, final int position) {
        final AssetType assetType = getItem(position);
        if (assetType != null)
            holder.bindTo(assetType);
        else
            holder.clear();
    }

    class ReceiveReportViewHolder extends RecyclerView.ViewHolder {
        ImageView assetTypeIcon;
        TextView definition, productCount;
        NumberProgressBar completionP;
        CircularTextView assetCount;

        ReceiveReportViewHolder(View itemView) {
            super(itemView);
            assetTypeIcon = itemView.findViewById(R.id.asset_type_card_image);
            definition = itemView.findViewById(R.id.asset_type_card_definition);
            productCount = itemView.findViewById(R.id.asset_type_card_product_count);
            completionP = itemView.findViewById(R.id.asset_type_card_counting_progress);
            assetCount = itemView.findViewById(R.id.asset_type_card_asset_count);
        }

        void bindTo(AssetType assetType) {
            definition.setText(assetType.getDefinition());
            assetTypeIcon.setImageResource(finder.findIconId(assetType.getAssetCode()));
            val query = AssetDAO.getDao().getBox().query().equal(Asset_.assetTypeId, assetType.getId()).and().in(Asset_.id, assetIds).build();
            val qSum = query.property(Asset_.quantity).sum();
            var invQ = 0;
            if (inventoryList != null) {
                for (val inv : inventoryList) {
                    if (inv.getTypeRemoteId().equals(assetType.getRemoteId())) {
                        invQ = inv.getQuantity();
                        break;
                    }
                }
            }
            productCount.setText(String.format(Locale.ENGLISH, "%d/%d", qSum, invQ));
            assetCount.setSolidColor("#2962FF");
            assetCount.setText(String.valueOf(query.count()));
            if (invQ == 0 || qSum > invQ) {
                completionP.setProgress(invQ == 0 ? 0 : 100);
                itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.red_300));
                return;
            }
            val rPercent = Math.round((float) qSum / (float) invQ * 100F);
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
            productCount.setText("");
        }
    }

    @Override
    public int getItemCount() {
        return super.getItemCount();
    }
}