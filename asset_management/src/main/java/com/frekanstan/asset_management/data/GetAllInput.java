package com.frekanstan.asset_management.data;

import java.io.Serializable;
import java.util.List;

public class GetAllInput<T> implements Serializable {
    private String sorting;
    private Integer skipCount;
    private Integer maxResultCount;
    private List<String> searchStrings;
    private List<T> ids;

    public GetAllInput() { }

    public GetAllInput(String sorting, Integer skipCount, Integer maxResultCount, List<String> searchStrings, List<T> ids) {
        this.sorting = sorting;
        this.skipCount = skipCount;
        this.maxResultCount = maxResultCount;
        this.searchStrings = searchStrings;
        this.ids = ids;
    }

    public String getSorting() {
        return sorting;
    }

    public void setSorting(String sorting) {
        this.sorting = sorting;
    }

    public Integer getSkipCount() {
        return skipCount;
    }

    public void setSkipCount(Integer skipCount) {
        this.skipCount = skipCount;
    }

    public Integer getMaxResultCount() {
        return maxResultCount;
    }

    public void setMaxResultCount(Integer maxResultCount) {
        this.maxResultCount = maxResultCount;
    }

    public List<String> getSearchStrings() {
        return searchStrings;
    }

    public void setSearchStrings(List<String> searchStrings) {
        this.searchStrings = searchStrings;
    }

    public List<T> getIds() {
        return ids;
    }

    public void setIds(List<T> ids) {
        this.ids = ids;
    }
}
