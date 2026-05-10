package com.arpit.myapplication.remote.model;

public class RequestOtpRequest {
    private final String phone;

    public RequestOtpRequest(String phone) {
        this.phone = phone;
    }

    public String getPhone() {
        return phone;
    }
}
