package view.controller.admin;

import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import model.auction.Auction;
import model.user.User;
import network.ClientNetworkManager;
import shared.Protocol;
import view.utility.navigation.SceneManager;
import view.utility.display.StatusDisplayHelper;

import java.net.URL;
import java.util.ResourceBundle;

public class AdminDashboardController implements Initializable {

    @FXML
    private HBox menuBar;

    // Stat Labels
    @FXML
    private Label lblTotalUsers;
    @FXML
    private Label lblTotalAuctions;
    @FXML
    private Label lblRunningAuctions;
    @FXML
    private Label lblFinishedAuctions;

    // Recent Auctions Table
    @FXML
    private TableView<Auction> tableRecentAuctions;
    @FXML
    private TableColumn<Auction, String> colAucId;
    @FXML
    private TableColumn<Auction, String> colAucName;
    @FXML
    private TableColumn<Auction, Double> colAucPrice;
    @FXML
    private TableColumn<Auction, String> colAucStatus;

    // Recent Users Table
    @FXML
    private TableView<User> tableRecentUsers;
    @FXML
    private TableColumn<User, String> colUserId;
    @FXML
    private TableColumn<User, String> colUserName;
    @FXML
    private TableColumn<User, String> colUserRole;
    @FXML
    private TableColumn<User, String> colUserStatus;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configureAuctionColumns(); // cấu hình các cột dữ liệu cho bảng phiên đấu giá
        configureUserColumns(); // cấu hình các cột dữ liệu cho bảng người dùng
        registerAuctionListListener(); // đăng ký lắng nghe danh sách phiên đấu giá để cập nhật thống kê và bảng recent auctions
        registerUserListListener(); // đăng ký lắng nghe danh sách người dùng để cập nhật thống kê và bảng recent users
        loadDashboardData();// tải dữ liệu thống kê và recent items từ server khi khởi tạo giao diện
    }

    private void configureAuctionColumns() {
        // === CẤU HÌNH CỘT BẢNG ĐẤU GIÁ ===
        // Cột STT: hiển thị số thứ tự dựa trên vị trí của item trong TableView
        colAucId.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getId()));
        // Cột Tên Sản Phẩm: hiển thị tên sản phẩm
        colAucName.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getItem().getName()));
        // Cột Giá Hiện Tại: hiển thị giá hiện tại của phiên đấu giá
        colAucPrice.setCellValueFactory(
                cellData -> new SimpleDoubleProperty(cellData.getValue().getCurrentPrice()).asObject());
        // Cột Trạng Thái: hiển thị trạng thái của phiên đấu giá
        colAucStatus.setCellValueFactory(cellData -> new SimpleStringProperty(
                StatusDisplayHelper.formatAuctionStatus(cellData.getValue().getStatus().name())));
    }

    private void configureUserColumns() {
        // Cột STT: hiển thị số thứ tự dựa trên vị trí của item trong TableView
        colUserId.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getId()));
        // Cột Tên Người Dùng: hiển thị tên người dùng
        colUserName.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getUsername()));
        // Cột Vai Trò: hiển thị vai trò của người dùng
        colUserRole.setCellValueFactory(cellData -> new SimpleStringProperty(
                StatusDisplayHelper.formatUserRole(cellData.getValue().getRole().name())));
        // Cột Trạng Thái: hiển thị trạng thái của người dùng
        colUserStatus.setCellValueFactory(cellData -> new SimpleStringProperty(
                StatusDisplayHelper.formatUserStatus(cellData.getValue().isActive())));
    }

    private void registerAuctionListListener() {
        // Đăng ký lắng nghe danh sách phiên đấu giá từ server để cập nhật thống kê và bảng recent auctions
        ClientNetworkManager.getInstance().clearAuctionListListeners(); // Xóa các listener cũ để tránh trùng lặp khi quay lại dashboard
        // Lắng nghe danh sách phiên đấu giá mới từ server
        ClientNetworkManager.getInstance().addAuctionListListener((listFromServer) -> {
            if (listFromServer != null) {
                Platform.runLater(() -> {
                    // Cập nhật thống kê
                    lblTotalAuctions.setText(String.valueOf(listFromServer.size()));
                    long running = listFromServer.stream().filter(a -> "RUNNING".equals(a.getStatus().name())).count();
                    long finished = listFromServer.stream().filter(a -> "FINISHED".equals(a.getStatus().name())).count();
                    lblRunningAuctions.setText(String.valueOf(running));
                    lblFinishedAuctions.setText(String.valueOf(finished));

                    // Hiển thị tối đa 5 phiên gần nhất
                    int limit = Math.min(5, listFromServer.size());
                    ObservableList<Auction> recentAuctions = FXCollections.observableArrayList(
                            listFromServer.subList(Math.max(0, listFromServer.size() - limit), listFromServer.size()));
                    tableRecentAuctions.setItems(recentAuctions);
                });
            }
        });
    }

    private void registerUserListListener() {
        ClientNetworkManager.getInstance().clearUserListListeners();
        ClientNetworkManager.getInstance().addUserListListener((listFromServer) -> {
            if (listFromServer != null) {
                Platform.runLater(() -> {
                    lblTotalUsers.setText(String.valueOf(listFromServer.size()));

                    // Hiển thị tối đa 5 user gần nhất
                    int limit = Math.min(5, listFromServer.size());
                    ObservableList<User> recentUsers = FXCollections.observableArrayList(
                            listFromServer.subList(Math.max(0, listFromServer.size() - limit), listFromServer.size()));
                    tableRecentUsers.setItems(recentUsers);
                });
            }
        });
    }

    private void loadDashboardData() {
        ClientNetworkManager.getInstance().sendData(Protocol.REQ_GET_AUCTIONS);
        ClientNetworkManager.getInstance().sendData(Protocol.REQ_GET_USERS);
    }

    // === ĐIỀU HƯỚNG ===

    @FXML
    private void goToUserManagement(ActionEvent event) {
        Stage stage = (Stage) menuBar.getScene().getWindow();
        SceneManager.goToAdminUserManagement(stage);
    }

    @FXML
    private void goToAuctionManagement(ActionEvent event) {
        Stage stage = (Stage) menuBar.getScene().getWindow();
        SceneManager.goToAdminAuctionManagement(stage);
    }

    @FXML
    private void logoutClicked(ActionEvent event) {
        ClientNetworkManager.getInstance().logout();
        Stage stage = (Stage) menuBar.getScene().getWindow();
        SceneManager.goToLogin(stage);
    }
}
