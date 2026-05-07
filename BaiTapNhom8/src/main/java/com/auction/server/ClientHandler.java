package com.auction.server;

import com.auction.gui.UserStore;
import com.auction.manager.AuctionManager;
import com.auction.model.entity.*;
import com.auction.model.enums.AuctionStatus;
import com.auction.model.enums.ItemType;
import com.auction.network.Message;
import com.auction.network.MessageType;
import com.auction.network.dto.*;
import com.google.gson.Gson;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mỗi client kết nối được xử lý bởi một ClientHandler riêng (1 thread).
 * Luồng hoạt động: đọc JSON từ socket → xử lý nghiệp vụ → gửi JSON phản hồi
 */
public class ClientHandler implements Runnable {

    private static final DateTimeFormatter DTF =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private User currentUser;   // User đang đăng nhập trên kết nối này

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
            System.out.println("[Handler] Client ngắt kết nối.");
        } finally {
            AuctionServer.removeClient(this);
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    /** Gửi 1 dòng JSON xuống client */
    public synchronized void sendMessage(String json) {
        if (out != null) out.println(json);
    }

    // ── Request dispatcher ────────────────────────────────────────────────────

    private Message handleRequest(Message req) {
        return switch (req.getType()) {
            case LOGIN             -> handleLogin(req);
            case REGISTER          -> handleRegister(req);
            case GET_AUCTIONS      -> handleGetAuctions();
            case PLACE_BID         -> handlePlaceBid(req);
            case CREATE_ITEM       -> handleCreateItem(req);
            case CREATE_AUCTION    -> handleCreateAuction(req);
            case END_AUCTION       -> handleEndAuction(req);
            case CANCEL_AUCTION    -> handleCancelAuction(req);
            case ADMIN_CANCEL_AUCTION -> handleAdminCancelAuction(req);
            case MARK_PAID         -> handleMarkPaid(req);
            case DEPOSIT           -> handleDeposit(req);
            default -> Message.error(MessageType.ERROR, "Không hỗ trợ: " + req.getType());
        };
    }

    // ── Auth handlers ─────────────────────────────────────────────────────────

    private Message handleLogin(Message req) {
        // Lấy payload
        var payload = req.getPayload(LoginPayload.class);
        User user = UserStore.findByUsername(payload.username);

        if (user == null || !user.login(payload.username, payload.password)) {
            return Message.error(MessageType.LOGIN_RESPONSE,
                    "Sai tên đăng nhập hoặc mật khẩu.");
        }

        currentUser = user;

        UserDto dto = new UserDto();
        dto.username = user.getUsername();
        dto.role = user instanceof Admin ? "ADMIN"
                : user instanceof Seller ? "SELLER" : "BIDDER";
        if (user instanceof Bidder b) dto.balance = b.getBalance();

        return Message.success(MessageType.LOGIN_RESPONSE, dto);
    }

    private Message handleRegister(Message req) {
        var payload = req.getPayload(RegisterPayload.class);

        if (UserStore.usernameExists(payload.username)) {
            return Message.error(MessageType.REGISTER_RESPONSE,
                    "Tên đăng nhập đã tồn tại.");
        }

        User newUser = "SELLER".equals(payload.role)
                ? new Seller(payload.username, payload.password, payload.email)
                : new Bidder(payload.username, payload.password, payload.email, 0);
        UserStore.addUser(newUser);

        return Message.success(MessageType.REGISTER_RESPONSE, "Đăng ký thành công!");
    }

    // ── Auction handlers ──────────────────────────────────────────────────────

    private Message handleGetAuctions() {
        AuctionManager.getInstance().checkAndCloseExpiredAuctions();
        List<AuctionDto> dtos = AuctionManager.getInstance().getAllAuctions()
                .stream().map(this::toDto).collect(Collectors.toList());
        return Message.success(MessageType.AUCTIONS_RESPONSE, dtos);
    }

    private Message handlePlaceBid(Message req) {
        if (!(currentUser instanceof Bidder bidder)) {
            return Message.error(MessageType.BID_RESPONSE, "Chỉ Bidder mới được đặt giá.");
        }

        BidDto dto = req.getPayload(BidDto.class);
        Auction auction = AuctionManager.getInstance().findById(dto.auctionId);
        if (auction == null) {
            return Message.error(MessageType.BID_RESPONSE, "Không tìm thấy phiên đấu giá.");
        }

        try {
            boolean ok = bidder.placeBid(auction, dto.amount);
            if (ok) {
                // BROADCAST cho tất cả client: có bid mới!
                AuctionServer.broadcast(
                        Message.success(MessageType.BID_UPDATE, toDto(auction))
                );
                return Message.success(MessageType.BID_RESPONSE, toDto(auction));
            }
            return Message.error(MessageType.BID_RESPONSE, "Đặt giá thất bại.");
        } catch (Exception e) {
            return Message.error(MessageType.BID_RESPONSE, e.getMessage());
        }
    }

    private Message handleCreateItem(Message req) {
        if (!(currentUser instanceof Seller seller)) {
            return Message.error(MessageType.CREATE_ITEM_RESPONSE,
                    "Chỉ Seller mới được tạo sản phẩm.");
        }
        CreateItemDto dto = req.getPayload(CreateItemDto.class);
        try {
            Item item = seller.createItem(dto.name, dto.description,
                    dto.startingPrice, ItemType.valueOf(dto.itemType), dto.params);

            ItemDto result = new ItemDto();
            result.id          = item.getId();
            result.name        = item.getName();
            result.description = item.getDescription();
            result.startingPrice = item.getStartingPrice();
            result.itemType    = item.getClass().getSimpleName();

            return Message.success(MessageType.CREATE_ITEM_RESPONSE, result);
        } catch (Exception e) {
            return Message.error(MessageType.CREATE_ITEM_RESPONSE, e.getMessage());
        }
    }

    private Message handleCreateAuction(Message req) {
        if (!(currentUser instanceof Seller seller)) {
            return Message.error(MessageType.CREATE_AUCTION_RESPONSE, "Chỉ Seller mới được tạo phiên.");
        }
        CreateAuctionDto dto = req.getPayload(CreateAuctionDto.class);
        try {
            // Tìm item theo id
            Item item = seller.getItems().stream()
                    .filter(i -> i.getId().equals(dto.itemId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm."));

            LocalDateTime now = LocalDateTime.now();
            Auction auction = seller.createAuction(item, now,
                    now.plusMinutes(dto.durationMinutes));
            AuctionManager.getInstance().registerAuction(auction);
            if (dto.startNow) auction.startAuction();

            return Message.success(MessageType.CREATE_AUCTION_RESPONSE, toDto(auction));
        } catch (Exception e) {
            return Message.error(MessageType.CREATE_AUCTION_RESPONSE, e.getMessage());
        }
    }

    private Message handleEndAuction(Message req) {
        if (!(currentUser instanceof Seller seller)) {
            return Message.error(MessageType.ERROR, "Không có quyền.");
        }
        String auctionId = req.getPayload(String.class);
        Auction auction = AuctionManager.getInstance().findById(auctionId);
        if (auction == null) return Message.error(MessageType.ERROR, "Không tìm thấy phiên.");
        try {
            seller.endAuctionEarly(auction);
            AuctionServer.broadcast(Message.success(MessageType.BID_UPDATE, toDto(auction)));
            return Message.success(MessageType.END_AUCTION, toDto(auction));
        } catch (Exception e) {
            return Message.error(MessageType.ERROR, e.getMessage());
        }
    }

    private Message handleCancelAuction(Message req) {
        if (!(currentUser instanceof Seller seller)) {
            return Message.error(MessageType.ERROR, "Không có quyền.");
        }
        String auctionId = req.getPayload(String.class);
        Auction auction = AuctionManager.getInstance().findById(auctionId);
        if (auction == null) return Message.error(MessageType.ERROR, "Không tìm thấy phiên.");
        try {
            seller.cancelAuction(auction, "Seller hủy phiên.");
            AuctionServer.broadcast(Message.success(MessageType.BID_UPDATE, toDto(auction)));
            return Message.success(MessageType.CANCEL_AUCTION, toDto(auction));
        } catch (Exception e) {
            return Message.error(MessageType.ERROR, e.getMessage());
        }
    }

    private Message handleAdminCancelAuction(Message req) {
        if (!(currentUser instanceof Admin admin)) {
            return Message.error(MessageType.ERROR, "Không có quyền.");
        }
        var payload = req.getPayload(AdminCancelPayload.class);
        Auction auction = AuctionManager.getInstance().findById(payload.auctionId);
        if (auction == null) return Message.error(MessageType.ERROR, "Không tìm thấy phiên.");
        try {
            admin.resolveDispute(auction, payload.reason);
            AuctionServer.broadcast(Message.success(MessageType.BID_UPDATE, toDto(auction)));
            return Message.success(MessageType.ADMIN_CANCEL_AUCTION, toDto(auction));
        } catch (Exception e) {
            return Message.error(MessageType.ERROR, e.getMessage());
        }
    }

    private Message handleMarkPaid(Message req) {
        String auctionId = req.getPayload(String.class);
        Auction auction = AuctionManager.getInstance().findById(auctionId);
        if (auction == null) return Message.error(MessageType.MARK_PAID_RESPONSE, "Không tìm thấy phiên.");
        try {
            auction.markAsPaid();
            AuctionServer.broadcast(Message.success(MessageType.BID_UPDATE, toDto(auction)));
            return Message.success(MessageType.MARK_PAID_RESPONSE, toDto(auction));
        } catch (Exception e) {
            return Message.error(MessageType.MARK_PAID_RESPONSE, e.getMessage());
        }
    }

    private Message handleDeposit(Message req) {
        if (!(currentUser instanceof Bidder bidder)) {
            return Message.error(MessageType.DEPOSIT_RESPONSE, "Chỉ Bidder mới được nạp tiền.");
        }
        double amount = req.getPayload(Double.class);
        try {
            bidder.deposit(amount);
            UserDto dto = new UserDto();
            dto.username = bidder.getUsername();
            dto.role = "BIDDER";
            dto.balance = bidder.getBalance();
            return Message.success(MessageType.DEPOSIT_RESPONSE, dto);
        } catch (Exception e) {
            return Message.error(MessageType.DEPOSIT_RESPONSE, e.getMessage());
        }
    }

    // ── Helper: convert Auction → AuctionDto ─────────────────────────────────

    private AuctionDto toDto(Auction a) {
        AuctionDto dto = new AuctionDto();
        dto.id             = a.getId();
        dto.itemName       = a.getItem().getName();
        dto.itemType       = a.getItem().getClass().getSimpleName();
        dto.description    = a.getItem().getDescription();
        dto.startingPrice  = a.getItem().getStartingPrice();
        dto.currentPrice   = a.getCurrentHighestPrice();
        dto.currentLeader  = a.getCurrentLeader() != null
                ? a.getCurrentLeader().getUsername() : null;
        dto.sellerUsername = a.getSeller().getUsername();
        dto.status         = a.getStatus().toString();
        dto.startTime      = a.getStartTime().format(DTF);
        dto.endTime        = a.getEndTime().format(DTF);
        dto.bidCount       = a.getBidHistory().size();
        return dto;
    }

    // ── Inner payload classes (dùng để deserialize) ───────────────────────────

    private static class LoginPayload {
        String username, password;
    }

    private static class RegisterPayload {
        String username, password, email, role;
    }

    private static class AdminCancelPayload {
        String auctionId, reason;
    }

    // Thêm ItemDto nội bộ
    private static class ItemDto {
        String id, name, description, itemType;
        double startingPrice;
    }
}