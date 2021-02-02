package com.frekanstan.asset_management.data.tracking;

import java.io.Serializable;
import java.util.Collection;

public class ChangeCountedStateInput implements Serializable {
    public Collection<ICountedStateChange> inputs;

    public ChangeCountedStateInput(Collection<ICountedStateChange> inputs) {
        this.inputs = inputs;
    }
}