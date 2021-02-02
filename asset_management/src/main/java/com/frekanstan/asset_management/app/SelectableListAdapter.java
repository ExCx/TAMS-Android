package com.frekanstan.asset_management.app;

import android.os.Bundle;

import androidx.appcompat.view.ActionMode;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.frekanstan.asset_management.R;
import com.frekanstan.asset_management.data.IEntity;
import com.frekanstan.asset_management.view.MainActivityBase;

import java.util.ArrayList;

import lombok.Getter;
import lombok.Setter;
import lombok.val;

public class SelectableListAdapter<TEntity extends IEntity> implements ISelectableList
{
    private MainActivityBase mContext;
    private PagedListAdapter mAdapter;
    @Getter @Setter
    public ActionMode actionMode;
    private RecyclerView mRecyclerView;
    private final IDAO<TEntity> mDao;
    @Getter @Setter
    private ArrayList<Long> selectedIds;
    private ActionMode.Callback actionModeCallback;

    public SelectableListAdapter(MainActivityBase mContext, PagedListAdapter mAdapter, RecyclerView mRecyclerView, IDAO<TEntity> dao, ActionMode.Callback actionModeCallback)
    {
        this.mContext = mContext;
        this.mAdapter = mAdapter;
        this.mRecyclerView = mRecyclerView;
        mDao = dao;
        this.actionModeCallback = actionModeCallback;
        selectedIds = new ArrayList<>();
    }

    @Override
    public void selectItem(Long id, boolean isScanned, String listType) {
        if (actionMode == null) {
            selectedIds.add(id);
            actionMode = mContext.startSupportActionMode(actionModeCallback);
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
            actionMode.setTitle(mContext.getString(R.string.selected_count, selectedIds.size()));
        /*if (isScanned) {
            val position = mDao.getIndexOf(listType, id);
            mRecyclerView.scrollToPosition(position);
        }*/
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void selectAll(String listType, long locationId, long personId, long assetTypeId)
    {
        if (actionMode == null)
            actionMode = mContext.startSupportActionMode(actionModeCallback);
        selectedIds = new ArrayList<>();

        val input = new Bundle();
        input.putString("listType", listType);
        input.putLong("locationId", locationId);
        input.putLong("personId", personId);
        input.putLong("assetTypeId", assetTypeId);
        selectedIds.addAll(mDao.getAllIds(input));

        /*for (long n : mDao.getIdIndex(listType))
            selectedIds.add(n);*/

        actionMode.setTitle(mContext.getString(R.string.selected_count, selectedIds.size()));
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void clearSelection()
    {
        if (actionMode != null)
            actionMode.finish();
    }
}