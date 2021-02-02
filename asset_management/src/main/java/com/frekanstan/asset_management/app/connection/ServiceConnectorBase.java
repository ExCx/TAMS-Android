package com.frekanstan.asset_management.app.connection;

import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.frekanstan.asset_management.R;
import com.frekanstan.asset_management.app.multitenancy.TenantGetAllInput;
import com.frekanstan.asset_management.app.webservice.AbpResult;
import com.frekanstan.asset_management.app.webservice.LoginInput;
import com.frekanstan.asset_management.data.GetAllInput;
import com.frekanstan.asset_management.data.assets.ChangeAssetDetailsInput;
import com.frekanstan.asset_management.data.assettypes.AssetTypeGetAllInput;
import com.frekanstan.asset_management.data.assignment.ChangeAssignmentInput;
import com.frekanstan.asset_management.data.labeling.ChangeLabeledStateInput;
import com.frekanstan.asset_management.data.photo.ChangePhotoAvailabilityInput;
import com.frekanstan.asset_management.data.tracking.ChangeCountedStateInput;
import com.frekanstan.asset_management.data.tracking.ICountedStateChange;
import com.frekanstan.asset_management.data.tracking.ICountingOp;
import com.frekanstan.asset_management.view.MainActivityBase;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.val;
import lombok.var;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;

public class ServiceConnectorBase {
    private final MainActivityBase mContext;
    private RequestQueue requestQueue;
    public final ObjectMapper mMapper;
    public HashMap<String, String> headers;
    public final String serviceUrl;

    public static boolean isLoggedIn;

    public ServiceConnectorBase(MainActivityBase context, String serviceUrl) {
        mContext = context;
        requestQueue = getRequestQueue();
        mMapper = new ObjectMapper();
        this.serviceUrl = serviceUrl;
    }

    private RequestQueue getRequestQueue() {
        if (requestQueue == null)
            requestQueue = Volley.newRequestQueue(mContext);
        return requestQueue;
    }

