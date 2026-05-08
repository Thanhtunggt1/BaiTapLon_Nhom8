package com.auction.network;

import com.auction.network.dto.*;
import com.google.gson.Gson;
import javafx.application.Platform;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

public class NetworkClient {

    private static final String HOST = "localhost";
    private static final int    PORT = 9999;
    private static final Gson   GSON = new Gson();

    private static volatile NetworkClient instance;

    public static NetworkClient getInstance() {
        if (instance == null) {
            synchronized (NetworkClient.class) {
                if (instance == null) instance = new NetworkClient();
            }
        }
        return instance;
    }

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Consumer<AuctionDto> onBidUpdate;
    private UserDto currentUser;

    private final BlockingQueue<Message> responseQueue = new LinkedBlockingQueue<>();

    public void connect() throws IOException {
        socket = new Socket(HOST, PORT);
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        Thread listener = new Thread(this::listenForPush, "Push-Listener");
        listener.setDaemon(true);
        listener.start();
    }

    private void listenForPush() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                Message msg = Message.fromJson(line);
                if (msg.getType() == MessageType.BID_UPDATE && onBidUpdate != null) {
                    AuctionDto dto = msg.getPayload(AuctionDto.class);
                    Platform.runLater(() -> onBidUpdate.accept(dto));
                } else {
                    responseQueue.put(msg);
                }
            }
        } catch (Exception ignored) {}
    }

    public Message login(String username, String password) { return sendAndReceive(new Message(MessageType.LOGIN, new LoginPayload(username, password))); }
    public Message register(String username, String password, String email, String role) { return sendAndReceive(new Message(MessageType.REGISTER, new RegisterPayload(username, password, email, role))); }
    public Message getAuctions() { return sendAndReceive(new Message(MessageType.GET_AUCTIONS)); }
    public Message placeBid(String auctionId, double amount) {
        BidDto dto = new BidDto(); dto.auctionId = auctionId; dto.amount = amount;
        return sendAndReceive(new Message(MessageType.PLACE_BID, dto));
    }
    public Message deposit(double amount) { return sendAndReceive(new Message(MessageType.DEPOSIT, amount)); }
    public Message createItem(CreateItemDto dto) { return sendAndReceive(new Message(MessageType.CREATE_ITEM, dto)); }

    // --- MỚI THÊM: Gửi lệnh cập nhật sản phẩm lên Server ---
    public Message updateItem(String itemId, String name, String description, double startingPrice) {
        return sendAndReceive(new Message(MessageType.UPDATE_ITEM, new UpdateItemPayload(itemId, name, description, startingPrice)));
    }

    public Message createAuction(CreateAuctionDto dto) { return sendAndReceive(new Message(MessageType.CREATE_AUCTION, dto)); }
    public Message endAuction(String auctionId) { return sendAndReceive(new Message(MessageType.END_AUCTION, auctionId)); }
    public Message cancelAuction(String auctionId) { return sendAndReceive(new Message(MessageType.CANCEL_AUCTION, auctionId)); }
    public Message adminCancelAuction(String auctionId, String reason) { return sendAndReceive(new Message(MessageType.ADMIN_CANCEL_AUCTION, new AdminCancelPayload(auctionId, reason))); }
    public Message markPaid(String auctionId) { return sendAndReceive(new Message(MessageType.MARK_PAID, auctionId)); }
    public Message setupAutoBid(String auctionId, double maxBid, double increment) { return sendAndReceive(new Message(MessageType.SETUP_AUTOBID, new AutoBidPayload(auctionId, maxBid, increment))); }
    public Message startAuction(String auctionId) { return sendAndReceive(new Message(MessageType.START_AUCTION, auctionId)); }

    public void setOnBidUpdate(Consumer<AuctionDto> callback) { this.onBidUpdate = callback; }
    public UserDto getCurrentUser()  { return currentUser; }
    public void setCurrentUser(UserDto u) { currentUser = u; }

    private synchronized Message sendAndReceive(Message request) {
        try {
            out.println(request.toJson());
            return responseQueue.take();
        } catch (Exception e) {
            return Message.error(MessageType.ERROR, "Lỗi kết nối: " + e.getMessage());
        }
    }

    private record LoginPayload(String username, String password) {}
    private record RegisterPayload(String username, String password, String email, String role) {}
    private record AdminCancelPayload(String auctionId, String reason) {}
    private record AutoBidPayload(String auctionId, double maxBid, double increment) {}
    private record UpdateItemPayload(String itemId, String name, String description, double startingPrice) {}
}