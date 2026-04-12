package model.Item;

import model.UserType.Seller;

public class Vehicle extends Item {

    private String brand;
    private int mileage;

    public Vehicle(String name, String description, double startPrice,
                   Seller seller,
                   double buyNowPrice, double reservePrice,
                   String condition, int quantity,
                   String brand, int mileage) {

        super(name, description, startPrice, seller,
              buyNowPrice, reservePrice,
              condition, quantity);

        this.brand = brand;
        this.mileage = mileage;
    }

    public String getBrand() { return brand; }
    public int getMileage() { return mileage; }

    @Override
    public String getItemType() {
        return "VEHICLE";
    }

    @Override
    public void printInfo() {
        super.printInfo();
        System.out.println("Brand: " + brand);
        System.out.println("Mileage: " + mileage);
    }
}