    public enum CrudType {
        CREATE, READ, UPDATE, DELETE
    }

    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }

    public void setHeaders(String token) {
        headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Bearer " + token);
    }

    public void getAuthentication(LoginInput input, Response.Listener<AbpResult<String>> onResponse) {
        @SuppressWarnings("unchecked")
        Map<String, Object> params = mMapper.convertValue(input, Map.class);
        ObjectRequest<AbpResult<String>> objectRequest = new ObjectRequest<>(
                Request.Method.POST,
                serviceUrl + "api/Account/Authenticate",
                AbpResult.class,
                params,
                onResponse,
                this::onErrorResponse);
        objectRequest.setRetryPolicy(new DefaultRetryPolicy(
                5000,
                3,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        addToRequestQueue(objectRequest);
    }

    public ObjectRequest<AbpResult<ArrayList>> getAllTenantsReq(TenantGetAllInput input) {
        @SuppressWarnings("unchecked")
        Map<String, Object> params = mMapper.convertValue(input, Map.class);
        ObjectRequest<AbpResult<ArrayList>> objectRequest = new ObjectRequest<>(
                Request.Method.POST,
                serviceUrl + "api/services/app/tenant/GetAllForClient",
                AbpResult.class,
                params,
                null,
                this::onErrorResponse);
        objectRequest.setRetryPolicy(new DefaultRetryPolicy(
                10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        return objectRequest;
    }

    public void getAllSettings(Response.Listener<AbpResult<ArrayList>> onResponse) {
        ObjectRequest<AbpResult<ArrayList>> objectRequest = new ObjectRequest<>(
                Request.Method.GET,
                serviceUrl + "api/Configuration/GetAllSettings",
                AbpResult.class,
                null,
                onResponse,
                this::onErrorResponse);
        objectRequest.setHeaders(headers);
        objectRequest.setRetryPolicy(new DefaultRetryPolicy(
                10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        addToRequestQueue(objectRequest);
    }

    public void getAllEntities(String controllerName, GetAllInput input, Response.Listener<AbpResult<ArrayList>> onResponse) {
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

    public void getAllAssetTypes(AssetTypeGetAllInput input, Response.Listener<AbpResult<ArrayList>> onResponse) {
        @SuppressWarnings("unchecked")
        Map<String, Object> params = mMapper.convertValue(input, Map.class);
        ObjectRequest<AbpResult<ArrayList>> objectRequest = new ObjectRequest<>(
                Request.Method.POST,
                serviceUrl + "api/services/app/assetType/GetAllForClient",
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

    public ObjectRequest<AbpResult<Double>> getCurrentPersonId() {
        ObjectRequest<AbpResult<Double>> objectRequest = new ObjectRequest<>(
                Request.Method.POST,
                serviceUrl + "api/services/app/person/GetCurrentPersonId",
                AbpResult.class,
                null,
                null,
                this::onErrorResponse);
        objectRequest.setHeaders(headers);
        objectRequest.setRetryPolicy(new DefaultRetryPolicy(
                5000,
                3,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        return objectRequest;
    }

    public ObjectRequest<AbpResult<Boolean>> getChangeCountedStateReq(ChangeCountedStateInput input) {
        @SuppressWarnings("unchecked")
        Map<String, Object> params = mMapper.convertValue(input, Map.class);
        ObjectRequest<AbpResult<Boolean>> objectRequest = new ObjectRequest<>(
                Request.Method.POST,
                serviceUrl + "api/services/app/asset/ChangeCountedState",
                AbpResult.class,
                params,
                null,
                this::onErrorResponse);
        objectRequest.setHeaders(headers);
        objectRequest.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                1,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        return objectRequest;
    }

    public ObjectRequest<AbpResult<Double>> getCountingOpReq(ICountingOp input) {
        @SuppressWarnings("unchecked")
        Map<String, Object> params = mMapper.convertValue(input, Map.class);
        ObjectRequest<AbpResult<Double>> objectRequest = new ObjectRequest<>(
                Request.Method.POST,
                serviceUrl + "api/services/app/countingOp/UpdateFromClient",
                AbpResult.class,
                params,
                null,
                this::onErrorResponse);
        objectRequest.setHeaders(headers);
        objectRequest.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                1,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        return objectRequest;
    }

    public ObjectRequest<AbpResult<ArrayList<Double>>> getCountedAssetIdsReq(ICountedStateChange input) {
        @SuppressWarnings("unchecked")
        Map<String, Object> params = mMapper.convertValue(input, Map.class);
        ObjectRequest<AbpResult<ArrayList<Double>>> objectRequest = new ObjectRequest<>(
                Request.Method.POST,
                serviceUrl + "api/services/app/countingOp/GetCountedAssetIds",
                AbpResult.class,
                params,
                null,
                this::onErrorResponse);
        objectRequest.setHeaders(headers);
        objectRequest.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                3,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        return objectRequest;
    }

    public ObjectRequest<AbpResult<Boolean>> getChangeAssignmentReq(ChangeAssignmentInput input) {
        @SuppressWarnings("unchecked")
        Map<String, Object> params = mMapper.convertValue(input, Map.class);
        ObjectRequest<AbpResult<Boolean>> objectRequest = new ObjectRequest<>(
                Request.Method.POST,
                serviceUrl + "api/services/app/asset/ChangeAssignmentState",
                AbpResult.class,
                params,
                null,
                this::onErrorResponse);
        objectRequest.setHeaders(headers);
        objectRequest.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                1,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        return objectRequest;
    }

    public ObjectRequest<AbpResult<Boolean>> getChangeLabeledStateReq(ChangeLabeledStateInput input, String type) {
        @SuppressWarnings("unchecked")
        Map<String, Object> params = mMapper.convertValue(input, Map.class);
        ObjectRequest<AbpResult<Boolean>> objectRequest = new ObjectRequest<>(
                Request.Method.POST,
                serviceUrl + "api/services/app/" + type + "/ChangeLabeledState",
                AbpResult.class,
                params,
                null,
                this::onErrorResponse);
        objectRequest.setHeaders(headers);
        objectRequest.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                1,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        return objectRequest;
    }

    public ObjectRequest<AbpResult<Boolean>> getChangeAssetDetailsReq(ChangeAssetDetailsInput input) {
        @SuppressWarnings("unchecked")
        Map<String, Object> params = mMapper.convertValue(input, Map.class);
        ObjectRequest<AbpResult<Boolean>> objectRequest = new ObjectRequest<>(
                Request.Method.POST,
                serviceUrl + "api/services/app/asset/ChangeAssetDetails",
                AbpResult.class,
                params,
                null,
                this::onErrorResponse);
        objectRequest.setHeaders(headers);
        objectRequest.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                1,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        return objectRequest;
    }

    public ObjectRequest<AbpResult<Boolean>> getSyncPhotosReq(ChangePhotoAvailabilityInput input) {
        @SuppressWarnings("unchecked")
        Map<String, Object> params = mMapper.convertValue(input, Map.class);
        ObjectRequest<AbpResult<Boolean>> objectRequest = new ObjectRequest<>(
                Request.Method.POST,
                serviceUrl + "api/services/app/synchronization/SyncImageInfo",
                AbpResult.class,
                params,
                null,
                this::onErrorResponse);
        objectRequest.setHeaders(headers);
        objectRequest.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                1,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        return objectRequest;
    }

    public ObjectRequest<AbpResult<ArrayList>> getAllWaitingLabel() {
        ObjectRequest<AbpResult<ArrayList>> objectRequest = new ObjectRequest<>(
                Request.Method.POST,
                serviceUrl + "api/services/app/asset/GetAllWaitingLabel",
                AbpResult.class,
                null,
                null,
                this::onErrorResponse);
        objectRequest.setHeaders(headers);
        objectRequest.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        return objectRequest;
    }

    public void uploadImages(List<File> images, String type) {
        try {
            OkHttpClient client = new OkHttpClient();
            var builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
            for (val image : images) {
                builder.addFormDataPart("file", image.getName(),
                        RequestBody.create(MediaType.parse("image/jpeg"), image));
            }
            val requestBody = builder.addFormDataPart("type", type)
                    .build();
            val request = new okhttp3.Request.Builder()
                    .url(serviceUrl + "api/Synchronization/UploadImages")
                    .post(requestBody)
                    .build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull final Call call, @NonNull final IOException e) {
                    call.cancel();
                    e.printStackTrace();
                }

                @Override
                public void onResponse(@NonNull final Call call, @NonNull final okhttp3.Response response) {
                    if (!response.isSuccessful()) {
                        Log.d("error", response.message());
                    }
                    Log.d("success", response.message());
                    // Upload successful
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void onErrorResponse(VolleyError error) {
        NetManager.isBusy = false;
        if (error instanceof TimeoutError)
            Toast.makeText(mContext, mContext.getString(R.string.alertTimeoutTryingAgain), Toast.LENGTH_LONG).show();
        else if (error.networkResponse == null)
            Toast.makeText(mContext, mContext.getString(R.string.alertServerOffline), Toast.LENGTH_LONG).show();
        else if (error.networkResponse.statusCode == 503)
            Toast.makeText(mContext, mContext.getString(R.string.alertServerOffline), Toast.LENGTH_LONG).show();
        else if (error.networkResponse.statusCode == 404)
            Toast.makeText(mContext, mContext.getString(R.string.alert_service_not_found), Toast.LENGTH_LONG).show();
        else if (error.networkResponse.statusCode == 401) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            getAuthentication(
                    new LoginInput(prefs.getString("tenancyName", ""),
                            prefs.getString("userName", ""),
                            prefs.getString("password", "")),
                    response -> setHeaders(response.getResult()));
        }
        else {
            try {
                AbpResult errorResponse = new Gson().fromJson(new String(error.networkResponse.data), AbpResult.class);
                if (errorResponse.getError() == null)
                    Toast.makeText(mContext, new String(error.networkResponse.data), Toast.LENGTH_LONG).show();
                else
                    Toast.makeText(mContext.getApplicationContext(), errorResponse.getError().getDetails(), Toast.LENGTH_LONG).show();
            }
            catch (Exception ex) {
                ex.printStackTrace();
                Toast.makeText(mContext, mContext.getString(R.string.alertServerOffline), Toast.LENGTH_LONG).show();
            }
        }
        mContext.progDialog.hide();
    }
}