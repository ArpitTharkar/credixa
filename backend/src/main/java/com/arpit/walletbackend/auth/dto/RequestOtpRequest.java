package com.arpit.walletbackend.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class RequestOtpRequest {

    @NotBlank(message = "Phone is required")
    private String phone;

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
