package com.frekanstan.dtys_mobil.view.mainmenu;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;

import com.frekanstan.dtys_mobil.R;
import com.frekanstan.dtys_mobil.databinding.MainMenuCountingFragmentBinding;
import com.frekanstan.dtys_mobil.view.MainActivity;

import lombok.val;

public class MainMenuCountingFragment extends Fragment {

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
        val view = MainMenuCountingFragmentBinding.inflate(inflater, container, false);
        setHasOptionsMenu(false);

        DrawableCompat.setTint(DrawableCompat.wrap(view.mainMenuGeneralCountingButton.getCompoundDrawablesRelative()[1]), getResources().getColor(android.R.color.holo_blue_dark));
        DrawableCompat.setTint(DrawableCompat.wrap(view.mainMenuByLocationButton.getCompoundDrawablesRelative()[1]), getResources().getColor(android.R.color.holo_blue_dark));
        DrawableCompat.setTint(DrawableCompat.wrap(view.mainMenuByResponsibleButton.getCompoundDrawablesRelative()[1]), getResources().getColor(android.R.color.holo_blue_dark));
        DrawableCompat.setTint(DrawableCompat.wrap(view.mainMenuByTypeButton.getCompoundDrawablesRelative()[1]), getResources().getColor(android.R.color.holo_blue_dark));

        view.mainMenuGeneralCountingButton.setOnClickListener(v -> context.nav.navigate(R.id.action_mainMenuCountingFragment_to_countingTabsFragment));
        view.mainMenuByLocationButton.setOnClickListener(v -> context.nav.navigate(R.id.action_mainMenuCountingFragment_to_locationListFragment));
        view.mainMenuByResponsibleButton.setOnClickListener(v -> context.nav.navigate(R.id.action_mainMenuCountingFragment_to_personListFragment));
        view.mainMenuByTypeButton.setOnClickListener(v -> context.nav.navigate(R.id.action_mainMenuCountingFragment_to_assetTypeListFragment));
        return view.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        context.actionButton.hide();
        context.showHideFooter(false);
    }
}