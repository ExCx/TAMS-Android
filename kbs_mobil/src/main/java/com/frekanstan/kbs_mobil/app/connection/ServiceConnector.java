package com.frekanstan.kbs_mobil.app.connection;

import com.frekanstan.kbs_mobil.R;
import com.frekanstan.kbs_mobil.view.MainActivity;

public class ServiceConnector extends com.frekanstan.asset_management.app.connection.ServiceConnectorBase {

    private static ServiceConnector instance;

    private ServiceConnector(MainActivity context) {
        super(context, context.getString(R.string.web_service_url));
    }

    public static synchronized ServiceConnector getInstance(MainActivity context){
        if (instance == null)
            instance = new ServiceConnector(context);
        return instance;
    }
}