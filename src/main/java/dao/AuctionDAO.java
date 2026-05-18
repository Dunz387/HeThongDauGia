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
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuctionDAO {
    private static final Logger LOGGER = Logger.getLogger(AuctionDAO.class.getName());

    public static boolean saveAuction(Auction auction) {
        String sql = "INSERT INTO auctions(id, item_name, item_description, item_type, starting_price, current_price, bid_increment, end_time, status, seller_id) VALUES(?,?,?,?,?,?,?,?,?,?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, auction.getId());
            pstmt.setString(2, auction.getItem().getName());
            pstmt.setString(3, auction.getItem().getDescription());

            String type = ItemFactory.getItemTypeString(auction.getItem());
            pstmt.setString(4, type);
            pstmt.setDouble(5, auction.getStartingPrice());
            pstmt.setDouble(6, auction.getCurrentPrice());
            pstmt.setDouble(7, auction.getBidIncrement());
            pstmt.setString(8, auction.getEndTime().toString());
            pstmt.setString(9, auction.getStatus().toString());

            // Lưu seller_id từ Item owner
            String sellerId = (auction.getItem().getOwner() != null) ? auction.getItem().getOwner().getId() : null;
            pstmt.setString(10, sellerId);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Lỗi lưu auction", e);
            return false;
        }
    }

    public static boolean updateAuction(Auction auction) {
        String sql = "UPDATE auctions SET current_price = ?, starting_price = ?, status = ?, highest_bidder_id = ?, item_name = ?, item_description = ?, item_type = ?, end_time = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, auction.getCurrentPrice());
            pstmt.setDouble(2, auction.getStartingPrice());
            pstmt.setString(3, auction.getStatus().toString());
            pstmt.setString(4, (auction.getHighestBidder() != null) ? auction.getHighestBidder().getId() : null);
            pstmt.setString(5, auction.getItem().getName());
            pstmt.setString(6, auction.getItem().getDescription());
            
            String type = ItemFactory.getItemTypeString(auction.getItem());
            pstmt.setString(7, type);
            
            pstmt.setString(8, auction.getEndTime().toString());
            pstmt.setString(9, auction.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Lỗi cập nhật auction", e);
            return false;
        }
    }

    public static boolean saveBidTransaction(model.auction.BidTransaction tx) {
        String sql = "INSERT INTO bid_transactions(id, auction_id, bidder_id, bid_amount, timestamp) VALUES(?,?,?,?,?)";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tx.getId());
            pstmt.setString(2, tx.getAuction().getId());
            pstmt.setString(3, tx.getBidder().getId());
            pstmt.setDouble(4, tx.getBidAmount());
            pstmt.setString(5, tx.getTimestamp().toString());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Lỗi lưu bid transaction", e);
            return false;
        }
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
                String itemDesc = rs.getString("item_description");
                if (itemDesc == null) itemDesc = "Mô tả sản phẩm";
                String itemType = rs.getString("item_type");
                if (itemType == null) itemType = "ELECTRONICS";

                double startingPrice = rs.getDouble("starting_price");
                double currentPrice = rs.getDouble("current_price");
                double bidIncrement = rs.getDouble("bid_increment");
                if (bidIncrement <= 0) bidIncrement = 10.0; // Giá trị mặc định
                String endTimeStr = rs.getString("end_time");
                String statusStr = rs.getString("status");
                String sellerId = rs.getString("seller_id");
                String bidderId = rs.getString("highest_bidder_id");

                // Tìm Owner thực tế từ danh sách users đã load (có thể là Seller hoặc Bidder)
                User owner = null;
                if (sellerId != null) {
                    for (User u : loadedUsers) {
                        if (u.getId().equals(sellerId)) {
                            owner = u;
                            break;
                        }
                    }
                }
                // Nếu không tìm thấy Owner, dùng placeholder
                if (owner == null) {
                    owner = new Seller("U-TEMP", "System", "123", 0.0);
                }

                model.item.Item tempItem = ItemFactory.createItem(
                        itemType, "ITEM-" + id, itemName, itemDesc, owner, "Unknown", 0);

                Auction auction = new Auction(id, tempItem, startingPrice, bidIncrement, LocalDateTime.parse(endTimeStr));
                auction.setStatus(AuctionStatus.valueOf(statusStr));

                // Cập nhật giá hiện tại thông qua setter (thay vì reflection)
                auction.setCurrentPrice(currentPrice);

                // Tìm và gán highest bidder
                if (bidderId != null) {
                    for (User u : loadedUsers) {
                        if (u.getId().equals(bidderId) && u instanceof Bidder) {
                            auction.setHighestBidder((Bidder) u);
                            break;
                        }
                    }
                }

                // T10: Load lịch sử đặt giá cho auction này
                loadBidHistoryForAuction(auction, loadedUsers);

                list.add(auction);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Lỗi load auctions từ DB", e);
        }
        return list;
    }

    private static void loadBidHistoryForAuction(Auction auction, List<User> loadedUsers) {
        String sql = "SELECT * FROM bid_transactions WHERE auction_id = ? ORDER BY timestamp ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, auction.getId());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String id = rs.getString("id");
                String bidderId = rs.getString("bidder_id");
                double amount = rs.getDouble("bid_amount");
                String timeStr = rs.getString("timestamp");

                Bidder bidder = null;
                for (User u : loadedUsers) {
                    if (u.getId().equals(bidderId) && u instanceof Bidder) {
                        bidder = (Bidder) u;
                        break;
                    }
                }
                if (bidder != null) {
                    model.auction.BidTransaction tx = new model.auction.BidTransaction(id, auction, bidder, amount, LocalDateTime.parse(timeStr));
                    auction.getBidHistory().add(tx);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Lỗi load bid history cho " + auction.getId(), e);
        }
    }
    public static boolean updateItemOwner(String auctionId, String newOwnerId) {
        String sql = "UPDATE auctions SET seller_id = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newOwnerId);
            pstmt.setString(2, auctionId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Lỗi cập nhật chủ sở hữu item", e);
            return false;
        }
    }
}
