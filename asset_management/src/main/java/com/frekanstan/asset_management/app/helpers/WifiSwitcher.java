package com.frekanstan.asset_management.app.helpers;

import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;

import lombok.var;

public class WifiSwitcher extends AsyncTask<String, Integer, Boolean> {

    //listener interface
    public interface WifiSwitcedListener {
        void onFail();
        void onSuccess();
    }

    private final WifiManager wifiManager;
    private final WifiSwitcedListener listener;

    public WifiSwitcher(WifiManager wifiManager, WifiSwitcedListener listener) {
        this.wifiManager = wifiManager;
        this.listener = listener;
    }

    @Override
    protected Boolean doInBackground(String... info) {
        for (WifiConfiguration config : wifiManager.getConfiguredNetworks()) {
            if (config.SSID != null && config.SSID.contains(info[0])) {
                wifiManager.disableNetwork(wifiManager.getConnectionInfo().getNetworkId());
                wifiManager.enableNetwork(config.networkId, true);
                wifiManager.reconnect();
                break;
            }
        }

        var wifiInfo = wifiManager.getConnectionInfo();
        while (true) {
            try {
                if (!wifiInfo.getSSID().equals(info[0]))
                {
                    Thread.sleep(500);
                    wifiInfo = wifiManager.getConnectionInfo();
                    continue;
                }
                for (int i = 0; i < 5; i++) {
                    if (wifiInfo.getSupplicantState() == SupplicantState.COMPLETED)
                        break;
                    else if (i == 4)
                        return false;
                    else {
                        Thread.sleep(1000);
                        wifiInfo = wifiManager.getConnectionInfo();
                    }
                }
                return true;
            } catch (InterruptedException e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if (listener == null)
            return;
        if (result)
            listener.onSuccess();
        else
            listener.onFail();
    }
}