package dao;

import model.user.User;
import model.user.Bidder;
import model.user.Seller;
import model.user.Admin;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AdminDAO {

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