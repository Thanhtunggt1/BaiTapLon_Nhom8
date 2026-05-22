package com.auction.gui.controller;

import com.auction.network.NetworkClient;
import com.auction.network.Message;
import com.auction.network.dto.AuctionDto;
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

    private final NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    @FXML
    public void initialize() {
        String rightAlign = "-fx-alignment: CENTER-RIGHT; -fx-padding: 0 10 0 0;";
        colId.setStyle(rightAlign);
        colAdminItem.setStyle(rightAlign);
        colAdminSeller.setStyle(rightAlign);
        colAdminPrice.setStyle(rightAlign);
        colAdminLeader.setStyle(rightAlign);
        colAdminStatus.setStyle(rightAlign);

        colId.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().id));
        colAdminItem.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().itemName));
        colAdminSeller.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().sellerUsername));
        colAdminPrice.setCellValueFactory(d -> new SimpleStringProperty(nf.format(d.getValue().currentPrice)));
        colAdminLeader.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().currentLeader == null ? "---" : d.getValue().currentLeader));
        colAdminStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().status));

        colAdminStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(rightAlign); return; }
                setText(s);
                String style = "";
                switch (s) {
                    case "OPEN": style = "-fx-text-fill: #f39c12;"; break;
                    case "RUNNING": style = "-fx-text-fill: #27ae60;"; break;
                    case "FINISHED": style = "-fx-text-fill: #2980b9;"; break;
                    case "CANCELED": style = "-fx-text-fill: #e74c3c;"; break;
                    case "PAID": style = "-fx-text-fill: #8e44ad;"; break;
                }
                setStyle(rightAlign + " " + style);
            }
        });

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

    private void loadData() {
        new Thread(() -> {
            Message res = NetworkClient.getInstance().getAuctions();
            if (res.isSuccess()) {
                List<AuctionDto> list = res.getPayload(new com.google.gson.reflect.TypeToken<List<AuctionDto>>(){}.getType());
                Platform.runLater(() -> {
                    allAuctionsTable.setItems(FXCollections.observableArrayList(list));
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