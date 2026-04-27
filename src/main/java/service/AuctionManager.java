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

    // SINGLETON PATTERN: Early Initialization để thread-safe 100%
    private static final AuctionManager instance = new AuctionManager();

    // KHO LƯU TRỮ TẠM THỜI (Chờ DAO)
    private List<User> users;
    private List<Auction> auctions;

    // Luồng chạy ngầm để quản lý thời gian (Mục 3)
    private ScheduledExecutorService scheduler;

    // Khóa Constructor: Cấm mọi hành vi tạo mới từ bên ngoài
    private AuctionManager() {
        this.users = DatabaseManager.loadUsers();
        this.auctions = DatabaseManager.loadAuctions();

        // Khởi động Robot đi tuần khi hệ thống bật
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        startAuctionMonitor();
    }

    public static AuctionManager getInstance() {
        return instance;
    }

    // Robot tự động quét 30 giây/lần
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

    // QUẢN LÝ TÀI KHOẢN

    public void registerUser(User user) {
        if (user != null) {
            users.add(user);
            saveSystemData();
        }
    }

    //Đón lõng tất cả các trường hợp đăng nhập lỗi bằng Exception
    public User login(String username, String password) throws AuthenticationException {
        for (User u : users) {
            if (u.getUsername().equals(username)) {
                // Tên đăng nhập đúng, bắt đầu kiểm tra trạng thái
                if (!u.isActive()) {
                    throw new AuthenticationException("Đăng nhập thất bại: Tài khoản của bạn đã bị Admin khóa.");
                }

                if (u.login(password)) {
                    return u; // Mọi thứ hoàn hảo, cho phép vào trong
                } else {
                    throw new AuthenticationException("Đăng nhập thất bại: Sai mật khẩu.");
                }
            }
        }

        // Đi hết danh sách mà không thấy user nào khớp
        throw new AuthenticationException("Đăng nhập thất bại: Tên tài khoản không tồn tại.");
    }

    // CHỨC NĂNG CỦA ADMIN

    public boolean banUser(Admin admin, User targetUser) {
        if (admin != null && targetUser != null && targetUser.getRole() != Role.ADMIN) {
            targetUser.setActive(false);
            saveSystemData();
            return true;
        }
        return false;
    }

    public boolean approveAuction(Admin admin, Auction auction) {
        if (admin != null && admin.getRole() == Role.ADMIN) {
            if (auction != null && auction.getStatus() == AuctionStatus.OPEN) {
                auction.setStatus(AuctionStatus.RUNNING);
                saveSystemData();
                return true;
            }
        }
        return false;
    }

    public boolean rejectAuction(Admin admin, Auction auction, String reason) {
        if (admin != null && auction != null && auction.getStatus() == AuctionStatus.OPEN) {
            auction.setStatus(AuctionStatus.CANCELED);
            auction.setReason(reason); //
            saveSystemData();
            return true;
        }
        return false;
    }

    // NGHIỆP VỤ ĐẤU GIÁ CORE

    public void registerAuction(Auction auction) {
        if (auction != null) {
            // NÂNG CẤP: Khóa list lại để thêm an toàn, tránh đụng chạm với Robot quét
            synchronized (auctions) {
                auctions.add(auction); // Trạng thái mặc định từ Constructor Auction là OPEN
                saveSystemData();
            }
        }
    }

    // NÂNG CẤP: Đã bỏ chữ synchronized ở đây, đón lõng Exception bằng try-catch
    public String processBid(Bidder bidder, Auction auction, double bidAmount) {
        if (auction == null || bidder == null) return "Lỗi: Dữ liệu không hợp lệ.";
        if (auction.getStatus() != AuctionStatus.RUNNING) return "Lỗi: Phiên đấu giá chưa mở hoặc đã kết thúc.";

        if (LocalDateTime.now().isAfter(auction.getEndTime())) {
            auction.setStatus(AuctionStatus.FINISHED);
            saveSystemData();
            return "Lỗi: Phiên đấu giá này vừa mới hết thời gian!";
        }

        if (bidder.getBalance() < bidAmount) {
            return "Lỗi: Bạn không đủ tiền trong ví để đặt mức giá này.";
        }

        try {
            auction.placeBid(bidder, bidAmount);
            saveSystemData();
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

        // Trường hợp 1: Không có ai mua
        if (winner == null) {
            auction.setStatus(AuctionStatus.CANCELED);
            auction.setReason("Hết giờ - Không có người tham gia đặt giá.");
            saveSystemData();
            return true;
        }

        // Trường hợp 2: Có người mua
        double finalPrice = auction.getCurrentPrice();

        if (winner.deductBalance(finalPrice)) { // Hàm này tự động check số dư một lần nữa
            seller.receivePayment(finalPrice);
            auction.setStatus(AuctionStatus.PAID);
            saveSystemData();
            return true;
        } else {
            // Lỗi hiếm: Người thắng rút hết tiền khỏi ví trước khi kết thúc
            auction.setStatus(AuctionStatus.CANCELED);
            auction.setReason("Hủy: Người thắng cuộc không đủ số dư để thanh toán.");
            saveSystemData();
            return false;
        }
    }

    // HỖ TRỢ VIEW (Lấy dữ liệu ra để in lên màn hình)

    public List<Auction> getAllAuctions() {
        return auctions;
    }

    public List<Auction> getRunningAuctions() {
        List<Auction> running = new ArrayList<>();
        for (Auction a : auctions) {
            if (a.getStatus() == AuctionStatus.RUNNING) {
                running.add(a);
            }
        }
        return running;
    }
    public void saveSystemData() {
        System.out.println("Đang tiến hành lưu toàn bộ dữ liệu hệ thống...");
        DatabaseManager.saveUsers(this.users);
        DatabaseManager.saveAuctions(this.auctions);
    }
}
