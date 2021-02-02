package com.frekanstan.asset_management.app;

import io.objectbox.android.ObjectBoxDataSource;
import io.objectbox.query.Query;

public class MyObjectBoxDataSource<T> extends ObjectBoxDataSource<T> {
    String q;

    public MyObjectBoxDataSource(Query<T> query) {
        super(query);
    }

    /*public static class Factory<Item> extends ObjectBoxDataSource.Factory<Item> {

        public Factory(Query<Item> query) {
            super(query);
        }

        @Override
        public DataSource<Integer, Item> create() {
            return new MyObjectBoxDataSource<>(query);
        }
    }*/

        /*void create(): DataSource<Int, Project> {
            return if (isAlphabetized) {
                if (query == "") {
                    dao.getProjectsAlphabetized(minBankers).map {
                        it
                    }.create()
                } else {
                    dao.searchProjectByName("%$query%", minBankers).map {
                        it
                    }.create()
                }
            } else {
                if (query == "") {
                    dao.getProjectsSortedByTime(minBankers).map {
                        it
                    }.create()
                } else {
                    dao.searchProjectByName("%$query%", minBankers).map {
                        it
                    }.create()
                }

            }
        }*/

        void search(String q) {
            this.q = q;
        }
}
