package GUI;

import javafx.animation.Animation;
import javafx.animation.ScaleTransition;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class AuctionMenuGUI extends Application {

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");

        // --- Tạo thanh Navbar ---
        HBox navbar = new HBox();
        navbar.getStyleClass().add("auction-navbar");

        // 1. Logo
        Label logo = new Label("BúaVàng.vn");
        logo.getStyleClass().add("logo");

        // 2. Nhóm Menu
        HBox navLinks = new HBox(25);
        navLinks.setAlignment(Pos.CENTER_LEFT);

        Button homeBtn = createNavButton("Trang chủ");

        // Menu "Đang diễn ra" kèm Badge LIVE
        HBox liveContainer = new HBox(5);
        liveContainer.setAlignment(Pos.CENTER);
        Button liveBtn = createNavButton("Đang diễn ra");
        Label liveBadge = new Label("LIVE");
        liveBadge.getStyleClass().add("live-badge");
        applyPulseAnimation(liveBadge); 
        liveContainer.getChildren().addAll(liveBtn, liveBadge);

        // Menu thả xuống
        MenuButton categoryMenu = new MenuButton("Danh mục tài sản");
        categoryMenu.getStyleClass().add("nav-dropdown");
        MenuItem artItem = new MenuItem("Nghệ thuật & Tranh ảnh");
        MenuItem watchItem = new MenuItem("Đồng hồ & Trang sức");
        MenuItem antiqueItem = new MenuItem("Đồ cổ & Sưu tầm");
        MenuItem realEstateItem = new MenuItem("Bất động sản");
        categoryMenu.getItems().addAll(artItem, watchItem, antiqueItem, realEstateItem);

        Button historyBtn = createNavButton("Lịch sử đấu giá");
        Button guideBtn = createNavButton("Hướng dẫn");

        navLinks.getChildren().addAll(homeBtn, liveContainer, categoryMenu, historyBtn, guideBtn);

        // Khoảng trống đẩy sang phải
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 3. Tìm kiếm & Đăng nhập
        HBox actions = new HBox(15);
        actions.setAlignment(Pos.CENTER_RIGHT);

        TextField searchBar = new TextField();
        searchBar.setPromptText("Tìm kiếm lô đấu giá...");
        searchBar.getStyleClass().add("search-bar");

        Button loginBtn = new Button("Đăng nhập");
        loginBtn.getStyleClass().add("btn-login");

        actions.getChildren().addAll(searchBar, loginBtn);

        // Lắp ráp Layout
        navbar.getChildren().addAll(logo, navLinks, spacer, actions);
        root.setTop(navbar);

        Scene scene = new Scene(root, 1100, 600);
        
        // --- NẠP CSS TRỰC TIẾP TỪ MÃ NGUỒN ---
        loadInlineCSS(scene);

        primaryStage.setTitle("Hệ Thống Đấu Giá - Single File");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Hàm tạo CSS tự động để gom chung vào 1 file
    private void loadInlineCSS(Scene scene) {
        String cssContent = 
            ".root { -fx-background-color: #f4f6f9; -fx-font-family: 'Segoe UI', Tahoma, sans-serif; }\n" +
            ".auction-navbar { -fx-background-color: #1a1a2e; -fx-padding: 15 40 15 40; -fx-alignment: center-left; -fx-spacing: 30; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 8, 0, 0, 4); }\n" +
            ".logo { -fx-text-fill: #e9c46a; -fx-font-size: 24px; -fx-font-weight: bold; }\n" +
            ".nav-link { -fx-background-color: transparent; -fx-text-fill: #ffffff; -fx-font-size: 15px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 5 0 5 0; }\n" +
            ".nav-link:hover { -fx-text-fill: #e9c46a; }\n" +
            ".live-badge { -fx-background-color: #e63946; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 2 6 2 6; -fx-background-radius: 4; }\n" +
            ".nav-dropdown { -fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 5 0 5 0; }\n" +
            ".nav-dropdown > .label { -fx-text-fill: #ffffff; -fx-font-size: 15px; -fx-font-weight: bold; }\n" +
            ".nav-dropdown:hover > .label { -fx-text-fill: #e9c46a; }\n" +
            ".nav-dropdown .arrow { -fx-background-color: #ffffff; }\n" +
            ".nav-dropdown:hover .arrow { -fx-background-color: #e9c46a; }\n" +
            ".nav-dropdown .context-menu { -fx-background-color: #ffffff; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 8, 0, 0, 4); }\n" +
            ".nav-dropdown .menu-item { -fx-padding: 8 15 8 15; }\n" +
            ".nav-dropdown .menu-item:hover { -fx-background-color: #f8f9fa; }\n" +
            ".nav-dropdown .menu-item > .label { -fx-text-fill: #333333; }\n" +
            ".search-bar { -fx-background-radius: 20; -fx-padding: 8 15 8 15; -fx-pref-width: 220; -fx-font-size: 14px; -fx-background-color: #ffffff; -fx-border-width: 0; }\n" +
            ".search-bar:focused { -fx-border-color: #e9c46a; -fx-border-radius: 20; }\n" +
            ".btn-login { -fx-background-color: transparent; -fx-text-fill: #e9c46a; -fx-border-color: #e9c46a; -fx-border-radius: 20; -fx-background-radius: 20; -fx-border-width: 2; -fx-padding: 6 20 6 20; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand; }\n" +
            ".btn-login:hover { -fx-background-color: #e9c46a; -fx-text-fill: #1a1a2e; }";

        try {
            // Tạo file tạm thời
            File tempCssFile = File.createTempFile("javafx_auction_style", ".css");
            tempCssFile.deleteOnExit(); // Tự động xóa khi tắt chương trình
            
            // Ghi chuỗi CSS vào file tạm
            Files.write(tempCssFile.toPath(), cssContent.getBytes(StandardCharsets.UTF_8));
            
            // Áp dụng file CSS vào giao diện
            scene.getStylesheets().add(tempCssFile.toURI().toString());
        } catch (IOException e) {
            System.err.println("Không thể tạo file CSS: " + e.getMessage());
        }
    }

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
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.play();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
