package model.item;
import model.user.User;

public class Electronics extends Item {

    public Electronics(String id, String name, String description, User owner, String brand, int warrantyMonths) {
        super(id, name, description, owner);
    }
}
