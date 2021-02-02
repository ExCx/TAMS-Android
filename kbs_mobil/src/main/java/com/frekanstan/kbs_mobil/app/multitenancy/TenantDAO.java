package com.frekanstan.kbs_mobil.app.multitenancy;

import android.os.Bundle;

import com.frekanstan.asset_management.app.DAO;
import com.frekanstan.asset_management.data.ObjectBox;
import com.frekanstan.kbs_mobil.data.Tenant;
import com.frekanstan.kbs_mobil.data.Tenant_;
import com.google.common.base.Strings;

import io.objectbox.Box;
import io.objectbox.query.Query;
import io.objectbox.query.QueryBuilder;
import lombok.val;

public class TenantDAO extends DAO<Tenant>
{
    private static TenantDAO instance;

    private TenantDAO() { }

    public static TenantDAO getDao() {
        if (instance == null)
            instance = new TenantDAO();
        return instance;
    }

    @Override
    public Box<Tenant> getBox() {
        return ObjectBox.get().boxFor(Tenant.class);
    }

    public Tenant get(String tenancyName) {
        return getBox().query().equal(Tenant_.tenancyName, tenancyName).build().findFirst();
    }

    @Override
    public Query<Tenant> createFilteredQuery(Bundle input) {
        val q = input.getString("query");
        QueryBuilder<Tenant> builder = getBox().query();
        if (!Strings.isNullOrEmpty(q))
            builder.startsWith(Tenant_.name, q).or().contains(Tenant_.name, " " + q);
        return builder.build();
    }
}