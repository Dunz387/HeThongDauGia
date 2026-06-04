package dao.core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Lớp trừu tượng đóng gói chung logic kết nối CSDL theo Template Method Pattern (Callback).
 * Giúp loại bỏ code lặp lại khi mở/đóng Connection, Statement và try-catch SQLException.
 */
public abstract class BaseDAO {
    private static final Logger LOGGER = Logger.getLogger(BaseDAO.class.getName());

    @FunctionalInterface
    protected interface PreparedStatementSetter {
        void setValues(PreparedStatement pstmt) throws SQLException;
    }

    @FunctionalInterface
    protected interface ResultSetExtractor<T> {
        T extractData(ResultSet rs) throws SQLException;
    }

    /**
     * Dành cho các truy vấn INSERT, UPDATE, DELETE
     */
    protected static boolean executeUpdate(String sql, PreparedStatementSetter setter, String errorMessage) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (setter != null) {
                setter.setValues(pstmt);
            }
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, errorMessage, e);
            return false;
        }
    }

    /**
     * Dành cho truy vấn SELECT có tham số
     */
    protected static <T> T executeQuery(String sql, PreparedStatementSetter setter, ResultSetExtractor<T> extractor, String errorMessage) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (setter != null) {
                setter.setValues(pstmt);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                return extractor.extractData(rs);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, errorMessage, e);
            return null;
        }
    }

    /**
     * Dành cho truy vấn SELECT không tham số (Dùng Statement)
     */
    protected static <T> T executeQuery(String sql, ResultSetExtractor<T> extractor, String errorMessage) {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return extractor.extractData(rs);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, errorMessage, e);
            return null;
        }
    }
}
