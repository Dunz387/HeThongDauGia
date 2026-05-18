package service;

import dao.AdminDAO;
import dao.AuctionDAO;
import dao.UserDAO;
import model.auction.Auction;
import model.auction.AuctionStatus;
import model.user.Bidder;
import model.user.Seller;
import model.user.User;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service chuyên xử lý các thao tác quản trị (Admin).
 * Tách từ AuctionManager để tuân thủ SRP.
 */
public class AdminService {
    private static final Logger LOGGER = Logger.getLogger(AdminService.class.getName());
    private static final AdminService instance = new AdminService();

    private AdminService() {}

    public static AdminService getInstance() { return instance; }

    public boolean banUser(String targetUserId, boolean status) {
        User u = UserService.getInstance().findUserById(targetUserId);
        if (u != null) {
            u.setActive(status);
            return AdminDAO.setUserActiveStatus(targetUserId, status);
        }
        return false;
    }

    public String updateUserBalanceForce(String targetUserId, double newBalance) {
        User u = UserService.getInstance().findUserById(targetUserId);
        if (u != null) {
            if (u instanceof Bidder) {
                Bidder bidder = (Bidder) u;
                if (bidder.getLockedBalance() > 0) {
                    return "Người dùng đang có $" + String.format("%.2f", bidder.getLockedBalance()) + " bị khóa trong các phiên đấu giá!";
                }
                bidder.setBalance(newBalance);
            } else if (u instanceof Seller) {
                ((Seller) u).setBalance(newBalance);
            }
            boolean ok = UserDAO.updateUserBalance(targetUserId, newBalance);
            return ok ? "SUCCESS" : "Lỗi lưu số dư vào cơ sở dữ liệu!";
        }
        return "Không tìm thấy người dùng!";
    }

    public boolean deleteAuctionForce(String auctionId) {
        AuctionManager.getInstance().removeAuctionById(auctionId);
        return AdminDAO.deleteAuctionForce(auctionId);
    }

    public boolean updateAuctionForce(String auctionId, String newName, String newDesc, String newType, double newPrice, int newDur) {
        Auction a = AuctionManager.getInstance().getAuctionById(auctionId);
        if (a != null) {
            a.getItem().setName(newName);
            a.getItem().setDescription(newDesc);

            String currentType = "ELECTRONICS";
            if (a.getItem() instanceof model.item.Arts) currentType = "ART";
            else if (a.getItem() instanceof model.item.Vehicle) currentType = "VEHICLE";

            if (!currentType.equalsIgnoreCase(newType)) {
                model.item.Item newItem = model.item.ItemFactory.createItem(newType, a.getItem().getId(), newName, newDesc, a.getItem().getOwner(), "Unknown", 0);
                try {
                    java.lang.reflect.Field itemField = Auction.class.getDeclaredField("item");
                    itemField.setAccessible(true);
                    itemField.set(a, newItem);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Lỗi đổi loại sản phẩm", e);
                }
            }

            a.setStartingPrice(newPrice);
            if (a.getBidHistory().isEmpty()) {
                a.setCurrentPrice(newPrice);
            }
            a.setEndTime(java.time.LocalDateTime.now().plusMinutes(newDur));
            return AuctionDAO.updateAuction(a);
        }
        return false;
    }
}
