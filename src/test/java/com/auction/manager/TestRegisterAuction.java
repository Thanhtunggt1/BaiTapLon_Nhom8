package com.auction.manager;

import com.auction.model.entity.Auction;
import com.auction.model.entity.Item;
import com.auction.model.entity.Seller;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestRegisterAuction {

    @Test
    void testRegisterAuction() {
        AuctionManager manager = AuctionManager.getInstance();

        Seller dummySeller = new Seller("s", "123456", "test@example.com");
        Item dummyItem = new Item("Đồng hồ", "Mô tả", 100) {};

        Auction newAuction = new Auction(dummyItem, dummySeller,
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusDays(1));

        // Đăng ký auction
        manager.registerAuction(newAuction);

        // Kiểm tra auction đã được thêm vào danh sách
        List<Auction> allAuctions = manager.getAllAuctions();
        assertTrue(allAuctions.contains(newAuction), "Auction phải được thêm vào danh sách");

        // Kiểm tra findById
        Auction found = manager.findById(newAuction.getId());
        assertNotNull(found, "Phải tìm thấy auction theo ID");
        assertEquals(newAuction, found, "Auction tìm thấy phải giống với auction đã đăng ký");
    }
}
