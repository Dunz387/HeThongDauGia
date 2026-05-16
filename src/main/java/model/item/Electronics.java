package model.item;
import model.user.User;

public class Electronics extends Item {

    private String brand;
    private int warrantyMonths;


    public Electronics(String id, String name, String description, User owner, String brand, int warrantyMonths) {
        super(id, name, description, owner);
        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public int getWarrantyMonths() {
        return warrantyMonths;
    }

    public void setWarrantyMonths(int warrantyMonths) {
        this.warrantyMonths = warrantyMonths;
    }

    @Override
    public String getDetails() {
        return String.format("[Điện tử] Hãng: %s | Bảo hành: %d tháng", brand, warrantyMonths);
    }
}
