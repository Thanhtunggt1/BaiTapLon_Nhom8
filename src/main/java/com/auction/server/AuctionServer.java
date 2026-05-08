package com.auction.server;

import com.auction.gui.DataInitializer;
import com.auction.manager.AuctionManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Server chính: lắng nghe kết nối TCP và tạo luồng xử lý cho mỗi client.
 */
public class AuctionServer {

    public static final int PORT = 9999;

    // Danh sách tất cả client đang kết nối (dùng CopyOnWriteArrayList vì thread-safe)
    private static final List<ClientHandler> connectedClients =
            new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        // Khởi tạo dữ liệu mẫu (giống DataInitializer trong GUI cũ)
        DataInitializer.init();
        System.out.println("[Server] Đã khởi tạo dữ liệu mẫu.");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[Server] Đang lắng nghe tại port " + PORT + " ...");

            while (true) {
                // Chờ client kết nối
                Socket clientSocket = serverSocket.accept();
                System.out.println("[Server] Client mới: " + clientSocket.getInetAddress());

                // Tạo handler riêng cho từng client, chạy trên thread riêng
                ClientHandler handler = new ClientHandler(clientSocket);
                connectedClients.add(handler);
                new Thread(handler, "Handler-" + clientSocket.getPort()).start();
            }
        } catch (IOException e) {
            System.err.println("[Server] Lỗi khởi động: " + e.getMessage());
        }
    }

    /**
     * Gửi tin nhắn broadcast đến TẤT CẢ client đang kết nối.
     * Dùng khi có bid mới → push cập nhật realtime cho mọi người đang xem.
     */
    public static void broadcast(com.auction.network.Message message) {
        String json = message.toJson();
        for (ClientHandler client : connectedClients) {
            client.sendMessage(json);
        }
    }

    /** Xóa client khỏi danh sách khi ngắt kết nối */
    public static void removeClient(ClientHandler handler) {
        connectedClients.remove(handler);
        System.out.println("[Server] Client đã ngắt kết nối. Còn: "
                + connectedClients.size());
    }
}