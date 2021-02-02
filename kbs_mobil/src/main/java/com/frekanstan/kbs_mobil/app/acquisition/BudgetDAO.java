package com.frekanstan.kbs_mobil.app.acquisition;

import com.frekanstan.asset_management.app.DAO;
import com.frekanstan.asset_management.data.ObjectBox;
import com.frekanstan.kbs_mobil.data.Budget;

import io.objectbox.Box;

public class BudgetDAO extends DAO<Budget>
{
    private static BudgetDAO instance;

    private BudgetDAO() { }

    public static BudgetDAO getDao() {
        if (instance == null)
            instance = new BudgetDAO();
        return instance;
    }

    @Override
    public Box<Budget> getBox() {
        return ObjectBox.get().boxFor(Budget.class);
    }
}