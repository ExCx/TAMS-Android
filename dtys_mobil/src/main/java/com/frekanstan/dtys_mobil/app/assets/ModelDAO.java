package com.frekanstan.dtys_mobil.app.assets;

import com.frekanstan.asset_management.app.DAO;
import com.frekanstan.asset_management.data.ObjectBox;
import com.frekanstan.dtys_mobil.data.Model;

import io.objectbox.Box;

public class ModelDAO extends DAO<Model>
{
    private static ModelDAO instance;

    private ModelDAO() { }

    public static ModelDAO getDao() {
        if (instance == null)
            instance = new ModelDAO();
        return instance;
    }

    @Override
    public Box<Model> getBox() {
        return ObjectBox.get().boxFor(Model.class);
    }
}