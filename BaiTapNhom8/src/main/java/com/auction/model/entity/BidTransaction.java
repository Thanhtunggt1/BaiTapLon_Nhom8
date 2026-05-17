package com.auction.model.entity;

import java.time.LocalDateTime;

public class BidTransaction extends Entity {

    private final Bidder bidder;
    private final Auction auction;
    private final double amount;
    private final LocalDateTime timestamp;

    public BidTransaction(Bidder bidder, Auction auction, double amount) {
        super();
        if (bidder == null) throw new IllegalArgumentException("Bidder không được null.");
        if (auction == null) throw new IllegalArgumentException("Auction không được null.");
        if (amount <= 0) throw new IllegalArgumentException("Số tiền đặt giá phải dương.");

        this.bidder = bidder;
        this.auction = auction;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
    }

    public boolean isValid() {
        if (auction.getStatus() != com.auction.model.enums.AuctionStatus.RUNNING) {
            return false;
        }
        return amount > auction.getCurrentHighestPrice();
    }

    public Bidder getBidder() { return bidder; }

    public Auction getAuction() { return auction; }

    public double getAmount() { return amount; }

    @Override
    public String toString() {
        return String.format("BidTransaction{id='%s', bidder='%s', amount=%.2f, time=%s}",
                getId(), bidder.getUsername(), amount, timestamp);
    }
}