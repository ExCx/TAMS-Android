package com.frekanstan.kbs_mobil.app.sync;

import android.os.Bundle;

import com.frekanstan.asset_management.app.DAO;
import com.frekanstan.asset_management.data.ObjectBox;
import com.frekanstan.kbs_mobil.data.AssignmentChange;
import com.frekanstan.kbs_mobil.data.AssignmentChange_;

import io.objectbox.Box;
import io.objectbox.query.Query;
import lombok.var;

public class AssignmentChangeDAO extends DAO<AssignmentChange>
{
    private static AssignmentChangeDAO instance;

    private AssignmentChangeDAO() { }

    public static AssignmentChangeDAO getDao() {
        if (instance == null)
            instance = new AssignmentChangeDAO();
        return instance;
    }

    @Override
    public Box<AssignmentChange> getBox() {
        return ObjectBox.get().boxFor(AssignmentChange.class);
    }

    @Override
    public Query<AssignmentChange> createFilteredQuery(Bundle input) {
        var builder = getBox().query();
        builder.equal(AssignmentChange_.sent, input.getBoolean("sent")); //sent filter
        return builder.build();
    }
}