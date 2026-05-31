package dao;

import model.auction.Auction;
import model.auction.AuctionStatus;
import model.user.Bidder;
import model.user.Seller;
import model.user.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuctionDAO extends BaseDAO {

    public static boolean saveAuction(Auction auction) {
        String sql = "INSERT INTO auctions(id, item_name, item_description, item_type, starting_price, current_price, bid_increment, end_time, status, seller_id) VALUES(?,?,?,?,?,?,?,?,?,?)";

        return executeUpdate(sql, pstmt -> {
            pstmt.setString(1, auction.getId());
            pstmt.setString(2, auction.getItem().getName());
            pstmt.setString(3, auction.getItem().getDescription());

            String type = auction.getItem().getTypeString();
            pstmt.setString(4, type);
            pstmt.setDouble(5, auction.getStartingPrice());
            pstmt.setDouble(6, auction.getCurrentPrice());
            pstmt.setDouble(7, auction.getBidIncrement());
            pstmt.setString(8, auction.getEndTime().toString());
            pstmt.setString(9, auction.getStatus().toString());

            String sellerId = (auction.getSeller() != null) ? auction.getSeller().getId() : null;
            pstmt.setString(10, sellerId);
        }, "❌ Lỗi lưu auction");
    }

    public static boolean updateAuction(Auction auction) {
        String sql = "UPDATE auctions SET current_price = ?, starting_price = ?, status = ?, highest_bidder_id = ?, item_name = ?, item_description = ?, item_type = ?, end_time = ? WHERE id = ?";
        return executeUpdate(sql, pstmt -> {
            pstmt.setDouble(1, auction.getCurrentPrice());
            pstmt.setDouble(2, auction.getStartingPrice());
            pstmt.setString(3, auction.getStatus().toString());
            pstmt.setString(4, (auction.getHighestBidder() != null) ? auction.getHighestBidder().getId() : null);
            pstmt.setString(5, auction.getItem().getName());
            pstmt.setString(6, auction.getItem().getDescription());
            
            String type = auction.getItem().getTypeString();
            pstmt.setString(7, type);
            
            pstmt.setString(8, auction.getEndTime().toString());
            pstmt.setString(9, auction.getId());
        }, "❌ Lỗi cập nhật auction");
    }

    public static boolean saveBidTransaction(model.auction.BidTransaction tx) {
        String sql = "INSERT INTO bid_transactions(id, auction_id, bidder_id, bid_amount, timestamp) VALUES(?,?,?,?,?)";
        return executeUpdate(sql, pstmt -> {
            pstmt.setString(1, tx.getId());
            pstmt.setString(2, tx.getAuction().getId());
            pstmt.setString(3, tx.getBidder().getId());
            pstmt.setDouble(4, tx.getBidAmount());
            pstmt.setString(5, tx.getTimestamp().toString());
        }, "❌ Lỗi lưu bid transaction");
    }

    public static List<Auction> loadAuctions(List<User> loadedUsers) {
        String sql = "SELECT * FROM auctions";

        List<Auction> result = executeQuery(sql, rs -> {
            List<Auction> list = new ArrayList<>();
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
                if (bidIncrement <= 0) bidIncrement = 10.0;
                String endTimeStr = rs.getString("end_time");
                String statusStr = rs.getString("status");
                String sellerId = rs.getString("seller_id");
                String bidderId = rs.getString("highest_bidder_id");

                User owner = null;
                if (sellerId != null) {
                    for (User u : loadedUsers) {
                        if (u.getId().equals(sellerId)) {
                            owner = u;
                            break;
                        }
                    }
                }
                if (owner == null) {
                    owner = new Seller("U-TEMP", "System", "123", 0.0);
                }

                model.item.Item tempItem = new model.item.ItemBuilder()
                        .setType(itemType)
                        .setId("ITEM-" + id)
                        .setName(itemName)
                        .setDescription(itemDesc)
                        .setOwner(owner)
                        .build();

                Auction auction = new Auction(id, tempItem, startingPrice, bidIncrement, LocalDateTime.parse(endTimeStr));
                auction.setStatus(AuctionStatus.valueOf(statusStr));
                auction.setCurrentPrice(currentPrice);

                if (bidderId != null) {
                    for (User u : loadedUsers) {
                        if (u.getId().equals(bidderId) && u instanceof Bidder) {
                            auction.setHighestBidder((Bidder) u);
                            break;
                        }
                    }
                }

                loadBidHistoryForAuction(auction, loadedUsers);
                list.add(auction);
            }
            return list;
        }, "❌ Lỗi load auctions từ DB");
        
        return result != null ? result : new ArrayList<>();
    }

    private static void loadBidHistoryForAuction(Auction auction, List<User> loadedUsers) {
        String sql = "SELECT * FROM bid_transactions WHERE auction_id = ? ORDER BY timestamp ASC";
        executeQuery(sql, pstmt -> pstmt.setString(1, auction.getId()), rs -> {
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
            return null; // Don't need to return anything
        }, "❌ Lỗi load bid history cho " + auction.getId());
    }
    
    public static boolean updateItemOwner(String auctionId, String newOwnerId) {
        String sql = "UPDATE auctions SET seller_id = ? WHERE id = ?";
        return executeUpdate(sql, pstmt -> {
            pstmt.setString(1, newOwnerId);
            pstmt.setString(2, auctionId);
        }, "❌ Lỗi cập nhật chủ sở hữu item");
    }
}
