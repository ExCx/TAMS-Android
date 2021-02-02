package com.frekanstan.dtys_mobil.app.sync;

import android.os.Bundle;

import com.frekanstan.asset_management.app.DAO;
import com.frekanstan.asset_management.data.ObjectBox;
import com.frekanstan.dtys_mobil.data.ImageToUpload;
import com.frekanstan.dtys_mobil.data.ImageToUpload_;

import io.objectbox.Box;
import io.objectbox.query.Query;
import lombok.val;
import lombok.var;

public class ImageToUploadDAO extends DAO<ImageToUpload>
{
    private static ImageToUploadDAO instance;

    private ImageToUploadDAO() { }

    public static ImageToUploadDAO getDao() {
        if (instance == null)
            instance = new ImageToUploadDAO();
        return instance;
    }

    @Override
    public Box<ImageToUpload> getBox() {
        return ObjectBox.get().boxFor(ImageToUpload.class);
    }

    @Override
    public Query<ImageToUpload> createFilteredQuery(Bundle input) {
        val isDelete = input.getByte("delete");
        val isMakeMain = input.getByte("makeMain");
        var builder = getBox().query();

        //delete filter
        if (isDelete == -1)
            builder.equal(ImageToUpload_.delete, false);
        else if (isDelete == 1)
            builder.equal(ImageToUpload_.delete, true);

        //makemain filter
        if (isMakeMain == -1)
            builder.equal(ImageToUpload_.makeMain, false);
        else if (isMakeMain == 1)
            builder.equal(ImageToUpload_.makeMain, true);

        return builder.build();
    }
}