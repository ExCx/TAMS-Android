package com.frekanstan.tatf_demo.view.mainmenu;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.frekanstan.tatf_demo.R;
import com.frekanstan.tatf_demo.databinding.MainMenuAssetLabelingFragmentBinding;
import com.frekanstan.tatf_demo.view.MainActivity;

import lombok.val;

public class MainMenuAssetLabelingFragment extends Fragment {

    private MainActivity context;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = (MainActivity)context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        val view = MainMenuAssetLabelingFragmentBinding.inflate(inflater, container, false);
        setHasOptionsMenu(false);
        view.mainMenuGeneralLabelingButton.setOnClickListener(v -> context.nav.navigate(R.id.action_mainMenuAssetLabelingFragment_to_labelingTabsFragment));
        view.mainMenuByLocationButton.setOnClickListener(v -> context.nav.navigate(R.id.action_mainMenuAssetLabelingFragment_to_locationListFragment));
        view.mainMenuByResponsibleButton.setOnClickListener(v -> context.nav.navigate(R.id.action_mainMenuAssetLabelingFragment_to_personListFragment));
        view.mainMenuByTypeButton.setOnClickListener(v -> context.nav.navigate(R.id.action_mainMenuAssetLabelingFragment_to_assetTypeListFragment));
        return view.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        context.actionButton.hide();
        context.showHideFooter(false);
    }
}