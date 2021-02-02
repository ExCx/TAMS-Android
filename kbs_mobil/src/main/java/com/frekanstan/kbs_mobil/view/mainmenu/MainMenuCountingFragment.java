package com.frekanstan.kbs_mobil.view.mainmenu;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.frekanstan.kbs_mobil.R;
import com.frekanstan.kbs_mobil.databinding.MainMenuCountingFragmentBinding;
import com.frekanstan.kbs_mobil.view.MainActivity;

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