package com.frekanstan.asset_management.data.labeling;

import java.io.Serializable;
import java.util.Collection;

public class ChangeLabeledStateInput implements Serializable {
    public Collection<ILabeledStateChange> inputs;

    public ChangeLabeledStateInput(Collection<ILabeledStateChange> inputs) {
        this.inputs = inputs;
    }
}