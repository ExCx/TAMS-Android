package com.frekanstan.asset_management.data.photo;

import com.frekanstan.asset_management.data.IEntity;

public interface IImageToUpload extends IEntity {
    String getPath();

    boolean getDelete();

    void setDelete(boolean delete);

    boolean getMakeMain();

    void setMakeMain(boolean makeMain);

    String getType();
}
