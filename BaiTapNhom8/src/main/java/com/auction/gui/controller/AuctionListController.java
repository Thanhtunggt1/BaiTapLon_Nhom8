package com.auction.gui.controller;

import com.auction.network.NetworkClient;
import com.auction.network.Message;
import com.auction.network.dto.AuctionDto;
import com.auction.network.dto.UserDto;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.util.List;

public class AuctionListController {
    @FXML private ComboBox<String> statusFilter;
    @FXML private TableView<AuctionDto> auctionTable;
    @FXML private TableColumn<AuctionDto, String> colItem;
    @FXML private TableColumn<AuctionDto, String> colType;
    @FXML private TableColumn<AuctionDto, String> colCurrentPrice;
    @FXML private TableColumn<AuctionDto, String> colStatus;
    @FXML private TableColumn<AuctionDto, Void> colAction;

    @FXML
    public void initialize() {
        setupColumns();
        loadAuctions();

        // Đăng ký nhận cập nhật Realtime từ Server
        NetworkClient.getInstance().setOnBidUpdate(updatedAuction -> {
            Platform.runLater(() -> {
                for (int i = 0; i < auctionTable.getItems().size(); i++) {
                    if (auctionTable.getItems().get(i).id.equals(updatedAuction.id)) {
                        auctionTable.getItems().set(i, updatedAuction);
                        break;
                    }
                }
                auctionTable.refresh();
            });
        });
    }

    private void setupColumns() {
        colItem.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().itemName));
        colType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().itemType));
        colCurrentPrice.setCellValueFactory(d -> new SimpleStringProperty(String.format("%,.0f ₫", d.getValue().currentPrice)));
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().status));

        // Cột chi tiết
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Xem");
            {
                btn.setOnAction(e -> openDetail(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    @FXML
    public void loadAuctions() {
        Message response = NetworkClient.getInstance().getAuctions();
        if (response.isSuccess()) {
            List<AuctionDto> dtos = response.getPayload(new com.google.gson.reflect.TypeToken<List<AuctionDto>>(){}.getType());
            auctionTable.setItems(FXCollections.observableArrayList(dtos));
        }
    }

    private void openDetail(AuctionDto dto) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/gui/auction_detail.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load()));
            AuctionDetailController ctrl = loader.getController();
            ctrl.setAuctionDto(dto); // Truyền DTO vào thay vì object Auction
            stage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }
}