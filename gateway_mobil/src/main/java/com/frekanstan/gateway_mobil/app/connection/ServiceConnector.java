package com.frekanstan.gateway_mobil.app.connection;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.frekanstan.asset_management.app.connection.ObjectRequest;
import com.frekanstan.asset_management.app.webservice.AbpResult;
import com.frekanstan.gateway_mobil.R;
import com.frekanstan.gateway_mobil.view.MainActivity;

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

    public ObjectRequest<AbpResult<ArrayList>> getAllInventoryReq(GetAllInventoryInput input, Response.Listener<AbpResult<ArrayList>> onResponse) {
        @SuppressWarnings("unchecked")
        Map<String, Object> params = mMapper.convertValue(input, Map.class);
        ObjectRequest<AbpResult<ArrayList>> objectRequest = new ObjectRequest<>(
                Request.Method.POST,
                serviceUrl + "api/services/app/gatewayControl/GetAllInventory",
                AbpResult.class,
                params,
                onResponse,
                this::onErrorResponse);
        objectRequest.setHeaders(headers);
        objectRequest.setRetryPolicy(new DefaultRetryPolicy(
                5000,
                3,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        return objectRequest;
    }

    public ObjectRequest<AbpResult<ArrayList>> getPackagesReq(GetPackagesInput input, Response.Listener<AbpResult<ArrayList>> onResponse) {
        @SuppressWarnings("unchecked")
        Map<String, Object> params = mMapper.convertValue(input, Map.class);
        ObjectRequest<AbpResult<ArrayList>> objectRequest = new ObjectRequest<>(
                Request.Method.POST,
                serviceUrl + "api/services/app/gatewayControl/GetPackages",
                AbpResult.class,
                params,
                onResponse,
                this::onErrorResponse);
        objectRequest.setHeaders(headers);
        objectRequest.setRetryPolicy(new DefaultRetryPolicy(
                5000,
                3,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        return objectRequest;
    }

    public ObjectRequest<AbpResult<String>> receivePackagesReq(ReceivePackagesInput input, Response.Listener<AbpResult<String>> onResponse) {
        @SuppressWarnings("unchecked")
        Map<String, Object> params = mMapper.convertValue(input, Map.class);
        ObjectRequest<AbpResult<String>> objectRequest = new ObjectRequest<>(
                Request.Method.POST,
                serviceUrl + "api/services/app/gatewayControl/ReceivePackages",
                AbpResult.class,
                params,
                onResponse,
                this::onErrorResponse);
        objectRequest.setHeaders(headers);
        objectRequest.setRetryPolicy(new DefaultRetryPolicy(
                10000,
                1,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        return objectRequest;
    }

    public ObjectRequest<AbpResult<String>> storageTransferReq(StorageTransferInput input, Response.Listener<AbpResult<String>> onResponse) {
        @SuppressWarnings("unchecked")
        Map<String, Object> params = mMapper.convertValue(input, Map.class);
        ObjectRequest<AbpResult<String>> objectRequest = new ObjectRequest<>(
                Request.Method.POST,
                serviceUrl + "api/services/app/gatewayControl/StorageTransfer",
                AbpResult.class,
                params,
                onResponse,
                this::onErrorResponse);
        objectRequest.setHeaders(headers);
        objectRequest.setRetryPolicy(new DefaultRetryPolicy(
                10000,
                1,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        return objectRequest;
    }

    public ObjectRequest<AbpResult<ArrayList>> getAllWarehousesReq(Response.Listener<AbpResult<ArrayList>> onResponse) {
        ObjectRequest<AbpResult<ArrayList>> objectRequest = new ObjectRequest<>(
                Request.Method.POST,
                serviceUrl + "api/services/app/gatewayControl/GetAllWarehouses",
                AbpResult.class,
                null,
                onResponse,
                this::onErrorResponse);
        objectRequest.setHeaders(headers);
        objectRequest.setRetryPolicy(new DefaultRetryPolicy(
                5000,
                3,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        return objectRequest;
    }

    public ObjectRequest<AbpResult<ArrayList>> getAllOrderReceipts(Response.Listener<AbpResult<ArrayList>> onResponse) {
        ObjectRequest<AbpResult<ArrayList>> objectRequest = new ObjectRequest<>(
                Request.Method.POST,
                serviceUrl + "api/services/app/gatewayControl/GetAllOrderReceipts",
                AbpResult.class,
                null,
                onResponse,
                this::onErrorResponse);
        objectRequest.setHeaders(headers);
        objectRequest.setRetryPolicy(new DefaultRetryPolicy(
                5000,
                3,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        return objectRequest;
    }

    public ObjectRequest<AbpResult<ArrayList>> getAllOrderedVariantsReq(GetAllDemandedVariantsInput input, Response.Listener<AbpResult<ArrayList>> onResponse) {
        @SuppressWarnings("unchecked")
        Map<String, Object> params = mMapper.convertValue(input, Map.class);
        ObjectRequest<AbpResult<ArrayList>> objectRequest = new ObjectRequest<>(
                Request.Method.POST,
                serviceUrl + "api/services/app/gatewayControl/GetAllOrderedVariants",
                AbpResult.class,
                params,
                onResponse,
                this::onErrorResponse);
        objectRequest.setHeaders(headers);
        objectRequest.setRetryPolicy(new DefaultRetryPolicy(
                5000,
                3,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        return objectRequest;
    }

    public ObjectRequest<AbpResult<ArrayList>> getAllDemandReceipts(Response.Listener<AbpResult<ArrayList>> onResponse) {
        ObjectRequest<AbpResult<ArrayList>> objectRequest = new ObjectRequest<>(
                Request.Method.POST,
                serviceUrl + "api/services/app/gatewayControl/GetAllDemandReceipts",
                AbpResult.class,
                null,
                onResponse,
                this::onErrorResponse);
        objectRequest.setHeaders(headers);
        objectRequest.setRetryPolicy(new DefaultRetryPolicy(
                5000,
                3,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        return objectRequest;
    }

    public ObjectRequest<AbpResult<ArrayList>> getAllDemandedVariantsReq(GetAllDemandedVariantsInput input, Response.Listener<AbpResult<ArrayList>> onResponse) {
        @SuppressWarnings("unchecked")
        Map<String, Object> params = mMapper.convertValue(input, Map.class);
        ObjectRequest<AbpResult<ArrayList>> objectRequest = new ObjectRequest<>(
                Request.Method.POST,
                serviceUrl + "api/services/app/gatewayControl/GetAllDemandedVariants",
                AbpResult.class,
                params,
                onResponse,
                this::onErrorResponse);
        objectRequest.setHeaders(headers);
        objectRequest.setRetryPolicy(new DefaultRetryPolicy(
                5000,
                3,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        return objectRequest;
    }
}