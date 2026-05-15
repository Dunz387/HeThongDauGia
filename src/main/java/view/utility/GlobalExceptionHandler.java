package view.utility;

import javafx.application.Platform;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Xử lý các lỗi ngoại lệ chưa được bắt (Uncaught Exceptions) trong ứng dụng UI.
 */
public class GlobalExceptionHandler {

    public static void setupHandler() {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            handleException(throwable);
        });
    }

    public static void handleException(Throwable e) {
        e.printStackTrace();
        
        // Luôn đảm bảo hiển thị thông báo trên UI thread
        Platform.runLater(() -> {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String exceptionText = sw.toString();

            AlertHelper.showError("Lỗi Hệ Thống", 
                "Đã xảy ra lỗi không mong muốn:\n" + e.getMessage() + 
                "\n\nVui lòng liên hệ quản trị viên.");
        });
    }
}
