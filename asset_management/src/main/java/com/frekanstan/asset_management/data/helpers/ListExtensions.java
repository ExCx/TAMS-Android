package com.frekanstan.asset_management.data.helpers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ListExtensions {
    public static <T> List<T> union(List<T> list1, List<T> list2) {
        Set<T> set = new HashSet<>();

        set.addAll(list1);
        set.addAll(list2);

        return new ArrayList<>(set);
    }

    public static <T> List<T> intersection(List<T> list1, List<T> list2) {
        List<T> list = new ArrayList<>();

        for (T t : list1) {
            if(list2.contains(t)) {
                list.add(t);
            }
        }

        return list;
    }
}
