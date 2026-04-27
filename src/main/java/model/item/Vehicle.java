package model.item;
import model.user.Seller;

public class Vehicle extends Item {

    private String engineType;
    private int mileage;

    private boolean hasServiceHistory;

    public Vehicle(String id, String name, String description, Seller owner, String engineType, int mileage) {
        super(id, name, description, owner);
        this.engineType = engineType;
        this.mileage = mileage;
        this.hasServiceHistory = false; // Chuẩn YAGNI: Mặc định chưa xác nhận
    }


    public String getEngineType() { return engineType; }
    public void setEngineType(String engineType) { this.engineType = engineType; }

    public int getMileage() { return mileage; }
    public void setMileage(int mileage) { this.mileage = mileage; }

    public boolean hasServiceHistory() { return hasServiceHistory; }
    public void setHasServiceHistory(boolean hasServiceHistory) { this.hasServiceHistory = hasServiceHistory; }


    @Override
    public String getDetails() {
        String serviceInfo = hasServiceHistory ? "[Full lịch sử hãng]" : "";
        return String.format("[Phương tiện] Động cơ: %s | ODO: %d km %s", engineType, mileage, serviceInfo);
    }
}
