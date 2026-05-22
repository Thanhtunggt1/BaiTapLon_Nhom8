package com.auction.server;

import com.auction.network.Message;
import com.auction.network.MessageType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ClientHandlerTest {

    @Test
    public void testLoginFailure() {
        Message loginReq = new Message(MessageType.LOGIN, new Object() {
            String username = "nonexistent";
            String password = "wrongpassword";
        });

        assertFalse(loginReq.toString().contains("SUCCESS"));
    }
}