package view.BaseMenuUI;

import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import model.auction.Auction;
import network.ClientNetworkManager;
import shared.Protocol;
import view.utility.SceneManager;
import view.utility.WindowManager;

import java.net.URL;
import java.util.ResourceBundle;

public class AssertsListController implements Initializable {
    @FXML private Pane darkOverlay;
    @FXML private ScrollPane notificationMenu;
    private NotificationMenuHandler notificationMenuHandler;

    @FXML private HBox menuBar;
    @FXML private TableView<Auction> tableAsserts;
    @FXML private TableColumn<Auction, Integer> colSTT;
    @FXML private TableColumn<Auction, String> colItemName;
    @FXML private TableColumn<Auction, String> colType;
    @FXML private TableColumn<Auction, String> colId;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colSTT.setCellValueFactory(cellData ->
                new SimpleIntegerProperty(tableAsserts.getItems().indexOf(cellData.getValue()) + 1).asObject());
        colItemName.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getItem().getName()));
        colType.setCellValueFactory(cellData -> {
            String type = cellData.getValue().getItem().getClass().getSimpleName();
            String displayType = switch (type) {
                case "Electronics" -> "🔌 Đồ điện";
                case "Vehicle" -> "🚗 Xe cộ";
                case "Arts" -> "🎨 Nghệ thuật";
                default -> type;
            };
            return new SimpleStringProperty(displayType);
        });
        colId.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getId()));

        notificationMenuHandler = new NotificationMenuHandler(darkOverlay, notificationMenu, 266);

        // LẮNG NGHE DANH SÁCH TỪ SERVER (REAL-TIME - Sẽ tự động chạy khi Server Broadcast)
        ClientNetworkManager.getInstance().setAuctionListListener((listFromServer) -> {
            if (listFromServer != null) {
                System.out.println("✅ UI: Đã nhận dữ liệu, đang tiến hành vẽ bảng tài sản...");
                ObservableList<Auction> data = FXCollections.observableArrayList(listFromServer);
                Platform.runLater(() -> tableAsserts.setItems(data));
            }
        });

        // Lắng nghe BROADCAST_NEW_BID để tự động cập nhật
        ClientNetworkManager.getInstance().registerListener(Protocol.BROADCAST_NEW_BID, (message) -> {
            ClientNetworkManager.getInstance().sendData(Protocol.REQ_GET_AUCTIONS);
        });

        // GỬI YÊU CẦU LẤY DANH SÁCH (Chạy lần đầu khi mở màn hình)
        ClientNetworkManager.getInstance().sendData(Protocol.REQ_GET_AUCTIONS);
    }

    @FXML
    private void joinNewBidding(ActionEvent event) {
        Stage currentStage = (Stage) menuBar.getScene().getWindow();
        WindowManager.openBidOrSellChoiceWindow(currentStage);
    }

    @FXML
    private void toggleNotificationMenu(ActionEvent event) {
        notificationMenuHandler.toggleMenu();
    }

    @FXML
    private void backToBaseMenuButtonClicked(ActionEvent event) {
        Stage stage = (Stage) menuBar.getScene().getWindow();
        SceneManager.switchScene(stage, "/view/BaseMenuUI/BaseMenu.fxml", "Base Menu");
    }

    @FXML
    private void backToLoiginButtonClicked(ActionEvent event) {
        Stage stage = (Stage) menuBar.getScene().getWindow();
        SceneManager.switchScene(stage, "/view/AuthenticationUI/LoginView/Login.fxml", "Login");
    }

    @FXML
    private void backToRegisterButtonClicked(ActionEvent event) {
        Stage stage = (Stage) menuBar.getScene().getWindow();
        SceneManager.switchScene(stage, "/view/AuthenticationUI/RegisterView/Register.fxml", "Register");
    }

    @FXML
    private void goToAssertsListButtonClicked(ActionEvent event) {
        // ĐÃ SỬA: Thay vì chuyển màn hình, chỉ gửi yêu cầu lấy danh sách mới nếu người dùng bấm thủ công
        System.out.println("🔄 Đang làm mới danh sách tài sản...");
        ClientNetworkManager.getInstance().sendData(Protocol.REQ_GET_AUCTIONS);
    }
}