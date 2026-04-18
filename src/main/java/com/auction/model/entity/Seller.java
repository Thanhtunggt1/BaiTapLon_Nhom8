package com.auction.model.entity;

import com.auction.model.enums.ItemType;
import com.auction.pattern.factory.ItemFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Người bán (Seller).
 * Có thể tạo, sửa, xóa sản phẩm và tạo phiên đấu giá.
 */
public class Seller extends User {

    private final List<Item> items;
    private final List<Auction> auctions;

    public Seller(String username, String password, String email) {
        super(username, password, email);
        this.items = new ArrayList<>();
        this.auctions = new ArrayList<>();
    }

    // ── Item management ───────────────────────────────────────────────────────

    /**
     * Tạo sản phẩm mới thông qua ItemFactory.
     *
     * @param name         tên sản phẩm
     * @param description  mô tả
     * @param startingPrice giá khởi điểm
     * @param type         loại sản phẩm
     * @param extraParams  các thuộc tính đặc thù theo loại (warrantyMonths, brand, v.v.)
     * @return Item vừa tạo
     */


    /*
    * Map<String, Object> extraParams trong Java có nghĩa là một tập hợp (map) chứa các cặp key–value, trong đó:
    * Key: là một chuỗi (String) dùng để đặt tên cho tham số bổ sung.
    * Value: là một đối tượng (Object) có thể mang bất kỳ kiểu dữ liệu nào (số, chuỗi, boolean, thậm chí là một đối tượng phức tạp)
    * Có thể điền dữ liệu vào Map<String, Object> bằng cách tạo một HashMap (hoặc bất kỳ implementation nào của Map)
    * Rồi dùng phương thức put(key, value) để thêm cặp key–value
    * */

    public Item createItem(String name, String description,
                           double startingPrice, ItemType type,
                           Map<String, Object> extraParams) {
        Item item = ItemFactory.getInstance().createItem(type, name, description, startingPrice, extraParams);
        items.add(item);
        System.out.printf("[Seller:%s] Đã tạo sản phẩm '%s' (id=%s)%n",
                getUsername(), item.getName(), item.getId());
        return item;
    }

    /**
     * Cập nhật thông tin sản phẩm.
     *
     * @param item        sản phẩm cần sửa (phải thuộc Seller này)
     * @param newName     tên mới (null = giữ nguyên)
     * @param newDesc     mô tả mới (null = giữ nguyên)
     * @param newPrice    giá mới (<=0 = giữ nguyên)
     * @return true nếu cập nhật thành công
     */
    public boolean updateItem(Item item, String newName, String newDesc, double newPrice) {
        if (!items.contains(item)) {
            System.out.println("[Seller] Sản phẩm không thuộc Seller này.");
            return false;
        }
        if (newName != null && !newName.isBlank()) item.setName(newName);
        if (newDesc != null && !newDesc.isBlank()) item.setDescription(newDesc);
        if (newPrice > 0) item.setStartingPrice(newPrice);
        System.out.printf("[Seller:%s] Đã cập nhật sản phẩm '%s'%n", getUsername(), item.getName());
        return true;
    }

    /**
     * Xóa sản phẩm khỏi danh sách (không thể xóa nếu đang trong phiên RUNNING).
     *
     * @param item sản phẩm cần xóa
     * @return true nếu xóa thành công
     */
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

    // ── Auction management ────────────────────────────────────────────────────

    /**
     * Tạo phiên đấu giá mới cho sản phẩm.
     *
     * @param item      sản phẩm muốn đấu giá (phải thuộc Seller này)
     * @param startTime thời gian bắt đầu
     * @param endTime   thời gian kết thúc
     * @return Auction vừa tạo
     */
    public Auction createAuction(Item item, LocalDateTime startTime, LocalDateTime endTime) {
        if (!items.contains(item)) {
            throw new IllegalArgumentException("Sản phẩm không thuộc Seller này.");
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

    // ── Getters ───────────────────────────────────────────────────────────────

    /*
     * Collections.unmodifiableList(items)
     * Dùng để tạo ra một danh sách không thể chỉnh sửa (read-only view) từ danh sách gốc items
     * */

    public List<Item> getItems() { return Collections.unmodifiableList(items); }

    public List<Auction> getAuctions() { return Collections.unmodifiableList(auctions); }

    @Override
    public void printInfo() {
        super.printInfo();
        System.out.printf("  └─ Số sản phẩm: %d | Số phiên đấu giá: %d%n",
                items.size(), auctions.size());
    }
}