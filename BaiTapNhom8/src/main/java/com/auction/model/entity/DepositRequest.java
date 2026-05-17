package com.auction.model.entity;

import com.auction.model.enums.DepositStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class DepositRequest {

    private final String id;
    private final Bidder bidder;
    private final double amount;
    private DepositStatus status;
    private final LocalDateTime createdAt;

    public DepositRequest(Bidder bidder, double amount) {
        if (bidder == null) {
            throw new IllegalArgumentException("Bidder không được null.");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Số tiền yêu cầu nạp phải dương.");
        }

        this.id = UUID.randomUUID().toString();
        this.bidder = bidder;
        this.amount = amount;
        this.status = DepositStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public Bidder getBidder() {
        return bidder;
    }

    public double getAmount() {
        return amount;
    }

    public DepositStatus getStatus() {
        return status;
    }

    public void setStatus(DepositStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
