package com.auction.gui.controller;

import com.auction.Main;
import com.auction.gui.SessionManager;
import com.auction.model.entity.Admin;
import com.auction.model.entity.Seller;
import com.auction.model.entity.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

public class MainController {

    @FXML private Label userInfoLabel;
    @FXML private TabPane mainTabPane;

    @FXML
    public void initialize() {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        String role;
        String icon;
        if (user instanceof Admin)  { role = "Admin"; }
        else if (user instanceof Seller) { role = "Seller"; }
        else                        { role = "Bidder"; }

        userInfoLabel.setText(user.getUsername() + "  |  " + role);

        try {
            // Tất cả user đều thấy danh sách đấu giá
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
        SessionManager.getCurrentUser().logout();
        SessionManager.logout();
        try {
            Main.showLogin();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}