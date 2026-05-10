package com.arpit.walletbackend.auth.dto;

import java.math.BigDecimal;

public class AuthBridgeResponse {
    private final Long userId;
    private final String phone;
    private final BigDecimal balance;
    private final boolean newlyCreated;

    public AuthBridgeResponse(Long userId, String phone, BigDecimal balance, boolean newlyCreated) {
        this.userId = userId;
        this.phone = phone;
        this.balance = balance;
        this.newlyCreated = newlyCreated;
    }

    public Long getUserId() {
        return userId;
    }

    public String getPhone() {
        return phone;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public boolean isNewlyCreated() {
        return newlyCreated;
    }
}
