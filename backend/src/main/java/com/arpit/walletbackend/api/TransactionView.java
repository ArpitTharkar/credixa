package com.arpit.walletbackend.api;

import com.arpit.walletbackend.tx.TransactionStatus;
import com.arpit.walletbackend.tx.WalletTransaction;

import java.math.BigDecimal;
import java.time.Instant;

public class TransactionView {
    private Long id;
    private Long senderUserId;
    private Long receiverUserId;
    private BigDecimal amount;
    private TransactionStatus status;
    private Instant createdAt;
    private String direction;

    public static TransactionView from(WalletTransaction tx, Long currentUserId) {
        TransactionView view = new TransactionView();
        view.id = tx.getId();
        view.senderUserId = tx.getSenderUserId();
        view.receiverUserId = tx.getReceiverUserId();
        view.amount = tx.getAmount();
        view.status = tx.getStatus();
        view.createdAt = tx.getCreatedAt();

        if (tx.getSenderUserId() != null && tx.getReceiverUserId() != null && tx.getSenderUserId().equals(tx.getReceiverUserId())) {
            view.direction = "ADD";
        } else if (currentUserId != null && currentUserId.equals(tx.getReceiverUserId())) {
            view.direction = "RECEIVED";
        } else {
            view.direction = "SEND";
        }
        return view;
    }

    public Long getId() {
        return id;
    }

    public Long getSenderUserId() {
        return senderUserId;
    }

    public Long getReceiverUserId() {
        return receiverUserId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getDirection() {
        return direction;
    }
}
