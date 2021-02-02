package com.frekanstan.asset_management.app.connection;

import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Objects;

import lombok.val;

public class ObjectRequest<T> extends JsonRequest<T> {
    private static final Gson gson = new Gson();
    private final Class classType;
    private Response.Listener<T> responseListener;
    private Map<String, String> headers;

    private static final String TAG = "ObjectRequest";

    public ObjectRequest(int method,
                  String url,
                  Class classType,
                  Map<String, Object> params,
                  Response.Listener<T> responseListener,
                  Response.ErrorListener errorListener) {
        super(method, url, gson.toJson(params), responseListener, errorListener);
        this.classType = classType;
        this.responseListener = responseListener;
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        return headers != null ? headers : super.getHeaders();
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public void setResponseListener(Response.Listener<T> responseListener) {
        this.responseListener = responseListener;
    }

    @Override
    protected void deliverResponse(T response) {
        responseListener.onResponse(response);
    }

    @Override
    protected Response parseNetworkResponse(NetworkResponse response) {
        try {
            String json = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
            if (classType.getName().equals(JSONObject.class.getName())) {
                try {
                    val object = new JSONObject(json);
                    return Response.success(object, HttpHeaderParser.parseCacheHeaders(response));
                } catch (JSONException ex) {
                    return Response.error(new ParseError(ex));
                }

            } else if (classType.getName().equals(JSONArray.class.getName())) {
                try {
                    val object = new JSONArray(json);
                    return Response.success(object, HttpHeaderParser.parseCacheHeaders(response));
                } catch (JSONException ex) {
                    return Response.error(new ParseError(ex));
                }
            } else {
                return Response.success(gson.fromJson(json, classType), HttpHeaderParser.parseCacheHeaders(response));
            }
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, Objects.requireNonNull(e.getMessage()));
            return Response.error(new ParseError(e));
        }
    }
}