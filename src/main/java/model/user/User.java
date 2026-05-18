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

    /** Kiểm tra đăng nhập bằng mật khẩu */
    public boolean login(String inputPassword){
        return this.isActive && this.password.equals(inputPassword);
    }
    /** Khóa hoặc mở khóa tài khoản */
    public void setActive(boolean active){
        this.isActive = active;
    }

    // --- Các hàm lấy thông tin (Getters) ---
    public String getUsername(){
        return username;
    }
    public Role getRole(){
        return role;
    }
    public String getPassword() {
        return password;
    }
    public boolean isActive(){
        return isActive;
    }
}
