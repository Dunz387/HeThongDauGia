package dao.core;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseManager {
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());

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
                item_description TEXT,
                item_type TEXT, 
                starting_price REAL,
                current_price REAL,
                bid_increment REAL DEFAULT 10.0,
                end_time TEXT,
                status TEXT,
                seller_id TEXT,
                highest_bidder_id TEXT,
                FOREIGN KEY (seller_id) REFERENCES users(id),
                FOREIGN KEY (highest_bidder_id) REFERENCES users(id)
            );
        """;

        String createBidTransactionsTable = """
            CREATE TABLE IF NOT EXISTS bid_transactions (
                id TEXT PRIMARY KEY,
                auction_id TEXT NOT NULL,
                bidder_id TEXT NOT NULL,
                bid_amount REAL NOT NULL,
                timestamp TEXT NOT NULL,
                FOREIGN KEY (auction_id) REFERENCES auctions(id),
                FOREIGN KEY (bidder_id) REFERENCES users(id)
            );
        """;

        String createNotificationsTable = """
            CREATE TABLE IF NOT EXISTS notifications (
                id TEXT PRIMARY KEY,
                user_id TEXT,
                content TEXT NOT NULL,
                timestamp TEXT NOT NULL,
                type TEXT
            );
        """;

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createUsersTable);
            stmt.execute(createAuctionsTable);
            
            // Migration: Add item_description if not exists
            try {
                stmt.execute("ALTER TABLE auctions ADD COLUMN item_description TEXT");
            } catch (SQLException ignored) {
                // Column already exists
            }

            stmt.execute(createBidTransactionsTable);
            stmt.execute(createNotificationsTable);
            LOGGER.info("✅ Database SQLite đã được khởi tạo thành công!");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Lỗi tạo bảng Database", e);
        }
    }
}
