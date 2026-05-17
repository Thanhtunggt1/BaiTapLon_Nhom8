package com.auction.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class AuctionServer {

    public static final int PORT = 9999;
    private static final List<ClientHandler> connectedClients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        DatabaseConnection.getConnection();
        DataLoader.loadAll();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[Server] Đang lắng nghe tại port " + PORT + " ...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket);
                connectedClients.add(handler);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.err.println("[Server] Lỗi: " + e.getMessage());
        }
    }

    public static void broadcast(String json) {
        for (ClientHandler client : connectedClients) {
            client.sendMessage(json);
        }
    }

    public static void removeClient(ClientHandler handler) {
        connectedClients.remove(handler);
    }
}