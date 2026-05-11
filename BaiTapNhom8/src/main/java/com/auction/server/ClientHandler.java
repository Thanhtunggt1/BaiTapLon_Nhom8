package com.auction.server;

import com.auction.manager.AuctionManager;
import com.auction.model.entity.*;
import com.auction.network.Message;
import com.auction.network.MessageType;
import com.auction.network.dto.*;

import java.io.*;
import java.net.Socket;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

public class ClientHandler implements Runnable {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private User currentUser;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

            String line;
            while ((line = in.readLine()) != null) {
                try {
                    Message request = Message.fromJson(line);
                    Message response = handleRequest(request);
                    if (response != null) sendMessage(response.toJson());
                } catch (Exception e) {
                    System.err.println("[Handler] Lỗi: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("[Server] Client ngắt kết nối.");
        } finally {
            AuctionServer.removeClient(this);
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private Message handleRequest(Message request) {
        switch (request.getType()) {
            case LOGIN: {
                LoginPayload loginDto = request.getPayload(LoginPayload.class);
                User user = UserDAO.findByUsername(loginDto.username);

                if (user != null && user.getPassword().equals(loginDto.password)) {
                    this.currentUser = user;
                    UserDto respDto = new UserDto();
                    respDto.username = user.getUsername();
                    if (user instanceof Admin) respDto.role = "ADMIN";
                    else if (user instanceof Seller) respDto.role = "SELLER";
                    else respDto.role = "BIDDER";

                    if (user instanceof Bidder) respDto.balance = ((Bidder) user).getBalance();
                    return Message.success(MessageType.LOGIN_RESPONSE, respDto);
                }
                return Message.error(MessageType.LOGIN_RESPONSE, "Sai tài khoản hoặc mật khẩu.");
            }

            case REGISTER: {
                RegisterPayload regDto = request.getPayload(RegisterPayload.class);
                if (UserDAO.findByUsername(regDto.username) != null) {
                    return Message.error(MessageType.REGISTER_RESPONSE, "Tên đã tồn tại.");
                }

                User newUser;
                if ("SELLER".equalsIgnoreCase(regDto.role)) {
                    newUser = new Seller(regDto.username, regDto.password, regDto.email);
                } else {
                    newUser = new Bidder(regDto.username, regDto.password, regDto.email, 0.0);
                }

                if (UserDAO.insertUser(newUser, regDto.role)) {
                    return Message.success(MessageType.REGISTER_RESPONSE, "Thành công!");
                }
                return Message.error(MessageType.REGISTER_RESPONSE, "Lỗi Database.");
            }

            case GET_AUCTIONS: {
                List<AuctionDto> dtoList = AuctionManager.getInstance().getAllAuctions().stream()
                        .map(this::toAuctionDto)
                        .collect(Collectors.toList());
                return Message.success(MessageType.AUCTIONS_RESPONSE, dtoList);
            }

            case DEPOSIT: {
                double amount = request.getPayload(Double.class);
                if (this.currentUser instanceof Bidder bidder) {
                    bidder.deposit(amount);
                    UserDAO.updateUserBalance(bidder);
                    UserDto respDto = new UserDto();
                    respDto.username = bidder.getUsername();
                    respDto.role = "BIDDER";
                    respDto.balance = bidder.getBalance();
                    return Message.success(MessageType.DEPOSIT_RESPONSE, respDto);
                }
                return Message.error(MessageType.DEPOSIT_RESPONSE, "Chỉ Bidder mới có thể nạp tiền.");
            }

            case CREATE_ITEM: {
                CreateItemDto dto = request.getPayload(CreateItemDto.class);
                if (this.currentUser instanceof Seller seller) {
                    try {
                        Item item = seller.createItem(dto.name, dto.description, dto.startingPrice,
                                com.auction.model.enums.ItemType.valueOf(dto.itemType), dto.params);
                        Map<String, String> payload = new HashMap<>();
                        payload.put("id", item.getId());
                        return Message.success(MessageType.CREATE_ITEM_RESPONSE, payload);
                    } catch (Exception e) {
                        return Message.error(MessageType.CREATE_ITEM_RESPONSE, e.getMessage());
                    }
                }
                return Message.error(MessageType.CREATE_ITEM_RESPONSE, "Lỗi quyền hạn.");
            }

            case UPDATE_ITEM: {
                UpdateItemPayload dto = request.getPayload(UpdateItemPayload.class);
                if (this.currentUser instanceof Seller seller) {
                    try {
                        for (Item item : seller.getItems()) {
                            if (item.getId().equals(dto.itemId)) {
                                seller.updateItem(item, dto.name, dto.description, dto.startingPrice);
                                return Message.success(MessageType.UPDATE_ITEM_RESPONSE, "Thành công");
                            }
                        }
                    } catch (Exception e) {
                        return Message.error(MessageType.UPDATE_ITEM_RESPONSE, e.getMessage());
                    }
                }
                return Message.error(MessageType.UPDATE_ITEM_RESPONSE, "Thao tác không hợp lệ.");
            }

            case CREATE_AUCTION: {
                CreateAuctionDto dto = request.getPayload(CreateAuctionDto.class);
                if (this.currentUser instanceof Seller seller) {
                    try {
                        Item targetItem = seller.getItems().stream()
                                .filter(i -> i.getId().equals(dto.itemId))
                                .findFirst().orElse(null);
                        if (targetItem == null) return Message.error(MessageType.CREATE_AUCTION_RESPONSE, "Không tìm thấy SP.");

                        Auction auction = seller.createAuction(targetItem, java.time.LocalDateTime.now(),
                                java.time.LocalDateTime.now().plusMinutes(dto.durationMinutes));
                        AuctionManager.getInstance().registerAuction(auction);
                        if (dto.startNow) AuctionManager.getInstance().startAuction(auction);

                        return Message.success(MessageType.CREATE_AUCTION_RESPONSE, "Thành công");
                    } catch (Exception e) {
                        return Message.error(MessageType.CREATE_AUCTION_RESPONSE, e.getMessage());
                    }
                }
                return Message.error(MessageType.CREATE_AUCTION_RESPONSE, "Lỗi quyền hạn.");
            }

            case PLACE_BID: {
                BidDto dto = request.getPayload(BidDto.class);
                if (this.currentUser instanceof Bidder bidder) {
                    try {
                        for (Auction a : AuctionManager.getInstance().getAllAuctions()) {
                            if (a.getId().equals(dto.auctionId)) {
                                bidder.placeBid(a, dto.amount);
                                AuctionServer.broadcast(new Message(MessageType.BID_UPDATE, toAuctionDto(a)).toJson());
                                return Message.success(MessageType.BID_RESPONSE, toAuctionDto(a));
                            }
                        }
                    } catch (Exception e) {
                        return Message.error(MessageType.BID_RESPONSE, e.getMessage());
                    }
                }
                return Message.error(MessageType.BID_RESPONSE, "Lỗi quyền hạn.");
            }

            case SETUP_AUTOBID: {
                AutoBidPayload dto = request.getPayload(AutoBidPayload.class);
                if (this.currentUser instanceof Bidder bidder) {
                    try {
                        for (Auction a : AuctionManager.getInstance().getAllAuctions()) {
                            if (a.getId().equals(dto.auctionId)) {
                                bidder.setupAutoBid(a, dto.maxBid, dto.increment);
                                return Message.success(MessageType.SETUP_AUTOBID_RESPONSE, "Thành công");
                            }
                        }
                    } catch (Exception e) {
                        return Message.error(MessageType.SETUP_AUTOBID_RESPONSE, e.getMessage());
                    }
                }
                return Message.error(MessageType.SETUP_AUTOBID_RESPONSE, "Lỗi quyền hạn.");
            }

            case START_AUCTION: {
                String id = request.getPayload(String.class);
                AuctionManager.getInstance().getAllAuctions().stream()
                        .filter(a -> a.getId().equals(id))
                        .forEach(a -> a.startAuction());
                return Message.success(MessageType.START_AUCTION_RESPONSE, "Đã bắt đầu");
            }

            case END_AUCTION: {
                String id = request.getPayload(String.class);
                AuctionManager.getInstance().getAllAuctions().stream()
                        .filter(a -> a.getId().equals(id))
                        .forEach(a -> a.endAuction());
                return Message.success(MessageType.END_AUCTION, "Đã kết thúc");
            }

            case MARK_PAID: {
                String id = request.getPayload(String.class);
                try {
                    for (Auction a : AuctionManager.getInstance().getAllAuctions()) {
                        if (a.getId().equals(id)) {
                            a.markAsPaid();
                            if (a.getCurrentLeader() != null) UserDAO.updateUserBalance(a.getCurrentLeader());
                            return Message.success(MessageType.MARK_PAID_RESPONSE, "Thành công");
                        }
                    }
                } catch (Exception e) {
                    return Message.error(MessageType.MARK_PAID_RESPONSE, e.getMessage());
                }
                return Message.error(MessageType.MARK_PAID_RESPONSE, "Lỗi thao tác.");
            }

            case ADMIN_CANCEL_AUCTION: {
                AdminCancelPayload dto = request.getPayload(AdminCancelPayload.class);
                if (this.currentUser instanceof Admin admin) {
                    AuctionManager.getInstance().getAllAuctions().stream()
                            .filter(a -> a.getId().equals(dto.auctionId))
                            .forEach(a -> admin.resolveDispute(a, dto.reason));
                    return Message.success(MessageType.ADMIN_CANCEL_AUCTION, "Đã hủy bởi Admin");
                }
                return Message.error(MessageType.ADMIN_CANCEL_AUCTION, "Quyền Admin yêu cầu.");
            }

            default:
                return null;
        }
    }

    public void sendMessage(String json) {
        if (out != null) out.println(json);
    }

    private AuctionDto toAuctionDto(Auction a) {
        AuctionDto dto = new AuctionDto();
        dto.id = a.getId();
        dto.itemName = a.getItem().getName();
        dto.itemType = a.getItem().getClass().getSimpleName().toUpperCase();
        dto.description = a.getItem().getDescription();
        dto.startingPrice = a.getItem().getStartingPrice();
        dto.currentPrice = a.getCurrentHighestPrice();
        dto.status = a.getStatus().toString();
        dto.startTime = a.getStartTime().format(DTF);
        dto.endTime = a.getEndTime().format(DTF);
        dto.sellerUsername = a.getSeller().getUsername();
        dto.currentLeader = (a.getCurrentLeader() != null) ? a.getCurrentLeader().getUsername() : null;
        dto.bidCount = a.getBidHistory().size();

        dto.history = a.getBidHistory().stream().map(tx -> {
            AuctionDto.BidEntryDto entry = new AuctionDto.BidEntryDto();
            entry.bidderName = tx.getBidder().getUsername();
            entry.amount = tx.getAmount();
            entry.time = java.time.LocalDateTime.now().format(DTF);
            return entry;
        }).collect(Collectors.toList());

        return dto;
    }

    private static class LoginPayload { String username; String password; }
    private static class RegisterPayload { String username; String password; String email; String role; }
    private static class UpdateItemPayload { String itemId; String name; String description; double startingPrice; }
    private static class AdminCancelPayload { String auctionId; String reason; }
    private static class AutoBidPayload { String auctionId; double maxBid; double increment; }
}