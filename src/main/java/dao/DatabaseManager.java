package dao;

import model.auction.Auction;
import model.auction.AuctionStatus;
import model.item.ItemFactory; // Import thêm ItemFactory mới tạo
import model.user.Admin;
import model.user.Bidder;
import model.user.Seller;
import model.user.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {

    // 1. Hàm tự động tạo bảng nếu chưa có
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

        String createItemsTable = """
    CREATE TABLE IF NOT EXISTS items (
        id TEXT PRIMARY KEY,
        name TEXT NOT NULL,
        description TEXT,
        type TEXT,
        extra1 TEXT,
        extra2 INTEGER,
        owner_id TEXT,
        FOREIGN KEY (owner_id) REFERENCES users(id)
    );
""";

        String createTransactionsTable = """
    CREATE TABLE IF NOT EXISTS bid_transactions (
        id TEXT PRIMARY KEY,
        auction_id TEXT,
        bidder_id TEXT,
        amount REAL,
        timestamp TEXT,
        FOREIGN KEY (auction_id) REFERENCES auctions(id),
        FOREIGN KEY (bidder_id) REFERENCES users(id)
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

    // 2. Lưu User mới xuống DB (Đã xóa Reflection, thay bằng getPassword chuẩn OOP)
    public static boolean saveUser(User user) {
        String sql = "INSERT INTO users(id, username, password, role, balance, isActive) VALUES(?,?,?,?,?,?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getId());
            pstmt.setString(2, user.getUsername());
            pstmt.setString(3, user.getPassword()); // Gọi trực tiếp getter
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

    // 3. Tải toàn bộ danh sách User từ DB lên RAM
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

    // 5. Cập nhật toàn bộ trạng thái User (Gồm cả tiền và khóa account)
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

    // 8. Load danh sách đấu giá (Đã vá lỗi trắng dữ liệu và dùng ItemFactory)
    public static List<Auction> loadAuctions(List<User> loadedUsers) {
        List<Auction> list = new ArrayList<>();
        String sql = "SELECT * FROM auctions";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String id = rs.getString("id");
                String itemName = rs.getString("item_name");
                double startingPrice = rs.getDouble("starting_price");
                double currentPrice = rs.getDouble("current_price");
                String endTimeStr = rs.getString("end_time");
                String statusStr = rs.getString("status");
                String bidderId = rs.getString("highest_bidder_id");

                // Tạo Seller tạm và sử dụng ItemFactory
                Seller tempSeller = new Seller("S-TEMP", "System", "123", 0.0);
                model.item.Item tempItem = ItemFactory.createItem(
                        "ELECTRONICS", "ITEM-" + id, itemName, "Khôi phục từ DB", tempSeller, "Unknown", 0);

                Auction auction = new Auction(
                        id, tempItem, startingPrice, 50.0, LocalDateTime.parse(endTimeStr));
                auction.setStatus(AuctionStatus.valueOf(statusStr));

                // Dùng Reflection để khôi phục biến currentPrice mà không gọi hàm placeBid
                java.lang.reflect.Field priceField = Auction.class.getDeclaredField("currentPrice");
                priceField.setAccessible(true);
                priceField.set(auction, currentPrice);

                // Nối lại người dẫn đầu (Highest Bidder) nếu có
                if (bidderId != null) {
                    for (User u : loadedUsers) {
                        if (u.getId().equals(bidderId) && u instanceof Bidder) {
                            java.lang.reflect.Field bidderField = Auction.class.getDeclaredField("highestBidder");
                            bidderField.setAccessible(true);
                            bidderField.set(auction, u);
                            break;
                        }
                    }
                }
                list.add(auction);
            }
        } catch (Exception e) {
            System.err.println("❌ Lỗi tải Auctions: " + e.getMessage());
        }
        return list;
    }
}