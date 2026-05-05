package shared;

public class Protocol {
    // 1. Nhóm lệnh từ Client gửi lên
    public static final String REQ_LOGIN = "LOGIN";
    public static final String REQ_REGISTER = "REGISTER";
    public static final String REQ_BID = "BID";

    // Nhóm lệnh Khám phá & Bán hàng (Thêm mới)
    public static final String REQ_GET_AUCTIONS = "GET_AUCTIONS";
    public static final String REQ_CREATE_ITEM = "CREATE_ITEM";

    // 2. Nhóm lệnh Server trả về
    public static final String RES_SUCCESS = "SUCCESS";
    public static final String RES_FAIL = "FAIL";

    // Nhóm phản hồi danh sách (Thêm mới)
    public static final String RES_AUCTION_LIST = "AUCTION_LIST";

    // 3. Ký tự phân cách
    public static final String SEPARATOR = "\\|"; // Dùng cho split()
    public static final String DELIMITER = "|";   // Dùng cho nối chuỗi cộng chuỗi
}