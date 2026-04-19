package com.auction.gui;

import com.auction.manager.AuctionManager;
import com.auction.model.entity.Auction;
import com.auction.model.entity.Bidder;
import com.auction.pattern.observer.Observer;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class BidderDashboard implements Observer {
    private BorderPane view;
    private Bidder currentBidder;

    private ListView<Auction> auctionListView;
    private Label lblCurrentPrice;
    private Label lblLeader;
    private Label lblItemDetails;
    private TextField txtBidAmount;
    private Button btnBid;
    
    private Auction currentAuctionViewing; // Phiên đấu giá đang mở xem

    public BidderDashboard() {
        this.currentBidder = (Bidder) MainApp.getCurrentUser();
        view = new BorderPane();
        view.setPadding(new Insets(15));

        HBox header = new HBox(20);
        Label lblWelcome = new Label("Bidder: " + currentBidder.getUsername() + " | Số dư: " + currentBidder.getBalance());
        Button btnLogout = new Button("Đăng xuất");
        btnLogout.setOnAction(e -> {

            if(currentAuctionViewing != null) currentAuctionViewing.detach(this);
            MainApp.showLoginScreen();
        });
        header.getChildren().addAll(lblWelcome, btnLogout);
        view.setTop(header);

        VBox leftBox = new VBox(10);
        leftBox.setPadding(new Insets(10));

        auctionListView = new ListView<>(FXCollections.observableArrayList(AuctionManager.getInstance().getRunningAuctions()));
        auctionListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Auction auction, boolean empty) {
                super.updateItem(auction, empty);
                if (empty || auction == null) setText(null);
                else setText(auction.getItem().getName() + " - ID: " + auction.getId());
            }
        });
        
        auctionListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) joinAuctionRoom(newVal);
        });
        leftBox.getChildren().addAll(new Label("Phiên đang diễn ra:"), auctionListView);
        view.setLeft(leftBox);

        VBox centerBox = new VBox(15);
        centerBox.setPadding(new Insets(10, 20, 10, 20));
        centerBox.setStyle("-fx-border-color: lightgray; -fx-border-width: 1;");

        lblItemDetails = new Label("Chọn một phiên đấu giá bên trái để tham gia.");
        lblItemDetails.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        lblCurrentPrice = new Label("Giá cao nhất: -");
        lblLeader = new Label("Người dẫn đầu: -");

        HBox bidBox = new HBox(10);
        txtBidAmount = new TextField();
        txtBidAmount.setPromptText("Nhập số tiền bid");
        btnBid = new Button("Đặt Giá");
        btnBid.setDisable(true); // Disable khi chưa chọn phiên

        btnBid.setOnAction(e -> placeBid());
        
        bidBox.getChildren().addAll(txtBidAmount, btnBid);
        centerBox.getChildren().addAll(lblItemDetails, lblCurrentPrice, lblLeader, bidBox);
        view.setCenter(centerBox);
    }

    private void joinAuctionRoom(Auction auction) {
        // Gỡ theo dõi phiên cũ nếu có
        if (currentAuctionViewing != null) {
            currentAuctionViewing.detach(this);
        }
        
        currentAuctionViewing = auction;

        currentAuctionViewing.attach(this);
        

        updateUIRoom(auction);
        btnBid.setDisable(false);
    }

    private void placeBid() {
        if (currentAuctionViewing == null) return;
        try {
            double amount = Double.parseDouble(txtBidAmount.getText());

            currentBidder.placeBid(currentAuctionViewing, amount);
            txtBidAmount.clear();
        } catch (NumberFormatException ex) {
            System.out.println("Vui lòng nhập số hợp lệ.");
        } catch (Exception ex) {

            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setContentText(ex.getMessage());
            alert.show();
        }
    }


    @Override
    public void update(Auction auction) {

        Platform.runLater(() -> {
            if (currentAuctionViewing != null && currentAuctionViewing.getId().equals(auction.getId())) {
                updateUIRoom(auction);
            }
        });
    }


    private void updateUIRoom(Auction auction) {
        lblItemDetails.setText("Sản phẩm: " + auction.getItem().getName() + " \nTrạng thái: " + auction.getStatus());
        lblCurrentPrice.setText(String.format("Giá cao nhất hiện tại: %.2f", auction.getCurrentHighestPrice()));
        
        String leaderName = (auction.getCurrentLeader() != null) ? auction.getCurrentLeader().getUsername() : "Chưa có";
        lblLeader.setText("Người dẫn đầu: " + leaderName);

        if (auction.getStatus() != com.auction.model.enums.AuctionStatus.RUNNING) {
            btnBid.setDisable(true);
        }
    }

    public BorderPane getView() {
        return view;
    }
}