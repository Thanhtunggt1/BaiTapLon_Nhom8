package com.auction.gui.controller;

import com.auction.network.NetworkClient;
import com.auction.network.Message;
import com.auction.network.dto.AuctionDto;
import com.auction.manager.AuctionManager;
import com.auction.model.entity.Admin;
import com.auction.model.entity.DepositRequest;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class AdminController {

    @FXML private Label totalAuctionsLabel;
    @FXML private Label runningLabel;
    @FXML private Label totalUsersLabel;
    @FXML private TextField promoteUsernameField;

    @FXML private TableView<AuctionDto> allAuctionsTable;
    @FXML private TableColumn<AuctionDto, String> colId;
    @FXML private TableColumn<AuctionDto, String> colAdminItem;
    @FXML private TableColumn<AuctionDto, String> colAdminSeller;
    @FXML private TableColumn<AuctionDto, String> colAdminPrice;
    @FXML private TableColumn<AuctionDto, String> colAdminLeader;
    @FXML private TableColumn<AuctionDto, String> colAdminStatus;
    @FXML private TableView<DepositRequest> depositRequestsTable;
    @FXML private TableColumn<DepositRequest, String> colDepositId;
    @FXML private TableColumn<DepositRequest, String> colDepositBidder;
    @FXML private TableColumn<DepositRequest, String> colDepositAmount;
    @FXML private TableColumn<DepositRequest, String> colDepositStatus;

    private final NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    @FXML
    public void initialize() {
        colId.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().id));
        colAdminItem.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().itemName));
        colAdminSeller.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().sellerUsername));
        colAdminPrice.setCellValueFactory(d -> new SimpleStringProperty(nf.format(d.getValue().currentPrice)));
        colAdminLeader.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().currentLeader == null ? "---" : d.getValue().currentLeader));
        colAdminStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().status));
        colDepositId.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getId()));
        colDepositBidder.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBidder().getUsername()));
        colDepositAmount.setCellValueFactory(d -> new SimpleStringProperty(nf.format(d.getValue().getAmount())));
        colDepositStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus().name()));
        loadData();
    }

    @FXML
    private void handleRefresh() { loadData(); }

    @FXML
    private void handlePromoteToAdmin() {
        String user = promoteUsernameField.getText().trim();
        if (user.isEmpty()) return;
        Message res = NetworkClient.getInstance().promoteUser(user, "ADMIN");
        if (res.isSuccess()) {
            alert("Thành công", "Đã nâng cấp " + user + " thành Admin.");
            promoteUsernameField.clear();
        } else alert("Lỗi", res.getErrorMessage());
    }

    @FXML
    private void handleCancelAuction() {
        AuctionDto sel = allAuctionsTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        NetworkClient.getInstance().adminCancelAuction(sel.id, "Admin hủy phiên.");
        loadData();
    }

    @FXML
    private void handleEndAuction() {
        AuctionDto sel = allAuctionsTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        NetworkClient.getInstance().endAuction(sel.id);
        loadData();
    }

    @FXML
    private void handleMarkPaid() {
        AuctionDto sel = allAuctionsTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        NetworkClient.getInstance().markPaid(sel.id);
        loadData();
    }
    @FXML
    private void handleApproveDeposit() {
        DepositRequest request = depositRequestsTable.getSelectionModel().getSelectedItem();
        if (request == null) return;

        Admin admin = new Admin("admin", "123456", "admin@gmail.com");
        admin.approveDeposit(request);

        loadData();
        alert("Thành công", "Đã duyệt yêu cầu nạp tiền.");
    }

    @FXML
    private void handleRejectDeposit() {
        DepositRequest request = depositRequestsTable.getSelectionModel().getSelectedItem();
        if (request == null) return;

        Admin admin = new Admin("admin", "123456", "admin@gmail.com");
        admin.rejectDeposit(request);

        loadData();
        alert("Thành công", "Đã từ chối yêu cầu nạp tiền.");
    }

    private void loadData() {
        new Thread(() -> {
            Message res = NetworkClient.getInstance().getAuctions();
            if (res.isSuccess()) {
                List<AuctionDto> list = res.getPayload(new com.google.gson.reflect.TypeToken<List<AuctionDto>>(){}.getType());
                Platform.runLater(() -> {
                    allAuctionsTable.setItems(FXCollections.observableArrayList(list));
                    depositRequestsTable.setItems(
                            FXCollections.observableArrayList(AuctionManager.getInstance().getDepositRequests())
                    );
                    totalAuctionsLabel.setText(String.valueOf(list.size()));
                    runningLabel.setText(String.valueOf(list.stream().filter(a -> "RUNNING".equals(a.status)).count()));
                });
            }
        }).start();
    }

    private void alert(String t, String m) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(t);
        a.setHeaderText(null);
        a.setContentText(m);
        a.showAndWait();
    }
}