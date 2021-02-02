package com.frekanstan.asset_management.app.webservice;

import java.io.Serializable;

public class AbpResult<T> implements Serializable {
    private T result;
    private Boolean success;
    private AbpError error;

    public T getResult() {
        return result;
    }

    public Boolean getSuccess() {
        return success;
    }

    public AbpError getError() {
        return error;
    }
}
