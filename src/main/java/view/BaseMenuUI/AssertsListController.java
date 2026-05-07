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
        colType.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getItem().getClass().getSimpleName()));
        colId.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getId()));

        notificationMenuHandler = new NotificationMenuHandler(darkOverlay, notificationMenu, 266);

        // LẮNG NGHE DANH SÁCH TỪ SERVER (Sẽ tự động chạy khi Server Broadcast)
        ClientNetworkManager.getInstance().setAuctionListListener((listFromServer) -> {
            if (listFromServer != null) {
                System.out.println("✅ UI: Đã nhận dữ liệu, đang tiến hành vẽ bảng tài sản...");
                ObservableList<Auction> data = FXCollections.observableArrayList(listFromServer);
                Platform.runLater(() -> tableAsserts.setItems(data));
            }
        });

        // GỬI YÊU CẦU LẤY DANH SÁCH (Chạy lần đầu khi mở màn hình)
        ClientNetworkManager.getInstance().sendData(Protocol.REQ_GET_AUCTIONS);
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
        // ĐÃ SỬA: Thay vì chuyển màn hình, chỉ gửi yêu cầu lấy danh sách mới nếu người dùng bấm thủ công
        System.out.println("🔄 Đang làm mới danh sách tài sản...");
        ClientNetworkManager.getInstance().sendData(Protocol.REQ_GET_AUCTIONS);
    }
}