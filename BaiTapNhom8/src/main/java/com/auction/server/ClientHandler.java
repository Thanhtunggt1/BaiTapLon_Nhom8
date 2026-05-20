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
import java.util.UUID;
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
                    System.err.println("Lỗi xử lý gói tin: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Mất kết nối Client: " + e.getMessage());
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
                    if (user instanceof Bidder && ((Bidder) user).isBanned()) {
                        return Message.error(MessageType.LOGIN_RESPONSE, "Tài khoản của bạn đã bị KHÓA do không thanh toán quá 3 lần.");
                    }

                    this.currentUser = user;
                    UserDto respDto = new UserDto();
                    respDto.username = user.getUsername();

                    if (user instanceof Admin) {
                        respDto.role = "ADMIN";
                    } else if (user instanceof Seller) {
                        respDto.role = "SELLER";
                        Seller seller = (Seller) user;
                        respDto.items = seller.getItems().stream().map(i -> {
                            com.auction.network.dto.ItemDto dto = new com.auction.network.dto.ItemDto();
                            dto.id = i.getId();
                            dto.name = i.getName();
                            dto.description = i.getDescription();
                            dto.startingPrice = i.getStartingPrice();
                            dto.itemType = i.getClass().getSimpleName().toUpperCase();
                            dto.imagesBase64 = i.getImagesBase64();

                            java.util.Map<String, Object> params = new java.util.HashMap<>();
                            if ("ELECTRONICS".equals(dto.itemType)) {
                                params.put("brand", "N/A");
                                params.put("warrantyMonths", 0);
                            } else if ("ART".equals(dto.itemType)) {
                                params.put("artistName", "N/A");
                                params.put("creationYear", 0);
                            } else if ("VEHICLE".equals(dto.itemType)) {
                                params.put("mileage", 0.0);
                                params.put("licensePlate", "N/A");
                            }
                            dto.params = params;
                            return dto;
                        }).collect(Collectors.toList());
                    } else {
                        respDto.role = "BIDDER";
                        respDto.balance = ((Bidder) user).getBalance();
                    }
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

            case PROMOTE_USER: {
                PromotePayload dto = request.getPayload(PromotePayload.class);
                if (this.currentUser instanceof Admin) {
                    User targetUser = UserDAO.findByUsername(dto.username);
                    if (targetUser == null) return Message.error(MessageType.PROMOTE_USER_RESPONSE, "Không tìm thấy user.");

                    if (UserDAO.updateUserRole(dto.username, dto.role)) {
                        User newUser;
                        if ("ADMIN".equals(dto.role)) {
                            newUser = new Admin(targetUser.getUsername(), targetUser.getPassword(), targetUser.getEmail());
                        } else if ("SELLER".equals(dto.role)) {
                            newUser = new Seller(targetUser.getUsername(), targetUser.getPassword(), targetUser.getEmail());
                        } else {
                            newUser = new Bidder(targetUser.getUsername(), targetUser.getPassword(), targetUser.getEmail(), 0.0);
                        }
                        newUser.setId(targetUser.getId());
                        UserDAO.userCache.put(targetUser.getUsername(), newUser);
                        UserDAO.userByIdCache.put(targetUser.getId(), newUser);
                        return Message.success(MessageType.PROMOTE_USER_RESPONSE, "Thành công");
                    }
                    return Message.error(MessageType.PROMOTE_USER_RESPONSE, "Lỗi cập nhật.");
                }
                return Message.error(MessageType.PROMOTE_USER_RESPONSE, "Từ chối quyền truy cập.");
            }

            case CREATE_ITEM: {
                CreateItemDto dto = request.getPayload(CreateItemDto.class);
                if (this.currentUser instanceof Seller seller) {
                    try {
                        Item item = seller.createItem(dto.name, dto.description, dto.startingPrice,
                                com.auction.model.enums.ItemType.valueOf(dto.itemType), dto.params);
                        item.setImagesBase64(dto.imagesBase64);
                        ItemDAO.insertItem(item, seller.getId(), dto.itemType, dto.params);
                        Map<String, String> payload = new HashMap<>();
                        payload.put("id", item.getId());
                        return Message.success(MessageType.CREATE_ITEM_RESPONSE, payload);
                    } catch (Exception e) {
                        return Message.error(MessageType.CREATE_ITEM_RESPONSE, e.getMessage());
                    }
                }
                return Message.error(MessageType.CREATE_ITEM_RESPONSE, "Lỗi quyền hạn.");
            }

            case CREATE_AUCTION: {
                CreateAuctionDto dto = request.getPayload(CreateAuctionDto.class);
                if (this.currentUser instanceof Seller seller) {
                    try {
                        Item targetItem = seller.getItems().stream()
                                .filter(i -> i.getId().equals(dto.itemId))
                                .findFirst().orElse(null);
                        if (targetItem == null) return Message.error(MessageType.CREATE_AUCTION_RESPONSE, "Lỗi dữ liệu.");
                        Auction auction = seller.createAuction(targetItem, java.time.LocalDateTime.now(),
                                java.time.LocalDateTime.now().plusMinutes(dto.durationMinutes));
                        AuctionManager.getInstance().registerAuction(auction);
                        AuctionDAO.insertAuction(auction);
                        if (dto.startNow) {
                            AuctionManager.getInstance().startAuction(auction);
                            AuctionDAO.updateAuctionStatus(auction.getId(), "RUNNING");
                        }
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
                                String txId = UUID.randomUUID().toString();
                                BidTransactionDAO.insertBidTransaction(txId, a.getId(), bidder.getId(), dto.amount);
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

            case START_AUCTION: {
                String id = request.getPayload(String.class);
                try {
                    AuctionManager.getInstance().getAllAuctions().stream()
                            .filter(a -> a.getId().equals(id))
                            .forEach(a -> {
                                a.startAuction();
                                AuctionDAO.updateAuctionStatus(a.getId(), "RUNNING");
                            });
                    return Message.success(MessageType.START_AUCTION_RESPONSE, "Đã bắt đầu");
                } catch (Exception e) {
                    return Message.error(MessageType.START_AUCTION_RESPONSE, e.getMessage());
                }
            }

            case END_AUCTION: {
                String id = request.getPayload(String.class);
                try {
                    AuctionManager.getInstance().getAllAuctions().stream()
                            .filter(a -> a.getId().equals(id))
                            .forEach(a -> {
                                a.endAuction();
                                AuctionDAO.updateAuctionStatus(a.getId(), a.getStatus().name());
                            });
                    return Message.success(MessageType.END_AUCTION, "Đã kết thúc");
                } catch (Exception e) {
                    return Message.error(MessageType.END_AUCTION, e.getMessage());
                }
            }

            case MARK_PAID: {
                String id = request.getPayload(String.class);
                try {
                    for (Auction a : AuctionManager.getInstance().getAllAuctions()) {
                        if (a.getId().equals(id)) {
                            if (a.getCurrentLeader() != null && a.getCurrentLeader().isBanned()) {
                                return Message.error(MessageType.MARK_PAID_RESPONSE, "Người chiến thắng đã bị khóa tài khoản, không thể thanh toán.");
                            }
                            a.markAsPaid();
                            AuctionDAO.updateAuctionStatus(a.getId(), "PAID");
                            if (a.getCurrentLeader() != null) UserDAO.updateUserBalance(a.getCurrentLeader());
                            return Message.success(MessageType.MARK_PAID_RESPONSE, "Thành công");
                        }
                    }
                } catch (Exception e) {
                    return Message.error(MessageType.MARK_PAID_RESPONSE, e.getMessage());
                }
                return Message.error(MessageType.MARK_PAID_RESPONSE, "Lỗi.");
            }

            case DEPOSIT: {
                DepositPayload dto = request.getPayload(DepositPayload.class);
                if (this.currentUser instanceof Bidder bidder) {
                    if (bidder.isBanned()) {
                        return Message.error(MessageType.DEPOSIT_RESPONSE, "Tài khoản của bạn đã bị khóa, không thể nạp tiền.");
                    }

                    if (bidder.isDepositLocked()) {
                        long secs = bidder.getDepositLockRemainingSeconds();
                        return Message.error(MessageType.DEPOSIT_RESPONSE, "LOCK_TIMER:" + secs);
                    }

                    if (!bidder.getPassword().equals(dto.password)) {
                        bidder.recordFailedDeposit();
                        if (bidder.isDepositLocked()) {
                            long secs = bidder.getDepositLockRemainingSeconds();
                            return Message.error(MessageType.DEPOSIT_RESPONSE, "LOCK_TIMER:" + secs);
                        } else {
                            return Message.error(MessageType.DEPOSIT_RESPONSE, "Sai mật khẩu! Bạn còn " + (3 - bidder.getFailedDepositAttempts()) + " lần thử.");
                        }
                    }

                    bidder.resetFailedDeposit();

                    double DAILY_LIMIT = 50000000.0;
                    double todayTotal = UserDAO.getTodayDepositTotal(bidder.getId());

                    if (todayTotal + dto.amount > DAILY_LIMIT) {
                        return Message.error(MessageType.DEPOSIT_RESPONSE,
                                "Vượt quá giới hạn nạp tiền! Bạn chỉ có thể nạp thêm tối đa " + String.format("%,.0f", (DAILY_LIMIT - todayTotal)) + " ₫ hôm nay.");
                    }

                    bidder.deposit(dto.amount);
                    UserDAO.updateUserBalance(bidder);
                    UserDAO.insertDepositHistory(bidder.getId(), dto.amount);

                    UserDto respDto = new UserDto();
                    respDto.username = bidder.getUsername();
                    respDto.role = "BIDDER";
                    respDto.balance = bidder.getBalance();
                    return Message.success(MessageType.DEPOSIT_RESPONSE, respDto);
                }
                return Message.error(MessageType.DEPOSIT_RESPONSE, "Lỗi quyền hạn.");
            }

            case SETUP_AUTOBID: {
                AutoBidPayload dto = request.getPayload(AutoBidPayload.class);
                if (this.currentUser instanceof Bidder bidder) {
                    try {
                        for (Auction a : AuctionManager.getInstance().getAllAuctions()) {
                            if (a.getId().equals(dto.auctionId)) {
                                bidder.setupAutoBid(a, dto.maxBid, dto.increment);
                                AutoBidConfigDAO.insertAutoBidConfig(UUID.randomUUID().toString(), a.getId(), bidder.getId(), dto.maxBid, dto.increment);
                                return Message.success(MessageType.SETUP_AUTOBID_RESPONSE, "Thành công");
                            }
                        }
                    } catch (Exception e) {
                        return Message.error(MessageType.SETUP_AUTOBID_RESPONSE, e.getMessage());
                    }
                }
                return Message.error(MessageType.SETUP_AUTOBID_RESPONSE, "Lỗi quyền hạn.");
            }

            case UPDATE_ITEM: {
                UpdateItemPayload dto = request.getPayload(UpdateItemPayload.class);
                if (this.currentUser instanceof Seller seller) {
                    try {
                        for (Item item : seller.getItems()) {
                            if (item.getId().equals(dto.itemId)) {
                                item.setName(dto.name);
                                item.setDescription(dto.description);
                                item.setStartingPrice(dto.startingPrice);
                                ItemDAO.updateItem(item);
                                return Message.success(MessageType.UPDATE_ITEM_RESPONSE, "Cập nhật thành công");
                            }
                        }
                    } catch (Exception e) {
                        return Message.error(MessageType.UPDATE_ITEM_RESPONSE, e.getMessage());
                    }
                }
                return Message.error(MessageType.UPDATE_ITEM_RESPONSE, "Lỗi quyền hạn.");
            }

            case ADMIN_CANCEL_AUCTION: {
                AdminCancelPayload dto = request.getPayload(AdminCancelPayload.class);
                if (this.currentUser instanceof Admin) {
                    try {
                        for (Auction a : AuctionManager.getInstance().getAllAuctions()) {
                            if (a.getId().equals(dto.auctionId)) {
                                a.cancelAuction();
                                AuctionDAO.updateAuctionStatus(a.getId(), "CANCELED");
                                return Message.success(MessageType.CANCEL_AUCTION, "Đã hủy bởi Admin: " + dto.reason);
                            }
                        }
                    } catch (Exception e) {
                        return Message.error(MessageType.CANCEL_AUCTION, e.getMessage());
                    }
                }
                return Message.error(MessageType.CANCEL_AUCTION, "Lỗi quyền hạn.");
            }

            case CANCEL_AUCTION: {
                String id = request.getPayload(String.class);
                try {
                    for (Auction a : AuctionManager.getInstance().getAllAuctions()) {
                        if (a.getId().equals(id)) {
                            a.cancelAuction();
                            AuctionDAO.updateAuctionStatus(a.getId(), "CANCELED");
                            return Message.success(MessageType.CANCEL_AUCTION, "Đã hủy phiên đấu giá thành công");
                        }
                    }
                } catch (Exception e) {
                    return Message.error(MessageType.CANCEL_AUCTION, e.getMessage());
                }
                return Message.error(MessageType.CANCEL_AUCTION, "Không tìm thấy phiên đấu giá này.");
            }

            default: return null;
        }
    }

    public void sendMessage(String json) { if (out != null) out.println(json); }

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
        dto.finishedTime = (a.getFinishedTime() != null) ? a.getFinishedTime().format(DTF) : null;
        dto.sellerUsername = a.getSeller().getUsername();
        dto.currentLeader = (a.getCurrentLeader() != null) ? a.getCurrentLeader().getUsername() : null;
        dto.bidCount = a.getBidHistory().size();
        dto.imagesBase64 = a.getItem().getImagesBase64();
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
    private static class PromotePayload { String username; String role; }
    private static class AutoBidPayload { String auctionId; double maxBid; double increment; }
    private static class UpdateItemPayload { String itemId; String name; String description; double startingPrice; }
    private static class AdminCancelPayload { String auctionId; String reason; }
    private static class DepositPayload { double amount; String password; }
}