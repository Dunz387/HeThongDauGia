package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static final String URL = "jdbc:sqlite:auction.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }
}