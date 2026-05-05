package shared;

public class Protocol {
    // 1. Nhóm lệnh từ Client gửi lên
    public static final String REQ_LOGIN = "LOGIN";
    public static final String REQ_REGISTER = "REGISTER";
    public static final String REQ_BID = "BID";

    // 2. Nhóm lệnh Server trả về
    public static final String RES_SUCCESS = "SUCCESS";
    public static final String RES_FAIL = "FAIL";

    // 3. Ký tự phân cách
    public static final String SEPARATOR = "\\|"; // Dùng cho split()
    public static final String DELIMITER = "|";   // Dùng cho nối chuỗi cộng chuỗi
}