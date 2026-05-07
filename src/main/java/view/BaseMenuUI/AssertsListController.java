package view.BaseMenuUI;

import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import model.auction.Auction;
import network.ClientNetworkManager;
import shared.Protocol;
import view.SceneManager;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;

import java.net.URL;
import java.util.List;
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
        // 1. Cấu hình các cột hiển thị[cite: 14]
        colSTT.setCellValueFactory(cellData ->
                new SimpleIntegerProperty(tableAsserts.getItems().indexOf(cellData.getValue()) + 1).asObject());
        colItemName.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getItem().getName()));
        colType.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getItem().getClass().getSimpleName()));
        colId.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getId()));

        // 2. Yêu cầu danh sách từ Server[cite: 14]
        ClientNetworkManager.getInstance().sendData(Protocol.REQ_GET_AUCTIONS);

        // khởi tạo handler cho menu thông báo
        notificationMenuHandler = new NotificationMenuHandler(darkOverlay, notificationMenu, 266);

        // 3. Đợi và cập nhật dữ liệu lên bảng
        loadDataFromServer();
    }

    private void loadDataFromServer() {
        new Thread(() -> {
            try {
                List<Auction> list = null;
                int retry = 50; // Chờ 5 giây
                while (list == null && retry > 0) {
                    list = ClientNetworkManager.getInstance().getLastAuctionList();
                    Thread.sleep(100);
                    retry--;
                }

                if (list != null) {
                    System.out.println("✅ UI: Đã nhận dữ liệu, đang tiến hành vẽ bảng...");
                    ObservableList<Auction> data = FXCollections.observableArrayList(list);
                    Platform.runLater(() -> tableAsserts.setItems(data));
                } else {
                    System.out.println("⚠️ UI: Không nhận được danh sách nào từ Server.");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void openCreateItemPopup(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/SellerUI/CreateItem.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Tạo phiên đấu giá mới");
            stage.show();
        } catch (Exception e) {
            System.err.println("Lỗi mở Popup: " + e.getMessage());
            e.printStackTrace();
        }
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
        Stage stage = (Stage) menuBar.getScene().getWindow();
        SceneManager.switchScene(stage, "/view/BaseMenuUI/AssertsList.fxml", "Asserts List");
    }
}