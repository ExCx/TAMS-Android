package com.frekanstan.tatf_demo.app.connection;

import com.frekanstan.tatf_demo.R;
import com.frekanstan.tatf_demo.view.MainActivity;

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