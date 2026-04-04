package model.Item;
import model.Entity;
import model.Seller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public abstract class Item extends Entity {
    // Thông tin cơ bản
    protected String name;
    protected String description;
    protected double startPrice;

    protected Seller seller;

    // Viết thêm ở Auction
    protected double buyNowPrice; //Mua ngay khi giá đấu quá cao
    protected double reservePrice;//Giá tối thiểu để đấu giá thành công

    // Chi tiết hàng Item
    protected String condition; // Mới, cũ
    protected int quantity;

    protected List<String> imageUrls; //Ảnh image hàng

    // Dữ liệu hàng khác
    protected LocalDateTime createdAt; // Thời gian thực
    protected String status; //Tình trạng hàng

    public Item(String name, String description, double startPrice,
                Seller seller,
                double buyNowPrice, double reservePrice,
                String condition, int quantity) {

        super();

        this.name = name;
        this.description = description;
        this.startPrice = startPrice;
        this.seller = seller;

        this.buyNowPrice = buyNowPrice;
        this.reservePrice = reservePrice;

        this.condition = condition;
        this.quantity = quantity;

        this.imageUrls = new ArrayList<>();

        this.createdAt = LocalDateTime.now();
        this.status = "AVAILABLE";
    }

    // Getter
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getStartPrice() { return startPrice; }
    public Seller getSeller() { return seller; }

    public double getBuyNowPrice() { return buyNowPrice; }
    public double getReservePrice() { return reservePrice; }

    public String getCondition() { return condition; }
    public int getQuantity() { return quantity; }

    public List<String> getImageUrls() { return imageUrls; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getStatus() { return status; }

    // Setter
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setStartPrice(double startPrice) { this.startPrice = startPrice; }

    public void setBuyNowPrice(double buyNowPrice) { this.buyNowPrice = buyNowPrice; }
    public void setReservePrice(double reservePrice) { this.reservePrice = reservePrice; }

    public void setCondition(String condition) { this.condition = condition; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public void setStatus(String status) { this.status = status; }

    // Ảnh item
    public void addImage(String url) {
        if (url != null && !url.isEmpty()) {
            imageUrls.add(url);
        }
    }

    public void removeImage(String url) {
        imageUrls.remove(url);
    }

    // Helper
    public boolean isAvailable() {
        return "AVAILABLE".equalsIgnoreCase(status);
    }

    // Abstract
    public abstract String getItemType();

    // Xuất ra màn hình
    public void printInfo() {
        System.out.println("ID: " + id);
        System.out.println("Name: " + name);
        System.out.println("Start Price: " + startPrice);
        System.out.println("Buy Now: " + buyNowPrice);
        System.out.println("Reserve: " + reservePrice);
        System.out.println("Condition: " + condition);
        System.out.println("Quantity: " + quantity);
        System.out.println("Status: " + status);
        System.out.println("Seller: " + seller.getUsername());
    }
}