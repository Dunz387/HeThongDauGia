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
        colStatus.setCellValueFactory(cellData -> {
            String status = cellData.getValue().getStatus().name();
            String displayStatus = switch (status) {
                case "OPEN" -> "⏳ Chưa bắt đầu";
                case "RUNNING" -> "🔥 Đang diễn ra";
                case "FINISHED" -> "✅ Đã kết thúc";
                case "PAID" -> "💰 Đã thanh toán";
                case "CANCELED" -> "❌ Đã hủy";
                default -> status;
            };
            return new SimpleStringProperty(displayStatus);
        });

        // --- TÍNH NĂNG THÊM: NHẤP ĐÚP CHUỘT VÀO DÒNG ĐỂ MỞ MENU LỰA CHỌN ---
        tableAuctions.setRowFactory(tv -> {
            TableRow<Auction> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    // Lấy Stage từ tableAuctions
                    Stage currentStage = (Stage) tableAuctions.getScene().getWindow();
                    WindowManager.openBidOrSellChoiceWindow(currentStage);
                }
            });
            return row;
        });

        // Lắng nghe dữ liệu danh sách đấu giá từ Server (REAL-TIME)
        ClientNetworkManager.getInstance().setAuctionListListener((listFromServer) -> {
            if (listFromServer != null) {
                ObservableList<Auction> observableList = FXCollections.observableArrayList(listFromServer);
                Platform.runLater(() -> {
                    tableAuctions.setItems(observableList);
                });
            }
        });

        // Lắng nghe lệnh BROADCAST_NEW_BID để cập nhật bảng real-time
        ClientNetworkManager.getInstance().registerListener(Protocol.BROADCAST_NEW_BID, (message) -> {
            // Khi có giá mới, tự động yêu cầu lấy danh sách đấu giá mới nhất
            ClientNetworkManager.getInstance().sendData(Protocol.REQ_GET_AUCTIONS);
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
        Stage currentStage = (Stage) menuBar.getScene().getWindow();
        WindowManager.openBidOrSellChoiceWindow(currentStage);
    }

    @FXML
    private void openChoiceMenu(ActionEvent event) {
        Stage currentStage = (Stage) menuBar.getScene().getWindow();
        WindowManager.openBidOrSellChoiceWindow(currentStage);
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