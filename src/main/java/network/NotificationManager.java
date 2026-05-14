package network;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.application.Platform;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
        Platform.runLater(() -> {
            notifications.add(0, new NotificationItem(message, time)); // Thêm vào đầu danh sách
            if (notifications.size() > 50) {
                notifications.remove(notifications.size() - 1);
            }
        });
    }
}
