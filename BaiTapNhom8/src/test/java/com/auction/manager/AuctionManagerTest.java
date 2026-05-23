package com.auction.manager;

import com.auction.model.entity.Auction;
import com.auction.model.entity.Item;
import com.auction.model.entity.Seller;
import com.auction.model.enums.AuctionStatus;
import com.auction.model.enums.ItemType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class AuctionManagerTest {

    private AuctionManager manager;
    private Auction auction;
    private Seller seller;
    private Item item;

    @BeforeEach
    public void setUp() throws Exception {
        manager = AuctionManager.getInstance();

        Field activeAuctionsField = AuctionManager.class.getDeclaredField("activeAuctions");
        activeAuctionsField.setAccessible(true);
        List<Auction> activeAuctions = (List<Auction>) activeAuctionsField.get(manager);
        activeAuctions.clear();

        seller = new Seller("manager_seller", "pass123", "seller@test.com");
        Map<String, Object> params = new HashMap<>();
        params.put("brand", "LG");
        params.put("warrantyMonths", 12);
        item = seller.createItem("Monitor", "27 inch", 3000.0, ItemType.ELECTRONICS, params);

        auction = new Auction(item, seller, LocalDateTime.now(), LocalDateTime.now().plusHours(2));
    }

    @Test
    public void testRegisterAuction() {
        manager.registerAuction(auction);
        List<Auction> list = manager.getAllAuctions();

        assertTrue(list.contains(auction));
        assertEquals(1, list.size());
    }

    @Test
    public void testStartUnregisteredAuctionThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> manager.startAuction(auction));
    }

    @Test
    public void testStartRegisteredAuction() {
        manager.registerAuction(auction);
        manager.startAuction(auction);

        assertEquals(AuctionStatus.RUNNING, auction.getStatus());
    }
}