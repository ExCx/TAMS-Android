package com.frekanstan.asset_management.view.tracking;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.frekanstan.asset_management.app.tracking.IRfidDeviceManager;
import com.frekanstan.asset_management.databinding.TagFinderDialogBinding;
import com.frekanstan.asset_management.view.MainActivityBase;

public class TagFinderDialogFragment extends DialogFragment {
    private MainActivityBase context;
    private TagFinderDialogBinding binding;

    public TagFinderDialogFragment() { }

    public static TagFinderDialogFragment newInstance() {
        return new TagFinderDialogFragment();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivityBase)
            this.context = (MainActivityBase) context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = TagFinderDialogBinding.inflate(inflater, container, false);
        setCancelable(true);
        return binding.getRoot();
    }

    public void onRangeScanned(int range) {
        context.runOnUiThread(() -> {
            binding.proximityPercentage.setText(String.format("%%%s", range));
            binding.proximityBar.setProgress(range);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        context.setTfDialog(this);
        if (context.rfidManager != null && context.rfidManager.isDeviceOnline())
            context.rfidManager.onResume(IRfidDeviceManager.OperationType.TagFinder);
    }

    @Override
    public void onPause() {
        super.onPause();
        context.setTfDialog(null);
        if (context.rfidManager != null && context.rfidManager.isDeviceOnline())
            context.rfidManager.onPause();
    }
}