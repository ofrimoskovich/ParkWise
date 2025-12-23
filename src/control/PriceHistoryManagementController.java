package control;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import entity.PriceHistory;

/**
 * PriceHistoryManagementController
 *
 * Rules:
 * - Only ONE active PriceHistory per ParkingLot is allowed.
 * - Assigning a new PriceList:
 *   1) closes the previous active record (effectiveTo = today)
 *   2) creates a new record (effectiveFrom = today, effectiveTo = null)
 */
public class PriceHistoryManagementController {

    private final AccessDb db;

    public PriceHistoryManagementController(AccessDb db) {
        this.db = db;
    }

    private void ensureDb() {
        if (db == null) {
            throw new IllegalStateException("Database is not configured.");
        }
    }

    /**
     * Returns FULL history for a parking lot (active + past).
     */
    public List<PriceHistory> getHistoryForParkingLot(int parkingLotId) {
        ensureDb();

        if (parkingLotId <= 0) {
            throw new IllegalArgumentException("Invalid parkingLotId.");
        }

        List<PriceHistory> list = new ArrayList<>();

        String sql =
            "SELECT ID, parkingLotID, priceListID, effectiveFrom, effectiveTo " +
            "FROM PriceHistory " +
            "WHERE parkingLotID = ? " +
            "ORDER BY effectiveFrom DESC";

        try (Connection conn = db.open();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, parkingLotId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new PriceHistory(
                        rs.getInt("ID"),
                        rs.getInt("parkingLotID"),
                        rs.getInt("priceListID"),
                        rs.getDate("effectiveFrom").toLocalDate(),
                        rs.getDate("effectiveTo") == null
                            ? null
                            : rs.getDate("effectiveTo").toLocalDate()
                    ));
                }
            }
            return list;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to load price history: " + e.getMessage(), e);
        }
    }

    /**
     * Assigns a new PriceList to a parking lot.
     * Enforces ONE active record rule.
     */
    public void assignPriceListToParkingLot(int parkingLotId, int priceListId) {
        ensureDb();

        if (parkingLotId <= 0 || priceListId <= 0) {
            throw new IllegalArgumentException("Invalid IDs.");
        }

        LocalDate today = LocalDate.now();

        try (Connection conn = db.open()) {
            conn.setAutoCommit(false);

            // Close previous active record
            String closeSql =
                "UPDATE PriceHistory " +
                "SET effectiveTo = ? " +
                "WHERE parkingLotID = ? AND effectiveTo IS NULL";

            try (PreparedStatement ps = conn.prepareStatement(closeSql)) {
                ps.setDate(1, java.sql.Date.valueOf(today));
                ps.setInt(2, parkingLotId);
                ps.executeUpdate();
            }

            // Insert new record
            String insertSql =
                "INSERT INTO PriceHistory (parkingLotID, priceListID, effectiveFrom, effectiveTo) " +
                "VALUES (?, ?, ?, NULL)";

            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setInt(1, parkingLotId);
                ps.setInt(2, priceListId);
                ps.setDate(3, java.sql.Date.valueOf(today));
                ps.executeUpdate();
            }

            conn.commit();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to assign price list: " + e.getMessage(), e);
        }
    }
}