package model.user;

public class Bidder extends User {

    private double balance;
    private double lockedBalance = 0.0; // Tiền đang bị giam ở các phiên đấu giá

    public Bidder(String id, String username, String password, double initialBalance) {
        super(id, username, password, Role.BIDDER);
        this.balance = initialBalance;
    }

    public double getBalance() {
        return balance;
    }

    // Tính toán số tiền thực sự có thể dùng
    public double getAvailableBalance() {
        return balance - lockedBalance;
    }

    // Khóa tiền khi đặt giá thành công
    public synchronized boolean lockBalance(double amount) {
        if (amount > 0 && getAvailableBalance() >= amount) {
            this.lockedBalance += amount;
            return true;
        }
        return false;
    }

    // Hoàn tiền đang giam khi bị người khác vượt giá
    public synchronized void unlockBalance(double amount) {
        if (amount > 0 && this.lockedBalance >= amount) {
            this.lockedBalance -= amount;
        }
    }

    public synchronized void addBalance(double amount) {
        if (amount > 0) {
            this.balance += amount;
        }
    }

    // Khi thanh toán thực sự, trừ cả số dư gốc và số dư bị giam
    public synchronized boolean deductBalance(double amount) {
        if (amount > 0 && this.balance >= amount && this.lockedBalance >= amount) {
            this.balance -= amount;
            this.lockedBalance -= amount;
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "[BIDDER] Tên: " + getUsername() + " | Khả dụng: $" + getAvailableBalance() + " (Đang giam: $" + lockedBalance + ")";
    }
}