package view.controller.auction;

import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import model.auction.Auction;
import model.auction.BidTransaction;
import network.NotificationManager;
import network.SessionManager;
import shared.Protocol;
import view.utility.display.AlertHelper;
import view.utility.auction.AuctionRoomCommandService;
import view.utility.auction.AuctionRoomHelper;
import view.utility.display.ChartHelper;
import view.utility.auction.ParticipantManagementDialog;
import view.utility.table.WrappingTextCellFactory;

import java.net.URL;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.ResourceBundle;

/**
 * Controller for the seller/admin view inside an auction room.
 * UI rendering stays here; room commands and participant popup are delegated.
 */
public class SellerInRoomController implements Initializable {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    @FXML private Label lblRoomId;
    @FXML private Label totalTimeLabel;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblMinStep;
    @FXML private javafx.scene.chart.AreaChart<Number, Number> priceChart;
    @FXML private TableView<double[]> tableBidHistory;
    @FXML private TableColumn<double[], Integer> colRound;
    @FXML private TableColumn<double[], Double> colBidPrice;
    @FXML private Label lblEarnings;
    @FXML private Label lblParticipants;
    @FXML private Label lblRounds;
    @FXML private Label topBidderLabel;
    @FXML private TableView<NotificationManager.NotificationItem> notificationTableView;
    @FXML private TableColumn<NotificationManager.NotificationItem, String> colNotifTime;
    @FXML private TableColumn<NotificationManager.NotificationItem, String> colNotifContent;

    private final ObservableList<double[]> bidHistory = FXCollections.observableArrayList();
    private final ObservableList<NotificationManager.NotificationItem> roomNotifications = FXCollections.observableArrayList();
    private final ObservableList<String> activeParticipants = FXCollections.observableArrayList();
    private final AuctionRoomCommandService commandService = new AuctionRoomCommandService();

