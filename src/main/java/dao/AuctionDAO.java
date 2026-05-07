package dao;

import model.auction.Auction;
import model.auction.AuctionStatus;
import model.item.ItemFactory;
import model.user.Bidder;
import model.user.Seller;
import model.user.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuctionDAO {

    public static boolean saveAuction(Auction auction) {
        String sql = "INSERT INTO auctions(id, item_name, item_type, starting_price, current_price, end_time, status) VALUES(?,?,?,?,?,?,?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, auction.getId());
            pstmt.setString(2, auction.getItem().getName());

            String type = "ELECTRONICS";
            if (auction.getItem() instanceof model.item.Arts) type = "ART";
            else if (auction.getItem() instanceof model.item.Vehicle) type = "VEHICLE";

            pstmt.setString(3, type);
            pstmt.setDouble(4, auction.getCurrentPrice());
            pstmt.setDouble(5, auction.getCurrentPrice());
            pstmt.setString(6, auction.getEndTime().toString());
            pstmt.setString(7, auction.getStatus().toString());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public static boolean updateAuction(Auction auction) {
        String sql = "UPDATE auctions SET current_price = ?, status = ?, highest_bidder_id = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, auction.getCurrentPrice());
            pstmt.setString(2, auction.getStatus().toString());
            pstmt.setString(3, (auction.getHighestBidder() != null) ? auction.getHighestBidder().getId() : null);
            pstmt.setString(4, auction.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public static List<Auction> loadAuctions(List<User> loadedUsers) {
        List<Auction> list = new ArrayList<>();
        String sql = "SELECT * FROM auctions";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String id = rs.getString("id");
                String itemName = rs.getString("item_name");
                String itemType = rs.getString("item_type");
                if (itemType == null) itemType = "ELECTRONICS";

                double startingPrice = rs.getDouble("starting_price");
                double currentPrice = rs.getDouble("current_price");
                String endTimeStr = rs.getString("end_time");
                String statusStr = rs.getString("status");
                String bidderId = rs.getString("highest_bidder_id");

                Seller tempSeller = new Seller("S-TEMP", "System", "123", 0.0);
                model.item.Item tempItem = ItemFactory.createItem(
                        itemType, "ITEM-" + id, itemName, "Khôi phục từ DB", tempSeller, "Unknown", 0);

                Auction auction = new Auction(id, tempItem, startingPrice, 50.0, LocalDateTime.parse(endTimeStr));
                auction.setStatus(AuctionStatus.valueOf(statusStr));

                java.lang.reflect.Field priceField = Auction.class.getDeclaredField("currentPrice");
                priceField.setAccessible(true);
                priceField.set(auction, currentPrice);

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
        } catch (Exception e) {}
        return list;
    }
}