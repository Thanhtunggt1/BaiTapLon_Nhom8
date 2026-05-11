package com.auction.model.entity;

import com.auction.model.enums.ItemType;
import com.auction.pattern.factory.ItemFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Seller extends User {

    private final List<Item> items;
    private final List<Auction> auctions;

    public Seller(String username, String password, String email) {
        super(username, password, email);
        this.items = new ArrayList<>();
        this.auctions = new ArrayList<>();
    }

    public Item createItem(String name, String description,
                           double startingPrice, ItemType type,
                           Map<String, Object> extraParams) {
        Item item = ItemFactory.getInstance().createItem(type, name, description, startingPrice, extraParams);
        items.add(item);
        System.out.printf("[Seller:%s] Đã tạo sản phẩm '%s' (id=%s)%n",
                getUsername(), item.getName(), item.getId());
        return item;
    }

    public boolean updateItem(Item item, String newName, String newDesc, double newPrice) {
        if (!items.contains(item)) {
            throw new IllegalArgumentException("Sản phẩm không thuộc Seller này.");
        }

        boolean isLocked = auctions.stream()
                .anyMatch(a -> a.getItem().equals(item)
                        && (a.getStatus() == com.auction.model.enums.AuctionStatus.RUNNING
                        || a.getStatus() == com.auction.model.enums.AuctionStatus.FINISHED
                        || a.getStatus() == com.auction.model.enums.AuctionStatus.PAID));

        if (isLocked) {
            throw new IllegalStateException("Không thể sửa thông tin! Sản phẩm đang được đấu giá hoặc đã bán thành công.");
        }

        if (newName != null && !newName.isBlank()) item.setName(newName);
        if (newDesc != null && !newDesc.isBlank()) item.setDescription(newDesc);
        if (newPrice > 0) item.setStartingPrice(newPrice);
        System.out.printf("[Seller:%s] Đã cập nhật sản phẩm '%s'%n", getUsername(), item.getName());
        return true;
    }

    public boolean deleteItem(Item item) {
        if (!items.contains(item)) {
            System.out.println("[Seller] Sản phẩm không thuộc Seller này.");
            return false;
        }
        boolean inActiveAuction = auctions.stream()
                .anyMatch(a -> a.getItem().equals(item)
                        && (a.getStatus() == com.auction.model.enums.AuctionStatus.RUNNING
                        || a.getStatus() == com.auction.model.enums.AuctionStatus.OPEN));
        if (inActiveAuction) {
            System.out.println("[Seller] Không thể xóa sản phẩm đang trong phiên đấu giá.");
            return false;
        }
        items.remove(item);
        System.out.printf("[Seller:%s] Đã xóa sản phẩm '%s'%n", getUsername(), item.getName());
        return true;
    }

    public Auction createAuction(Item item, LocalDateTime startTime, LocalDateTime endTime) {
        if (!items.contains(item)) {
            throw new IllegalArgumentException("Sản phẩm không thuộc Seller này.");
        }

        // Lọc xem sản phẩm đã có phiên đấu giá nào đang chạy/đã bán chưa (ngoại trừ phiên bị CANCELED)
        boolean isBusyOrSold = auctions.stream().anyMatch(a ->
                a.getItem().equals(item) &&
                        a.getStatus() != com.auction.model.enums.AuctionStatus.CANCELED
        );
        if (isBusyOrSold) {
            throw new IllegalStateException("Sản phẩm này đang được đấu giá hoặc đã bán thành công.");
        }

        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("Thời gian đấu giá không hợp lệ.");
        }
        Auction auction = new Auction(item, this, startTime, endTime);
        auctions.add(auction);
        System.out.printf("[Seller:%s] Đã tạo phiên đấu giá cho '%s' từ %s đến %s%n",
                getUsername(), item.getName(), startTime, endTime);
        return auction;
    }

    public List<Item> getItems() { return Collections.unmodifiableList(items); }

    @Override
    public void printInfo() {
        super.printInfo();
        System.out.printf("  └─ Số sản phẩm: %d | Số phiên đấu giá: %d%n",
                items.size(), auctions.size());
    }
}