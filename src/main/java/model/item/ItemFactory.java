package model.item;

import model.user.User;

public class ItemFactory {

    public static Item createItem(String itemType, String id, String name, String description, User owner, String extra1, int extra2) {
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

    public static String getItemTypeString(Item item) {
        if (item instanceof Arts) return "ART";
        if (item instanceof Vehicle) return "VEHICLE";
        return "ELECTRONICS";
    }
}