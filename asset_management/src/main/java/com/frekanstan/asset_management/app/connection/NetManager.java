package com.frekanstan.asset_management.app.connection;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;

import androidx.core.app.ActivityCompat;

import com.google.common.base.Strings;

import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.UnknownHostException;

import lombok.val;

import static android.content.Context.WIFI_SERVICE;

public class NetManager {
    private static NetManager ourInstance;
    public static Boolean isOnline;
    public static Boolean isBusy = false;

    //----------------------------------------------------------------------------------------------
    // Initialization
    //----------------------------------------------------------------------------------------------

    public static NetManager getInstance() {
        if (ourInstance == null)
            ourInstance = new NetManager();
        return ourInstance;
    }

    //----------------------------------------------------------------------------------------------
    // Public Functions
    //----------------------------------------------------------------------------------------------

    public static void checkNet(Context context) {
        if (isBusy)
            return;
        new NetChecker(context).execute();
    }

    public static WifiConfiguration findKnownWifiConfig(WifiManager wifiManager, String ssidContains) {
        for (WifiConfiguration config : wifiManager.getConfiguredNetworks()) {
            if (!Strings.isNullOrEmpty(config.SSID) && config.SSID.contains(ssidContains))
                return config;
        }
        return null;
    }

    //----------------------------------------------------------------------------------------------
    // Misc
    //----------------------------------------------------------------------------------------------

    public interface NetCheckedListener {
        void onNetChecked(Boolean result);
    }

    private static class NetChecker extends AsyncTask<Void, Void, Boolean> {
        private final WeakReference<Context> context;

        NetChecker(Context activity) {
            this.context = new WeakReference<>(activity);
        }

        protected Boolean doInBackground(Void... voids) {
            return isConnected(context.get());
        }

        protected void onPostExecute(Boolean result) {
            ((NetCheckedListener)context.get()).onNetChecked(result);
        }
    }

    private static boolean isConnected(Context context) {
        val cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null)
            return false;
        val activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnectedOrConnecting())
        {
            /*val wifiManager = ((WifiManager)context.getApplicationContext().getSystemService(WIFI_SERVICE));
            if (wifiManager != null) {
                if (wifiManager.isWifiEnabled()) {
                    if (wifiManager.getConnectionInfo() != null) {
                        if (!wifiManager.getConnectionInfo().getSSID().isEmpty()) {
                            if (wifiManager.getConnectionInfo().getSSID().contains("DIRECT"))
                                return false;
                        }
                    }
                }
            }*/
            try {
                val address = InetAddress.getByName("www.google.com");
                return !address.toString().equals("");
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}
