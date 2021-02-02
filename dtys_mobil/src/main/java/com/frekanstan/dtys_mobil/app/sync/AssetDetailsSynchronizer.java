package com.frekanstan.dtys_mobil.app.sync;

import androidx.annotation.NonNull;

import com.frekanstan.asset_management.app.connection.NetManager;
import com.frekanstan.asset_management.app.connection.ObjectRequest;
import com.frekanstan.asset_management.app.connection.ServiceConnectorBase;
import com.frekanstan.asset_management.app.webservice.AbpResult;
import com.frekanstan.asset_management.data.assets.ChangeAssetDetailsInput;
import com.frekanstan.dtys_mobil.app.assets.AssetDAO;
import com.frekanstan.dtys_mobil.app.connection.ServiceConnector;
import com.frekanstan.dtys_mobil.data.Asset;
import com.frekanstan.dtys_mobil.view.MainActivity;

import java.util.ArrayList;
import java.util.List;

import io.objectbox.reactive.DataObserver;
import lombok.val;

public class AssetDetailsSynchronizer implements DataObserver<List<Asset>> {
    private static AssetDetailsSynchronizer ourInstance;
    private static boolean isBusy;
    private static ServiceConnectorBase conn;
    private static List<Asset> cache = new ArrayList<>();

    private AssetDetailsSynchronizer(MainActivity context) {
        isBusy = false;
        conn = ServiceConnector.getInstance(context);
    }

    public static synchronized AssetDetailsSynchronizer getInstance(MainActivity context){
        if (ourInstance == null)
            ourInstance = new AssetDetailsSynchronizer(context);
        return ourInstance;
    }

    @Override
    public void onData(@NonNull List<Asset> data) {
        if (data.size() == 0)
            return;
        if (!NetManager.isOnline || isBusy)
            cache = data;
        else {
            isBusy = true;
            ObjectRequest<AbpResult<Boolean>> request = conn.getChangeAssetDetailsReq(new ChangeAssetDetailsInput(new ArrayList<>(data)));
            request.setResponseListener(response -> {
                if (response.getSuccess()) {
                    for (val asset : data) {
                        asset.setIsUpdated(false);
                        AssetDAO.getDao().put(asset);
                    }
                    cache.clear();
                }
                isBusy = false;
            });
            conn.addToRequestQueue(request);
        }
    }

    public void syncCache() {
        onData(cache);
    }
}
