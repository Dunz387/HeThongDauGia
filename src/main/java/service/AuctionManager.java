package service;

import dao.DatabaseManager;
import exception.AuctionClosedException;
import exception.AuthenticationException;
import exception.InvalidBidException;
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

public class AuctionManager {
    private static final AuctionManager instance = new AuctionManager();
    private List<User> users;
    private List<Auction> auctions;
    private ScheduledExecutorService scheduler;

    private AuctionManager() {
        DatabaseManager.initializeDatabase();
        this.users = DatabaseManager.loadUsers();
        this.auctions = DatabaseManager.loadAuctions(this.users);
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

    public List<Auction> getAllAuctions() {
        synchronized (auctions) {
            // Trả về một bản sao để tránh lỗi đồng bộ luồng
            return new ArrayList<>(auctions);
        }
    }

    public synchronized void registerUser(User user) {
        if (user != null) {
            for (User u : users) {
                if (u.getUsername().equals(user.getUsername())) return;
            }
            users.add(user);
            DatabaseManager.saveUser(user);
        }
    }

    public User authenticateUser(String username, String password) {
        for (User u : users) {
            if (u.getUsername().equals(username) && u.login(password)) return u;
        }
        return null;
    }

    public boolean registerNewUser(String username, String password) {
        for (User u : users) {
            if (u.getUsername().equalsIgnoreCase(username)) return false;
        }
        Bidder newUser = new Bidder("U-" + System.currentTimeMillis(), username, password, 5000.0);
        this.registerUser(newUser);
        return true;
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
        if (auction == null || bidder == null) return "Lỗi dữ liệu";
        if (auction.getStatus() != AuctionStatus.RUNNING) return "Phiên đã kết thúc";
        try {
            auction.placeBid(bidder, bidAmount);
            DatabaseManager.updateAuction(auction);
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
            DatabaseManager.updateAuction(auction);
        }
    }
}