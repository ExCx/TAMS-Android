package com.frekanstan.dtys_mobil.data;

import java.io.Serializable;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

public class GetAllPersons implements Serializable {
    @Getter @Setter
    private Boolean hasLeftJob;
    @Getter @Setter
    private String sorting;
    @Getter @Setter
    private Integer skipCount;
    @Getter @Setter
    private Integer maxResultCount;
    @Getter @Setter
    private List<String> searchStrings;
    @Getter @Setter
    private List<Long> ids;
}