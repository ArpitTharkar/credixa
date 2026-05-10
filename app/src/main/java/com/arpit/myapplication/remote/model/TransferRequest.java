package com.arpit.myapplication.remote.model;

import java.math.BigDecimal;

public class TransferRequest {
    public Long senderUserId;
    public Long receiverUserId;
    public BigDecimal amount;
    public String idempotencyKey;

    public TransferRequest(Long senderUserId, Long receiverUserId, BigDecimal amount, String idempotencyKey) {
        this.senderUserId = senderUserId;
        this.receiverUserId = receiverUserId;
        this.amount = amount;
        this.idempotencyKey = idempotencyKey;
    }
}
