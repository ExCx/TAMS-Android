package com.frekanstan.asset_management.app.assets;

import android.content.Context;
import android.text.TextUtils;

import java.util.Arrays;

import lombok.var;

public class AssetTypeIconFinder {

    private Context mContext;

    public AssetTypeIconFinder(Context context) {
        mContext = context;
    }

    public int findIconId(String regNo) {
        String[] splitted = regNo.split("-");
        for (var i = 0; i < splitted.length; i++)
            splitted[i] = String.valueOf(Integer.parseInt(splitted[i]));
        int id = 0;
        do {
            regNo = TextUtils.join("_", splitted);
            id = mContext.getResources().getIdentifier("cat_icon_" + regNo, "drawable", mContext.getPackageName());
            if (id != 0)
                break;
            splitted = Arrays.copyOf(splitted, splitted.length - 1);
        } while (splitted.length > 0);
        return id != 0 ? id : mContext.getResources().getIdentifier("cat_icon_255", "drawable", mContext.getPackageName());
    }
}