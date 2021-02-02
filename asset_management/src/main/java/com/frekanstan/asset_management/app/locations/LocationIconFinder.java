package com.frekanstan.asset_management.app.locations;

import com.frekanstan.asset_management.R;

public class LocationIconFinder {
    public static int findLocationIconId(String name) {
        int id = R.drawable.door_96px_gray;
        if (name.contains("MUTFAK"))
            id = R.drawable.kitchen_96px_gray;
        return id;
    }
}