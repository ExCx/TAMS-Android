package com.frekanstan.asset_management.app.multitenancy;

import com.frekanstan.asset_management.data.GetAllInput;

import java.util.List;

public class TenantGetAllInput extends GetAllInput<Integer> {
    private boolean isActive;

    public TenantGetAllInput() { }

    public TenantGetAllInput(String sorting, Integer skipCount, Integer maxResultCount, List<String> searchStrings, List<Integer> ids, boolean isActive) {
        super(sorting, skipCount, maxResultCount, searchStrings, ids);
        this.isActive = isActive;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}