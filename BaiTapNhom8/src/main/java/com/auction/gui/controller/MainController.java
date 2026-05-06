package com.auction.gui.controller;

import com.auction.Main;
import com.auction.gui.SessionManager;
import com.auction.model.entity.Admin;
import com.auction.model.entity.Auction;
import com.auction.model.entity.Bidder;
import com.auction.model.entity.Seller;
import com.auction.model.entity.User;
import com.auction.model.enums.AuctionStatus;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;

import java.text.NumberFormat;
import java.util.Locale;

public class MainController {

    private static MainController instance;

    public static MainController getInstance() {
        return instance;
    }

    @FXML private Label userInfoLabel;
    @FXML private Label userBalanceLabel;
    @FXML private Button headerDepositButton;
    @FXML private TabPane mainTabPane;

    @FXML
    public void initialize() {
        instance = this;

        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        String role = (user instanceof Admin) ? "Admin" : (user instanceof Seller ? "Seller" : "Bidder");
        userInfoLabel.setText(user.getUsername() + "  |  " + role);

        refreshBalanceView();

        try {
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
     * Hàm cập nhật giao diện Header:
     * - Bidder: Hiện Số dư + Nút nạp tiền
     * - Seller: Hiện Tổng doanh thu
     * - Admin: Ẩn cả hai
     */
    public void refreshBalanceView() {
        User user = SessionManager.getCurrentUser();
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));

        if (user instanceof Bidder bidder) {
            userBalanceLabel.setText("Số dư: " + nf.format(bidder.getBalance()) + " ₫");
            userBalanceLabel.setVisible(true);
            userBalanceLabel.setManaged(true);
            if (headerDepositButton != null) {
                headerDepositButton.setVisible(true);
                headerDepositButton.setManaged(true);
            }
        } else if (user instanceof Seller seller) {
            // ---> LOGIC TÍNH TỔNG DOANH THU CHO SELLER <---
            double totalEarnings = seller.getAuctions().stream()
                    .filter(a -> a.getStatus() == AuctionStatus.PAID)
                    .mapToDouble(Auction::getCurrentHighestPrice)
                    .sum();

            userBalanceLabel.setText("Tổng doanh thu: " + nf.format(totalEarnings) + " ₫");
            userBalanceLabel.setVisible(true);
            userBalanceLabel.setManaged(true);

            // Seller thì không cần nút nạp tiền
            if (headerDepositButton != null) {
                headerDepositButton.setVisible(false);
                headerDepositButton.setManaged(false);
            }
        } else {
            // Admin thì ẩn hết
            userBalanceLabel.setVisible(false);
            userBalanceLabel.setManaged(false);
            if (headerDepositButton != null) {
                headerDepositButton.setVisible(false);
                headerDepositButton.setManaged(false);
            }
        }
    }

    @FXML
    private void handleDeposit() {
        User user = SessionManager.getCurrentUser();
        if (!(user instanceof Bidder bidder)) return;

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nạp Tiền");
        dialog.setHeaderText("Nạp thêm tiền vào tài khoản");
        dialog.setContentText("Nhập số tiền cần nạp (VNĐ):");

        dialog.showAndWait().ifPresent(input -> {
            try {
                double amount = Double.parseDouble(input.replace(",", "").replace(".", "").trim());
                if (amount <= 0) {
                    Alert a = new Alert(Alert.AlertType.ERROR, "Số tiền nạp phải lớn hơn 0.");
                    a.setHeaderText("Lỗi nhập liệu");
                    a.showAndWait();
                    return;
                }

                bidder.deposit(amount);
                refreshBalanceView();

                NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
                Alert a = new Alert(Alert.AlertType.INFORMATION, "Nạp thành công " + nf.format(amount) + " ₫!");
                a.setTitle("Thành công");
                a.setHeaderText(null);
                a.showAndWait();

            } catch (NumberFormatException ex) {
                Alert a = new Alert(Alert.AlertType.ERROR, "Số tiền không hợp lệ. Vui lòng chỉ nhập số.");
                a.setHeaderText("Lỗi định dạng");
                a.showAndWait();
            }
        });
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