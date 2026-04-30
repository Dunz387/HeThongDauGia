package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    // Đường dẫn đến file database (nó sẽ tự động tạo ở thư mục gốc dự án)
    private static final String URL = "jdbc:sqlite:auction.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }
}