package network;

import javafx.application.Platform;
import shared.Protocol; // Import file Protocol bạn vừa tạo ở mục 1.1

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientNetworkManager {
    // Áp dụng Singleton: Chỉ cho phép có 1 ống mạng duy nhất
    private static ClientNetworkManager instance;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    // Biến để lưu trữ tạm thời phản hồi từ Server cho các lệnh đồng bộ (như Login/Register)
    private String lastResponse = null;

    private ClientNetworkManager() {
        // Constructor rỗng, ngăn không cho tạo instance mới bằng từ khóa new
    }

    public static ClientNetworkManager getInstance() {
        if (instance == null) {
            instance = new ClientNetworkManager();
        }
        return instance;
    }

    /**
     * Mở đường ống kết nối đến Server
     */
    public boolean connect(String ip, int port) {
        try {
            socket = new Socket(ip, port);

            // QUAN TRỌNG: Khởi tạo luồng Out trước và flush ngay để tránh deadlock với Server
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            // Mở luồng chạy ngầm để liên tục lắng nghe Server chửi/khen
            startListeningThread();

            System.out.println("✅ Đã kết nối thành công đến Server!");
            return true;
        } catch (Exception e) {
            System.out.println("❌ Không thể kết nối đến Server: " + e.getMessage());
            return false;
        }
    }

    /**
     * Ném dữ liệu lên Server
     */
    public void sendData(String data) {
        try {
            if (out != null) {
                out.writeObject(data);
                out.reset(); // Xóa cache object
                out.flush();
            }
        } catch (Exception e) {
            System.out.println("Lỗi khi gửi dữ liệu: " + e.getMessage());
        }
    }

    /**
     * Luồng chạy ngầm (Background Thread) liên tục nghe ngóng
     */
    private void startListeningThread() {
        Thread listenerThread = new Thread(() -> {
            try {
                Object inputObj;
                while ((inputObj = in.readObject()) != null) {
                    if (inputObj instanceof String) {
                        String message = (String) inputObj;
                        System.out.println("[Server trả về]: " + message);

                        // Phân loại data nhận được
                        processIncomingMessage(message);
                    }
                }
            } catch (Exception e) {
                System.out.println("Mất kết nối với Server.");
            }
        });

        listenerThread.setDaemon(true); // Tự động chết khi tắt app
        listenerThread.start();
    }

    /**
     * Xử lý gói tin Server ném về
     */
    private void processIncomingMessage(String message) {
        // Tách lệnh tương tự như Backend
        String[] parts = message.split(Protocol.SEPARATOR);
        String command = parts[0];

        switch (command) {
            case Protocol.REQ_LOGIN:
            case Protocol.REQ_REGISTER:
                // Lưu lại kết quả để Controller kiểm tra
                this.lastResponse = message;
                break;

            case "BROADCAST": // Ví dụ sau này cho Realtime
                // NHỚ QUY TẮC THÉP: Cập nhật UI thì phải bọc trong Platform.runLater
                Platform.runLater(() -> {
                    // Update nhãn giá tiền, đồng hồ... (Làm ở Bước 3)
                });
                break;
        }
    }

    // Hàm tiện ích để Controller lấy kết quả trả về
    public String getLastResponse() {
        String temp = lastResponse;
        lastResponse = null; // Đọc xong thì reset để không bị trùng
        return temp;
    }
}