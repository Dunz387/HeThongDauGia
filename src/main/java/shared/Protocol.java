package shared;

public class Protocol {
    // =================================================================
    // 1. KÝ TỰ PHÂN CÁCH ĐƯỜNG TRUYỀN (Đã sửa lỗi dùng dấu |)
    // =================================================================
    // Sử dụng chuỗi đặc biệt ";;;" để không bao giờ bị trùng với dữ liệu người dùng nhập
    public static final String DELIMITER = ";;;"; // Ký tự phân cách đường truyền

    // =================================================================
    // 2. NHÓM LỆNH TỪ CLIENT GỬI LÊN (REQUEST)
    // =================================================================
    // Xác thực
    public static final String REQ_LOGIN = "LOGIN";
    public static final String REQ_REGISTER = "REGISTER";

    // Quản lý sản phẩm (Seller)
    public static final String REQ_GET_AUCTIONS = "GET_AUCTIONS";
    public static final String REQ_CREATE_ITEM = "CREATE_ITEM";
    public static final String REQ_UPDATE_ITEM = "UPDATE_ITEM"; // Dành cho tính năng Sửa sau này
    public static final String REQ_DELETE_ITEM = "DELETE_ITEM"; // Dành cho tính năng Xóa sau này

    // Giao dịch Tài chính
    public static final String REQ_DEPOSIT = "DEPOSIT";
    public static final String REQ_WITHDRAW = "WITHDRAW";

    // Đấu giá (Bidder)
    public static final String REQ_BID = "BID";
    public static final String REQ_AUTOBID = "AUTOBID";
    public static final String REQ_JOIN_ROOM = "JOIN_ROOM";
    public static final String REQ_LEAVE_ROOM = "LEAVE_ROOM";
    public static final String REQ_LOGOUT = "LOGOUT";

    // Quản trị viên (Admin)
    public static final String REQ_GET_USERS = "GET_USERS";
    public static final String REQ_BAN_USER = "BAN_USER";
    public static final String REQ_DELETE_AUCTION = "DELETE_AUCTION";
    public static final String REQ_UPDATE_USER_BALANCE = "UPDATE_USER_BALANCE";
    public static final String REQ_KICK_USER = "KICK_USER";

    // =================================================================
    // 3. NHÓM LỆNH SERVER TRẢ VỀ (RESPONSE 1-1)
    // =================================================================
    // Phản hồi trạng thái chung
    public static final String RES_SUCCESS = "SUCCESS";
    public static final String RES_FAIL = "FAIL";

    // Phản hồi gửi kèm dữ liệu
    public static final String RES_AUCTION_LIST = "AUCTION_LIST";
    public static final String RES_USER_LIST = "USER_LIST";
    public static final String RES_UPDATE_BALANCE = "UPDATE_BALANCE";

    // =================================================================
    // 4. NHÓM LỆNH SERVER TỰ ĐỘNG BẮN XUỐNG (BROADCAST / OBSERVER)
    // =================================================================
    // Đây là các lệnh mà Server tự động gửi cho tất cả Client đang online
    public static final String BROADCAST_NEW_BID = "NEW_BID";
    public static final String BROADCAST_AUCTION_FINISHED = "AUCTION_FINISHED";
    public static final String BROADCAST_AUCTION_START = "AUCTION_START";
    public static final String BROADCAST_ROUND_FINISHED = "ROUND_FINISHED";
    public static final String BROADCAST_TIME_EXTENDED = "TIME_EXTENDED";
    public static final String BROADCAST_PARTICIPANTS = "PARTICIPANTS_COUNT";
    public static final String BROADCAST_ROOM_KICKED = "ROOM_KICKED";
    public static final String BROADCAST_FORCE_LOGOUT = "FORCE_LOGOUT";
}
