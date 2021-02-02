package com.frekanstan.dtys_mobil.data;

import com.frekanstan.asset_management.data.EntityBase;
import com.frekanstan.asset_management.data.photo.IImageToUpload;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
public class ImageToUpload extends EntityBase implements IImageToUpload {
    @Id
    @Getter @Setter
    public long id;

    @Getter
    private String type;

    @Getter
    private String path;

    @Setter
    private boolean delete;

    @Setter
    private boolean makeMain;

    public ImageToUpload() {
    }

    public ImageToUpload(String path, String type, boolean delete, boolean makeMain) {
        this.path = path;
        this.type = type;
        this.delete = delete;
        this.makeMain = makeMain;
    }

    public boolean getDelete() {
        return delete;
    }

    public boolean getMakeMain() {
        return makeMain;
    }
}