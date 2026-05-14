package com.auction.network.dto;

import java.util.List;
import java.util.Map;

public class ItemDto {
    public String id;
    public String name;
    public String description;
    public double startingPrice;
    public String itemType;
    public Map<String, Object> params;
    public List<String> imagesBase64; // Đổi thành List
}