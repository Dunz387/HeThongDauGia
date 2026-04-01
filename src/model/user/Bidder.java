package model;

public class Bidder extends User {

    // Constructor kế thừa từ User
    public Bidder(String username, String password, String email, String fullName, String phone,
                  String address, String city, String country) {
        super(username, password, email, fullName, phone, address, city, country);
    }

    // Bắt buộc ghi đè phương thức abstract của lớp User
    @Override
    public String getRole() {
        return "BIDDER";
    }

    // Ghi đè hàm printInfo để hiển thị rõ đây là tài khoản Bidder
    @Override
    public void printInfo() {
        System.out.println("========== BIDDER PROFILE ==========");
        super.printInfo(); // Gọi lại hàm in của lớp cha cho nhanh, đỡ phải viết lại dài dòng
        System.out.println("====================================");
    }
}
