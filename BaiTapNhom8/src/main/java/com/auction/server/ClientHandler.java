package com.auction.server;

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
import java.util.Map;
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
                // Gọi tới UserDAO để check Database
                User user = UserDAO.findByUsername(loginDto.username);

                if (user != null && user.getPassword().equals(loginDto.password)) {
                    this.currentUser = user;
                    UserDto respDto = new UserDto();
                    respDto.username = user.getUsername();

                    if (user instanceof Admin) respDto.role = "ADMIN";
                    else if (user instanceof Seller) respDto.role = "SELLER";
                    else respDto.role = "BIDDER";

                    if (user instanceof Bidder) {
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
        dto.currentPrice = a.getCurrentHighestPrice();
        dto.status = a.getStatus().toString();
        dto.startTime = a.getStartTime().format(DTF);
        dto.endTime = a.getEndTime().format(DTF);
        dto.sellerUsername = a.getSeller().getUsername();
        return dto;
    }

    // --- CÁC LỚP ĐỆM ĐỂ NHẬN DỮ LIỆU ---
    private static class LoginPayload { String username; String password; }
    private static class RegisterPayload { String username; String password; String email; String role; }
}