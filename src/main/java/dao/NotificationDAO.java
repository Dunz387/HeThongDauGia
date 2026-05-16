package dao;

import network.NotificationManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NotificationDAO {
    private static final Logger LOGGER = Logger.getLogger(NotificationDAO.class.getName());
    public static void saveNotification(String userId, String content, String type) {
        String sql = "INSERT INTO notifications(id, user_id, content, timestamp, type) VALUES(?,?,?,?,?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "NOTIF-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000));
            pstmt.setString(2, userId);
            pstmt.setString(3, content);
            pstmt.setString(4, java.time.LocalDateTime.now().toString());
            pstmt.setString(5, type);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Lỗi lưu thông báo", e);
        }
    }

    public static List<NotificationManager.NotificationItem> loadNotifications(String userId) {
        List<NotificationManager.NotificationItem> list = new ArrayList<>();
        // Load cả thông báo chung (user_id IS NULL) và thông báo riêng (user_id = ?)
        String sql = "SELECT * FROM notifications WHERE user_id IS NULL OR user_id = ? ORDER BY timestamp DESC LIMIT 50";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String content = rs.getString("content");
                String timestamp = rs.getString("timestamp");
                // Chuyển đổi timestamp sang HH:mm:ss để hiển thị
                java.time.LocalDateTime dt = java.time.LocalDateTime.parse(timestamp);
                String timeStr = dt.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
                list.add(new NotificationManager.NotificationItem(content, timeStr));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Lỗi load thông báo", e);
        }
        return list;
    }
}
