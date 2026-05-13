package dao;

import model.user.Admin;
import model.user.Bidder;
import model.user.Seller;
import model.user.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    public static boolean saveUser(User user) {
        String sql = "INSERT INTO users(id, username, password, role, balance, isActive) VALUES(?,?,?,?,?,?)";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getId());
            pstmt.setString(2, user.getUsername());
            pstmt.setString(3, user.getPassword());
            pstmt.setString(4, user.getRole().toString());
            double balance = 0.0;
            if (user instanceof Bidder) balance = ((Bidder) user).getBalance();
            else if (user instanceof Seller) balance = ((Seller) user).getBalance();
            pstmt.setDouble(5, balance);
            pstmt.setInt(6, user.isActive() ? 1 : 0);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("❌ Lỗi lưu user: " + e.getMessage());
            return false;
        }
    }

    public static List<User> loadUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";
        try (Connection conn = DBConnection.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String id = rs.getString("id");
                String username = rs.getString("username");
                String password = rs.getString("password");
                String roleStr = rs.getString("role");
                double balance = rs.getDouble("balance");
                boolean isActive = rs.getInt("isActive") == 1;

                User newUser = null;
                if (roleStr.equals("BIDDER")) newUser = new Bidder(id, username, password, balance);
                else if (roleStr.equals("SELLER")) newUser = new Seller(id, username, password, balance);
                else if (roleStr.equals("ADMIN")) newUser = new Admin(id, username, password);

                if (newUser != null) {
                    newUser.setActive(isActive);
                    users.add(newUser);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi load users từ DB: " + e.getMessage());
        }
        return users;
    }

    public static boolean updateUserBalance(String userId, double newBalance) {
        String sql = "UPDATE users SET balance = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, newBalance);
            pstmt.setString(2, userId);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) { 
            System.err.println("❌ Lỗi cập nhật số dư user: " + e.getMessage());
            return false; 
        }
    }

    public static boolean updateUser(User user) {
        String sql = "UPDATE users SET balance = ?, isActive = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            double balance = 0.0;
            if (user instanceof Bidder) balance = ((Bidder)user).getBalance();
            else if (user instanceof Seller) balance = ((Seller)user).getBalance();
            pstmt.setDouble(1, balance);
            pstmt.setInt(2, user.isActive() ? 1 : 0);
            pstmt.setString(3, user.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { 
            System.err.println("❌ Lỗi cập nhật user: " + e.getMessage());
            return false; 
        }
    }
}