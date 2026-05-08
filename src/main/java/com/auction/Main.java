package com.auction;

import com.auction.network.NetworkClient;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;

        // --- KẾT NỐI ĐẾN SERVER TRƯỚC KHI HIỆN UI ---
        try {
            // Thử kết nối đến AuctionServer (mặc định localhost:9999)
            NetworkClient.getInstance().connect();
            System.out.println("[Main] Kết nối server thành công.");
        } catch (IOException e) {
            // Nếu Server chưa bật, thông báo lỗi và thoát ứng dụng ngay
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "Không thể kết nối đến máy chủ đấu giá tại localhost:9999.\n" +
                            "Vui lòng đảm bảo AuctionServer đã được khởi động!",
                    ButtonType.OK);
            alert.setTitle("Lỗi Kết Nối");
            alert.setHeaderText("Mất kết nối với Server");
            alert.showAndWait();

            System.err.println("[Main] Lỗi kết nối: " + e.getMessage());
            Platform.exit();
            return;
        }

        // Nếu kết nối thành công, bắt đầu ở màn hình đăng nhập
        showLogin();
    }

    /**
     * Hiển thị màn hình Đăng nhập / Đăng ký
     */
    public static void showLogin() throws Exception {
        FXMLLoader loader = new FXMLLoader(
                Main.class.getResource("/com/auction/gui/login.fxml"));
        if (loader.getLocation() == null) {
            throw new IOException("Không tìm thấy file login.fxml. Vui lòng kiểm tra lại đường dẫn resources.");
        }
        Scene scene = new Scene(loader.load(), 450, 500); // Tăng nhẹ kích thước để tránh vỡ giao diện

        primaryStage.setTitle("Hệ Thống Đấu Giá Trực Tuyến - Đăng nhập");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    /**
     * Hiển thị màn hình chính sau khi đăng nhập thành công
     */
    public static void showMain() throws Exception {
        FXMLLoader loader = new FXMLLoader(
                Main.class.getResource("/com/auction/gui/main.fxml"));
        // Kích thước chuẩn cho màn hình chính (Dashboard)
        Scene scene = new Scene(loader.load(), 1150, 720);

        primaryStage.setTitle("Hệ Thống Đấu Giá Trực Tuyến - Dashboard");
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}