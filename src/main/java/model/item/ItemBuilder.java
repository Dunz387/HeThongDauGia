package model.item;

import model.user.User;

public class ItemBuilder {
    private String itemType;
    private String id;
    private String name;
    private String description;
    private User owner;

    public ItemBuilder setType(String itemType) {
        this.itemType = itemType;
        return this;
    }

    public ItemBuilder setId(String id) {
        this.id = id;
        return this;
    }

    public ItemBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public ItemBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    public ItemBuilder setOwner(User owner) {
        this.owner = owner;
        return this;
    }

    public Item build() {
        if (itemType == null) throw new IllegalStateException("Item type must be set");
        
        switch (itemType.toUpperCase()) {
            case "ELECTRONICS":
                return new Electronics(id, name, description, owner);
            case "ART":
                return new Arts(id, name, description, owner);
            case "VEHICLE":
                return new Vehicle(id, name, description, owner);
            default:
                throw new IllegalArgumentException("Loại sản phẩm không hợp lệ: " + itemType);
        }
    }
}
