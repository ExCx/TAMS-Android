package com.frekanstan.tatf_demo.view.tracking;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.frekanstan.tatf_demo.view.assetlist.AssetListFragment;

import lombok.val;

public class CountingTabPagerAdapter extends FragmentPagerAdapter {
    private AssetListFragment counted, notcounted, foreign;
    private boolean withForeign = true;

    CountingTabPagerAdapter(FragmentManager fm, Bundle prefilter) {
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        if (prefilter.getLong("locationId") == 0 && prefilter.getLong("personId") == 0)
            withForeign = false;
        val bundle = (Bundle)prefilter.clone();
        bundle.putString("listType", "counted");
        counted = AssetListFragment.newInstance(bundle);
        val bundle2 = (Bundle)prefilter.clone();
        bundle2.putString("listType", "notcounted");
        notcounted = AssetListFragment.newInstance(bundle2);
        if (withForeign) {
            val bundle3 = (Bundle)prefilter.clone();
            bundle3.putString("listType", "foreign");
            foreign = AssetListFragment.newInstance(bundle3);
        }
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return counted;
            case 1:
                return notcounted;
            case 2:
                return foreign;
            default:
                return AssetListFragment.newInstance(new Bundle());
        }
    }

    @Override
    public int getCount() {
        return withForeign ? 3 : 2;
    }
}