package com.frekanstan.tatf_demo.app.tracking;

import android.os.Bundle;

import com.frekanstan.asset_management.app.DAO;
import com.frekanstan.asset_management.data.ObjectBox;
import com.frekanstan.tatf_demo.app.multitenancy.TenantRepository;
import com.frekanstan.tatf_demo.data.CountingOp;
import com.frekanstan.tatf_demo.data.CountingOp_;

import java.util.ArrayList;

import io.objectbox.Box;
import io.objectbox.query.Query;
import lombok.val;
import lombok.var;

public class CountingOpDAO extends DAO<CountingOp>
{
    private static CountingOpDAO instance;

    private CountingOpDAO() { }

    public static CountingOpDAO getDao() {
        if (instance == null)
            instance = new CountingOpDAO();
        return instance;
    }

    @Override
    public Box<CountingOp> getBox() {
        return ObjectBox.get().boxFor(CountingOp.class);
    }

    @Override
    public ArrayList<CountingOp> getAll() {
        return new ArrayList<>(getBox().query().equal(CountingOp_.tenantId, TenantRepository.getCurrentTenant().getId())
                .order(CountingOp_.creationTime).build().find());
    }

    @Override
    public Query<CountingOp> createFilteredQuery(Bundle input) {
        var builder = getBox().query().equal(CountingOp_.tenantId, TenantRepository.getCurrentTenant().getId()); //tenant filter
        val isUpdated = input.getBoolean("isUpdated", false);

        if (isUpdated)
            builder.equal(CountingOp_.isUpdated, true);

        return builder.build();
    }
}