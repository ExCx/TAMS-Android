package com.frekanstan.gateway_mobil.view.assetdetails;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.frekanstan.asset_management.data.assets.IAsset;

public class AssetDetailsTabPagerAdapter extends FragmentPagerAdapter {
    private IAsset mAsset;

    AssetDetailsTabPagerAdapter(FragmentManager fm, IAsset asset) {
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        this.mAsset = asset;
    }

    @SuppressWarnings("ConstantConditions")
    @NonNull
    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return AssetDetailsGeneralFragment.newInstance(mAsset);
            case 1:
                return AssetDetailsAssignmentFragment.newInstance(mAsset);
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return 2;
    }
}