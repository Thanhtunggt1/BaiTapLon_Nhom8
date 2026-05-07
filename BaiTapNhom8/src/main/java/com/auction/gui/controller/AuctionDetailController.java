package com.auction.gui.controller;

import com.auction.network.NetworkClient;
import com.auction.network.Message;
import com.auction.network.dto.AuctionDto;
import com.auction.network.dto.UserDto;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class AuctionDetailController {
    @FXML private Label itemNameLabel;
    @FXML private Label currentPriceLabel;
    @FXML private TextField bidAmountField;
    @FXML private Label bidMessage;

    private AuctionDto auctionDto;

    public void setAuctionDto(AuctionDto dto) {
        this.auctionDto = dto;
        refreshUI();
    }

    private void refreshUI() {
        itemNameLabel.setText(auctionDto.itemName);
        currentPriceLabel.setText(String.format("%,.0f ₫", auctionDto.currentPrice));
        // Cập nhật các label khác tương tự...
    }

    @FXML
    private void handlePlaceBid() {
        try {
            double amount = Double.parseDouble(bidAmountField.getText().trim());
            Message response = NetworkClient.getInstance().placeBid(auctionDto.id, amount);

            if (response.isSuccess()) {
                bidMessage.setText("Đặt giá thành công!");
                bidMessage.setStyle("-fx-text-fill: #27ae60;");
                this.auctionDto = response.getPayload(AuctionDto.class);
                refreshUI();
            } else {
                bidMessage.setText(response.getErrorMessage());
                bidMessage.setStyle("-fx-text-fill: #e74c3c;");
            }
        } catch (Exception e) {
            bidMessage.setText("Số tiền không hợp lệ.");
        }
    }

    @FXML
    private void handleDeposit() {
        // Tương tự, gọi NetworkClient.getInstance().deposit(amount)
        // Cập nhật lại số dư trong UI sau khi Server phản hồi thành công
    }
}