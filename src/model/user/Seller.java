package model;

import java.util.ArrayList;
import java.util.List;

public class Seller extends User {
    // Attributes
    private String storeName;
    private String storeDescription;
    private double rating;
    private int feedbackCount;
    private boolean isVerified;
    private double balance;
    private String bankAccount;
    private List<String> activeAuctions; 

    // Constructor
    public Seller(String username, String password, String email, String fullName, 
        String phone, String address, String city, String country, 
        String storeName, String bankAccount) {
        
        // Calling the parent constructor
        super(username, password, email, fullName, phone, address, city, country);
        this.activeAuctions = new ArrayList<>();
        this.storeName = storeName;
        this.bankAccount = bankAccount;
        this.storeDescription = "Welcome to " + storeName;
        this.rating = 0.0;
        this.feedbackCount = 0;
        this.isVerified = false;
        this.balance = 0.0;
    }

    @Override
    public String getRole() {
        return "SELLER";
    }

    // Behaviors
    public void createAuction(String itemName, double startingPrice) {
        System.out.println("[Auction] " + storeName + " is selling: " + itemName + " starting at $" + startingPrice);
    }

    public void receiveFeedback(double newRating) {
        // Calculate the new average rating
        if (newRating < 0 || newRating > 5) {
            System.out.println("Invalid rating! Must be between 0 and 5.");
            return;
        }
        this.rating = (this.rating * feedbackCount + newRating) / (feedbackCount + 1);
        this.feedbackCount++;
        System.out.println("New rating updated: " + String.format("%.1f", this.rating) + " stars.");
    }

    public void withdrawFunds(double amount) {
        if (amount <= balance && amount > 0) {
            this.balance -= amount;
            System.out.println("Withdrew $" + amount + " to bank account: " + bankAccount);
        } else {
            System.out.println("Insufficient balance!");
        }
    }

    public void addBalance(double amount) {
        if (amount > 0) {
            this.balance += amount;
            System.out.println("Added $" + amount + " to balance. Current balance: $" + balance);
        } else {
            System.out.println("Invalid amount to add!");
        }
    }

    @Override
    public void displayProfile() {
        System.out.println(" SELLER PROFILE ");
        super.displayProfile(); // Call User's display method
        System.out.println("Store Name: " + storeName);
        System.out.println("Description: " + storeDescription);
        System.out.println("Rating: " + String.format("%.1f", rating) + " (" + feedbackCount + " reviews)");
        System.out.println("Verified Status: " + (isVerified ? "YES" : "NO"));
        System.out.println("Current Balance: $" + balance);
    }
    
    // 1. Auction Control
    public void setReservePrice(Item item, double minPrice) {
        System.out.println("[System] Reserve price for " + item.getId() + " set to $" + minPrice);
    }

    public void endAuctionEarly(Item item) {
        System.out.println("[Alert] Seller " + getStoreName() + " has ended auction " + item.getId() + " early.");
    }

    // 2. Order Fulfillment
    public void processShipping(Item item, String trackingNumber) {
        System.out.println("[Logistics] Item " + item.getId() + " is being shipped. Tracking: " + trackingNumber);
    }

    public void confirmPayment(double amount) {
        this.addBalance(amount);
        System.out.println("[Finance] Payment of $" + amount + " confirmed. New balance: $" + getBalance());
    }

    // 3. Customer Service
    public void replyToInquiry(String customerName, String response) {
        System.out.println("[Message] Reply to " + customerName + ": " + response);
    }

    // Setters for verification and rating
    public void setVerified(boolean verified) { isVerified = verified; }
    public String getStoreName() { return storeName; }
    public double getRating() { return rating; }
    public double getBalance() { return balance; }
    public String getBankAccount() { return bankAccount; }
    public int getFeedbackCount() { return feedbackCount; }
    public boolean isVerified() { return isVerified; }
}