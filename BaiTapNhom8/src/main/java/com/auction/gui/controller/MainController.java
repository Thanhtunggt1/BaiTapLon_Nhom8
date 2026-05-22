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
import com.auction.model.entity.DepositHistory;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.time.format.DateTimeFormatter;

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
    @FXML private TableView<DepositHistory> depositHistoryTable;
    @FXML private TableColumn<DepositHistory, String> colDepositTime;
    @FXML private TableColumn<DepositHistory, String> colDepositAmount;
    @FXML private TableColumn<DepositHistory, String> colBalanceAfter;

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
        setupDepositHistoryTable();
        loadDepositHistory();

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

        Dialog<javafx.util.Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Xác thực Nạp Tiền");
        dialog.setHeaderText("Vui lòng nhập số tiền và mật khẩu của bạn");

        ButtonType confirmButtonType = new ButtonType("Xác nhận", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField amountField = new TextField();
        amountField.setPromptText("Số tiền nạp (VNĐ)...");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Nhập mật khẩu...");

        grid.add(new Label("Số tiền:"), 0, 0);
        grid.add(amountField, 1, 0);
        grid.add(new Label("Mật khẩu:"), 0, 1);
        grid.add(passwordField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        javafx.application.Platform.runLater(amountField::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == confirmButtonType) {
                return new javafx.util.Pair<>(amountField.getText(), passwordField.getText());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            try {
 HEAD

                double amount = Double.parseDouble(result.getKey().replace(",", "").replace(".", "").trim());
                String password = result.getValue();



 6fc3332 (ham xac nhan tien)
                NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));

                double amount = Double.parseDouble(input.replace(",", "").replace(".", "").trim());
 6fc3332 (ham xac nhan tien)
                if (amount <= 0) {
                    Alert a = new Alert(Alert.AlertType.ERROR, "Số tiền nạp phải lớn hơn 0.");
                    a.showAndWait();
                    return;
                }
                if (password.trim().isEmpty()) {
                    Alert a = new Alert(Alert.AlertType.ERROR, "Mật khẩu không được để trống!");
                    a.showAndWait();
                    return;
                }

 HEAD

                Message response = NetworkClient.getInstance().deposit(amount, password);

                com.auction.model.entity.User localUser = SessionManager.getCurrentUser();
 6fc3332 (ham xac nhan tien)

                if (localUser instanceof com.auction.model.entity.Bidder bidder) {
                    DepositRequest request = bidder.requestDeposit(amount);
                    AuctionManager.getInstance().addDepositRequest(request);


                    com.auction.model.entity.User localUser = SessionManager.getCurrentUser();
                    if (localUser instanceof com.auction.model.entity.Bidder bidder) {
                        bidder.deposit(amount);
                    }

                    refreshBalanceView();
                    loadDepositHistory();
                    try {
                        AuctionDetailController.updateAllBalances();
                    } catch (Exception ignored) {}

                    NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
                    Alert a = new Alert(Alert.AlertType.INFORMATION, "Nạp thành công " + nf.format(amount) + " ₫!");

                    Alert a = new Alert(Alert.AlertType.INFORMATION,
                            "Đã gửi yêu cầu nạp " + nf.format(amount) + " ₫. Vui lòng chờ Admin xác nhận.");
 6fc3332 (ham xac nhan tien)

                com.auction.model.entity.User localUser = SessionManager.getCurrentUser();

                if (localUser instanceof com.auction.model.entity.Bidder bidder) {
                    DepositRequest request = bidder.requestDeposit(amount);
                    AuctionManager.getInstance().addDepositRequest(request);

                    Alert a = new Alert(Alert.AlertType.INFORMATION,
                            "Đã gửi yêu cầu nạp " + nf.format(amount) + " ₫. Vui lòng chờ Admin xác nhận.");
 6fc3332 (ham xac nhan tien)
                    a.setHeaderText(null);
                    a.showAndWait();

                    refreshBalanceView();
                } else {
 HEAD

                    String errMsg = response.getErrorMessage();
                    if (errMsg.startsWith("LOCK_TIMER:")) {
                        int initialSecs = 180;
                        try {
                            initialSecs = Integer.parseInt(errMsg.split(":")[1]);
                        } catch (Exception ignored) {}

                        int[] secsLeft = { initialSecs };
                        Alert a = new Alert(Alert.AlertType.ERROR);
                        a.setTitle("Lỗi xác thực");
                        a.setHeaderText("Tạm khóa chức năng nạp tiền");

                        Runnable updateText = () -> {
                            if (secsLeft[0] > 0) {
                                a.setContentText("Bạn đã nhập sai mật khẩu quá 3 lần.\nVui lòng thử lại sau: " + secsLeft[0] + " giây.");
                                secsLeft[0]--;
                            } else {
                                a.setContentText("Thời gian khóa đã hết. Vui lòng đóng bảng này và thử lại.");
                            }
                        };

                        updateText.run();
                        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), ev -> updateText.run())
                        );
                        timeline.setCycleCount(javafx.animation.Timeline.INDEFINITE);
                        timeline.play();

                        a.setOnHidden(ev -> timeline.stop());
                        a.showAndWait();
                    } else {
                        Alert a = new Alert(Alert.AlertType.ERROR, errMsg);
                        a.setHeaderText(null);
                        a.showAndWait();
                    }


 6fc3332 (ham xac nhan tien)
                    Alert a = new Alert(Alert.AlertType.ERROR, "Chỉ Bidder mới được gửi yêu cầu nạp tiền.");
                    a.showAndWait();
 6fc3332 (ham xac nhan tien)
                }

            } catch (NumberFormatException ex) {
                Alert a = new Alert(Alert.AlertType.ERROR, "Số tiền không hợp lệ. Vui lòng chỉ nhập số.");
                a.showAndWait();
            }
        });
    }

    private void setupDepositHistoryTable() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));

        colDepositTime.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getCreatedAt().format(formatter))
        );

        colDepositAmount.setCellValueFactory(d ->
                new SimpleStringProperty(nf.format(d.getValue().getAmount()) + " đ")
        );

        colBalanceAfter.setCellValueFactory(d ->
                new SimpleStringProperty(nf.format(d.getValue().getBalanceAfterDeposit()) + " đ")
        );
    }

    private void loadDepositHistory() {
        com.auction.model.entity.User localUser = SessionManager.getCurrentUser();

        if (localUser instanceof com.auction.model.entity.Bidder bidder) {
            depositHistoryTable.setItems(
                    FXCollections.observableArrayList(bidder.getDepositHistories())
            );
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