package com.frekanstan.gateway_mobil.app.locations;

import android.os.Bundle;

import com.frekanstan.asset_management.data.locations.ELocationType;
import com.frekanstan.asset_management.data.locations.ILocation;
import com.frekanstan.gateway_mobil.app.assets.AssetDAO;
import com.frekanstan.gateway_mobil.data.Location;

import java.util.ArrayList;

import lombok.Setter;
import lombok.var;

public class LocationRepository
{
    @Setter
    private static long[] assignedLocationIds;
    public static long[] getAssignedLocationIds() {
        if (assignedLocationIds == null)
            assignedLocationIds = AssetDAO.getDao().getAllAssignedLocationIds();
        return assignedLocationIds;
    }

    public static ArrayList<ILocation> getAllStorages() {
        var input = new Bundle();
        input.putInt("locationType", ELocationType.Warehouse.id);
        return new ArrayList<>(LocationDAO.getDao().getAll(input));
    }

    public static ArrayList<Location> getAllUnits() {
        var input = new Bundle();
        input.putInt("locationType", ELocationType.Unit.id);
        return new ArrayList<>(LocationDAO.getDao().getAll(input));
    }
}