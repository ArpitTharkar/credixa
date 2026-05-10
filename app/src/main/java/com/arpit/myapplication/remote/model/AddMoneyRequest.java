package com.arpit.myapplication.remote.model;

import java.math.BigDecimal;

public class AddMoneyRequest {
    public BigDecimal amount;

    public AddMoneyRequest(BigDecimal amount) { this.amount = amount; }
}
