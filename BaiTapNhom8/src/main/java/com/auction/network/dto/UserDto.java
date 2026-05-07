package com.auction.network.dto;

public class UserDto {
    public String username;
    public String role;      // "BIDDER", "SELLER", "ADMIN"
    public double balance;   // Chỉ có ý nghĩa với BIDDER
}
