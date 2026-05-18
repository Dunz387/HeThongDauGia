package service;

import dao.AuctionDAO;
import dao.DatabaseManager;
import dao.UserDAO;
import model.auction.Auction;
import model.auction.AuctionStatus;
import model.user.Bidder;
import model.user.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Quản lý logic đấu giá: CRUD Auction, xử lý bid, giám sát phiên.
 * Đã tách User/Admin logic sang UserService và AdminService (SRP).
 */
public class AuctionManager {
    private static final Logger LOGGER = Logger.getLogger(AuctionManager.class.getName());
    private static final AuctionManager instance = new AuctionManager();
    private final List<Auction> auctions;
    private final ScheduledExecutorService scheduler;

    // Callback để thông báo Server khi phiên đấu giá kết thúc
    private BiConsumer<Auction, User> auctionFinishedCallback = null;

    private AuctionManager() {
        DatabaseManager.initializeDatabase();
        // Đảm bảo UserService khởi tạo trước (load users từ DB)
        UserService.getInstance();
        this.auctions = AuctionDAO.loadAuctions(UserService.getInstance().getUsersRef());
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
        startAuctionMonitor();
    }

    public static AuctionManager getInstance() { return instance; }

    public void setAuctionFinishedCallback(BiConsumer<Auction, User> callback) {
        this.auctionFinishedCallback = callback;
    }

    public List<Auction> getAllAuctions() {
        synchronized (auctions) { return new ArrayList<>(auctions); }
    }

    public Auction getAuctionById(String id) {
        synchronized (auctions) {
            for (Auction a : auctions) {
                if (a.getId().equals(id)) return a;
            }
        }
        return null;
    }

    public void removeAuctionById(String auctionId) {
        synchronized (auctions) {
            auctions.removeIf(a -> a.getId().equals(auctionId));
        }
    }

    // ==========================================
    // NHÓM QUYỀN CỦA SELLER
    // ==========================================
    public boolean deleteAuctionBySeller(String auctionId, String sellerId) {
        synchronized (auctions) {
            Auction a = getAuctionById(auctionId);
            if (a != null && a.getItem().getOwner().getId().equals(sellerId)) {
                if (a.getStatus() != AuctionStatus.FINISHED && a.getBidHistory().isEmpty()) {
                    auctions.remove(a);
                    return dao.AdminDAO.deleteAuctionForce(auctionId);
                }
            }
            return false;
        }
    }

    public boolean updateAuctionBySeller(String auctionId, String sellerId, String newName, String newDesc, String newType) {
        synchronized (auctions) {
            Auction a = getAuctionById(auctionId);
            if (a != null && a.getItem().getOwner().getId().equals(sellerId)) {
                if (a.getStatus() != AuctionStatus.FINISHED && a.getBidHistory().isEmpty()) {
                    a.getItem().setName(newName);
                    a.getItem().setDescription(newDesc);

                    String currentType = model.item.ItemFactory.getItemTypeString(a.getItem());

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

                    return AuctionDAO.updateAuction(a);
                }
            }
            return false;
        }
    }
    // ==========================================

    public void registerAuction(Auction auction) {
        if (auction != null) {
            synchronized (auctions) {
                auctions.add(auction);
                AuctionDAO.saveAuction(auction);
            }
        }
    }

    public String processBid(Bidder bidder, Auction auction, double bidAmount) {
        if (auction == null || bidder == null) return "Lỗi dữ liệu";
        if (auction.getStatus() != AuctionStatus.RUNNING) return "Phiên đã kết thúc";
        try {
            auction.placeBid(bidder, bidAmount);
            AuctionDAO.updateAuction(auction);
            java.util.List<model.auction.BidTransaction> history = auction.getBidHistory();
            if (!history.isEmpty()) {
                dao.AuctionDAO.saveBidTransaction(history.get(history.size() - 1));
            }
            return "Thành công!";
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    public void stopManager() {
        if (scheduler != null) scheduler.shutdownNow();
    }

    private void startAuctionMonitor() {
        scheduler.scheduleAtFixedRate(() -> {
            LocalDateTime now = LocalDateTime.now();
            synchronized (auctions) {
                for (Auction a : auctions) {
                    if (a.getStatus() == AuctionStatus.RUNNING) {
                        if (now.isAfter(a.getEndTime())) {
                            concludeAuction(a);
                        }
                    }
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    public synchronized void concludeAuction(Auction auction) {
        if (auction != null && auction.getStatus() == AuctionStatus.RUNNING) {
            auction.setStatus(AuctionStatus.FINISHED);

            User seller = auction.getItem().getOwner();

            Bidder highestBidder = auction.getHighestBidder();
            if (highestBidder != null) {
                double winPrice = auction.getCurrentPrice();

                if (highestBidder.deductBalance(winPrice)) {
                    UserDAO.updateUserBalance(highestBidder.getId(), highestBidder.getBalance());

                    auction.getItem().setOwner(highestBidder);
                    AuctionDAO.updateItemOwner(auction.getId(), highestBidder.getId());

                    if (seller instanceof model.user.Seller) {
                        ((model.user.Seller) seller).receivePayment(winPrice);
                        UserDAO.updateUserBalance(seller.getId(), ((model.user.Seller) seller).getBalance());
                    }
                    LOGGER.info(String.format("💰 [THANH TOÁN] Đã chuyển %.2f từ %s cho %s",
                        winPrice, highestBidder.getUsername(), (seller != null ? seller.getUsername() : "Hệ thống")));
                }
            }

            AuctionDAO.updateAuction(auction);
            LOGGER.info(String.format("✅ [AuctionManager] Phiên %s đã kết thúc.", auction.getId()));

            if (auctionFinishedCallback != null) {
                auctionFinishedCallback.accept(auction, seller);
            }
        }
    }
}