package com.frekanstan.dtys_mobil.view.assetlist;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;

import com.frekanstan.dtys_mobil.R;
import com.frekanstan.dtys_mobil.app.assets.AssetDAO;
import com.frekanstan.dtys_mobil.app.labeling.LabelPrinter;
import com.frekanstan.dtys_mobil.view.MainActivity;
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
        boolean isTagNoNull;
        val selectedIds = mAdapter.getSelectableList().getSelectedIds();
        val selectedItems = AssetDAO.getDao().getAll(selectedIds);
        //tag no control
        isTagNoNull = false;
        for (val asset : selectedItems) {
            if (asset.getRemoteId() == null) {
                isTagNoNull = true;
                break;
            }
        }
        if (isTagNoNull) {
            Toast.makeText(mContext, R.string.no_tag_dialog_message, Toast.LENGTH_LONG).show();
            return false;
        }
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