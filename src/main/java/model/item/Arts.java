package model.item;
import model.user.User;

public class Arts extends Item {

    public Arts(String id, String name, String description, User owner, String artist, int creationYear) {
        super(id, name, description, owner);
    }
}
