package model.item;
import model.base.Entity;
import model.user.User;

public abstract class Item extends Entity {
    private String name;
    private String description;
    private User owner;
    private String imagePath;

    public Item(String id, String name, String description, User owner) {
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

    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }


    public abstract String getDetails();
}
