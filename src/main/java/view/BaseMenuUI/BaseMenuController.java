package view.BaseMenuUI;

import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
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

public class BaseMenuController implements Initializable {
    @FXML private Pane darkOverlay;
    @FXML private ScrollPane notificationMenu;
    private NotificationMenuHandler notificationMenuHandler;

    @FXML private HBox menuBar;

    @FXML private TableView<Auction> tableAuctions;
    @FXML private TableColumn<Auction, String> colId;
    @FXML private TableColumn<Auction, String> colName;
    @FXML private TableColumn<Auction, Double> colPrice;
    @FXML private TableColumn<Auction, String> colStatus;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Cấu hình hiển thị dữ liệu cho các cột trong bảng
        colId.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getId()));
        colName.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getItem().getName()));
        colPrice.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getCurrentPrice()).asObject());
        colStatus.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatus().name()));

        // --- TÍNH NĂNG THÊM: NHẤP ĐÚP CHUỘT VÀO DÒNG ĐỂ MỞ MENU LỰA CHỌN ---
        tableAuctions.setRowFactory(tv -> {
            TableRow<Auction> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    WindowManager.openBidOrSellChoiceWindow();
                }
            });
            return row;
        });

        // Lắng nghe dữ liệu danh sách đấu giá từ Server
        ClientNetworkManager.getInstance().setAuctionListListener((listFromServer) -> {
            if (listFromServer != null) {
                ObservableList<Auction> observableList = FXCollections.observableArrayList(listFromServer);
                Platform.runLater(() -> {
                    tableAuctions.setItems(observableList);
                });
            }
        });

        // Gửi yêu cầu lấy danh sách đấu giá mới nhất khi vừa vào trang
        ClientNetworkManager.getInstance().sendData(Protocol.REQ_GET_AUCTIONS);

        notificationMenuHandler = new NotificationMenuHandler(darkOverlay, notificationMenu, 266);
    }

    // --- CÁC HÀM XỬ LÝ SỰ KIỆN CLICK TỪ GIAO DIỆN (FXML) ---

    // HÀM MỚI BỔ SUNG ĐỂ SỬA LỖI #joinBidding
    @FXML
    private void joinBidding(ActionEvent event) {
        // Mở menu lựa chọn Đấu giá hay Bán khi người dùng bấm nút
        WindowManager.openBidOrSellChoiceWindow();
    }

    @FXML
    private void openChoiceMenu(ActionEvent event) {
        WindowManager.openBidOrSellChoiceWindow();
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

    @FXML
    private void notificationClicked(ActionEvent event){
        notificationMenuHandler.toggleMenu();
    }

    @FXML
    private void openProfile(ActionEvent event) {
        WindowManager.openUserProfileWindow();
    }
}