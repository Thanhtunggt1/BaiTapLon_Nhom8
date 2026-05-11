package com.auction.network.dto;

public class BidDto {
    public String auctionId;
    public double amount;

    // Bổ sung thêm constructor này
    public BidDto(String auctionId, double amount) {
        this.auctionId = auctionId;
        this.amount = amount;
    }
}