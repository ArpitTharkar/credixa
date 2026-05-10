package com.arpit.myapplication.remote.model;

import java.math.BigDecimal;
import java.time.Instant;

public class TransactionDto {
    public Long id;
    public Long senderUserId;
    public Long receiverUserId;
    public BigDecimal amount;
    public String status;
    public String createdAt;
    public String direction;

    public Long getId() { return id; }
}
