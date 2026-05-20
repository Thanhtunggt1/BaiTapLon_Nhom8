package com.auction.network.dto;

import java.util.List;

public class AuctionDto {
    public String id;
    public String itemName;
    public String itemType;
    public String description;
    public double startingPrice;
    public double currentPrice;
    public String currentLeader;
    public String sellerUsername;
    public String status;
    public String startTime;
    public String endTime;
    public String finishedTime;
    public int bidCount;
    public List<String> imagesBase64; // Đổi thành List

    public List<BidEntryDto> history;

    public static class BidEntryDto {
        public String bidderName;
        public double amount;
        public String time;
    }
}