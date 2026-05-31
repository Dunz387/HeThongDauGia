package model.item;
import model.user.User;

public class Electronics extends Item {

    public Electronics(String id, String name, String description, User owner) {
        super(id, name, description, owner);
    }

    @Override
    public String getTypeString() {
        return "ELECTRONICS";
    }
}
