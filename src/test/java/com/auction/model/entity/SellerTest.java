package com.auction.model.entity;

import com.auction.model.enums.AuctionStatus;
import com.auction.model.enums.ItemType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SellerTest {

    private Seller seller;
    private Item artItem;

    @BeforeEach
    void setUp() {
        seller = new Seller("seller_art", "pass1234", "art@test.com");
        artItem = seller.createItem("Tranh Mona Lisa", "Bản sao", 10000.0, ItemType.ART,
                Map.of("artistName", "Da Vinci", "creationYear", 1503));
    }

    @Test
    void testUpdateItem_Success() {
        // Cập nhật khi sản phẩm chưa được đem đi đấu giá
        boolean result = seller.updateItem(artItem, "Tranh Mona Lisa (Rep 1:1)", null, 12000.0);

        assertTrue(result);
        assertEquals("Tranh Mona Lisa (Rep 1:1)", artItem.getName());
        assertEquals(12000.0, artItem.getStartingPrice());
    }

    @Test
    void testUpdateItem_LockedWhenAuctionIsRunning() {
        // Tạo và chạy phiên đấu giá cho sản phẩm này
        Auction auction = seller.createAuction(artItem, LocalDateTime.now(), LocalDateTime.now().plusDays(2));
        auction.startAuction();

        // Cố gắng sửa thông tin sản phẩm -> Phải văng lỗi IllegalStateException
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            seller.updateItem(artItem, "Đổi tên lừa đảo", "Mô tả ảo", 500.0);
        });
        assertTrue(exception.getMessage().contains("Không thể sửa thông tin! Sản phẩm đang được đấu giá"));
    }

    @Test
    void testCreateAuction_ItemAlreadyInAuction() {
        // Tạo phiên đấu giá lần 1 (Hợp lệ)
        seller.createAuction(artItem, LocalDateTime.now(), LocalDateTime.now().plusDays(1));

        // Cố tình đem chính sản phẩm đó tạo thêm một phiên đấu giá thứ 2 cùng lúc
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            seller.createAuction(artItem, LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(3));
        });
        assertTrue(exception.getMessage().contains("Sản phẩm này đang được đấu giá hoặc đã bán thành công"));
    }
}