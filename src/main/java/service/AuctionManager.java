package service;

import dao.AdminDAO;
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

public class AuctionManager {
    private static final AuctionManager instance = new AuctionManager();
    private List<User> users;
    private List<Auction> auctions;
    private ScheduledExecutorService scheduler;

    private AuctionManager() {
        DatabaseManager.initializeDatabase();
        this.users = UserDAO.loadUsers();
        this.auctions = AuctionDAO.loadAuctions(this.users);
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
        startAuctionMonitor();
    }

    public static AuctionManager getInstance() { return instance; }

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

    // ==========================================
    // NHÓM QUYỀN CỦA ADMIN
    // ==========================================
    public List<User> getAllUsers() {
        return new ArrayList<>(users); // Trả về danh sách user cho Admin xem
    }

    public boolean banUser(String targetUserId, boolean status) {
        for (User u : users) {
            if (u.getId().equals(targetUserId)) {
                u.setActive(status); // 1. Cập nhật RAM ngay lập tức
                return AdminDAO.setUserActiveStatus(targetUserId, status); // 2. Cập nhật xuống DB
            }
        }
        return false;
    }

    public boolean deleteAuctionForce(String auctionId) {
        synchronized (auctions) {
            auctions.removeIf(a -> a.getId().equals(auctionId)); // 1. Xóa khỏi RAM
            return AdminDAO.deleteAuctionForce(auctionId);       // 2. Xóa dưới DB
        }
    }
    // ==========================================

    public User authenticateUser(String username, String password) {
        for (User u : users) {
            if (u.getUsername().equals(username) && u.login(password)) {
                // ĐÃ SỬA: Nếu isActive == false (Bị Ban) thì không cho đăng nhập
                if (!u.isActive()) return null;
                return u;
            }
        }
        return null;
    }

    public boolean registerNewUser(String username, String password) {
        for (User u : users) {
            if (u.getUsername().equals(username)) return false;
        }
        Bidder newBidder = new Bidder("U-" + System.currentTimeMillis(), username, password, 100000.0);
        users.add(newBidder);
        UserDAO.saveUser(newBidder);
        return true;
    }

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
                    if (a.getStatus() == AuctionStatus.RUNNING && now.isAfter(a.getEndTime())) {
                        concludeAuction(a);
                    }
                }
            }
        }, 0, 10, TimeUnit.SECONDS);
    }

    public synchronized void concludeAuction(Auction auction) {
        if (auction != null) {
            auction.setStatus(AuctionStatus.FINISHED);
            AuctionDAO.updateAuction(auction);
        }
    }
}