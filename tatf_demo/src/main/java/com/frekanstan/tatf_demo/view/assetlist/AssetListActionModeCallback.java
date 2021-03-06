package com.frekanstan.tatf_demo.view.assetlist;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;

import com.frekanstan.tatf_demo.R;
import com.frekanstan.tatf_demo.app.assets.AssetDAO;
import com.frekanstan.tatf_demo.app.labeling.LabelPrinter;
import com.frekanstan.tatf_demo.view.MainActivity;
import com.google.common.primitives.Longs;

import java.util.ArrayList;

import lombok.val;
import lombok.var;

public class AssetListActionModeCallback implements ActionMode.Callback
{
    private final MainActivity mContext;
    private final AssetListAdapter mAdapter;

    AssetListActionModeCallback(MainActivity mContext, AssetListAdapter mAdapter) {
        this.mContext = mContext;
        this.mAdapter = mAdapter;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(R.menu.assetlist_actions, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        val selectedIds = mAdapter.getSelectableList().getSelectedIds();
        val selectedItems = AssetDAO.getDao().getAll(selectedIds);
        int itemId = item.getItemId();
        if (itemId == R.id.print) {
            boolean isAlreadyPrinted = false;
            for (val asset : selectedItems) {
                if (asset.getLabelingDateTime() != null) {
                    isAlreadyPrinted = true;
                    break;
                }
            }
            if (isAlreadyPrinted) {
                new AlertDialog.Builder(mContext).setMessage(R.string.label_already_printed)
                        .setPositiveButton(mContext.getString(R.string.confirm), (dialog, id) -> {
                            new LabelPrinter(mContext, selectedIds, "asset", mAdapter).print();
                            mode.finish();
                        })
                        .setNegativeButton(mContext.getString(R.string.cancel_title), (dialog, id) -> dialog.dismiss())
                        .show();
            } else { //print labels
                new LabelPrinter(mContext, selectedIds, "asset", mAdapter).print();
                mode.finish();
            }
        } else if (itemId == R.id.assign) {
            var bundle = new Bundle();
            bundle.putLongArray("assetIds", Longs.toArray(selectedIds));
            mContext.nav.navigate(R.id.assignmentDialogFragment, bundle);
            return true;
        }
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        mAdapter.getSelectableList().setActionMode(null);
        mAdapter.getSelectableList().setSelectedIds(new ArrayList<>());
        mAdapter.notifyDataSetChanged();
    }
}