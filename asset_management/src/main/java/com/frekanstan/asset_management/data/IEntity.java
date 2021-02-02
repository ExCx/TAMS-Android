package com.frekanstan.asset_management.data;

import java.io.Serializable;

public interface IEntity extends Serializable {
    long getId();

    void setId(long id);

    int hashCode();

    boolean equals(Object o);
}
