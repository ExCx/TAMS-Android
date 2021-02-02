package com.frekanstan.asset_management.app.webservice;

import java.io.Serializable;

public class LoginInput implements Serializable {
    private String tenancyName;
    private String usernameOrEmailAddress;
    private String password;

    public LoginInput(String tenancyName, String username, String password){
        setTenancyName(tenancyName);
        setUsernameOrEmailAddress(username);
        setPassword(password);
    }

    public String getTenancyName() {
        return tenancyName;
    }

    public void setTenancyName(String tenancyName) {
        this.tenancyName = tenancyName;
    }

    public String getUsernameOrEmailAddress() {
        return usernameOrEmailAddress;
    }

    public void setUsernameOrEmailAddress(String usernameOrEmailAddress) {
        this.usernameOrEmailAddress = usernameOrEmailAddress;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
