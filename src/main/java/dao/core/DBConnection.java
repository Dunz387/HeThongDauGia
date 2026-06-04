package dao.core;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    // Tái cấu trúc mã nguồn sử dụng API java.nio.file.Path để tương thích trên nhiều OS
    private static final Path DB_PATH = Paths.get(System.getProperty("user.dir"), "auction.db");
    private static final String URL = "jdbc:sqlite:" + DB_PATH.toString();

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }
}
