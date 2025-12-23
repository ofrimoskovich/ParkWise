package control;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import entity.ParkingLot;

public class ParkingLotManagementController {

    private final AccessDb db;

    public ParkingLotManagementController(AccessDb db) {
        this.db = db;
    }

    private void ensureDb() {
        if (db == null) throw new IllegalStateException("Access DB is not configured");
    }

    // ✅ ADD בלי ID (AutoNumber) + מחזיר אובייקט עם ה-ID שנוצר
    public ParkingLot addParkingLot(String name, String address, String city, int availableSpaces) {
        ensureDb();

        final String sql =
                "INSERT INTO ParkingLot ([name],[addres],[city],[availablaSpaces]) VALUES (?,?,?,?)";

        try (Connection conn = db.open();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, name);
            ps.setString(2, address);
            ps.setString(3, city);
            ps.setInt(4, availableSpaces);
            ps.executeUpdate();

            int newId = -1;

            // ניסיון 1: generated keys
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys != null && keys.next()) newId = keys.getInt(1);
            } catch (Exception ignore) {}

            // ניסיון 2: Access identity
            if (newId <= 0) {
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT @@IDENTITY")) {
                    if (rs.next()) newId = rs.getInt(1);
                }
            }

            if (newId <= 0) throw new RuntimeException("Insert succeeded but could not read generated ID");

            return new ParkingLot(newId, name, address, city, availableSpaces);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to add parking lot: " + e.getMessage(), e);
        }
    }

    public void updateParkingLot(int id, String name, String address, String city, int availableSpaces) {
        ensureDb();

        final String sql =
                "UPDATE ParkingLot SET [name]=?, [addres]=?, [city]=?, [availablaSpaces]=? WHERE [ID]=?";

        try (Connection conn = db.open();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, address);
            ps.setString(3, city);
            ps.setInt(4, availableSpaces);
            ps.setInt(5, id);

            int updated = ps.executeUpdate();
            if (updated == 0) throw new IllegalArgumentException("ParkingLot not found: " + id);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update parking lot: " + e.getMessage(), e);
        }
    }

    public void deleteParkingLot(int id) {
        ensureDb();

        final String sql = "DELETE FROM ParkingLot WHERE [ID]=?";

        try (Connection conn = db.open();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete parking lot: " + e.getMessage(), e);
        }
    }

    public ParkingLot getParkingLot(int id) {
        ensureDb();

        final String sql =
                "SELECT [ID],[name],[addres],[city],[availablaSpaces] FROM ParkingLot WHERE [ID]=?";

        try (Connection conn = db.open();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("ParkingLot not found: " + id);

                return new ParkingLot(
                        rs.getInt("ID"),
                        rs.getString("name"),
                        rs.getString("addres"),
                        rs.getString("city"),
                        rs.getInt("availablaSpaces")
                );
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to load parking lot: " + e.getMessage(), e);
        }
    }

    public List<ParkingLot> getAllParkingLots() {
        ensureDb();

        List<ParkingLot> lots = new ArrayList<>();

        final String sql =
                "SELECT [ID],[name],[addres],[city],[availablaSpaces] FROM ParkingLot ORDER BY [ID]";

        try (Connection conn = db.open();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lots.add(new ParkingLot(
                        rs.getInt("ID"),
                        rs.getString("name"),
                        rs.getString("addres"),
                        rs.getString("city"),
                        rs.getInt("availablaSpaces")
                ));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to load parking lots: " + e.getMessage(), e);
        }

        return lots;
    }
}