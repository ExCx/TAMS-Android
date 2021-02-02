package com.frekanstan.tatf_demo.app.sync;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.frekanstan.asset_management.app.connection.NetManager;
import com.frekanstan.asset_management.app.connection.ObjectRequest;
import com.frekanstan.asset_management.app.connection.ServiceConnectorBase;
import com.frekanstan.asset_management.app.webservice.AbpResult;
import com.frekanstan.asset_management.data.tracking.ChangeCountedStateInput;
import com.frekanstan.tatf_demo.app.connection.ServiceConnector;
import com.frekanstan.tatf_demo.data.CountedStateChange;
import com.frekanstan.tatf_demo.view.MainActivity;

import java.util.ArrayList;
import java.util.List;

import io.objectbox.reactive.DataObserver;
import lombok.val;

public class CountingSynchronizer implements DataObserver<List<CountedStateChange>> {
    private static CountingSynchronizer ourInstance;
    private static boolean isCaching = false;
    private static ServiceConnectorBase conn;
    private static List<CountedStateChange> cache = new ArrayList<>();

    private CountingSynchronizer(MainActivity context) {
        conn = ServiceConnector.getInstance(context);
    }

    public static synchronized CountingSynchronizer getInstance(MainActivity context){
        if (ourInstance == null)
            ourInstance = new CountingSynchronizer(context);
        return ourInstance;
    }

    @Override
    public void onData(@NonNull List<CountedStateChange> data) {
        if (data.size() == 0)
            return;
        if (!NetManager.isOnline || isCaching)
            cache = data;
        else {
            isCaching = true;
            cache = data;
            new Handler(Looper.getMainLooper())
                    .postDelayed(CountingSynchronizer::sendData, 5000);
        }
    }

    private static void sendData() {
        isCaching = false;
        val sentData = new ArrayList<>(cache);
        ObjectRequest<AbpResult<Boolean>> request = conn.getChangeCountedStateReq(new ChangeCountedStateInput(new ArrayList<>(cache)));
        request.setResponseListener(response -> {
            if (response.getSuccess()) {
                CountedStateChangeDAO.getDao().removeAll(sentData);
                cache.removeAll(sentData);
            }
        });
        conn.addToRequestQueue(request);
    }

    public void syncCache() {
        onData(cache);
    }
}