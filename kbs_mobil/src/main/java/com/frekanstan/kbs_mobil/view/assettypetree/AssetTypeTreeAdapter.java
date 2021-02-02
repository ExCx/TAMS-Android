package com.frekanstan.kbs_mobil.view.assettypetree;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.daimajia.numberprogressbar.NumberProgressBar;
import com.frekanstan.asset_management.app.assets.AssetTypeIconFinder;
import com.frekanstan.asset_management.view.widgets.CircularTextView;
import com.frekanstan.kbs_mobil.R;
import com.frekanstan.kbs_mobil.view.MainActivity;

import lombok.val;
import lombok.var;
import tellh.com.recyclertreeview_lib.TreeNode;
import tellh.com.recyclertreeview_lib.TreeViewBinder;

public class AssetTypeTreeAdapter extends TreeViewBinder<AssetTypeTreeAdapter.ViewHolder> {

    private MainActivity mContext;
    private AssetTypeIconFinder finder;
    private Bundle args;

    public AssetTypeTreeAdapter(MainActivity context, Bundle args) {
        mContext = context;
        finder = new AssetTypeIconFinder(mContext);
        this.args = args;
    }

    @Override
    public ViewHolder provideViewHolder(View itemView) {
        return new ViewHolder(itemView);
    }

    public class ViewHolder extends TreeViewBinder.ViewHolder implements View.OnLongClickListener {
        TextView definition, assetCode, id;
        ImageView typeIcon, arrow;
        CircularTextView assetCount;
        LinearLayout cardBody;
        NumberProgressBar completionP;

        public ViewHolder(View rootView) {
            super(rootView);
            this.id = rootView.findViewById(R.id.asset_type_card_id);
            this.definition = rootView.findViewById(R.id.asset_type_card_definition);
            this.assetCode = rootView.findViewById(R.id.asset_type_card_asset_code);
            this.typeIcon = rootView.findViewById(R.id.asset_type_card_image);
            this.assetCount = rootView.findViewById(R.id.asset_type_card_asset_count);
            this.cardBody = rootView.findViewById(R.id.asset_type_card_body_layout);
            this.completionP = rootView.findViewById(R.id.asset_type_card_counting_progress);
            this.arrow = rootView.findViewById(R.id.tree_arrow);

            itemView.setOnLongClickListener(this);
        }

        @Override
        public boolean onLongClick(View v) {
            val id = (TextView)(((LinearLayout)v).getChildAt(0));
            var bundle = new Bundle();
            bundle.putLong("assetTypeId", Long.parseLong(id.getText().toString()));
            val op = args.getString("operation");
            assert op != null;
            switch (op) {
                case "counting":
                    mContext.nav.navigate(R.id.countingTabsFragment, bundle);
                    break;
                case "labeling":
                    mContext.nav.navigate(R.id.labelingTabsFragment, bundle);
                    break;
            }
            return true;
        }

        public ImageView getArrow() {
            return arrow;
        }
    }

    @Override
    public void bindView(ViewHolder holder, int position, TreeNode node) {
        var layout = (AssetTypeLayout) node.getContent();
        val assetType = layout.getAssetType();
        holder.id.setText(String.valueOf(assetType.getId()));
        holder.typeIcon.setImageResource(finder.findIconId(assetType.getAssetCode()));
        holder.definition.setText(assetType.getDefinition());
        holder.assetCode.setText(assetType.getAssetCode());
        holder.assetCount.setSolidColor("#2962FF");
        holder.assetCount.setText(String.valueOf(layout.getAssetCount()));
        float percent = 0;
        if (args.getString("operation").equals("counting"))
            percent = (float)layout.getCountedCount() / (float)layout.getAssetCount() * 100F;
        else if (args.getString("operation").equals("labeling"))
            percent = (float)layout.getLabeledCount() / (float)layout.getAssetCount() * 100F;
        int rPercent = Math.round(percent);
        holder.completionP.setProgress(rPercent);
        if (AssetTypeTreeFragment.getHighlightedId() == assetType.getId())
            holder.cardBody.setBackgroundColor(ContextCompat.getColor(mContext, R.color.blue_100));
        else
            holder.cardBody.setBackgroundColor(ContextCompat.getColor(mContext, R.color.white));
    }

    @Override
    public int getLayoutId() {
        return R.layout.asset_type_card;
    }
}