package service;

import dao.DatabaseManager;
import exception.AuctionClosedException;
import exception.AuthenticationException;
import exception.InvalidBidException;
import model.auction.Auction;
import model.auction.AuctionStatus;
import model.user.Admin;
import model.user.Bidder;
import model.user.Role;
import model.user.Seller;
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
        this.users = DatabaseManager.loadUsers();
        this.auctions = DatabaseManager.loadAuctions(this.users);

        // TỐI ƯU: Đảm bảo Robot đi tuần là Daemon Thread[cite: 27]
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
        startAuctionMonitor();
    }

    public static AuctionManager getInstance() {
        return instance;
    }

    // THÊM MỚI: Hàm dừng chuyên nghiệp để giải phóng tài nguyên[cite: 27]
    public void stopManager() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            System.out.println("✅ Đã dừng Robot quét phiên đấu giá.");
        }
    }

    private void startAuctionMonitor() {
        scheduler.scheduleAtFixedRate(() -> {
            LocalDateTime now = LocalDateTime.now();
            synchronized (auctions) {
                for (Auction auction : auctions) {
                    if (auction.getStatus() == AuctionStatus.RUNNING && now.isAfter(auction.getEndTime())) {
                        concludeAuction(auction);
                    }
                }
            }
        }, 0, 30, TimeUnit.SECONDS);
    }

    // --- Các hàm registerUser, login, banUser, processBid... giữ nguyên từ Source 28 ---
    // [Giữ nguyên logic của bạn để đảm bảo không sai lệch nghiệp vụ]

    public synchronized void registerUser(User user) {
        if (user != null) {
            // Kiểm tra xem tên đăng nhập đã có trong RAM chưa
            for (User u : users) {
                if (u.getUsername().equals(user.getUsername())) return;
            }
            users.add(user);
            DatabaseManager.saveUser(user);
        }
    }

    public User login(String username, String password) throws AuthenticationException {
        for (User u : users) {
            if (u.getUsername().equals(username)) {
                if (!u.isActive()) {
                    throw new AuthenticationException("Đăng nhập thất bại: Tài khoản của bạn đã bị Admin khóa.");
                }
                if (u.login(password)) return u;
                else throw new AuthenticationException("Đăng nhập thất bại: Sai mật khẩu.");
            }
        }
        throw new AuthenticationException("Đăng nhập thất bại: Tên tài khoản không tồn tại.");
    }

    public void registerAuction(Auction auction) {
        if (auction != null) {
            synchronized (auctions) {
                auctions.add(auction);
                DatabaseManager.saveAuction(auction);
            }
        }
    }

    public String processBid(Bidder bidder, Auction auction, double bidAmount) {
        if (auction == null || bidder == null) return "Lỗi: Dữ liệu không hợp lệ.";
        if (auction.getStatus() != AuctionStatus.RUNNING) return "Lỗi: Phiên đấu giá chưa mở hoặc đã kết thúc.";
        if (LocalDateTime.now().isAfter(auction.getEndTime())) {
            auction.setStatus(AuctionStatus.FINISHED);
            DatabaseManager.updateAuction(auction);
            return "Lỗi: Phiên đấu giá này vừa mới hết thời gian!";
        }
        if (bidder.getAvailableBalance() < bidAmount) {
            return "Lỗi: Bạn không đủ tiền khả dụng trong ví để đặt mức giá này.";
        }
        try {
            auction.placeBid(bidder, bidAmount);
            DatabaseManager.updateAuction(auction);
            return "Thành công: Bạn đang là người trả giá cao nhất!";
        } catch (AuctionClosedException | InvalidBidException e) {
            return "Thất bại: " + e.getMessage();
        }
    }

    public synchronized boolean concludeAuction(Auction auction) {
        if (auction == null || (auction.getStatus() != AuctionStatus.RUNNING && auction.getStatus() != AuctionStatus.FINISHED)) {
            return false;
        }
        auction.setStatus(AuctionStatus.FINISHED);
        Bidder winner = auction.getHighestBidder();
        Seller seller = auction.getItem().getOwner();
        if (winner == null) {
            auction.setStatus(AuctionStatus.CANCELED);
            auction.setReason("Hết giờ - Không có người tham gia đặt giá.");
            DatabaseManager.updateAuction(auction);
            return true;
        }
        double finalPrice = auction.getCurrentPrice();
        if (winner.deductBalance(finalPrice)) {
            seller.receivePayment(finalPrice);
            auction.setStatus(AuctionStatus.PAID);
            DatabaseManager.updateAuction(auction);
            DatabaseManager.updateUser(winner);
            DatabaseManager.updateUser(seller);
            return true;
        } else {
            auction.setStatus(AuctionStatus.CANCELED);
            auction.setReason("Hủy: Người thắng cuộc không đủ số dư để thanh toán.");
            DatabaseManager.updateAuction(auction);
            return false;
        }
    }

    public List<Auction> getRunningAuctions() {
        List<Auction> running = new ArrayList<>();
        synchronized (auctions) {
            for (Auction a : auctions) {
                if (a.getStatus() == AuctionStatus.RUNNING) running.add(a);
            }
        }
        return running;
    }
}