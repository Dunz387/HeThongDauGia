package model;

public class Electronics extends Item {

    private String brand;
    private int warrantyMonths;

    public Electronics(String name, String description, double startPrice,
                       Seller seller,
                       double buyNowPrice, double reservePrice,
                       String condition, int quantity,
                       String brand, int warrantyMonths) {

        super(name, description, startPrice, seller,
              buyNowPrice, reservePrice,
              condition, quantity);

        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
    }

    public String getBrand() { return brand; }
    public int getWarrantyMonths() { return warrantyMonths; }

    @Override
    public String getItemType() {
        return "ELECTRONICS";
    }

    @Override
    public void printInfo() {
        super.printInfo();
        System.out.println("Brand: " + brand);
        System.out.println("Warranty: " + warrantyMonths + " months");
    }
}
