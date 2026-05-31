package model.item;
import model.user.User;

public class Vehicle extends Item {

    public Vehicle(String id, String name, String description, User owner) {
        super(id, name, description, owner);
    }

    @Override
    public String getTypeString() {
        return "VEHICLE";
    }
}
