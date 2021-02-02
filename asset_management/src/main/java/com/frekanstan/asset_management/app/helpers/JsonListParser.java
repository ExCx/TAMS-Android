package com.frekanstan.asset_management.app.helpers;

import android.os.AsyncTask;

import com.frekanstan.asset_management.app.IDAO;
import com.frekanstan.asset_management.app.webservice.AbpResult;
import com.frekanstan.asset_management.data.IEntity;
import com.google.gson.Gson;

import java.util.ArrayList;

import lombok.val;
import lombok.var;

public class JsonListParser<T extends IEntity> extends AsyncTask<Boolean, Void, Boolean>
{
    private Gson gson;
    private Class<T> clazz;
    private AbpResult<ArrayList> response;
    //private AbpResult<ArrayList> callback;
    private IDAO<T> dao;

    public JsonListParser(Gson gson, Class<T> clazz, AbpResult<ArrayList> response, IDAO<T> dao) {
        this.gson = gson;
        this.clazz = clazz;
        this.response = response;
        this.dao = dao;
    }

    @Override
    protected Boolean doInBackground(Boolean... initDb) {
        var list = new ArrayList<T>();
        for (val obj : response.getResult())
            list.add(gson.fromJson(gson.toJsonTree(obj).getAsJsonObject(), clazz));
        dao.putAll(list);
        return true;
    }
}
