package com.frekanstan.tatf_demo.app.assets;

import com.frekanstan.asset_management.app.DAO;
import com.frekanstan.asset_management.data.ObjectBox;
import com.frekanstan.tatf_demo.data.Brand;

import io.objectbox.Box;

public class BrandDAO extends DAO<Brand>
{
    private static BrandDAO instance;

    private BrandDAO() { }

    public static BrandDAO getDao() {
        if (instance == null)
            instance = new BrandDAO();
        return instance;
    }

    @Override
    public Box<Brand> getBox() {
        return ObjectBox.get().boxFor(Brand.class);
    }
}