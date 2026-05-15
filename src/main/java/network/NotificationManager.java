package network;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.application.Platform;

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

    private final ObservableList<NotificationItem> notifications = FXCollections.observableArrayList();

    private NotificationManager() {}

    public static NotificationManager getInstance() {
        return instance;
    }

    public ObservableList<NotificationItem> getNotifications() {
        return notifications;
    }

    public void addNotification(String message) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String userId = SessionManager.getInstance().getUserId();
        
        // 1. Lưu xuống DB để persistent (nếu đã login)
        if (userId != null) {
            dao.NotificationDAO.saveNotification(userId, message, "GLOBAL");
        } else {
            // Thông báo hệ thống chung
            dao.NotificationDAO.saveNotification(null, message, "SYSTEM");
        }

        // 2. Cập nhật UI ngay lập tức
        Platform.runLater(() -> {
            notifications.add(0, new NotificationItem(message, time)); // Thêm vào đầu danh sách
            if (notifications.size() > 50) {
                notifications.remove(notifications.size() - 1);
            }
        });
    }

    public void loadFromDatabase(String userId) {
        List<NotificationItem> dbNotifs = dao.NotificationDAO.loadNotifications(userId);
        Platform.runLater(() -> {
            notifications.clear();
            notifications.addAll(dbNotifs);
        });
    }
}
