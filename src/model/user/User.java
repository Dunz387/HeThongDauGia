package model.user;
import model.base.Entity;

public abstract class User extends Entity{
    private String username;
    private String password;
    private Role role;
    private boolean isActive;

    public User(String id, String username, String password, Role role){
        super(id);
        this.username = username;
        this.password = password;
        this.role = role;
        this.isActive = true;
    }

    public boolean login(String inputPassword){ //Password Authenticator
        return this.isActive && this.password.equals(inputPassword);
    }
    //Setter
    public void setActive(boolean active){
        this.isActive = active;
    }

    //Getters

    public String getUsername(){
        return username;
    }
    public Role getRole(){
        return role;
    }
    public boolean isActive(){
        return isActive;
    }
}