package view.utility;

import java.util.regex.Pattern;

/**
 * Lớp tiện ích xử lý kiểm tra tính hợp lệ của dữ liệu đầu vào.
 */
public class ValidationHelper {

    // Regex: Chỉ cho phép chữ cái, số, dấu gạch dưới, từ 3-20 ký tự
    private static final String USERNAME_REGEX = "^[a-zA-Z0-9_]{3,20}$";
    
    // Regex: Mật khẩu tối thiểu 8 ký tự, ít nhất 1 chữ cái và 1 số
    private static final String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$";

    /**
     * Kiểm tra tên người dùng có hợp lệ không.
     */
    public static boolean isValidUsername(String username) {
        if (username == null) return false;
        return Pattern.matches(USERNAME_REGEX, username);
    }

    /**
     * Kiểm tra mật khẩu có đủ mạnh không.
     */
    public static boolean isStrongPassword(String password) {
        if (password == null) return false;
        return Pattern.matches(PASSWORD_REGEX, password);
    }

    /**
     * Kiểm tra giá khởi điểm (phải là số thực và > 0).
     */
    public static boolean isValidStartPrice(String priceStr) {
        try {
            double price = Double.parseDouble(priceStr);
            return price > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Kiểm tra thời lượng phiên (phải là số nguyên và > 0).
     */
    public static boolean isValidDuration(String durationStr) {
        try {
            int duration = Integer.parseInt(durationStr);
            return duration > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Kiểm tra số tiền giao dịch (phải là số thực và > 0).
     */
    public static boolean isValidAmount(String amountStr) {
        try {
            double amount = Double.parseDouble(amountStr);
            return amount > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Kiểm tra một chuỗi có trống hay không (sau khi trim).
     */
    public static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}
