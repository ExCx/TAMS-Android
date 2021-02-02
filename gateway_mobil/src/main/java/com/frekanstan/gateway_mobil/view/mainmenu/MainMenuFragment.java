package com.frekanstan.gateway_mobil.view.mainmenu;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.frekanstan.gateway_mobil.R;
import com.frekanstan.gateway_mobil.databinding.MainMenuFragmentBinding;
import com.frekanstan.gateway_mobil.view.MainActivity;

import lombok.val;

public class MainMenuFragment extends Fragment {

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
        val view = MainMenuFragmentBinding.inflate(inflater, container, false);
        setHasOptionsMenu(false);
        context.progDialog.dismiss();

        view.mainMenuLabelingButton.setOnClickListener(v -> context.nav.navigate(R.id.action_mainMenuFragment_to_mainMenuLabelingFragment));
        view.mainMenuCountingButton.setOnClickListener(v -> context.nav.navigate(R.id.action_mainMenuFragment_to_countingOpListFragment));
        view.mainMenuLocateButton.setOnClickListener(v -> {
            if (context.controlRfid())
                context.nav.navigate(R.id.action_mainMenuFragment_to_locateRfidFragment);
            else
                context.nav.navigate(R.id.settingsFragment);
        });
        view.mainMenuReceiveAssetsButton.setOnClickListener(v -> {
            val bundle = new Bundle();
            bundle.putString("operation", "receive_assets");
            bundle.putBoolean("reset", true);
            context.nav.navigate(R.id.action_mainMenuFragment_to_receiptListFragment, bundle);
        });
        view.mainMenuStorageTransferButton.setOnClickListener(v -> {
            val bundle = new Bundle();
            bundle.putString("operation", "storage_transfer");
            bundle.putBoolean("reset", true);
            context.nav.navigate(R.id.action_mainMenuFragment_to_receiptListFragment, bundle);
        });
        view.mainMenuSendAssetsButton.setOnClickListener(v -> {
            val bundle = new Bundle();
            bundle.putString("operation", "send_assets");
            context.nav.navigate(R.id.countingTabsFragment, bundle);
        });
        view.mainMenuSettingsButton.setOnClickListener(v -> context.nav.navigate(R.id.action_mainMenuFragment_to_settingsFragment));
        view.mainMenuLogoutButton.setOnClickListener(v -> {
            new AlertDialog.Builder(context).setMessage(R.string.logging_out)
                    .setPositiveButton(context.getString(R.string.yes), (dialog, id) -> {
                        context.getBinding().toolbar.setVisibility(View.GONE);
                        context.nav.popBackStack(R.id.loginFragment, true);
                    })
                    .setNegativeButton(context.getString(R.string.no), (dialog, id) -> dialog.dismiss())
                    .show();
        });
        return view.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        context.actionButton.hide();
        context.showHideFooter(true);
    }
}
