package com.frekanstan.asset_management.app.tracking;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;

import androidx.core.content.ContextCompat;

import com.frekanstan.asset_management.R;
import com.frekanstan.asset_management.view.MainActivityBase;

import lombok.var;

public class MountedBarcodeReaderManager implements IMountedBarcodeReaderManager {
    private Activity context;
    private Boolean isScanning = false;

    private static final String ACTION_BCR_TRIGGER = "oem.android.bcr.ACTION_BCR_TRIGGER";
    private static final String ACTION_BCR_TRIGGER_KEYCODE = "oem.android.bcr.ACTION_BCR_TRIGGER_KEYCODE";
    private static final String ACTION_FEEDBACK = "oem.android.bcr.ACTION_FEEDBACK";

    public MountedBarcodeReaderManager(Activity context) {
        this.context = context;
    }

    @Override
    public void scan(MainActivityBase context) {
        if (isScanning)
            return;
        isScanning = true;
        context.actionButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorAccentDark)));
        var intent = new Intent(ACTION_BCR_TRIGGER);
        intent.putExtra(ACTION_BCR_TRIGGER_KEYCODE, 118);
        context.sendBroadcast(intent);
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            isScanning = false;
            if (ACTION_FEEDBACK.equals(intent.getAction()))
                ((MainActivityBase)context).onBarcodeScanned(intent.getStringExtra(Intent.EXTRA_TEXT));
        }
    };

    @Override
    public void onResume() {
        var filter = new IntentFilter();
        filter.addAction(ACTION_FEEDBACK);
        filter.addAction(ACTION_BCR_TRIGGER);
        context.registerReceiver(broadcastReceiver, filter);
    }

    @Override
    public void onPause() {
        context.unregisterReceiver(broadcastReceiver);
    }
}