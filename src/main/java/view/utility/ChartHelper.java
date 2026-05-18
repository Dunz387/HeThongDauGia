package view.utility;

import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/**
 * Helper class cấu hình biểu đồ giá đấu giá.
 * Trích xuất từ InRoomController và SellerInRoomController (DRY).
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
                @Override public String toString(Number object) {
                    double val = object.doubleValue();
                    if (Math.abs(val - Math.round(val)) < 0.0001) {
                        int intVal = (int) Math.round(val);
                        if (intVal < 0) return "";
                        if (intVal == 0) return "BĐ";
                        return String.valueOf(intVal);
                    }
                    return "";
                }
                @Override public Number fromString(String string) { return 0; }
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
        if (increment < 1) return 1.0;
        else if (increment < 10) return Math.floor(increment);
        else if (increment < 100) return Math.floor(increment / 5) * 5;
        else return Math.floor(increment / 10) * 10;
    }

    /**
     * Format thời gian mm:ss.
     */
    public static String formatTime(int totalSeconds) {
        return String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60);
    }
}
