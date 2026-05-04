package dao;

import model.auction.Auction;
import model.user.Admin;
import model.user.Bidder;
import model.user.Seller;
import model.user.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {

    // 1. Hàm tự động tạo bảng nếu chưa có (Đã bổ sung cột isActive cho Users)
    public static void initializeDatabase() {
        String createUsersTable = """
            CREATE TABLE IF NOT EXISTS users (
                id TEXT PRIMARY KEY,
                username TEXT UNIQUE NOT NULL,
                password TEXT NOT NULL,
                role TEXT NOT NULL,
                balance REAL DEFAULT 0.0,
                isActive INTEGER DEFAULT 1
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

    // 2. Lưu User mới xuống DB (Đã thêm thông số isActive)
    public static boolean saveUser(User user) {
        String sql = "INSERT INTO users(id, username, password, role, balance, isActive) VALUES(?,?,?,?,?,?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getId());
            pstmt.setString(2, user.getUsername());

            String password = "";
            try {
                java.lang.reflect.Field passField = User.class.getDeclaredField("password");
                passField.setAccessible(true);
                password = (String) passField.get(user);
            } catch (Exception e) {
                System.err.println("Cảnh báo: Không thể bẻ khóa biến password - " + e.getMessage());
            }
            pstmt.setString(3, password);

            pstmt.setString(4, user.getRole().toString());

            double balance = 0.0;
            if (user instanceof Bidder) {
                balance = ((Bidder) user).getBalance();
            } else if (user instanceof Seller) {
                balance = ((Seller) user).getBalance();
            }

            pstmt.setDouble(5, balance);
            pstmt.setInt(6, user.isActive() ? 1 : 0);

            pstmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.err.println("❌ Lỗi lưu User vào Database: " + e.getMessage());
            return false;
        }
    }

    // 3. Tải toàn bộ danh sách User từ DB lên RAM (Đã cứu lại ông Admin)
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
                boolean isActive = rs.getInt("isActive") == 1;

                User newUser = null;
                if (roleStr.equals("BIDDER")) {
                    newUser = new Bidder(id, username, password, balance);
                } else if (roleStr.equals("SELLER")) {
                    newUser = new Seller(id, username, password, balance);
                } else if (roleStr.equals("ADMIN")) {
                    newUser = new Admin(id, username, password);
                }

                if (newUser != null) {
                    newUser.setActive(isActive);
                    users.add(newUser);
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

    // 5. Cập nhật toàn bộ trạng thái User (Gồm cả tiền và khóa mồm)
    public static boolean updateUser(User user) {
        String sql = "UPDATE users SET balance = ?, isActive = ? WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            double balance = 0.0;
            if (user instanceof Bidder) balance = ((Bidder)user).getBalance();
            else if (user instanceof Seller) balance = ((Seller)user).getBalance();

            pstmt.setDouble(1, balance);
            pstmt.setInt(2, user.isActive() ? 1 : 0);
            pstmt.setString(3, user.getId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    // 6. Lưu một phiên đấu giá mới vào DB
    public static boolean saveAuction(Auction auction) {
        String sql = "INSERT INTO auctions(id, item_name, starting_price, current_price, end_time, status) VALUES(?,?,?,?,?,?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, auction.getId());
            pstmt.setString(2, auction.getItem().getName());
            pstmt.setDouble(3, auction.getCurrentPrice());
            pstmt.setDouble(4, auction.getCurrentPrice());
            pstmt.setString(5, auction.getEndTime().toString());
            pstmt.setString(6, auction.getStatus().toString());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    // 7. Cập nhật giá và người dẫn đầu khi đang đấu giá
    public static boolean updateAuction(Auction auction) {
        String sql = "UPDATE auctions SET current_price = ?, status = ?, highest_bidder_id = ? WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, auction.getCurrentPrice());
            pstmt.setString(2, auction.getStatus().toString());
            pstmt.setString(3, (auction.getHighestBidder() != null) ? auction.getHighestBidder().getId() : null);
            pstmt.setString(4, auction.getId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    // 8. Load danh sách đấu giá
    public static List<Auction> loadAuctions() {
        return new ArrayList<>();
    }
}