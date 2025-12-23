package control;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import entity.City;
import entity.ParkingLot;

/**
 * ParkingLotManagementController
 * ------------------------------
 * Works with Access table ParkingLot:
 *   ParkingLot(ID, name, addres, cityID, availablaSpaces)
 *
 * IMPORTANT RULES (per requirements):
 * - ID is AutoNumber (no manual set)
 * - Update: only name/address/city can change (availableSpaces is NOT updated here)
 * - availableSpaces is loaded from DB and shown, but not editable in update.
 */
public class ParkingLotManagementController {

    private final AccessDb db;

    public ParkingLotManagementController(AccessDb db) {
        this.db = db;
    }

    private void ensureDb() {
        if (db == null) throw new IllegalStateException("Access DB is not configured");
    }

    /**
     * Adds a new ParkingLot (ID AutoNumber).
     * Note: availableSpaces is allowed on INSERT (initial value).
     */
    public ParkingLot addParkingLot(String name, String address, City city, int availableSpaces) {
        ensureDb();

        String n = name == null ? "" : name.trim();
        String a = address == null ? "" : address.trim();
        if (n.isEmpty()) throw new IllegalArgumentException("Parking lot name is required.");
        if (a.isEmpty()) throw new IllegalArgumentException("Parking lot address is required.");
        if (city == null) throw new IllegalArgumentException("City is required.");
        if (availableSpaces < 0) throw new IllegalArgumentException("Available spaces must be 0 or higher.");

        final String sql =
                "INSERT INTO ParkingLot ([name],[addres],[cityID],[availablaSpaces]) VALUES (?,?,?,?)";

        try (Connection conn = db.open();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, n);
            ps.setString(2, a);
            ps.setInt(3, city.getId());
            ps.setInt(4, availableSpaces);
            ps.executeUpdate();

            int newId = readGeneratedId(ps, conn);
            if (newId <= 0) throw new RuntimeException("Insert succeeded but could not read generated ID");

            return new ParkingLot(newId, n, a, city, availableSpaces);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to add parking lot: " + e.getMessage(), e);
        }
    }

    /**
     * Updates ParkingLot by ID:
     * - updates ONLY name/address/cityID
     * - does NOT change availableSpaces
     */
    public void updateParkingLot(int id, String name, String address, City city) {
        ensureDb();

        String n = name == null ? "" : name.trim();
        String a = address == null ? "" : address.trim();
        if (n.isEmpty()) throw new IllegalArgumentException("Parking lot name is required.");
        if (a.isEmpty()) throw new IllegalArgumentException("Parking lot address is required.");
        if (city == null) throw new IllegalArgumentException("City is required.");

        final String sql =
                "UPDATE ParkingLot SET [name]=?, [addres]=?, [cityID]=? WHERE [ID]=?";

        try (Connection conn = db.open();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, n);
            ps.setString(2, a);
            ps.setInt(3, city.getId());
            ps.setInt(4, id);

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

    /**
     * Loads a ParkingLot including City object (join City table).
     */
    public ParkingLot getParkingLot(int id) {
        ensureDb();

        final String sql =
                "SELECT p.[ID], p.[name], p.[addres], p.[availablaSpaces], " +
                "       c.[ID] AS CityID, c.[cityName] AS CityName " +
                "FROM ParkingLot p " +
                "LEFT JOIN City c ON p.[cityID] = c.[ID] " +
                "WHERE p.[ID]=?";

        try (Connection conn = db.open();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("ParkingLot not found: " + id);

                City city = null;
                int cityId = rs.getInt("CityID");
                if (!rs.wasNull()) {
                    city = new City(cityId, rs.getString("CityName"));
                }

                return new ParkingLot(
                        rs.getInt("ID"),
                        rs.getString("name"),
                        rs.getString("addres"),
                        city,
                        rs.getInt("availablaSpaces")
                );
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to load parking lot: " + e.getMessage(), e);
        }
    }

    /**
     * Loads all parking lots including City object (join City table).
     */
    public List<ParkingLot> getAllParkingLots() {
        ensureDb();

        List<ParkingLot> lots = new ArrayList<>();

        final String sql =
                "SELECT p.[ID], p.[name], p.[addres], p.[availablaSpaces], " +
                "       c.[ID] AS CityID, c.[cityName] AS CityName " +
                "FROM ParkingLot p " +
                "LEFT JOIN City c ON p.[cityID] = c.[ID] " +
                "ORDER BY p.[ID]";

        try (Connection conn = db.open();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                City city = null;
                int cityId = rs.getInt("CityID");
                if (!rs.wasNull()) {
                    city = new City(cityId, rs.getString("CityName"));
                }

                lots.add(new ParkingLot(
                        rs.getInt("ID"),
                        rs.getString("name"),
                        rs.getString("addres"),
                        city,
                        rs.getInt("availablaSpaces")
                ));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to load parking lots: " + e.getMessage(), e);
        }

        return lots;
    }

    private int readGeneratedId(PreparedStatement ps, Connection conn) {
        int newId = -1;

        try (ResultSet keys = ps.getGeneratedKeys()) {
            if (keys != null && keys.next()) newId = keys.getInt(1);
        } catch (Exception ignore) {}

        if (newId <= 0) {
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT @@IDENTITY")) {
                if (rs.next()) newId = rs.getInt(1);
            } catch (Exception ignore) {}
        }

        return newId;
    }
}