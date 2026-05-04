package model.item;

import model.user.Seller;

public class ItemFactory {

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