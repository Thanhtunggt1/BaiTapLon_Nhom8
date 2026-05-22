package com.auction.pattern.observer;

import com.auction.model.entity.*;
import com.auction.model.enums.ItemType;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class AuctionObserverTest {

    @Test
    public void testObserverNotification() {
        Seller seller = new Seller("s", "pass123", "s@t.com");
        Bidder bidder = new Bidder("b", "pass123", "b@t.com", 10000.0);

        Map<String, Object> params = new HashMap<>();
        params.put("brand", "B"); params.put("warrantyMonths", 1);
        Item item = seller.createItem("I", "D", 100.0, ItemType.ELECTRONICS, params);

        Auction auction = new Auction(item, seller, LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        auction.startAuction();
        auction.placeBid(new BidTransaction(bidder, auction, 200.0));

        assertEquals(200.0, auction.getCurrentHighestPrice());
    }
}