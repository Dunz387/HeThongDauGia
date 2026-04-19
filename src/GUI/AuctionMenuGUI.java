package GUI;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javafx.animation.ScaleTransition;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

public class AuctionMenuGUI extends Application {

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");

        // Gắn thanh điều hướng vào trên cùng
        HBox navbar = createNavbar(primaryStage);
        root.setTop(navbar);

        Scene scene = new Scene(root, 1100, 600);
        loadInlineCSS(scene);

        primaryStage.setTitle("Hệ Thống Đấu Giá - BúaVàng.vn");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // --- CÁC HÀM TẠO GIAO DIỆN ---

    private HBox createNavbar(Stage primaryStage) {
        HBox navbar = new HBox();
        navbar.getStyleClass().add("auction-navbar");

        Label logo = new Label("BúaVàng.vn");
        logo.getStyleClass().add("logo");

        HBox navLinks = createNavLinks();

        // Tạo khoảng trống đẩy các nút chức năng về bên phải
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox actions = createActionButtons(primaryStage);

        // Thêm tất cả vào navbar (Không cần new Node[]{})
        navbar.getChildren().addAll(logo, navLinks, spacer, actions);
        return navbar;
    }

    private HBox createNavLinks() {
        HBox navLinks = new HBox(25); // Khoảng cách giữa các mục là 25
        navLinks.setAlignment(Pos.CENTER_LEFT);

        Button homeBtn = createNavButton("Trang chủ");

        // --- Cụm nút "Đang diễn ra" kèm Badge LIVE ---
        HBox liveContainer = new HBox(5);
        liveContainer.setAlignment(Pos.CENTER);
        Button liveBtn = createNavButton("Đang diễn ra");
        Label liveBadge = new Label("LIVE");
        liveBadge.getStyleClass().add("live-badge");
        applyPulseAnimation(liveBadge);
        liveContainer.getChildren().addAll(liveBtn, liveBadge);

        // --- Dropdown Danh mục ---
        MenuButton categoryMenu = new MenuButton("Danh mục tài sản");
        categoryMenu.getStyleClass().add("nav-dropdown");
        categoryMenu.getItems().addAll(
            new MenuItem("Nghệ thuật & Tranh ảnh"),
            new MenuItem("Đồng hồ & Trang sức"),
            new MenuItem("Đồ cổ & Sưu tầm"),
            new MenuItem("Bất động sản")
        );

        Button historyBtn = createNavButton("Lịch sử đấu giá");
        Button guideBtn = createNavButton("Hướng dẫn");

        navLinks.getChildren().addAll(homeBtn, liveContainer, categoryMenu, historyBtn, guideBtn);
        return navLinks;
    }

    private HBox createActionButtons(Stage primaryStage) {
        HBox actions = new HBox(15);
        actions.setAlignment(Pos.CENTER_RIGHT);

        TextField searchBar = new TextField();
        searchBar.setPromptText("Tìm kiếm lô đấu giá...");
        searchBar.getStyleClass().add("search-bar");

        Button loginBtn = new Button("Đăng nhập");
        loginBtn.getStyleClass().add("btn-login");
        loginBtn.setOnAction(e -> {
            // Mở comment đoạn này khi bạn đã có class LoginGUI trong project
            /*
            LoginGUI loginGUI = new LoginGUI();
            loginGUI.start(primaryStage);
            */
            System.out.println("Đang mở form Đăng nhập...");
        });

        actions.getChildren().addAll(searchBar, loginBtn);
        return actions;
    }

    // --- CÁC HÀM TIỆN ÍCH HỖ TRỢ ---

    private Button createNavButton(String text) {
        Button btn = new Button(text);
        btn.getStyleClass().add("nav-link");
        return btn;
    }

    private void applyPulseAnimation(Label node) {
        ScaleTransition pulse = new ScaleTransition(Duration.seconds(0.75), node);
        pulse.setFromX(1.0);
        pulse.setFromY(1.0);
        pulse.setToX(1.1);
        pulse.setToY(1.1);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(ScaleTransition.INDEFINITE); // Chạy vô hạn thay vì để số âm khó hiểu
        pulse.play();
    }

    private void loadInlineCSS(Scene scene) {
        String cssContent = """
            .root { -fx-background-color: #F4F6F9; -fx-font-family: 'Segoe UI', Tahoma, sans-serif; }
            .auction-navbar { -fx-background-color: #2B2D42; -fx-padding: 15 40 15 40; -fx-alignment: center-left; -fx-spacing: 30; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 8, 0, 0, 4); }
            .logo { -fx-text-fill: #F77F00; -fx-font-size: 26px; -fx-font-weight: bold; }
            
            .nav-link { -fx-background-color: transparent; -fx-text-fill: #EDF2F4; -fx-font-size: 15px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 5 0 5 0; }
            .nav-link:hover { -fx-text-fill: #F77F00; }
            
            .live-badge { -fx-background-color: #D62828; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 2 6 2 6; -fx-background-radius: 4; }
            
            .nav-dropdown { -fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 5 0 5 0; }
            .nav-dropdown > .label { -fx-text-fill: #EDF2F4; -fx-font-size: 15px; -fx-font-weight: bold; }
            .nav-dropdown:hover > .label { -fx-text-fill: #F77F00; }
            .nav-dropdown .arrow { -fx-background-color: #EDF2F4; }
            .nav-dropdown:hover .arrow { -fx-background-color: #F77F00; }
            
            .nav-dropdown .context-menu { -fx-background-color: #FFFFFF; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 8, 0, 0, 4); }
            .nav-dropdown .menu-item { -fx-padding: 8 15 8 15; }
            .nav-dropdown .menu-item:hover { -fx-background-color: #F8F9FA; }
            .nav-dropdown .menu-item > .label { -fx-text-fill: #2B2D42; }
            
            .search-bar { -fx-background-radius: 20; -fx-padding: 8 15 8 15; -fx-pref-width: 220; -fx-font-size: 14px; -fx-background-color: #FFFFFF; -fx-border-width: 0; }
            .search-bar:focused { -fx-border-color: #F77F00; -fx-border-radius: 20; }
            
            .btn-login { -fx-background-color: transparent; -fx-text-fill: #F77F00; -fx-border-color: #F77F00; -fx-border-radius: 20; -fx-background-radius: 20; -fx-border-width: 2; -fx-padding: 6 20 6 20; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand; }
            .btn-login:hover { -fx-background-color: #F77F00; -fx-text-fill: #2B2D42; }
            """;

        try {
            File tempCssFile = File.createTempFile("javafx_auction_style_new", ".css");
            tempCssFile.deleteOnExit();
            Files.write(tempCssFile.toPath(), cssContent.getBytes(StandardCharsets.UTF_8));
            scene.getStylesheets().add(tempCssFile.toURI().toString());
        } catch (IOException e) {
            System.err.println("Không thể tạo file CSS: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
