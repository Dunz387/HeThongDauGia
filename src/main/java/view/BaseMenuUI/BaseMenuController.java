package view.BaseMenuUI;

import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
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

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class BaseMenuController implements Initializable {
    @FXML private HBox menuBar;

    // Khai báo các thành phần giao diện của Bảng
    @FXML private TableView<Auction> tableAuctions;
    @FXML private TableColumn<Auction, String> colId;
    @FXML private TableColumn<Auction, String> colName;
    @FXML private TableColumn<Auction, Double> colPrice;
    @FXML private TableColumn<Auction, String> colStatus;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1. Cấu hình cách lấy dữ liệu cho từng cột trong Bảng
        colId.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getId()));
        colName.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getItem().getName()));
        colPrice.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getCurrentPrice()).asObject());
        colStatus.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatus().name()));

        // 2. Gửi yêu cầu lấy danh sách về Server
        ClientNetworkManager.getInstance().sendData(Protocol.REQ_GET_AUCTIONS);

        // 3. Mở luồng chạy ngầm để chờ Server trả danh sách
        new Thread(() -> {
            try {
                List<Auction> listFromServer = null;
                int timeout = 50; // Chờ tối đa 5 giây

                while (listFromServer == null && timeout > 0) {
                    listFromServer = ClientNetworkManager.getInstance().getLastAuctionList();
                    Thread.sleep(100);
                    timeout--;
                }

                // 4. Có dữ liệu thì bắt buộc dùng Platform.runLater để vẽ lên UI
                if (listFromServer != null) {
                    ObservableList<Auction> observableList = FXCollections.observableArrayList(listFromServer);

                    Platform.runLater(() -> {
                        tableAuctions.setItems(observableList);
                        System.out.println("Đã load thành công " + observableList.size() + " món hàng lên UI!");
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
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

    // THÊM MỚI: Hàm mở Popup Tạo phiên đấu giá
    @FXML
    private void openCreateItemPopup(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/SellerUI/CreateItem.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Đăng bán sản phẩm");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}