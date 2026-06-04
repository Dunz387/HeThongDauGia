package network.notification;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.application.Platform;
import network.session.SessionManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Singleton quản lý danh sách thông báo trên Client.
 */
public class NotificationManager {
    private static final NotificationManager instance = new NotificationManager();

    public static class NotificationItem {
        private String content;
        private String time;

        public NotificationItem(String content, String time) {
            this.content = content;
            this.time = time;
        }

        public String getContent() { return content; }
        public String getTime() { return time; }
    }

    // Danh sách thông báo hiển thị trên UI
    private final ObservableList<NotificationItem> notifications = FXCollections.observableArrayList();

    // Private constructor để đảm bảo singleton
    private NotificationManager() {}

    // Lấy instance của NotificationManager
    public static NotificationManager getInstance() {
        return instance;
    }

    // Lấy danh sách thông báo để bind với UI
    public ObservableList<NotificationItem> getNotifications() {
        return notifications;
    }

    // Thêm một thông báo mới
    public void addNotification(String message) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String userId = SessionManager.getInstance().getUserId();
        
        // 1. Lưu xuống DB để persistent (nếu đã login)
        if (userId != null) {
            dao.notification.NotificationDAO.saveNotification(userId, message, "GLOBAL");
        } else {
            // Thông báo hệ thống chung
            dao.notification.NotificationDAO.saveNotification(null, message, "SYSTEM");
        }

        // 2. Cập nhật UI ngay lập tức
        Platform.runLater(() -> {
            notifications.add(0, new NotificationItem(message, time)); // Thêm vào đầu danh sách
            if (notifications.size() > 50) {
                notifications.remove(notifications.size() - 1);
            }
        });
    }

    // Tải thông báo từ database khi người dùng đăng nhập
    public void loadFromDatabase(String userId) {
        List<NotificationItem> dbNotifs = dao.notification.NotificationDAO.loadNotifications(userId);
        Platform.runLater(() -> {
            notifications.clear();
            notifications.addAll(dbNotifs);
        });
    }
}
