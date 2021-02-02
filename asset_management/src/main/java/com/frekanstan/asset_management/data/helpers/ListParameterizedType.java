package com.frekanstan.asset_management.data.helpers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;

public class ListParameterizedType implements ParameterizedType {

    private Type type;

    public ListParameterizedType(Type type) {
        this.type = type;
    }

    @NonNull
    @Override
    public Type[] getActualTypeArguments() {
        return new Type[] {type};
    }

    @NonNull
    @Override
    public Type getRawType() {
        return ArrayList.class;
    }

    @Override
    public Type getOwnerType() {
        return null;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return super.equals(obj);
    }
}