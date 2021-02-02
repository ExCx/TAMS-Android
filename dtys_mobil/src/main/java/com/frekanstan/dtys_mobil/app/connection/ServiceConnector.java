package com.frekanstan.dtys_mobil.app.connection;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.frekanstan.asset_management.app.connection.ObjectRequest;
import com.frekanstan.asset_management.app.webservice.AbpResult;
import com.frekanstan.dtys_mobil.R;
import com.frekanstan.dtys_mobil.data.GetAllPersons;
import com.frekanstan.dtys_mobil.view.MainActivity;

import java.util.ArrayList;
import java.util.Map;

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

    public void getAllPersons(String controllerName, GetAllPersons input, Response.Listener<AbpResult<ArrayList>> onResponse) {
        @SuppressWarnings("unchecked")
        Map<String, Object> params = mMapper.convertValue(input, Map.class);
        ObjectRequest<AbpResult<ArrayList>> objectRequest = new ObjectRequest<>(
                Request.Method.POST,
                serviceUrl + "api/services/app/" + controllerName + "/GetAllForClient",
                AbpResult.class,
                params,
                onResponse,
                this::onErrorResponse);
        objectRequest.setHeaders(headers);
        objectRequest.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        addToRequestQueue(objectRequest);
    }
}