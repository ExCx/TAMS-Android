package com.frekanstan.asset_management.data;

import java.util.Objects;

public abstract class EntityBase implements IEntity
{
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        return getId() == ((IEntity)o).getId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    @Override
    public abstract long getId();

    @Override
    public abstract void setId(long id);
}
