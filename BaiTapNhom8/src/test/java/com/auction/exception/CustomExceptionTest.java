package com.auction.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CustomExceptionTest {

    @Test
    void testAuctionClosedExceptionMessage() {
        String message = "Phiên đấu giá đã đóng";
        AuctionClosedException exception = new AuctionClosedException(message);

        assertEquals(message, exception.getMessage());
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    void testAuctionClosedExceptionThrow() {
        assertThrows(AuctionClosedException.class, () -> {
            throw new AuctionClosedException("Test exception");
        });
    }

    @Test
    void testInvalidBidExceptionMessage() {
        String message = "Giá đặt không hợp lệ";
        InvalidBidException exception = new InvalidBidException(message);

        assertEquals(message, exception.getMessage());
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    void testInvalidBidExceptionThrow() {
        assertThrows(InvalidBidException.class, () -> {
            throw new InvalidBidException("Bid too low");
        });
    }

    @Test
    void testInsufficientBalanceExceptionMessage() {
        String message = "Số dư không đủ";
        InsufficientBalanceException exception = new InsufficientBalanceException(message);

        assertEquals(message, exception.getMessage());
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    void testInsufficientBalanceExceptionThrow() {
        assertThrows(InsufficientBalanceException.class, () -> {
            throw new InsufficientBalanceException("Insufficient balance");
        });
    }

    @Test
    void testExceptionStackTrace() {
        try {
            throw new AuctionClosedException("Test exception");
        } catch (AuctionClosedException e) {
            assertNotNull(e.getStackTrace());
            assertTrue(e.getStackTrace().length > 0);
        }
    }

    @Test
    void testExceptionCause() {
        Throwable cause = new RuntimeException("Root cause");
        AuctionClosedException exception = new AuctionClosedException("Auction closed");

        assertNull(exception.getCause());
    }

    @Test
    void testMultipleExceptionsMessage() {
        AuctionClosedException ex1 = new AuctionClosedException("Message 1");
        InvalidBidException ex2 = new InvalidBidException("Message 2");
        InsufficientBalanceException ex3 = new InsufficientBalanceException("Message 3");

        assertEquals("Message 1", ex1.getMessage());
        assertEquals("Message 2", ex2.getMessage());
        assertEquals("Message 3", ex3.getMessage());
    }

    @Test
    void testExceptionMessageWithSpecialCharacters() {
        String specialMessage = "Phiên [ABC-123] không ở trạng thái RUNNING!";
        AuctionClosedException exception = new AuctionClosedException(specialMessage);

        assertEquals(specialMessage, exception.getMessage());
        assertTrue(exception.getMessage().contains("ABC-123"));
    }

    @Test
    void testExceptionIsRuntimeException() {
        AuctionClosedException ex1 = new AuctionClosedException("test");
        InvalidBidException ex2 = new InvalidBidException("test");
        InsufficientBalanceException ex3 = new InsufficientBalanceException("test");

        assertTrue(ex1 instanceof RuntimeException);
        assertTrue(ex2 instanceof RuntimeException);
        assertTrue(ex3 instanceof RuntimeException);
    }
}

