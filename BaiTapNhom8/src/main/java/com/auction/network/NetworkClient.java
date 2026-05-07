package com.auction.network;

import com.auction.network.dto.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.function.Consumer;

/**
 * Singleton quản lý kết nối Socket từ phía Client (JavaFX).
 *
 * Cách dùng trong controller:
 *   NetworkClient.getInstance().login("bidder1", "bidder123", response -> { ... });
 */
public class NetworkClient {

    private static final String HOST = "localhost";
    private static final int    PORT = 9999;
    private static final Gson   GSON = new Gson();

    // ── Singleton ─────────────────────────────────────────────────────────────

    private static volatile NetworkClient instance;

    public static NetworkClient getInstance() {
        if (instance == null) {
            synchronized (NetworkClient.class) {
                if (instance == null) instance = new NetworkClient();
            }
        }
        return instance;
    }

    // ── Fields ────────────────────────────────────────────────────────────────

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    /** Callback nhận BID_UPDATE push từ server (realtime) */
    private Consumer<AuctionDto> onBidUpdate;

    /** User hiện tại sau khi đăng nhập thành công */
    private UserDto currentUser;

    // ── Connect ───────────────────────────────────────────────────────────────

    /**
     * Kết nối đến server. Gọi trước khi dùng bất kỳ phương thức nào.
     */
    public void connect() throws IOException {
        socket = new Socket(HOST, PORT);
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        System.out.println("[Client] Đã kết nối đến server " + HOST + ":" + PORT);

        // Luồng lắng nghe push từ server (BID_UPDATE, v.v.)
        Thread listener = new Thread(this::listenForPush, "Push-Listener");
        listener.setDaemon(true);
        listener.start();
    }

    /**
     * Luồng chạy nền, liên tục đọc push từ server.
     * Khi nhận BID_UPDATE → gọi callback onBidUpdate trên JavaFX thread.
     */
    private void listenForPush() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                Message msg = Message.fromJson(line);
                if (msg.getType() == MessageType.BID_UPDATE && onBidUpdate != null) {
                    AuctionDto dto = msg.getPayload(AuctionDto.class);
                    // Đảm bảo cập nhật UI trên JavaFX Application Thread
                    Platform.runLater(() -> onBidUpdate.accept(dto));
                }
                // Có thể thêm xử lý các loại push khác ở đây
            }
        } catch (IOException e) {
            System.out.println("[Client] Mất kết nối server.");
        }
    }

    // ── API methods ───────────────────────────────────────────────────────────

    public Message login(String username, String password) {
        return sendAndReceive(new Message(MessageType.LOGIN,
                new LoginPayload(username, password)));
    }

    public Message register(String username, String password,
                            String email, String role) {
        return sendAndReceive(new Message(MessageType.REGISTER,
                new RegisterPayload(username, password, email, role)));
    }

    public Message getAuctions() {
        return sendAndReceive(new Message(MessageType.GET_AUCTIONS));
    }

    public Message placeBid(String auctionId, double amount) {
        BidDto dto = new BidDto();
        dto.auctionId = auctionId;
        dto.amount    = amount;
        return sendAndReceive(new Message(MessageType.PLACE_BID, dto));
    }

    public Message deposit(double amount) {
        return sendAndReceive(new Message(MessageType.DEPOSIT, amount));
    }

    public Message createItem(CreateItemDto dto) {
        return sendAndReceive(new Message(MessageType.CREATE_ITEM, dto));
    }

    public Message createAuction(CreateAuctionDto dto) {
        return sendAndReceive(new Message(MessageType.CREATE_AUCTION, dto));
    }

    public Message endAuction(String auctionId) {
        return sendAndReceive(new Message(MessageType.END_AUCTION, auctionId));
    }

    public Message cancelAuction(String auctionId) {
        return sendAndReceive(new Message(MessageType.CANCEL_AUCTION, auctionId));
    }

    public Message adminCancelAuction(String auctionId, String reason) {
        return sendAndReceive(new Message(MessageType.ADMIN_CANCEL_AUCTION,
                new AdminCancelPayload(auctionId, reason)));
    }

    public Message markPaid(String auctionId) {
        return sendAndReceive(new Message(MessageType.MARK_PAID, auctionId));
    }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setOnBidUpdate(Consumer<AuctionDto> callback) {
        this.onBidUpdate = callback;
    }

    public UserDto getCurrentUser()  { return currentUser; }
    public void setCurrentUser(UserDto u) { currentUser = u; }

    // ── Internal ──────────────────────────────────────────────────────────────

    /**
     * Gửi request và chờ response (synchronous, blocking).
     * NOTE: Vì listenForPush đang đọc trong luồng khác,
     *       cần đảm bảo không có race condition khi đọc response.
     *       Giải pháp đơn giản: dùng synchronized hoặc BlockingQueue.
     *       (Xem phần "Nâng cao" bên dưới.)
     */
    private synchronized Message sendAndReceive(Message request) {
        try {
            out.println(request.toJson());
            // Đọc response (1 dòng)
            String line = in.readLine();
            return Message.fromJson(line);
        } catch (IOException e) {
            return Message.error(MessageType.ERROR, "Lỗi kết nối: " + e.getMessage());
        }
    }

    // ── Inner payload classes ─────────────────────────────────────────────────

    private record LoginPayload(String username, String password) {}
    private record RegisterPayload(String username, String password,
                                   String email, String role) {}
    private record AdminCancelPayload(String auctionId, String reason) {}
}