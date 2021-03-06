package com.frekanstan.tatf_demo.app.sync;

import com.frekanstan.asset_management.app.DAO;
import com.frekanstan.asset_management.data.ObjectBox;
import com.frekanstan.tatf_demo.data.LabeledStateChange;

import io.objectbox.Box;

public class LabeledStateChangeDAO extends DAO<LabeledStateChange>
{
    private static LabeledStateChangeDAO instance;

    private LabeledStateChangeDAO() { }

    public static LabeledStateChangeDAO getDao() {
        if (instance == null)
            instance = new LabeledStateChangeDAO();
        return instance;
    }

    @Override
    public Box<LabeledStateChange> getBox() {
        return ObjectBox.get().boxFor(LabeledStateChange.class);
    }
}