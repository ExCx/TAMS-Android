package com.frekanstan.gateway_mobil.view.mainmenu;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.frekanstan.gateway_mobil.R;
import com.frekanstan.gateway_mobil.databinding.MainMenuLabelingFragmentBinding;
import com.frekanstan.gateway_mobil.view.MainActivity;

import lombok.val;

public class MainMenuLabelingFragment extends Fragment {

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
        val view = MainMenuLabelingFragmentBinding.inflate(inflater, container, false);
        setHasOptionsMenu(false);
        view.mainMenuAssetLabelingButton.setOnClickListener(v -> context.nav.navigate(R.id.action_mainMenuLabelingFragment_to_mainMenuAssetLabelingFragment));
        view.mainMenuLocationLabelingButton.setOnClickListener(v -> {
            val input = new Bundle();
            input.putString("operation", "location_labeling");
            context.nav.navigate(R.id.action_mainMenuLabelingFragment_to_labelingTabsFragment, input);
        });
        view.mainMenuPersonLabelingButton.setOnClickListener(v -> {
            val input = new Bundle();
            input.putString("operation", "person_labeling");
            context.nav.navigate(R.id.action_mainMenuLabelingFragment_to_labelingTabsFragment, input);
        });
        return view.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        context.actionButton.hide();
        context.showHideFooter(false);
    }
}