package com.auction.network.dto;

import java.util.Map;

public class CreateItemDto {
    public String name;
    public String description;
    public double startingPrice;
    public String itemType;          // "ELECTRONICS", "ART", "VEHICLE"
    public Map<String, Object> params; // brand, warrantyMonths, v.v.
}