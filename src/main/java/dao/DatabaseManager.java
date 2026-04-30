package dao;

import model.auction.Auction;
import model.user.Bidder;
import model.user.Seller;
import model.user.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {

    // 1. Hàm tự động tạo bảng nếu chưa có (Chạy lúc khởi động hệ thống)
    public static void initializeDatabase() {
        String createUsersTable = """
            CREATE TABLE IF NOT EXISTS users (
                id TEXT PRIMARY KEY,
                username TEXT UNIQUE NOT NULL,
                password TEXT NOT NULL,
                role TEXT NOT NULL,
                balance REAL DEFAULT 0.0
            );
        """;

        String createAuctionsTable = """
            CREATE TABLE IF NOT EXISTS auctions (
                id TEXT PRIMARY KEY,
                item_name TEXT NOT NULL,
                starting_price REAL,
                current_price REAL,
                end_time TEXT,
                status TEXT,
                highest_bidder_id TEXT,
                FOREIGN KEY (highest_bidder_id) REFERENCES users(id)
            );
        """;

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(createUsersTable);
            stmt.execute(createAuctionsTable);
            System.out.println("✅ Database SQLite đã sẵn sàng!");

        } catch (SQLException e) {
            System.err.println("❌ Lỗi tạo bảng Database: " + e.getMessage());
        }
    }

    // 2. Lưu User mới xuống DB (Thay thế cho lưu ra file)
    public static boolean saveUser(User user) {
        String sql = "INSERT INTO users(id, username, password, role, balance) VALUES(?,?,?,?,?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // 1. Lưu các thông tin cơ bản có sẵn getter
            pstmt.setString(1, user.getId());
            pstmt.setString(2, user.getUsername());

            // 2. "Ma thuật" Java Reflection: Lấy password dù nó bị khóa private và không có getter
            String password = "";
            try {
                // Nhắm thẳng vào biến 'password' trong class cha User
                java.lang.reflect.Field passField = User.class.getDeclaredField("password");
                passField.setAccessible(true); // Tạm thời phá vỡ lớp vỏ bọc private
                password = (String) passField.get(user); // Lấy giá trị ra
            } catch (Exception e) {
                System.err.println("Cảnh báo: Không thể bẻ khóa biến password - " + e.getMessage());
            }
            pstmt.setString(3, password);

            pstmt.setString(4, user.getRole().toString());

            // 3. Phân luồng dòng tiền thông minh
            double balance = 0.0;
            if (user instanceof Bidder) {
                balance = ((Bidder) user).getBalance(); // Ép kiểu sang Bidder để lấy tiền
            } else if (user instanceof Seller) {
                balance = ((Seller) user).getBalance(); // Ép kiểu sang Seller để lấy tiền
            }
            // Nếu là Admin, code sẽ tự động lọt qua if-else này và giữ nguyên mức tiền = 0.0

            pstmt.setDouble(5, balance);

            // Thực thi lệnh SQL
            pstmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.err.println("❌ Lỗi lưu User vào Database: " + e.getMessage());
            return false;
        }
    }

    // 3. Tải toàn bộ danh sách User từ DB lên RAM (Thay thế cho đọc file)
    public static List<User> loadUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String id = rs.getString("id");
                String username = rs.getString("username");
                String password = rs.getString("password");
                String roleStr = rs.getString("role");
                double balance = rs.getDouble("balance");

                // Khôi phục lại Object chuẩn OOP dựa theo Role
                if (roleStr.equals("BIDDER")) {
                    users.add(new Bidder(id, username, password, balance));
                } else if (roleStr.equals("SELLER")) {
                    users.add(new Seller(id, username, password, balance));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi tải Users: " + e.getMessage());
        }
        return users;
    }

    // 4. Cập nhật số dư tiền tệ khi đấu giá thành công
    public static boolean updateUserBalance(String userId, double newBalance) {
        String sql = "UPDATE users SET balance = ? WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, newBalance);
            pstmt.setString(2, userId);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("❌ Lỗi cập nhật tiền: " + e.getMessage());
            return false;
        }
    }

    // (Thêm vào DatabaseManager.java)

    public static boolean updateUser(User user) {
        // Code SQL UPDATE user sẽ viết sau
        return true;
    }

    public static boolean saveAuction(Auction auction) {
        // Code SQL INSERT auction sẽ viết sau
        return true;
    }

    public static boolean updateAuction(Auction auction) {
        // Code SQL UPDATE auction sẽ viết sau
        return true;
    }

    public static List<Auction> loadAuctions() {
        // Code SQL SELECT auctions sẽ viết sau
        return new ArrayList<>();
    }
}