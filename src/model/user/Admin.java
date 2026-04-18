package model.user;

public class Admin extends User{
    public Admin(String id, String username, String password){
        super(id, username, password, Role.ADMIN);
    }

    @Override
    public String toString(){
        return "[ADMIN] " + getUsername() + " (ID: " + getId() + ")";
    }
}
