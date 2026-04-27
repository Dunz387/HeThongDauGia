package model.item;
import model.base.Entity;
import model.user.Seller;

public abstract class Item extends Entity {
    private String name;
    private String description;
    private Seller owner;
    private String imagePath;

    public Item(String id, String name, String description, Seller owner) {
        super(id);
        this.name = name;
        this.description = description;
        this.owner = owner;
        this.imagePath = "default_item.png";
    }

    // Getters & Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Seller getOwner() { return owner; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }


    public abstract String getDetails();

    public static Item createItem(String itemType, String id, String name, String description, Seller owner, String extra1, int extra2) {
        if (itemType == null) return null;

        switch (itemType.toUpperCase()) {
            case "ELECTRONICS":
                return new Electronics(id, name, description, owner, extra1, extra2);
            case "ART":
                return new Arts(id, name, description, owner, extra1, extra2);
            case "VEHICLE":
                return new Vehicle(id, name, description, owner, extra1, extra2);
            default:
                throw new IllegalArgumentException("Loại sản phẩm không hợp lệ: " + itemType);
        }
    }
}
