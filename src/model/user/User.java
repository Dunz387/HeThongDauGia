package model;
import java.util.ArrayList;
import java.util.List;

public abstract class User extends Entity{
    protected String username;
    protected String password;
    protected String email;
    protected String fullName;
    protected String phone;
    protected String address;
    protected String city;
    protected String country;
    protected boolean isActive;
    protected double balance; //số dư
    protected List<Item> watchList; //hàng Item chờ xem của User;
    protected List<Notification> notifications; //thông báo đặt cược;
    protected List<BidTransaction> bidHistory; //lịch sử đặt cược


    public User(String username, String password, String email, String fullName, String phone,
                String address, String city, String country){
        super(); //kế thừa ID, và thời gian thực từ lớp Entity
        this.username = username;
        this.password = password;
        this.email = email;
        this.fullName = fullName;
        this.phone = phone;
        this.address = address;
        this.city = city;
        this.country = country;

        this.isActive = true;
        this.balance = 0;
        this.watchList = new ArrayList<>();
        this.notifications = new ArrayList<>();
        this.bidHistory = new ArrayList<>();
    }
// Getter
    public String getUsername(){
        return username;
    }
    public String getPassword() {
        return password;
    }
    public String getEmail(){
        return email;
    }
    public String getFullName(){
        return fullName;
    }
    public String getPhone(){
        return phone;
    }
    public String getAddress(){
        return address;
    }
    public String getCity(){
        return city;
    }
    public String getCountry(){
        return country;
    }
    public boolean isActive(){
        return isActive;
    }
    public double getBalance(){
        return balance;
    }
    public List<Item> getWatchList() {
        return watchList;
    }

    public List<Notification> getNotifications() {
        return notifications;
    }
    public List<BidTransaction> getBidHistory() {
        return bidHistory;
    }

// Setter
    public void setUsername(String username){
        this.username = username;
    }
    public void setPassword(String password){
        this.password = password;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setAddress(String address){
        this.address = address;
    }

    public void setCity(String city){
        this.city = city;
    }

    public void setCountry(String country){
        this.country = country;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
    // Balance
    public void deposit(double amount) {
        if (amount <= 0) return;
        balance += amount;
    }

    public void withdraw(double amount) {
        if (amount <= 0 || amount > balance) return;
        balance -= amount;
    }
    //Watchlist
    public void addToWatchList(Item item) {
        if (item != null && !watchList.contains(item)) {
            watchList.add(item);
        }
    }

    public void removeFromWatchList(Item item) {
        if (item != null) {
            watchList.remove(item);
        }
    }

    //Notification
    public void addNotification(String message) {
        if (message != null && !message.isEmpty()) {
            notifications.add(new Notification(message));
        }
    }

    //Bid history
    public void addBid(BidTransaction bid) {
        if (bid != null) {
            bidHistory.add(bid);
        }
    }

    public abstract String getRole(); //Các lớp kế thừa dùng user.getRole() để dùng cái loại User(Bidder, Seller,Admin)

    public void printInfo() {
        System.out.println("ID: " + id);
        System.out.println("Username: " + username);
        System.out.println("Full Name: " + fullName);
        System.out.println("Email: " + email);
        System.out.println("Phone: " + phone);
        System.out.println("Location: " + address + ", " + city + ", " + country);
        System.out.println("Balance: " + balance);
        System.out.println("Active: " + isActive);
    }
}

