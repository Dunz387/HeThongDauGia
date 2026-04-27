package dao;

import model.auction.Auction;
import model.user.User;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static final String USER_FILE = "users.ser";
    private static final String AUCTION_FILE = "auctions.ser";

    // --- 1. LƯU VÀ TẢI DỮ LIỆU USER ---
    public static void saveUsers(List<User> users) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(USER_FILE))) {
            oos.writeObject(users);
            System.out.println("✅ Đã lưu danh sách User thành công!");
        } catch (IOException e) {
            System.out.println("❌ Lỗi khi lưu danh sách User: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public static List<User> loadUsers() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(USER_FILE))) {
            return (List<User>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("⚠️ Chưa có dữ liệu User hoặc file trống. Sẽ khởi tạo danh sách mới.");
            return new ArrayList<>(); // Trả về list rỗng nếu chưa có file
        }
    }

    // --- 2. LƯU VÀ TẢI DỮ LIỆU AUCTION ---
    public static void saveAuctions(List<Auction> auctions) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(AUCTION_FILE))) {
            oos.writeObject(auctions);
            System.out.println("✅ Đã lưu danh sách Phiên đấu giá thành công!");
        } catch (IOException e) {
            System.out.println("❌ Lỗi khi lưu danh sách Auction: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public static List<Auction> loadAuctions() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(AUCTION_FILE))) {
            return (List<Auction>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("⚠️ Chưa có dữ liệu Auction hoặc file trống. Sẽ khởi tạo danh sách mới.");
            return new ArrayList<>();
        }
    }
}
