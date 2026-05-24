package view.utility;

import javafx.scene.chart.NumberAxis;

/**
 * Helper class cấu hình biểu đồ giá đấu giá. Trích xuất từ InRoomController và
 * SellerInRoomController (DRY).
 */
public class ChartHelper {

    /**
     * Cấu hình biểu đồ giá: trục X theo lượt bid, trục Y theo giá, tắt animation.
     */
    public static void configureAreaChart(javafx.scene.chart.AreaChart<Number, Number> chart) {
        chart.setAnimated(false);

        if (chart.getYAxis() instanceof NumberAxis) {
            NumberAxis yAxis = (NumberAxis) chart.getYAxis();
            yAxis.setAnimated(false);
            yAxis.setForceZeroInRange(false);
        }

        if (chart.getXAxis() instanceof NumberAxis) {
            NumberAxis xAxis = (NumberAxis) chart.getXAxis();
            xAxis.setMinorTickVisible(false);
            xAxis.setMinorTickCount(0);
            xAxis.setTickUnit(1.0);
            xAxis.setAutoRanging(false);
            xAxis.setLowerBound(0);
            xAxis.setUpperBound(10);

            xAxis.setTickLabelFormatter(new javafx.util.StringConverter<Number>() {
                @Override
                public String toString(Number object) {
                    double val = object.doubleValue();
                    if (Math.abs(val - Math.round(val)) < 0.0001) {
                        int intVal = (int) Math.round(val);
                        if (intVal < 0)
                            return "";
                        if (intVal == 0)
                            return "BĐ";
                        return String.valueOf(intVal);
                    }
                    return "";
                }

                @Override
                public Number fromString(String string) {
                    return 0;
                }
            });
        }

        // CSS Styling
        if (chart.lookup(".chart-plot-background") != null) {
            chart.lookup(".chart-plot-background").setStyle("-fx-background-color: transparent;");
        }
    }

    /**
     * Cập nhật giới hạn trục X sau khi có bid mới.
     */
    public static void updateXAxisBounds(javafx.scene.chart.AreaChart<Number, Number> chart, int bidCount) {
        if (chart.getXAxis() instanceof NumberAxis) {
            NumberAxis xAxis = (NumberAxis) chart.getXAxis();
            xAxis.setUpperBound(Math.max(10, bidCount + 2));
            xAxis.setLowerBound(Math.max(0, bidCount - 15));
        }
    }

    /**
     * Tính bước giá tối thiểu (10% làm tròn) — đồng bộ logic với Auction.java.
     */
    public static double calculateMinIncrement(double currentPrice) {
        double increment = currentPrice * 0.1;
        if (increment < 1)
            return 1.0;
        return Math.round(increment);
    }

    /**
     * Format thời gian mm:ss.
     */
    public static String formatTime(int totalSeconds) {
        return String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60);
    }

    /**
     * Format số thực để hiển thị đầy đủ phần thập phân đến chữ số khác 0 cuối cùng,
     * tránh làm tròn số nguyên và tránh dạng mũ khoa học.
     */
    public static String formatDouble(double val) {
        java.text.DecimalFormatSymbols symbols = new java.text.DecimalFormatSymbols(java.util.Locale.US);
        java.text.DecimalFormat df = new java.text.DecimalFormat("#,##0.###########", symbols);
        return df.format(val);
    }
}
