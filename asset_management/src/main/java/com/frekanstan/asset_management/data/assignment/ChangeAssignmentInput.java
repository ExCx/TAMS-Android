package com.frekanstan.asset_management.data.assignment;

import java.io.Serializable;
import java.util.Collection;

public class ChangeAssignmentInput implements Serializable {
    public Collection<IAssignmentChange> inputs;

    public ChangeAssignmentInput(Collection<IAssignmentChange> inputs) {
        this.inputs = inputs;
    }
}