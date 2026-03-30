package model;

public class Admin extends User {

    // Constructor kế thừa từ User
    public Admin(String username, String password, String email, String fullName, String phone,
                 String address, String city, String country) {
        super(username, password, email, fullName, phone, address, city, country);
    }

    // Bắt buộc ghi đè phương thức abstract của lớp User
    @Override
    public String getRole() {
        return "ADMIN";
    }

    // --- CÁC HÀM ĐẶC THÙ CỦA QUẢN TRỊ VIÊN ---
    
    // Hàm khóa tài khoản người dùng
    public void banUser(User user) {
        if (user != null) {
            user.setActive(false);
            System.out.println("[SYSTEM] Admin " + this.username + " đã khóa tài khoản: " + user.getUsername());
        }
    }

    // Hàm mở khóa tài khoản người dùng
    public void unbanUser(User user) {
        if (user != null) {
            user.setActive(true);
            System.out.println("[SYSTEM] Admin " + this.username + " đã mở khóa tài khoản: " + user.getUsername());
        }
    }

    // Ghi đè hàm printInfo
    @Override
    public void printInfo() {
        System.out.println("========== ADMIN PROFILE ==========");
        super.printInfo();
        System.out.println("===================================");
    }
}
