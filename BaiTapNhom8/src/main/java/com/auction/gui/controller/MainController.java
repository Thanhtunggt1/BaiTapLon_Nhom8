package com.auction.gui.controller;

import com.auction.Main;
import com.auction.gui.SessionManager;
import com.auction.network.NetworkClient;
import com.auction.network.Message;
import com.auction.network.dto.AuctionDto;
import com.auction.network.dto.UserDto;
import com.auction.manager.AuctionManager;
import com.auction.model.entity.DepositRequest;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
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

        UserDto user = SessionManager.getCurrentUserDto();
        if (user == null) return;

        String role = "";
        if ("ADMIN".equals(user.role)) role = "Admin";
        else if ("SELLER".equals(user.role)) role = "Seller";
        else role = "Bidder";

        userInfoLabel.setText(user.username + "  |  " + role);

        refreshBalanceView();

        try {
            addTab("Danh Sách Đấu Giá", "/com/auction/gui/auction_list.fxml");

            if ("SELLER".equals(user.role)) {
                addTab("Quản Lý Bán Hàng", "/com/auction/gui/seller.fxml");
            }
            if ("ADMIN".equals(user.role)) {
                addTab("Quản Trị Hệ Thống", "/com/auction/gui/admin.fxml");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void refreshBalanceView() {
        UserDto user = SessionManager.getCurrentUserDto();
        if (user == null) return;
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));

        if ("BIDDER".equals(user.role)) {
            userBalanceLabel.setText("Số dư: " + nf.format(user.balance) + " ₫");
            userBalanceLabel.setVisible(true);
            userBalanceLabel.setManaged(true);
            if (headerDepositButton != null) {
                headerDepositButton.setVisible(true);
                headerDepositButton.setManaged(true);
            }
        } else if ("SELLER".equals(user.role)) {
            // --- GỌI MẠNG LẤY TỔNG DOANH THU THAY VÌ LẤY Ở LOCAL ---
            Message response = NetworkClient.getInstance().getAuctions();
            double totalEarnings = 0;
            if (response.isSuccess()) {
                List<AuctionDto> dtos = response.getPayload(new com.google.gson.reflect.TypeToken<List<AuctionDto>>(){}.getType());
                totalEarnings = dtos.stream()
                        .filter(a -> "PAID".equals(a.status) && user.username.equals(a.sellerUsername))
                        .mapToDouble(a -> a.currentPrice)
                        .sum();
            }

            userBalanceLabel.setText("Tổng doanh thu: " + nf.format(totalEarnings) + " ₫");
            userBalanceLabel.setVisible(true);
            userBalanceLabel.setManaged(true);

            if (headerDepositButton != null) {
                headerDepositButton.setVisible(false);
                headerDepositButton.setManaged(false);
            }
        } else {
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
        UserDto user = SessionManager.getCurrentUserDto();
        if (user == null || !"BIDDER".equals(user.role)) return;

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nạp Tiền");
        dialog.setHeaderText("Nạp thêm tiền vào tài khoản");
        dialog.setContentText("Nhập số tiền cần nạp (VNĐ):");

        dialog.showAndWait().ifPresent(input -> {
            try {
                NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));

                double amount = Double.parseDouble(input.replace(",", "").replace(".", "").trim());
                if (amount <= 0) {
                    Alert a = new Alert(Alert.AlertType.ERROR, "Số tiền nạp phải lớn hơn 0.");
                    a.showAndWait();
                    return;
                }

                com.auction.model.entity.User localUser = SessionManager.getCurrentUser();

                if (localUser instanceof com.auction.model.entity.Bidder bidder) {
                    DepositRequest request = bidder.requestDeposit(amount);
                    AuctionManager.getInstance().addDepositRequest(request);

                    Alert a = new Alert(Alert.AlertType.INFORMATION,
                            "Đã gửi yêu cầu nạp " + nf.format(amount) + " ₫. Vui lòng chờ Admin xác nhận.");
                    a.setHeaderText(null);
                    a.showAndWait();

                    refreshBalanceView();
                } else {
                    Alert a = new Alert(Alert.AlertType.ERROR, "Chỉ Bidder mới được gửi yêu cầu nạp tiền.");
                    a.showAndWait();
                }

            } catch (NumberFormatException ex) {
                Alert a = new Alert(Alert.AlertType.ERROR, "Số tiền không hợp lệ. Vui lòng chỉ nhập số.");
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
        com.auction.model.entity.User currentUser = SessionManager.getCurrentUser();
        if (currentUser != null) currentUser.logout();
        SessionManager.logout();

        List<Window> openWindows = new ArrayList<>(Window.getWindows());
        for (Window window : openWindows) {
            if (window instanceof Stage && window != Main.getPrimaryStage()) {
                ((Stage) window).close();
            }
        }

        try {
            Main.showLogin();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}