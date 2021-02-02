package com.frekanstan.asset_management.app;

import androidx.appcompat.view.ActionMode;

import java.util.ArrayList;

public interface ISelectableList {
    void selectItem(Long id, boolean isScanned, String listType);

    void selectAll(String listType, long locationId, long personId, long assetTypeId);

    void clearSelection();

    ActionMode getActionMode();

    void setActionMode(ActionMode actionMode);

    ArrayList<Long> getSelectedIds();

    void setSelectedIds(ArrayList<Long> selectedIds);
}
