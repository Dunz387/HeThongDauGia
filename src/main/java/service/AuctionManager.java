package service;

import dao.AdminDAO;
import dao.AuctionDAO;
import dao.DatabaseManager;
import dao.UserDAO;
import model.auction.Auction;
import model.auction.AuctionStatus;
import model.user.Bidder;
import model.user.Seller;
import model.user.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class AuctionManager {
    private static final AuctionManager instance = new AuctionManager();
    private List<User> users;
    private List<Auction> auctions;
    private ScheduledExecutorService scheduler;

    // Callback để thông báo Server khi phiên đấu giá kết thúc
    private Consumer<Auction> auctionFinishedCallback = null;
    
    // T10: Callback để thông báo Server khi kết thúc 1 vòng (90s không ai đặt)
    private Consumer<Auction> roundFinishedCallback = null;

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

    /**
     * Đăng ký callback để Server nhận thông báo khi phiên đấu giá kết thúc tự động.
     */
    public void setAuctionFinishedCallback(Consumer<Auction> callback) {
        this.auctionFinishedCallback = callback;
    }

    public void setRoundFinishedCallback(Consumer<Auction> callback) {
        this.roundFinishedCallback = callback;
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

    public boolean updateAuctionForce(String auctionId, String newName, String newDesc, String newType, double newPrice) {
        synchronized (auctions) {
            Auction a = getAuctionById(auctionId);
            if (a != null) {
                // Sửa thông tin Item
                a.getItem().setName(newName);
                a.getItem().setDescription(newDesc);
                
                // Nếu đổi type thì phải tạo Item mới (ItemFactory)
                String currentType = "ELECTRONICS";
                if (a.getItem() instanceof model.item.Arts) currentType = "ART";
                else if (a.getItem() instanceof model.item.Vehicle) currentType = "VEHICLE";
                
                if (!currentType.equalsIgnoreCase(newType)) {
                    model.item.Item newItem = model.item.ItemFactory.createItem(newType, a.getItem().getId(), newName, newDesc, a.getItem().getOwner(), "Unknown", 0);
                    // Dùng reflection hoặc gán lại item trong Auction nếu item không final.
                    // Trong code hiện tại Item item không final.
                    try {
                        java.lang.reflect.Field itemField = Auction.class.getDeclaredField("item");
                        itemField.setAccessible(true);
                        itemField.set(a, newItem);
                    } catch (Exception e) {
                        System.err.println("Lỗi đổi loại sản phẩm: " + e.getMessage());
                    }
                }

                a.setStartingPrice(newPrice);
                // Nếu chưa ai đặt giá thì cập nhật currentPrice luôn
                if (a.getBidHistory().isEmpty()) {
                    a.setCurrentPrice(newPrice);
                }
                
                return AuctionDAO.updateAuction(a);
            }
            return false;
        }
    }
    // ==========================================

    // ==========================================
    // NHÓM QUYỀN CỦA SELLER
    // ==========================================
    public boolean deleteAuctionBySeller(String auctionId, String sellerId) {
        synchronized (auctions) {
            Auction a = getAuctionById(auctionId);
            if (a != null && a.getItem().getOwner().getId().equals(sellerId)) {
                if (a.getStatus() != AuctionStatus.FINISHED && a.getBidHistory().isEmpty()) {
                    auctions.remove(a);
                    return AdminDAO.deleteAuctionForce(auctionId); // Tạm dùng chung DAO với Admin
                }
            }
            return false;
        }
    }

    public boolean updateAuctionBySeller(String auctionId, String sellerId, String newName, String newDesc, String newType) {
        synchronized (auctions) {
            Auction a = getAuctionById(auctionId);
            if (a != null && a.getItem().getOwner().getId().equals(sellerId)) {
                // Chỉ cho sửa nếu chưa có ai đặt giá và phiên chưa kết thúc
                if (a.getStatus() != AuctionStatus.FINISHED && a.getBidHistory().isEmpty()) {
                    a.getItem().setName(newName);
                    a.getItem().setDescription(newDesc);
                    
                    // Xử lý đổi loại sản phẩm
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
                             System.err.println("Lỗi đổi loại sản phẩm: " + e.getMessage());
                        }
                    }
                    
                    return AuctionDAO.updateAuction(a);
                }
            }
            return false;
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

    public boolean registerNewUser(String username, String password, String role) {
        for (User u : users) {
            if (u.getUsername().equals(username)) return false;
        }
        User newUser;
        if ("SELLER".equals(role)) {
            newUser = new Seller("U-" + System.currentTimeMillis(), username, password, 0.0);
        } else {
            newUser = new Bidder("U-" + System.currentTimeMillis(), username, password, 100000.0);
        }
        users.add(newUser);
        UserDAO.saveUser(newUser);
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
            // Lấy transaction vừa tạo (là phần tử cuối) và lưu vào DB
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
                        } else if (java.time.temporal.ChronoUnit.SECONDS.between(a.getLastActivityTime(), now) >= 90) {
                            // T10: Đã 90s trôi qua không ai đặt giá -> Hết vòng
                            if (roundFinishedCallback != null) {
                                roundFinishedCallback.accept(a);
                            }
                            a.updateActivityTime(); // Bắt đầu tính giờ cho vòng mới
                        }
                    }
                }
            }
        }, 0, 1, TimeUnit.SECONDS); // Chạy mỗi 1 giây để kiểm tra chính xác hơn
    }

    public synchronized void concludeAuction(Auction auction) {
        if (auction != null && auction.getStatus() == AuctionStatus.RUNNING) {
            auction.setStatus(AuctionStatus.FINISHED);
            
            // THỰC HIỆN THANH TOÁN (Phase 3)
            Bidder highestBidder = auction.getHighestBidder();
            if (highestBidder != null) {
                double winPrice = auction.getCurrentPrice();
                // Trừ tiền người mua (tiền đã bị giam ở vòng placeBid)
                if (highestBidder.deductBalance(winPrice)) {
                    UserDAO.updateUserBalance(highestBidder.getId(), highestBidder.getBalance());
                    
                    // Cộng tiền cho người bán
                    Seller seller = (Seller) auction.getItem().getOwner(); // Cast an toàn vì owner luôn là Seller
                    if (seller != null) {
                        seller.receivePayment(winPrice);
                        UserDAO.updateUserBalance(seller.getId(), seller.getBalance());
                    }
                    System.out.println("💰 [THANH TOÁN] Đã chuyển " + winPrice + " từ " + highestBidder.getUsername() + " cho " + (seller != null ? seller.getUsername() : "Hệ thống"));
                }
            }

            AuctionDAO.updateAuction(auction);
            System.out.println("✅ [AuctionManager] Phiên " + auction.getId() + " đã kết thúc.");

            // Thông báo Server để broadcast cho tất cả Client
            if (auctionFinishedCallback != null) {
                auctionFinishedCallback.accept(auction);
            }
        }
    }
}