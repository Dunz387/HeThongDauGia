package model.user;

public class Bidder extends User {

    private double balance;

    public Bidder(String id, String username, String password, double initialBalance) {
        super(id, username, password, Role.BIDDER);
        this.balance = initialBalance;
    }

    public double getBalance() {
        return balance;
    }

    public synchronized void addBalance(double amount) {
        if (amount > 0) {
            this.balance += amount;
        }
    }

    public synchronized boolean deductBalance(double amount) {
        if (amount > 0 && this.balance >= amount) {
            this.balance -= amount;
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "[BIDDER] Tên: " + getUsername() + " | Số dư ví: $" + this.balance;
    }
}
