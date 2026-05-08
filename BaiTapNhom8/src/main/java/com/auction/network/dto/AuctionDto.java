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
    public int bidCount;

    // --- MỚI THÊM: Danh sách lịch sử để vẽ Biểu đồ và Bảng ---
    public List<BidEntryDto> history;

    // Lớp nội bộ để lưu từng dòng lịch sử đấu giá
    public static class BidEntryDto {
        public String bidderName;
        public double amount;
        public String time;
    }
}