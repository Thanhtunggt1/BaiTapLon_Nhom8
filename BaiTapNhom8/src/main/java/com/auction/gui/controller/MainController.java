package com.auction.gui.controller;

import com.auction.Main;
import com.auction.gui.SessionManager;
import com.auction.model.entity.Admin;
import com.auction.model.entity.Bidder;
import com.auction.model.entity.Seller;
import com.auction.model.entity.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import java.text.NumberFormat;
import java.util.Locale;

public class MainController {

    // Tạo một instance static để các Controller khác có thể truy cập và ra lệnh cập nhật UI
    private static MainController instance;

    public static MainController getInstance() {
        return instance;
    }

    @FXML private Label userInfoLabel;
    @FXML private Label userBalanceLabel;
    @FXML private TabPane mainTabPane;

    @FXML
    public void initialize() {
        instance = this; // Gán instance hiện tại

        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        // Xác định vai trò để hiển thị trên Header
        String role = (user instanceof Admin) ? "Admin" : (user instanceof Seller ? "Seller" : "Bidder");
        userInfoLabel.setText(user.getUsername() + "  |  " + role);

        // Hiển thị số dư ban đầu
        refreshBalanceView();

        try {
            // Khởi tạo các Tab dựa trên quyền hạn của User
            addTab("Danh Sách Đấu Giá", "/com/auction/gui/auction_list.fxml");

            if (user instanceof Seller) {
                addTab("Quản Lý Bán Hàng", "/com/auction/gui/seller.fxml");
            }
            if (user instanceof Admin) {
                addTab("Quản Trị Hệ Thống", "/com/auction/gui/admin.fxml");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Hàm cập nhật lại số dư trên Header cho Bidder
     */
    public void refreshBalanceView() {
        User user = SessionManager.getCurrentUser();
        if (user instanceof Bidder bidder) {
            NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
            userBalanceLabel.setText("Số dư: " + nf.format(bidder.getBalance()) + " ₫");
            userBalanceLabel.setVisible(true);
        } else {
            userBalanceLabel.setVisible(false);
        }
    }

    private void addTab(String title, String fxmlPath) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Node content = loader.load();
        Tab tab = new Tab(title);
        tab.setContent(content);
        tab.setClosable(false);
        mainTabPane.getTabs().add(tab);
    }

    @FXML
    private void handleLogout() {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser != null) {
            currentUser.logout();
        }
        SessionManager.logout();
        try {
            Main.showLogin();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}