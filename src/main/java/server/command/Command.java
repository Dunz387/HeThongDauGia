package server.command;

import server.ClientHandler;

/**
 * Interface cho Command Pattern xử lý các gói tin từ Client.
 */
@FunctionalInterface
public interface Command {
    /**
     * Thực thi lệnh dựa trên dữ liệu gửi lên.
     * @param parts Mảng chứa các tham số của gói tin (parts[0] là cmd).
     * @param client Context chứa thông tin kết nối và các handler liên quan.
     */
    void execute(String[] parts, ClientHandler client);
}
