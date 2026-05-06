package com.auction.gui.controller;

import com.auction.gui.SessionManager;
import com.auction.manager.AuctionManager;
import com.auction.model.entity.Auction;
import com.auction.model.entity.BidTransaction;
import com.auction.model.entity.Bidder;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BidderController {

    @FXML private Label balanceLabel;
    @FXML private TableView<BidTransaction> bidHistoryTable;
    @FXML private TableColumn<BidTransaction, String> colItemName;
    @FXML private TableColumn<BidTransaction, String> colBidAmount;
    @FXML private TableColumn<BidTransaction, String> colBidTime;
    @FXML private TableColumn<BidTransaction, String> colCurrentHighest;
    @FXML private TableColumn<BidTransaction, String> colAuctionStatus;
    @FXML private TableColumn<BidTransaction, Void> colAction;

    private static final NumberFormat NF = NumberFormat.getInstance(new Locale("vi", "VN"));
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @FXML
    public void initialize() {
        setupTable();
        loadData();
    }

    private void setupTable() {
        colItemName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAuction().getItem().getName()));
        colBidAmount.setCellValueFactory(d -> new SimpleStringProperty(NF.format(d.getValue().getAmount()) + " ₫"));
        colBidTime.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTimestamp().format(DTF)));
        colCurrentHighest.setCellValueFactory(d -> new SimpleStringProperty(NF.format(d.getValue().getAuction().getCurrentHighestPrice()) + " ₫"));
        colAuctionStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAuction().getStatus().toString()));

        // Định dạng màu trạng thái
        colAuctionStatus.setCellFactory(col -> new TableCell<BidTransaction, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                String style = "-fx-text-fill: #f39c12;";
                switch (item) {
                    case "RUNNING": style = "-fx-text-fill: #27ae60;"; break;
                    case "FINISHED": style = "-fx-text-fill: #2980b9;"; break;
                    case "PAID": style = "-fx-text-fill: #8e44ad;"; break;
                    case "CANCELED": style = "-fx-text-fill: #e74c3c;"; break;
                }
                setStyle(style);
            }
        });

        // Nút Xem phiên
        colAction.setCellFactory(col -> new TableCell<BidTransaction, Void>() {
            private final Button btn = new Button("Xem");
            {
                btn.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; -fx-cursor: hand;");
                btn.setOnAction(e -> openDetail(getTableView().getItems().get(getIndex()).getAuction()));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    @FXML
    public void handleRefresh() {
        loadData();
        // Cập nhật cả số dư trên thanh Header
        if (MainController.getInstance() != null) {
            MainController.getInstance().refreshBalanceView();
        }
    }

    @FXML
    private void handleDeposit() {
        Bidder bidder = (Bidder) SessionManager.getCurrentUser();
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nạp Tiền");
        dialog.setHeaderText("Nạp thêm tiền vào tài khoản");
        dialog.setContentText("Nhập số tiền (VNĐ):");

        dialog.showAndWait().ifPresent(input -> {
            try {
                double amount = Double.parseDouble(input.replace(",", "").replace(".", "").trim());
                if (amount > 0) {
                    bidder.deposit(amount);
                    handleRefresh();
                    new Alert(Alert.AlertType.INFORMATION, "Nạp thành công " + NF.format(amount) + " ₫!").showAndWait();
                }
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Số tiền không hợp lệ! Chỉ nhập số.").showAndWait();
            }
        });
    }

    private void loadData() {
        Bidder bidder = (Bidder) SessionManager.getCurrentUser();
        if (bidder == null) return;

        // 1. Cập nhật số dư
        balanceLabel.setText(NF.format(bidder.getBalance()) + " ₫");

        // 2. Lấy toàn bộ lịch sử đấu giá của Bidder này
        List<BidTransaction> myBids = new ArrayList<>();
        for (Auction auction : AuctionManager.getInstance().getAllAuctions()) {
            for (BidTransaction bid : auction.getBidHistory()) {
                if (bid.getBidder().equals(bidder)) {
                    myBids.add(bid);
                }
            }
        }

        // 3. Sắp xếp các giao dịch mới nhất lên đầu tiên
        myBids.sort((b1, b2) -> b2.getTimestamp().compareTo(b1.getTimestamp()));

        bidHistoryTable.setItems(FXCollections.observableArrayList(myBids));
    }

    private void openDetail(Auction auction) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/gui/auction_detail.fxml"));
            Scene scene = new Scene(loader.load(), 960, 680);
            AuctionDetailController ctrl = loader.getController();
            ctrl.setAuction(auction);

            Stage stage = new Stage();
            stage.setTitle("Chi Tiết – " + auction.getItem().getName());
            stage.setScene(scene);
            stage.setOnHidden(e -> handleRefresh());
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}