package com.auction.network.dto;

public class AuctionDto {
    public String id;
    public String itemName;
    public String itemType;       // "Electronics", "Art", "Vehicle"
    public String description;
    public double startingPrice;
    public double currentPrice;
    public String currentLeader;  // username người dẫn đầu
    public String sellerUsername;
    public String status;         // "OPEN", "RUNNING", "FINISHED"...
    public String startTime;
    public String endTime;
    public int bidCount;
}