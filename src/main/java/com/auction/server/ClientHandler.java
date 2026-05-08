package com.auction.server;

import com.auction.gui.UserStore;
import com.auction.manager.AuctionManager;
import com.auction.model.entity.*;
import com.auction.model.enums.ItemType;
import com.auction.network.Message;
import com.auction.network.MessageType;
import com.auction.network.dto.*;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
                    sendMessage(Message.error(MessageType.ERROR, e.getMessage()).toJson());
                }
            }
        } catch (IOException e) {
            System.out.println("[Server] Client disconnected: " + socket.getInetAddress());
        } finally {
            AuctionServer.removeClient(this);
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    public synchronized void sendMessage(String json) {
        if (out != null) out.println(json);
    }

    private Message handleRequest(Message req) {
        return switch (req.getType()) {
            case LOGIN             -> handleLogin(req);
            case REGISTER          -> handleRegister(req);
            case GET_AUCTIONS      -> handleGetAuctions();
            case PLACE_BID         -> handlePlaceBid(req);
            case CREATE_ITEM       -> handleCreateItem(req);
            case UPDATE_ITEM       -> handleUpdateItem(req); // --- GẮN API ---
            case CREATE_AUCTION    -> handleCreateAuction(req);
            case END_AUCTION       -> handleEndAuction(req);
            case CANCEL_AUCTION    -> handleCancelAuction(req);
            case ADMIN_CANCEL_AUCTION -> handleAdminCancelAuction(req);
            case MARK_PAID         -> handleMarkPaid(req);
            case DEPOSIT           -> handleDeposit(req);
            case SETUP_AUTOBID     -> handleSetupAutoBid(req);
            case START_AUCTION     -> handleStartAuction(req);
            default -> Message.error(MessageType.ERROR, "Không hỗ trợ: " + req.getType());
        };
    }

    private Message handleLogin(Message req) {
        var payload = req.getPayload(LoginPayload.class);
        if (payload == null) return Message.error(MessageType.LOGIN_RESPONSE, "Dữ liệu đăng nhập không hợp lệ.");
        User user = UserStore.findByUsername(payload.username);
        if (user == null || !user.login(payload.username, payload.password)) {
            return Message.error(MessageType.LOGIN_RESPONSE, "Sai tên đăng nhập hoặc mật khẩu.");
        }
        currentUser = user;
        UserDto dto = new UserDto();
        dto.username = user.getUsername();
        dto.role = user instanceof Admin ? "ADMIN" : user instanceof Seller ? "SELLER" : "BIDDER";
        if (user instanceof Bidder b) dto.balance = b.getBalance();
        return Message.success(MessageType.LOGIN_RESPONSE, dto);
    }

    private Message handleRegister(Message req) {
        var payload = req.getPayload(RegisterPayload.class);
        if (payload == null) return Message.error(MessageType.REGISTER_RESPONSE, "Dữ liệu đăng ký không hợp lệ.");
        if (UserStore.usernameExists(payload.username)) {
            return Message.error(MessageType.REGISTER_RESPONSE, "Tên đăng nhập đã tồn tại.");
        }
        User newUser = "SELLER".equals(payload.role) ? new Seller(payload.username, payload.password, payload.email) : new Bidder(payload.username, payload.password, payload.email, 0);
        UserStore.addUser(newUser);
        return Message.success(MessageType.REGISTER_RESPONSE, "Đăng ký thành công!");
    }

    private Message handleGetAuctions() {
        AuctionManager.getInstance().checkAndCloseExpiredAuctions();
        List<AuctionDto> dtos = AuctionManager.getInstance().getAllAuctions().stream().map(this::toDto).collect(Collectors.toList());
        return Message.success(MessageType.AUCTIONS_RESPONSE, dtos);
    }

    private Message handlePlaceBid(Message req) {
        if (!(currentUser instanceof Bidder bidder)) return Message.error(MessageType.BID_RESPONSE, "Chỉ Bidder mới được đặt giá.");
        BidDto dto = req.getPayload(BidDto.class);
        if (dto == null || dto.auctionId == null) return Message.error(MessageType.BID_RESPONSE, "Dữ liệu đặt giá không hợp lệ.");
        Auction auction = AuctionManager.getInstance().findById(dto.auctionId);
        if (auction == null) return Message.error(MessageType.BID_RESPONSE, "Không tìm thấy phiên.");
        try {
            if (bidder.placeBid(auction, dto.amount)) {
                AuctionServer.broadcast(Message.success(MessageType.BID_UPDATE, toDto(auction)));
                return Message.success(MessageType.BID_RESPONSE, toDto(auction));
            }
            return Message.error(MessageType.BID_RESPONSE, "Đặt giá thất bại.");
        } catch (Exception e) { return Message.error(MessageType.BID_RESPONSE, e.getMessage()); }
    }

    private Message handleSetupAutoBid(Message req) {
        if (!(currentUser instanceof Bidder bidder)) {
            return Message.error(MessageType.SETUP_AUTOBID_RESPONSE, "Chỉ Bidder mới được cài đặt Auto-Bid.");
        }
        var payload = req.getPayload(AutoBidPayload.class);
        if (payload == null || payload.auctionId == null) return Message.error(MessageType.SETUP_AUTOBID_RESPONSE, "Dữ liệu không hợp lệ.");
        Auction auction = AuctionManager.getInstance().findById(payload.auctionId);
        if (auction == null) {
            return Message.error(MessageType.SETUP_AUTOBID_RESPONSE, "Không tìm thấy phiên đấu giá.");
        }

        try {
            bidder.setupAutoBid(auction, payload.maxBid, payload.increment);
            return Message.success(MessageType.SETUP_AUTOBID_RESPONSE, "Cài đặt Auto-Bid thành công!");
        } catch (Exception e) {
            return Message.error(MessageType.SETUP_AUTOBID_RESPONSE, e.getMessage());
        }
    }

    private Message handleCreateItem(Message req) {
        if (!(currentUser instanceof Seller seller)) return Message.error(MessageType.CREATE_ITEM_RESPONSE, "Chỉ Seller mới được tạo sản phẩm.");
        CreateItemDto dto = req.getPayload(CreateItemDto.class);
        if (dto == null) return Message.error(MessageType.CREATE_ITEM_RESPONSE, "Dữ liệu sản phẩm không hợp lệ.");
        try {
            Item item = seller.createItem(dto.name, dto.description, dto.startingPrice, ItemType.valueOf(dto.itemType), dto.params);
            ItemDto result = new ItemDto(); result.id = item.getId(); result.name = item.getName(); result.description = item.getDescription(); result.startingPrice = item.getStartingPrice(); result.itemType = item.getClass().getSimpleName();
            return Message.success(MessageType.CREATE_ITEM_RESPONSE, result);
        } catch (Exception e) { return Message.error(MessageType.CREATE_ITEM_RESPONSE, e.getMessage()); }
    }

    // --- MỚI THÊM: Logic sửa sản phẩm trên Server ---
    private Message handleUpdateItem(Message req) {
        if (!(currentUser instanceof Seller seller)) return Message.error(MessageType.ERROR, "Không có quyền.");
        var payload = req.getPayload(UpdateItemPayload.class);
        if (payload == null) return Message.error(MessageType.ERROR, "Dữ liệu cập nhật không hợp lệ.");

        Item item = seller.getItems().stream().filter(i -> i.getId().equals(payload.itemId)).findFirst().orElse(null);
        if (item == null) return Message.error(MessageType.ERROR, "Không tìm thấy sản phẩm trên Server.");

        try {
            seller.updateItem(item, payload.name, payload.description, payload.startingPrice);

            // Gửi tín hiệu làm mới cho tất cả các phiên đấu giá đang chứa sản phẩm này
            AuctionManager.getInstance().getAllAuctions().forEach(a -> {
                if(a.getItem().getId().equals(item.getId())) {
                    AuctionServer.broadcast(Message.success(MessageType.BID_UPDATE, toDto(a)));
                }
            });
            return Message.success(MessageType.UPDATE_ITEM_RESPONSE, "Cập nhật thành công!");
        } catch (Exception e) {
            return Message.error(MessageType.ERROR, e.getMessage());
        }
    }

    private Message handleCreateAuction(Message req) {
        if (!(currentUser instanceof Seller seller)) return Message.error(MessageType.CREATE_AUCTION_RESPONSE, "Chỉ Seller mới được tạo phiên.");
        CreateAuctionDto dto = req.getPayload(CreateAuctionDto.class);
        if (dto == null) return Message.error(MessageType.CREATE_AUCTION_RESPONSE, "Dữ liệu phiên đấu giá không hợp lệ.");
        try {
            Item item = seller.getItems().stream().filter(i -> i.getId().equals(dto.itemId)).findFirst().orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm."));

            LocalDateTime start = dto.startNow ? LocalDateTime.now() : LocalDateTime.now().plusYears(10);
            LocalDateTime end = start.plusMinutes(dto.durationMinutes);

            Auction auction = seller.createAuction(item, start, end);
            AuctionManager.getInstance().registerAuction(auction);

            if (dto.startNow) auction.startAuction();

            return Message.success(MessageType.CREATE_AUCTION_RESPONSE, toDto(auction));
        } catch (Exception e) { return Message.error(MessageType.CREATE_AUCTION_RESPONSE, e.getMessage()); }
    }

    private Message handleStartAuction(Message req) {
        if (!(currentUser instanceof Seller)) return Message.error(MessageType.ERROR, "Không có quyền.");
        String auctionId = req.getPayload(String.class);
        Auction auction = AuctionManager.getInstance().findById(auctionId);
        if (auction == null) return Message.error(MessageType.ERROR, "Không tìm thấy phiên.");

        try {
            long minutes = java.time.temporal.ChronoUnit.MINUTES.between(auction.getStartTime(), auction.getEndTime());

            try {
                java.lang.reflect.Field sf = Auction.class.getDeclaredField("startTime");
                sf.setAccessible(true);
                sf.set(auction, LocalDateTime.now());

                java.lang.reflect.Field ef = Auction.class.getDeclaredField("endTime");
                ef.setAccessible(true);
                ef.set(auction, LocalDateTime.now().plusMinutes(minutes));
            } catch (Exception reflectionEx) {
                // Cảnh báo nếu không thể ghi đè thời gian qua reflection (do cơ chế bảo mật của JDK 17)
                System.err.println("Warning: Could not update timing via reflection: " + reflectionEx.getMessage());
            }

            AuctionManager.getInstance().startAuction(auction);
            AuctionServer.broadcast(Message.success(MessageType.BID_UPDATE, toDto(auction)));
            return Message.success(MessageType.START_AUCTION_RESPONSE, toDto(auction));
        } catch (Exception e) {
            return Message.error(MessageType.ERROR, e.getMessage());
        }
    }

    private Message handleEndAuction(Message req) {
        if (!(currentUser instanceof Seller seller)) return Message.error(MessageType.ERROR, "Không có quyền.");
        String auctionId = req.getPayload(String.class); Auction auction = AuctionManager.getInstance().findById(auctionId);
        if (auction == null) return Message.error(MessageType.ERROR, "Không tìm thấy phiên.");
        try { seller.endAuctionEarly(auction); AuctionServer.broadcast(Message.success(MessageType.BID_UPDATE, toDto(auction))); return Message.success(MessageType.END_AUCTION, toDto(auction)); } catch (Exception e) { return Message.error(MessageType.ERROR, e.getMessage()); }
    }

    private Message handleCancelAuction(Message req) {
        if (!(currentUser instanceof Seller seller)) return Message.error(MessageType.ERROR, "Không có quyền.");
        String auctionId = req.getPayload(String.class); Auction auction = AuctionManager.getInstance().findById(auctionId);
        if (auction == null) return Message.error(MessageType.ERROR, "Không tìm thấy phiên.");
        try { seller.cancelAuction(auction, "Seller hủy phiên."); AuctionServer.broadcast(Message.success(MessageType.BID_UPDATE, toDto(auction))); return Message.success(MessageType.CANCEL_AUCTION, toDto(auction)); } catch (Exception e) { return Message.error(MessageType.ERROR, e.getMessage()); }
    }

    private Message handleAdminCancelAuction(Message req) {
        if (!(currentUser instanceof Admin admin)) return Message.error(MessageType.ERROR, "Không có quyền.");
        var payload = req.getPayload(AdminCancelPayload.class);
        if (payload == null || payload.auctionId == null) return Message.error(MessageType.ERROR, "Dữ liệu không hợp lệ.");
        Auction auction = AuctionManager.getInstance().findById(payload.auctionId);
        if (auction == null) return Message.error(MessageType.ERROR, "Không tìm thấy phiên.");
        try { admin.resolveDispute(auction, payload.reason); AuctionServer.broadcast(Message.success(MessageType.BID_UPDATE, toDto(auction))); return Message.success(MessageType.ADMIN_CANCEL_AUCTION, toDto(auction)); } catch (Exception e) { return Message.error(MessageType.ERROR, e.getMessage()); }
    }

    private Message handleMarkPaid(Message req) {
        String auctionId = req.getPayload(String.class); Auction auction = AuctionManager.getInstance().findById(auctionId);
        if (auction == null) return Message.error(MessageType.MARK_PAID_RESPONSE, "Không tìm thấy phiên.");
        try { auction.markAsPaid(); AuctionServer.broadcast(Message.success(MessageType.BID_UPDATE, toDto(auction))); return Message.success(MessageType.MARK_PAID_RESPONSE, toDto(auction)); } catch (Exception e) { return Message.error(MessageType.MARK_PAID_RESPONSE, e.getMessage()); }
    }

    private Message handleDeposit(Message req) {
        if (!(currentUser instanceof Bidder bidder)) return Message.error(MessageType.DEPOSIT_RESPONSE, "Chỉ Bidder mới được nạp tiền.");
        double amount = req.getPayload(Double.class);
        if (amount <= 0) return Message.error(MessageType.DEPOSIT_RESPONSE, "Số tiền không hợp lệ.");
        try { bidder.deposit(amount); UserDto dto = new UserDto(); dto.username = bidder.getUsername(); dto.role = "BIDDER"; dto.balance = bidder.getBalance(); return Message.success(MessageType.DEPOSIT_RESPONSE, dto); } catch (Exception e) { return Message.error(MessageType.DEPOSIT_RESPONSE, e.getMessage()); }
    }

    private AuctionDto toDto(Auction a) {
        AuctionDto dto = new AuctionDto();
        dto.id             = a.getId();
        dto.itemName       = a.getItem().getName();
        dto.itemType       = a.getItem().getClass().getSimpleName();
        dto.description    = a.getItem().getDescription();
        dto.startingPrice  = a.getItem().getStartingPrice();
        dto.currentPrice   = a.getCurrentHighestPrice();
        dto.currentLeader  = a.getCurrentLeader() != null ? a.getCurrentLeader().getUsername() : null;
        dto.sellerUsername = a.getSeller().getUsername();
        dto.status         = a.getStatus().toString();
        dto.startTime      = a.getStartTime().format(DTF);
        dto.endTime        = a.getEndTime().format(DTF);

        List<BidTransaction> bids = a.getBidHistory();
        dto.bidCount = (bids != null) ? bids.size() : 0;

        if (bids != null) {
            dto.history = a.getBidHistory().stream().map(b -> {
                AuctionDto.BidEntryDto entry = new AuctionDto.BidEntryDto();
                entry.bidderName = b.getBidder() != null ? b.getBidder().getUsername() : "N/A";
                entry.amount = b.getAmount();
                entry.time = b.getTimestamp() != null ? b.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy")) : "";
                return entry;
            }).collect(Collectors.toList());
        }

        return dto;
    }

    private static class LoginPayload { String username, password; }
    private static class RegisterPayload { String username, password, email, role; }
    private static class AdminCancelPayload { String auctionId, reason; }
    private static class ItemDto { String id, name, description, itemType; double startingPrice; }
    private static class AutoBidPayload { String auctionId; double maxBid, increment; }
    private static class UpdateItemPayload { String itemId, name, description; double startingPrice; }
}