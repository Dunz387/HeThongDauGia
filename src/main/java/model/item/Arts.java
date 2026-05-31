package model.item;
import model.user.User;

public class Arts extends Item {

    public Arts(String id, String name, String description, User owner) {
        super(id, name, description, owner);
    }

    @Override
    public String getTypeString() {
        return "ART";
    }
}
