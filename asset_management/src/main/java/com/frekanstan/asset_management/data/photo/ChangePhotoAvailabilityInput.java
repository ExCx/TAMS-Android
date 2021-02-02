package com.frekanstan.asset_management.data.photo;

import java.io.Serializable;
import java.util.Collection;

public class ChangePhotoAvailabilityInput implements Serializable {
    public Collection<IImageToUpload> inputs;

    public ChangePhotoAvailabilityInput(Collection<IImageToUpload> inputs) {
        this.inputs = inputs;
    }
}