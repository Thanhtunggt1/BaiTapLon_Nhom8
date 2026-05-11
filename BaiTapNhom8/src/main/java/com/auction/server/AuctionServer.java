package com.auction.server;

import com.auction.gui.DataInitializer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class AuctionServer {

    public static final int PORT = 9999;

    private static final List<ClientHandler> connectedClients =
            new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        DataInitializer.init();
        DatabaseConnection.getConnection();
        System.out.println("[Server] Đã khởi tạo dữ liệu mẫu.");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[Server] Đang lắng nghe tại port " + PORT + " ...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[Server] Client mới: " + clientSocket.getInetAddress());

                ClientHandler handler = new ClientHandler(clientSocket);
                connectedClients.add(handler);
                new Thread(handler, "Handler-" + clientSocket.getPort()).start();
            }
        } catch (IOException e) {
            System.err.println("[Server] Lỗi khởi động: " + e.getMessage());
        }
    }

    public static void removeClient(ClientHandler handler) {
        connectedClients.remove(handler);
        System.out.println("[Server] Client đã ngắt kết nối. Còn: "
                + connectedClients.size());
    }
}