package control;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import entity.ParkingLot;

public class ParkingLotManagementController {

    private final AccessDb db;
    private final Map<Integer, ParkingLot> cache = new HashMap<>();

    // ✅ שמות אמיתיים מה-Access שלך
    private static final String TBL = "ParkingLot";
    private static final String COL_ID = "ID";                 // AutoNumber
    private static final String COL_NAME = "name";
    private static final String COL_ADDRESS = "addres";        // כן, בלי s
    private static final String COL_CITY = "city";
    private static final String COL_SPACES = "availablaSpaces"; // כן, כתוב ככה

    public ParkingLotManagementController(AccessDb db) {
        this.db = db;
    }

    private void ensureDb() {
        if (db == null) throw new IllegalStateException("Access DB is not configured");
    }

    // ✅ ADD: בלי ID (כי זה AutoNumber)
    public ParkingLot addParkingLot(String name, String address, String city, int availableSpaces) {
        ensureDb();

        String sql = "INSERT INTO " + TBL +
                " ([" + COL_NAME + "],[" + COL_ADDRESS + "],[" + COL_CITY + "],[" + COL_SPACES + "]) " +
                "VALUES (?,?,?,?)";

        try (Connection conn = db.open();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, name);
            ps.setString(2, address);
            ps.setString(3, city);
            ps.setInt(4, availableSpaces);
            ps.executeUpdate();

            int newId = fetchGeneratedId(conn, ps);
            ParkingLot lot = new ParkingLot(newId, name, address, city, availableSpaces);
            cache.put(newId, lot);
            return lot;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to add parking lot: " + e.getMessage(), e);
        }
    }

    // ✅ UPDATE: עם השמות הנכונים
    public void updateParkingLot(int id, String name, String address, String city, int availableSpaces) {
        ensureDb();

        String sql = "UPDATE " + TBL +
                " SET [" + COL_NAME + "]=?, [" + COL_ADDRESS + "]=?, [" + COL_CITY + "]=?, [" + COL_SPACES + "]=? " +
                "WHERE [" + COL_ID + "]=?";

        try (Connection conn = db.open();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, address);
            ps.setString(3, city);
            ps.setInt(4, availableSpaces);
            ps.setInt(5, id);

            int updated = ps.executeUpdate();
            if (updated == 0) throw new IllegalArgumentException("ParkingLot not found: " + id);

            ParkingLot cached = cache.get(id);
            if (cached != null) {
                cached.setName(name);
                cached.setAddress(address);
                cached.setCity(city);
                cached.setAvailableSpaces(availableSpaces);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update parking lot: " + e.getMessage(), e);
        }
    }

    public void deleteParkingLot(int id) {
        ensureDb();

        String sql = "DELETE FROM " + TBL + " WHERE [" + COL_ID + "]=?";

        try (Connection conn = db.open();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();
            cache.remove(id);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete parking lot: " + e.getMessage(), e);
        }
    }

    public ParkingLot getParkingLot(int id) {
        ensureDb();

        ParkingLot cached = cache.get(id);
        if (cached != null) return cached;

        String sql = "SELECT * FROM " + TBL + " WHERE [" + COL_ID + "]=?";

        try (Connection conn = db.open();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("ParkingLot not found: " + id);

                ParkingLot lot = mapRow(rs);
                cache.put(lot.getId(), lot);
                return lot;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to load parking lot: " + e.getMessage(), e);
        }
    }

    public List<ParkingLot> getAllParkingLots() {
        ensureDb();

        List<ParkingLot> lots = new ArrayList<>();
        String sql = "SELECT * FROM " + TBL + " ORDER BY [" + COL_ID + "]";

        try (Connection conn = db.open();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            cache.clear();

            while (rs.next()) {
                ParkingLot lot = mapRow(rs);
                lots.add(lot);
                cache.put(lot.getId(), lot);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to load parking lots: " + e.getMessage(), e);
        }

        return lots;
    }

    private ParkingLot mapRow(ResultSet rs) throws SQLException {
        int id = rs.getInt(COL_ID);
        String name = rs.getString(COL_NAME);
        String address = rs.getString(COL_ADDRESS);
        String city = rs.getString(COL_CITY);
        int spaces = rs.getInt(COL_SPACES);
        return new ParkingLot(id, name, address, city, spaces);
    }

    private int fetchGeneratedId(Connection conn, PreparedStatement ps) throws SQLException {
        try (ResultSet keys = ps.getGeneratedKeys()) {
            if (keys != null && keys.next()) return keys.getInt(1);
        }
        try (PreparedStatement ps2 = conn.prepareStatement("SELECT @@IDENTITY");
             ResultSet rs = ps2.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        }
        throw new SQLException("Failed to retrieve generated ID");
    }
}