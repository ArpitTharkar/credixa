package com.arpit.myapplication.remote.model;

public class AuthBridgeResponse {
    private Long userId;
    private String phone;
    private String balance;
    private boolean newlyCreated;

    public Long getUserId() {
        return userId;
    }

    public String getPhone() {
        return phone;
    }

    public String getBalance() {
        return balance;
    }

    public boolean isNewlyCreated() {
        return newlyCreated;
    }
}
