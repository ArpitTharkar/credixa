package com.arpit.walletbackend.auth.dto;

public class RequestOtpResponse {
    private final String message;
    private final String otpForDev;

    public RequestOtpResponse(String message, String otpForDev) {
        this.message = message;
        this.otpForDev = otpForDev;
    }

    public String getMessage() {
        return message;
    }

    public String getOtpForDev() {
        return otpForDev;
    }
}
