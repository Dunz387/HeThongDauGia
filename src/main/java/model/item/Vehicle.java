package model.item;
import model.user.User;

public class Vehicle extends Item {

    public Vehicle(String id, String name, String description, User owner, String engineType, int mileage) {
        super(id, name, description, owner);
    }
}
