package com.frekanstan.dtys_mobil.app.sync;

import androidx.annotation.NonNull;

import com.frekanstan.asset_management.app.connection.NetManager;
import com.frekanstan.asset_management.app.connection.ObjectRequest;
import com.frekanstan.asset_management.app.connection.ServiceConnectorBase;
import com.frekanstan.asset_management.app.webservice.AbpResult;
import com.frekanstan.asset_management.data.labeling.ChangeLabeledStateInput;
import com.frekanstan.asset_management.data.labeling.ILabeledStateChange;
import com.frekanstan.dtys_mobil.app.connection.ServiceConnector;
import com.frekanstan.dtys_mobil.data.LabeledStateChange;
import com.frekanstan.dtys_mobil.data.LabeledStateChange_;
import com.frekanstan.dtys_mobil.view.MainActivity;

import java.util.ArrayList;
import java.util.List;

import io.objectbox.reactive.DataObserver;
import lombok.val;

public class LabelSynchronizer implements DataObserver<List<LabeledStateChange>> {
    private static LabelSynchronizer ourInstance;
    private static boolean isBusy;
    private static ServiceConnectorBase conn;
    private static List<LabeledStateChange> cache = new ArrayList<>();

    private LabelSynchronizer(MainActivity context) {
        isBusy = false;
        conn = ServiceConnector.getInstance(context);
    }

    public static synchronized LabelSynchronizer getInstance(MainActivity context) {
        if (ourInstance == null)
            ourInstance = new LabelSynchronizer(context);
        return ourInstance;
    }

    @Override
    public void onData(@NonNull List<LabeledStateChange> data) {
        if (data.size() == 0)
            return;
        if (!NetManager.isOnline || isBusy)
            cache = data;
        else {
            isBusy = true;
            ArrayList<ILabeledStateChange> assetData = new ArrayList<>();
            ArrayList<ILabeledStateChange> locationData = new ArrayList<>();
            ArrayList<ILabeledStateChange> personData = new ArrayList<>();
            for (val lsc : data) {
                switch (lsc.getType()) {
                    case "asset":
                        assetData.add(lsc);
                        break;
                    case "location":
                        locationData.add(lsc);
                        break;
                    case "person":
                        personData.add(lsc);
                        break;
                }
            }
            for (val type : new String[]{"asset", "location", "person"}) {
                ObjectRequest<AbpResult<Boolean>> request = null;
                switch (type) {
                    case "asset":
                        request = conn.getChangeLabeledStateReq(new ChangeLabeledStateInput(assetData), type);
                        break;
                    case "location":
                        request = conn.getChangeLabeledStateReq(new ChangeLabeledStateInput(locationData), type);
                        break;
                    case "person":
                        request = conn.getChangeLabeledStateReq(new ChangeLabeledStateInput(personData), type);
                        break;
                }
                assert request != null;
                request.setResponseListener(response -> {
                    if (response.getSuccess()) {
                        LabeledStateChangeDAO.getDao().getBox().query().equal(LabeledStateChange_.type, type).build().remove();
                        cache.clear();
                    }
                    isBusy = false;
                });
                conn.addToRequestQueue(request);
            }
        }
    }

    public void syncCache() {
        onData(cache);
    }
}