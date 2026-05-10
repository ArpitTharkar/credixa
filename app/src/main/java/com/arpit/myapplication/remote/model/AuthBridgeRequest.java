package com.arpit.myapplication.remote.model;

public class AuthBridgeRequest {
    private final String phone;

    public AuthBridgeRequest(String phone) {
        this.phone = phone;
    }

    public String getPhone() {
        return phone;
    }
}
