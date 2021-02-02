package com.frekanstan.tatf_demo.view.labeling;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.frekanstan.tatf_demo.view.assetlist.AssetListFragment;
import com.frekanstan.tatf_demo.view.locationlist.LocationListFragment;
import com.frekanstan.tatf_demo.view.personlist.PersonListFragment;

import lombok.Getter;
import lombok.val;

public class LabelingTabPagerAdapter extends FragmentPagerAdapter {
    @Getter
    private Fragment labeled, notlabeled;

    LabelingTabPagerAdapter(FragmentManager fm, Bundle prefilter) {
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        switch (prefilter.getString("operation", "")) {
            case "labeling": { //asset labeling
                val bundle = (Bundle) prefilter.clone();
                bundle.putString("listType", "labeled");
                labeled = AssetListFragment.newInstance(bundle);
                val bundle2 = (Bundle) prefilter.clone();
                bundle2.putString("listType", "notlabeled");
                notlabeled = AssetListFragment.newInstance(bundle2);
                break;
            }
            case "location_labeling": { //location labeling
                val bundle = (Bundle) prefilter.clone();
                bundle.putString("listType", "labeled");
                labeled = LocationListFragment.newInstance(bundle);
                val bundle2 = (Bundle) prefilter.clone();
                bundle2.putString("listType", "notlabeled");
                notlabeled = LocationListFragment.newInstance(bundle2);
                break;
            }
            case "person_labeling": { //person labeling
                val bundle = (Bundle) prefilter.clone();
                bundle.putString("listType", "labeled");
                labeled = PersonListFragment.newInstance(bundle);
                val bundle2 = (Bundle) prefilter.clone();
                bundle2.putString("listType", "notlabeled");
                notlabeled = PersonListFragment.newInstance(bundle2);
                break;
            }
        }
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return labeled;
            case 1:
                return notlabeled;
            default:
                return AssetListFragment.newInstance(new Bundle());
        }
    }

    @Override
    public int getCount() {
        return 2;
    }
}