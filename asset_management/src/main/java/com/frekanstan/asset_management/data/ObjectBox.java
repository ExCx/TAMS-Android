package com.frekanstan.asset_management.data;

import io.objectbox.BoxStore;

public class ObjectBox {
    private static BoxStore boxStore;

    public static void init(BoxStore store) {
        boxStore = store;
    }

    public static BoxStore get() { return boxStore; }
}