package com.frekanstan.gateway_mobil.app.settings;

import android.os.Bundle;

import com.frekanstan.asset_management.app.DAO;
import com.frekanstan.asset_management.data.ObjectBox;
import com.frekanstan.gateway_mobil.data.Setting;
import com.frekanstan.gateway_mobil.data.Setting_;

import io.objectbox.Box;
import io.objectbox.query.Query;

public class SettingDAO extends DAO<Setting>
{
    private static SettingDAO instance;

    private SettingDAO() { }

    public static SettingDAO getDao() {
        if (instance == null)
            instance = new SettingDAO();
        return instance;
    }

    @Override
    public Box<Setting> getBox() {
        return ObjectBox.get().boxFor(Setting.class);
    }

    public Setting getByName(String name) {
        return getBox().query().equal(Setting_.name, name).build().findFirst();
    }

    @Override
    public Query<Setting> createFilteredQuery(Bundle input) {
        return null;
    }
}