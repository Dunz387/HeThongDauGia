package view;

/**
 * Lớp Launcher phụ để khởi chạy ứng dụng.
 * Việc không kế thừa javafx.application.Application giúp tránh lỗi
 * "JavaFX runtime components are missing" khi chạy ứng dụng trực tiếp bằng java -jar
 * hoặc trong một số môi trường IDE mà không cần cấu hình module-path phức tạp.
 */
public class Launcher {
    public static void main(String[] args) {
        Main.main(args);
    }
}
