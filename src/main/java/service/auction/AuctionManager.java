package service.auction;

import dao.admin.AdminDAO;
import dao.auction.AuctionDAO;
import dao.core.DatabaseManager;
import model.auction.Auction;
import model.auction.AuctionStatus;
import model.user.Bidder;
import model.user.User;
import service.user.UserService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * Quản lý logic đấu giá: CRUD Auction, xử lý bid, giám sát phiên.
 * Đã tách User/Admin logic sang UserService và AdminService (SRP).
 */
public class AuctionManager {
    private static final AuctionManager instance = new AuctionManager();
    private final List<Auction> auctions;
    private final ScheduledExecutorService scheduler;

    // Callback để thông báo Server khi phiên đấu giá kết thúc
    private BiConsumer<Auction, User> auctionFinishedCallback = null;

    private AuctionManager() {
        DatabaseManager.initializeDatabase();
        // Đảm bảo UserService khởi tạo trước (load users từ DB) để nạp danh sách đấu giá
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
                    return AdminDAO.deleteAuctionForce(auctionId);
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
                    AuctionItemUpdater.updateItem(a, newName, newDesc, newType);
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
        return AuctionBidService.processBid(bidder, auction, bidAmount);
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
        AuctionSettlementService.concludeAuction(auction, auctionFinishedCallback);
    }
}
