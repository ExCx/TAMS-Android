package com.frekanstan.gateway_mobil.app.sync;

import androidx.annotation.NonNull;

import com.frekanstan.asset_management.app.connection.NetManager;
import com.frekanstan.asset_management.app.connection.ObjectRequest;
import com.frekanstan.asset_management.app.connection.ServiceConnectorBase;
import com.frekanstan.asset_management.app.webservice.AbpResult;
import com.frekanstan.gateway_mobil.app.connection.ServiceConnector;
import com.frekanstan.gateway_mobil.app.tracking.CountingOpDAO;
import com.frekanstan.gateway_mobil.data.CountedStateChange;
import com.frekanstan.gateway_mobil.data.CountedStateChange_;
import com.frekanstan.gateway_mobil.data.CountingOp;
import com.frekanstan.gateway_mobil.view.MainActivity;

import java.util.ArrayList;
import java.util.List;

import io.objectbox.reactive.DataObserver;
import lombok.val;
import lombok.var;

public class CountingOpSynchronizer implements DataObserver<List<CountingOp>> {
    private static CountingOpSynchronizer ourInstance;
    private static boolean isBusy;
    private static ServiceConnectorBase conn;
    private static List<CountingOp> cache = new ArrayList<>();

    private CountingOpSynchronizer(MainActivity context) {
        isBusy = false;
        conn = ServiceConnector.getInstance(context);
    }

    public static synchronized CountingOpSynchronizer getInstance(MainActivity context){
        if (ourInstance == null)
            ourInstance = new CountingOpSynchronizer(context);
        return ourInstance;
    }

    @Override
    public void onData(@NonNull List<CountingOp> data) {
        if (data.size() == 0)
            return;
        if (!NetManager.isOnline)
            cache = data;
        else {
            isBusy = true;
            var listToUpdate = new ArrayList<CountingOp>();
            for (val op : data) {
                ObjectRequest<AbpResult<Double>> request = conn.getCountingOpReq(op);
                request.setResponseListener(response -> {
                    if (response.getSuccess()) {
                        cache.remove(op);
                        op.setGlobalId(response.getResult().longValue());
                        var cscToUpdate = new ArrayList<CountedStateChange>();
                        for (val csc : CountedStateChangeDAO.getDao().getBox().query()
                                .equal(CountedStateChange_.countingOpId, op.getId()).and()
                                .equal(CountedStateChange_.globalId, 0).build().find()) {
                            csc.setGlobalId(response.getResult().longValue());
                            cscToUpdate.add(csc);
                        }
                        CountedStateChangeDAO.getDao().putAll(cscToUpdate);
                        op.setIsUpdated(false);
                        listToUpdate.add(op);
                        if (listToUpdate.size() == data.size()) {
                            CountingOpDAO.getDao().putAll(listToUpdate);
                            isBusy = false;
                        }
                    }
                });
                conn.addToRequestQueue(request);
            }
        }
    }

    public void syncCache() {
        onData(cache);
    }
}