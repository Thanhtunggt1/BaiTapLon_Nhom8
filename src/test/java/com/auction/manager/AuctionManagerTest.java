package com.auction.manager;

import com.auction.model.entity.Auction;
import com.auction.model.entity.Item;
import com.auction.model.entity.Seller;
import com.auction.model.enums.AuctionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AuctionManagerTest {

    private AuctionManager manager;

    @BeforeEach
    void setUp() {
        manager = AuctionManager.getInstance();
    }

    @Test
    void testSingleton() {
        AuctionManager manager2 = AuctionManager.getInstance();
        assertSame(manager, manager2, "Manager phải là Singleton, chỉ có 1 instance duy nhất");
    }

    @Test
    void testCheckAndCloseExpiredAuctions() {
        Seller dummySeller = new Seller("s", "1", "e");
        Item dummyItem = new Item("Đồng hồ", "Mô tả", 100) {};

        // Tạo một phiên đấu giá với thời gian giả lập: Bắt đầu từ 2 ngày trước, Kết thúc từ 1 ngày trước
        // Điều này giúp phiên bị tính là isExpired() == true ngay khi vừa tạo
        Auction expiredAuction = new Auction(dummyItem, dummySeller,
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusDays(1));

        manager.registerAuction(expiredAuction);
        manager.startAuction(expiredAuction); // Trạng thái OPEN -> RUNNING

        assertEquals(AuctionStatus.RUNNING, expiredAuction.getStatus());

        // Gọi thủ công hàm dọn dẹp của Manager (bình thường hàm này chạy ngầm mỗi 5 giây)
        manager.checkAndCloseExpiredAuctions();

        // Kiểm tra xem Manager đã tự động đóng phiên chưa (Do không có ai bid nên sẽ về CANCELED)
        assertEquals(AuctionStatus.CANCELED, expiredAuction.getStatus(), "Phiên hết hạn phải được tự động đóng");
    }
}