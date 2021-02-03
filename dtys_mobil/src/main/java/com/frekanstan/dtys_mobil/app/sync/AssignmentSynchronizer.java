package com.frekanstan.dtys_mobil.app.sync;

import androidx.annotation.NonNull;

import com.frekanstan.asset_management.app.connection.NetManager;
import com.frekanstan.asset_management.app.connection.ObjectRequest;
import com.frekanstan.asset_management.app.connection.ServiceConnectorBase;
import com.frekanstan.asset_management.app.webservice.AbpResult;
import com.frekanstan.asset_management.data.assignment.ChangeAssignmentInput;
import com.frekanstan.dtys_mobil.app.connection.ServiceConnector;
import com.frekanstan.dtys_mobil.data.AssignmentChange;
import com.frekanstan.dtys_mobil.view.MainActivity;

import java.util.ArrayList;
import java.util.List;

import io.objectbox.reactive.DataObserver;
import lombok.val;

public class AssignmentSynchronizer implements DataObserver<List<AssignmentChange>> {
    private static AssignmentSynchronizer ourInstance;
    private static boolean isBusy;
    private static ServiceConnectorBase conn;
    private static List<AssignmentChange> cache = new ArrayList<>();

    private AssignmentSynchronizer(MainActivity context) {
        isBusy = false;
        conn = ServiceConnector.getInstance(context);
    }

    public static synchronized AssignmentSynchronizer getInstance(MainActivity context){
        if (ourInstance == null)
            ourInstance = new AssignmentSynchronizer(context);
        return ourInstance;
    }

    @Override
    public void onData(@NonNull List<AssignmentChange> data) {
        if (data.size() == 0)
            return;
        if (!NetManager.isOnline || isBusy)
            cache = data;
        else {
            isBusy = true;
            ObjectRequest<AbpResult<Boolean>> request = conn.getChangeAssignmentReq(new ChangeAssignmentInput(new ArrayList<>(data)));
            request.setResponseListener(response -> {
                if (response.getSuccess()) {
                    for (val ac : data) {
                        ac.setSent(true);
                        AssignmentChangeDAO.getDao().put(ac);
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