    private XYChart.Series<Number, Number> priceSeries;
    private int bidCount = 0;
    private Auction auction;
    private String currentAuctionId;
    private double currentHighestPrice = 0;
    private AuctionRoomHelper roomHelper;
    private boolean isExiting = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configureChart();
        configureBidHistoryTable();
        configureNotificationTable();
        addKickButtonForAdmin();
    }

    public void setAuction(Auction auction) {
        this.auction = auction;
        this.currentAuctionId = auction.getId();
        if (lblRoomId != null) {
            lblRoomId.setText("ID phong: " + auction.getId());
        }

        resetRoomHelper();
        registerNetworkListeners();
        commandService.joinRoom(currentAuctionId);
        restoreAuctionState();
    }

    public void cleanupRoom() {
        if (roomHelper != null) {
            roomHelper.cleanup();
        }
    }

    @FXML
    public void exitRoom(ActionEvent event) {
        if (isExiting) {
            return;
        }
        isExiting = true;
        if (roomHelper != null) {
            roomHelper.exitRoom(getCurrentStage());
        }
    }

    private void configureChart() {
        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Bien dong gia ($)");
        priceChart.getData().add(priceSeries);
        ChartHelper.configureAreaChart(priceChart);
    }

    private void configureBidHistoryTable() {
        colRound.setCellValueFactory(cd -> new SimpleIntegerProperty((int) cd.getValue()[0]).asObject());
        colBidPrice.setCellValueFactory(cd -> new SimpleDoubleProperty(cd.getValue()[1]).asObject());
        tableBidHistory.setItems(bidHistory);
    }

    private void configureNotificationTable() {
        colNotifTime.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("time"));
        colNotifContent.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("content"));
        colNotifContent.setCellFactory(new WrappingTextCellFactory());
        notificationTableView.setItems(roomNotifications);
    }

    private void addKickButtonForAdmin() {
        if (!SessionManager.getInstance().isAdmin()) {
            return;
        }
        Button btnKick = new Button("Duoi nguoi dung");
        btnKick.getStyleClass().setAll("btn-danger");
        btnKick.setStyle("-fx-font-weight: bold; -fx-padding: 5 15;");
        btnKick.setOnAction(e -> showParticipantManagement());
        Platform.runLater(() -> {
            if (lblRoomId != null && lblRoomId.getParent() instanceof javafx.scene.layout.VBox parent) {
                parent.getChildren().add(btnKick);
            }
        });
    }

    private void showParticipantManagement() {
        ParticipantManagementDialog.show(getCurrentStage(), activeParticipants,
                username -> commandService.kickUser(currentAuctionId, username));
    }

    private void resetRoomHelper() {
        if (roomHelper != null) {
            roomHelper.cleanup();
        }

        roomHelper = new AuctionRoomHelper(currentAuctionId);
        roomHelper.setOnTimeUpdate(this::updateTimeLabel);

        if (auction.getEndTimeEpoch() > 0) {
            roomHelper.initTimer(auction.getEndTimeEpoch());
            roomHelper.startTimer();
        } else if (auction.getEndTime() != null) {
            roomHelper.initTimer(auction.getEndTime());
            roomHelper.startTimer();
        }

        roomHelper.registerTimeExtendedListener(() -> {
            updateTimeLabel();
            addNotification("Thoi gian duoc gia han");
        });
        roomHelper.registerParticipantsListener(count -> {
            if (lblParticipants != null) {
                lblParticipants.setText(count + " nguoi");
            }
        });
        roomHelper.registerRoomKickedListener(() -> exitRoom(null));
    }

    private void registerNetworkListeners() {
        roomHelper.registerRoomListener(Protocol.BROADCAST_NEW_BID, this::handleNewBidBroadcast);
        roomHelper.registerRoomListener(Protocol.BROADCAST_AUCTION_FINISHED, this::handleAuctionFinishedBroadcast);
        roomHelper.registerRoomListener(Protocol.BROADCAST_PARTICIPANTS, this::handleParticipantsBroadcast);
    }

    private void handleNewBidBroadcast(String message) {
        String[] parts = message.split(Protocol.DELIMITER);
        if (parts.length < 4 || !java.util.Objects.equals(parts[1], currentAuctionId)) {
            return;
        }

        double newPrice = Double.parseDouble(parts[2]);
        String topBidder = parts[3];
        currentHighestPrice = newPrice;

        Platform.runLater(() -> {
            updatePriceLabels(newPrice);
            if (topBidderLabel != null) {
                topBidderLabel.setText(topBidder);
            }
            bidCount++;
            ChartHelper.updateXAxisBounds(priceChart, bidCount);
            priceChart.setAnimated(true);
            priceSeries.getData().add(new XYChart.Data<>(bidCount, newPrice));
            bidHistory.add(0, new double[]{bidCount, newPrice});
            if (lblRounds != null) {
                lblRounds.setText(String.valueOf(bidCount));
            }
            updateIncrementDisplay(newPrice);
            if (auction != null) {
                addNotification("[" + auction.getItem().getName() + "] - Luot #" + bidCount + ": "
                        + topBidder + " vua dat $" + ChartHelper.formatDouble(newPrice));
            }
        });
    }

    private void handleAuctionFinishedBroadcast(String message) {
        String[] parts = message.split(Protocol.DELIMITER);
        if (parts.length < 2 || !java.util.Objects.equals(parts[1], currentAuctionId)) {
            return;
        }

        String winner = parts.length > 2 ? parts[2] : "Khong co";
        double finalPrice = parts.length > 3 ? Double.parseDouble(parts[3]) : 0;
        Platform.runLater(() -> {
            if (roomHelper != null) {
                roomHelper.stopTimer();
            }
            updatePriceLabels(finalPrice);
            if (topBidderLabel != null) {
                topBidderLabel.setText(winner);
            }
            addNotification("PHIEN DAU GIA KET THUC! Nguoi thang: "
                    + winner + " ($" + ChartHelper.formatDouble(finalPrice) + ")");
            AlertHelper.showInfo("Phien dau gia ket thuc!",
                    "Nguoi chien thang: " + winner + "\nGia cuoi cung: " + ChartHelper.formatDouble(finalPrice) + " $");
            if (!SessionManager.getInstance().isAdmin()) {
                exitRoom(null);
            }
        });
    }

    private void handleParticipantsBroadcast(String message) {
        String[] parts = message.split(Protocol.DELIMITER);
        if (parts.length >= 3 && java.util.Objects.equals(parts[1], currentAuctionId)) {
            Platform.runLater(() -> {
                activeParticipants.clear();
                activeParticipants.addAll(Arrays.asList(parts).subList(3, parts.length));
            });
        }
    }

    private void restoreAuctionState() {
        Platform.runLater(() -> {
            priceChart.setAnimated(false);
            priceSeries.getData().clear();
            bidHistory.clear();
            roomNotifications.clear();
            priceSeries.getData().add(new XYChart.Data<>(0, auction.getStartingPrice()));

            bidCount = 0;
            if (auction.getBidHistory() != null) {
                for (BidTransaction tx : auction.getBidHistory()) {
                    bidCount++;
                    priceSeries.getData().add(new XYChart.Data<>(bidCount, tx.getBidAmount()));
                    bidHistory.add(0, new double[]{bidCount, tx.getBidAmount()});
                    addNotification("[" + auction.getItem().getName() + "] - Luot #" + bidCount + ": "
                            + tx.getBidder().getUsername() + " da dat $" + ChartHelper.formatDouble(tx.getBidAmount()),
                            tx.getTimestamp().format(TIME_FORMATTER));
                }
            }

            if (auction.getHighestBidder() != null) {
                currentHighestPrice = auction.getCurrentPrice();
                updatePriceLabels(currentHighestPrice);
                if (topBidderLabel != null) {
                    topBidderLabel.setText(auction.getHighestBidder().getUsername());
                }
                if (lblRounds != null) {
                    lblRounds.setText(String.valueOf(bidCount));
                }
            } else {
                currentHighestPrice = auction.getStartingPrice();
                updatePriceLabels(currentHighestPrice);
                if (lblEarnings != null) {
                    lblEarnings.setText("0 $");
                }
                if (topBidderLabel != null) {
                    topBidderLabel.setText("Chua co");
                }
                if (lblRounds != null) {
                    lblRounds.setText("0");
                }
            }

            ChartHelper.updateXAxisBounds(priceChart, bidCount);
            updateIncrementDisplay(currentHighestPrice);
        });
    }

    private void updateTimeLabel() {
        if (totalTimeLabel != null && roomHelper != null) {
            totalTimeLabel.setText(ChartHelper.formatTime(roomHelper.getTotalTimeRemaining()));
        }
    }

    private void updatePriceLabels(double price) {
        if (lblCurrentPrice != null) {
            lblCurrentPrice.setText(ChartHelper.formatDouble(price) + " $");
        }
        if (lblEarnings != null) {
            lblEarnings.setText(ChartHelper.formatDouble(price) + " $");
        }
    }

    private void updateIncrementDisplay(double currentPrice) {
        double roundedIncrement = ChartHelper.calculateMinIncrement(currentPrice);
        Platform.runLater(() -> {
            if (lblMinStep != null) {
                lblMinStep.setText(ChartHelper.formatDouble(roundedIncrement) + " $");
            }
        });
    }

    private void addNotification(String content) {
        addNotification(content, LocalTime.now().format(TIME_FORMATTER));
    }

    private void addNotification(String content, String time) {
        roomNotifications.add(0, new NotificationManager.NotificationItem(content, time));
    }

    private Stage getCurrentStage() {
        if (priceChart != null && priceChart.getScene() != null
                && priceChart.getScene().getWindow() instanceof Stage stage) {
            return stage;
        }
        return null;
    }
}
