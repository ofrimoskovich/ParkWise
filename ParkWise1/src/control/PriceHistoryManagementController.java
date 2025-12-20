package control;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PriceHistoryManagementController {

    private final AccessDb db;
    private final PriceListManagementController priceListController;

    public PriceHistoryManagementController(AccessDb db, PriceListManagementController priceListController) {
        this.db = db;
        this.priceListController = priceListController;
    }

    private void ensureDb() {
        if (db == null) throw new IllegalStateException("Access DB is not configured");
    }

    // ✅ קובע מחיר פעיל: סוגר את הפעיל הקודם (אם יש) ומכניס חדש (ID נוצר אוטומטית)
    public void setActivePriceForParkingLot(int parkingLotId, int priceListId, LocalDate effectiveFrom) {
        ensureDb();

        try (Connection conn = db.open()) {
            conn.setAutoCommit(false);

            // close previous active (effectiveTo IS NULL)
            String closeSql = "UPDATE PriceHistory SET [effectiveTo]=? WHERE [parkingLotID]=? AND [effectiveTo] IS NULL";
            try (PreparedStatement ps = conn.prepareStatement(closeSql)) {
                ps.setDate(1, java.sql.Date.valueOf(effectiveFrom));
                ps.setInt(2, parkingLotId);
                ps.executeUpdate();
            }

            // insert new active (no ID column!)
            String insertSql = "INSERT INTO PriceHistory ([parkingLotID],[priceListID],[effectiveFrom],[effectiveTo]) VALUES (?,?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setInt(1, parkingLotId);
                ps.setInt(2, priceListId);
                ps.setDate(3, java.sql.Date.valueOf(effectiveFrom));
                ps.setNull(4, Types.DATE);
                ps.executeUpdate();
            }

            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to set active price: " + e.getMessage(), e);
        }
    }

    // ✅ ADD בלי HistoryID (AutoNumber)
    public void addHistoryRow(int parkingLotId, int priceListId, LocalDate from, LocalDate toOrNull) {
        ensureDb();

        String sql = "INSERT INTO PriceHistory ([parkingLotID],[priceListID],[effectiveFrom],[effectiveTo]) VALUES (?,?,?,?)";
        try (Connection conn = db.open(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, parkingLotId);
            ps.setInt(2, priceListId);
            ps.setDate(3, java.sql.Date.valueOf(from));
            if (toOrNull == null) ps.setNull(4, Types.DATE);
            else ps.setDate(4, java.sql.Date.valueOf(toOrNull));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add price history row: " + e.getMessage(), e);
        }
    }

    public void updateHistoryRow(int id, int priceListId, LocalDate from, LocalDate toOrNull) {
        ensureDb();

        String sql = "UPDATE PriceHistory SET [priceListID]=?, [effectiveFrom]=?, [effectiveTo]=? WHERE [ID]=?";
        try (Connection conn = db.open(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, priceListId);
            ps.setDate(2, java.sql.Date.valueOf(from));
            if (toOrNull == null) ps.setNull(3, Types.DATE);
            else ps.setDate(3, java.sql.Date.valueOf(toOrNull));
            ps.setInt(4, id);

            int updated = ps.executeUpdate();
            if (updated == 0) throw new IllegalArgumentException("PriceHistory not found: " + id);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update price history row: " + e.getMessage(), e);
        }
    }

    public void deleteHistoryRow(int id) {
        ensureDb();

        String sql = "DELETE FROM PriceHistory WHERE [ID]=?";
        try (Connection conn = db.open(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            int deleted = ps.executeUpdate();
            if (deleted == 0) throw new IllegalArgumentException("PriceHistory not found: " + id);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete price history row: " + e.getMessage(), e);
        }
    }

    public List<PriceHistoryRow> getHistoryRows(int parkingLotId) {
        ensureDb();

        List<PriceHistoryRow> rows = new ArrayList<>();
        String sql = "SELECT [ID],[priceListID],[effectiveFrom],[effectiveTo] FROM PriceHistory WHERE [parkingLotID]=? ORDER BY [effectiveFrom]";
        try (Connection conn = db.open(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, parkingLotId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int historyId = rs.getInt("ID");
                    int priceListId = rs.getInt("priceListID");
                    LocalDate from = rs.getDate("effectiveFrom") == null ? null : rs.getDate("effectiveFrom").toLocalDate();
                    LocalDate to = rs.getDate("effectiveTo") == null ? null : rs.getDate("effectiveTo").toLocalDate();

                    rows.add(PriceHistoryRow.fromHistoryRow(historyId, parkingLotId, priceListId, from, to, priceListController));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load price history: " + e.getMessage(), e);
        }
        return rows;
    }

    public PriceHistoryRow getCurrentActiveRow(int parkingLotId) {
        ensureDb();

        String sql = "SELECT TOP 1 [ID],[priceListID],[effectiveFrom],[effectiveTo] FROM PriceHistory WHERE [parkingLotID]=? AND [effectiveTo] IS NULL ORDER BY [effectiveFrom] DESC";
        try (Connection conn = db.open(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, parkingLotId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                int historyId = rs.getInt("ID");
                int priceListId = rs.getInt("priceListID");
                LocalDate from = rs.getDate("effectiveFrom") == null ? null : rs.getDate("effectiveFrom").toLocalDate();
                LocalDate to = rs.getDate("effectiveTo") == null ? null : rs.getDate("effectiveTo").toLocalDate();

                return PriceHistoryRow.fromHistoryRow(historyId, parkingLotId, priceListId, from, to, priceListController);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load active price row: " + e.getMessage(), e);
        }
    }
}