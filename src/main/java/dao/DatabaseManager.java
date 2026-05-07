package dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

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
                item_type TEXT, 
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
            System.out.println("✅ Database SQLite đã được khởi tạo thành công!");
        } catch (SQLException e) {
            System.err.println("❌ Lỗi tạo bảng Database: " + e.getMessage());
        }
    }
}