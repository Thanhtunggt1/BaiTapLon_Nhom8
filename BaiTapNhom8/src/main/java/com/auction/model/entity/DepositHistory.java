package com.auction.model.entity;

import java.time.LocalDateTime;

public class DepositHistory {
    private final double amount;
    private final double balanceAfterDeposit;
    private final LocalDateTime createdAt;

    public DepositHistory(double amount, double balanceAfterDeposit) {
        this.amount = amount;
        this.balanceAfterDeposit = balanceAfterDeposit;
        this.createdAt = LocalDateTime.now();
    }

    public double getAmount() {
        return amount;
    }

    public double getBalanceAfterDeposit() {
        return balanceAfterDeposit;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
