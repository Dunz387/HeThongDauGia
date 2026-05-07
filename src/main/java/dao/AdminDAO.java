package dao;

import model.user.User;
import model.user.Bidder;
import model.user.Seller;
import model.user.Admin;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AdminDAO {

    // 1. Lấy toàn bộ danh sách User để Admin quản lý (khác với loadUsers thông thường)
    public static List<User> getAllUsersForManagement() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String roleStr = rs.getString("role");
                User u = null;
                if (roleStr.equals("BIDDER")) u = new Bidder(rs.getString("id"), rs.getString("username"), "", rs.getDouble("balance"));
                else if (roleStr.equals("SELLER")) u = new Seller(rs.getString("id"), rs.getString("username"), "", rs.getDouble("balance"));
                else if (roleStr.equals("ADMIN")) u = new Admin(rs.getString("id"), rs.getString("username"), "");

                if (u != null) {
                    u.setActive(rs.getInt("isActive") == 1);
                    users.add(u);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return users;
    }

    // 2. Quyền khóa/Mở khóa tài khoản (Ban User)
    public static boolean setUserActiveStatus(String userId, boolean status) {
        String sql = "UPDATE users SET isActive = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, status ? 1 : 0);
            pstmt.setString(2, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    // 3. Quyền xóa một phiên đấu giá vi phạm
    public static boolean deleteAuctionForce(String auctionId) {
        String sql = "DELETE FROM auctions WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, auctionId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }
}