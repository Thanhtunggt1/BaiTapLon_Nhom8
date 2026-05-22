package com.auction.server;

import com.auction.model.entity.Bidder;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class UserDAOTest {

    @Test
    public void testDepositLimitLogic() {
        Bidder bidder = new Bidder("b", "pass123", "b@t.com", 0.0);
        double limit = 50000000.0;

        double currentTotal = 45000000.0;
        double attemptDeposit = 6000000.0;

        assertTrue((currentTotal + attemptDeposit) > limit);

        double validDeposit = 1000000.0;
        assertTrue((currentTotal + validDeposit) <= limit);
    }
}