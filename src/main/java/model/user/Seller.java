package model.user;

public class Seller extends User {

    private double balance;

    public Seller(String id, String username, String password, double initialBalance) {
        super(id, username, password, Role.SELLER);
        this.balance = initialBalance;
    }

    public synchronized void receivePayment(double amount) {
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

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }
}
