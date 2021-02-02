package com.frekanstan.tatf_demo.app.sync;

import android.os.Bundle;

import com.frekanstan.asset_management.app.DAO;
import com.frekanstan.asset_management.data.ObjectBox;
import com.frekanstan.tatf_demo.data.CountedStateChange;
import com.frekanstan.tatf_demo.data.CountedStateChange_;

import io.objectbox.Box;
import io.objectbox.query.Query;
import lombok.val;
import lombok.var;

public class CountedStateChangeDAO extends DAO<CountedStateChange>
{
    private static CountedStateChangeDAO instance;

    private CountedStateChangeDAO() { }

    public static CountedStateChangeDAO getDao() {
        if (instance == null)
            instance = new CountedStateChangeDAO();
        return instance;
    }

    @Override
    public Box<CountedStateChange> getBox() {
        return ObjectBox.get().boxFor(CountedStateChange.class);
    }

    @Override
    public Query<CountedStateChange> createFilteredQuery(Bundle input) {
        val readyToSend = input.getBoolean("readyToSend");
        var builder = getBox().query();
        if (readyToSend)
            builder.notEqual(CountedStateChange_.globalId, 0); //sent filter
        return builder.build();
    }
}