package control;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import entity.City;

/**
 * CityManagementController
 * ------------------------
 * CRUD + lookup for City table in Access DB.
 *
 * Table expected:
 *   City(ID AUTONUMBER PK, cityName TEXT)
 */
public class CityManagementController {

    private final AccessDb db;

    public CityManagementController(AccessDb db) {
        this.db = db;
    }

    private void ensureDb() {
        if (db == null) throw new IllegalStateException("Access DB is not configured");
    }

    /**
     * Loads all cities ordered by ID.
     */
    public List<City> getAllCities() {
        ensureDb();

        List<City> list = new ArrayList<>();
        final String sql = "SELECT [ID], [cityName] FROM City ORDER BY [ID]";

        try (Connection conn = db.open();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(new City(rs.getInt("ID"), rs.getString("cityName")));
            }
            return list;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to load cities: " + e.getMessage(), e);
        }
    }

    /**
     * Inserts a new city (ID is AutoNumber) and returns the created City with generated ID.
     */
    public City addCity(String cityName) {
        ensureDb();

        String name = cityName == null ? "" : cityName.trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("City name is required.");
        }

        final String sql = "INSERT INTO City ([cityName]) VALUES (?)";

        try (Connection conn = db.open();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, name);
            ps.executeUpdate();

            int newId = readGeneratedId(ps, conn);
            if (newId <= 0) throw new RuntimeException("Insert succeeded but could not read generated ID");

            return new City(newId, name);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to add city: " + e.getMessage(), e);
        }
    }

    /**
     * Updates city name (ID cannot change).
     */
    public void updateCityName(int cityId, String newName) {
        ensureDb();

        String name = newName == null ? "" : newName.trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("City name is required.");
        }

        final String sql = "UPDATE City SET [cityName]=? WHERE [ID]=?";

        try (Connection conn = db.open();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setInt(2, cityId);

            int updated = ps.executeUpdate();
            if (updated == 0) throw new IllegalArgumentException("City not found: " + cityId);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update city: " + e.getMessage(), e);
        }
    }

    /**
     * Finds a city by exact name (case-insensitive). Returns null if not found.
     */
    public City findCityByName(String cityName) {
        ensureDb();

        String name = cityName == null ? "" : cityName.trim();
        if (name.isEmpty()) return null;

        final String sql = "SELECT [ID], [cityName] FROM City WHERE LCASE([cityName]) = LCASE(?)";

        try (Connection conn = db.open();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new City(rs.getInt("ID"), rs.getString("cityName"));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find city: " + e.getMessage(), e);
        }
    }

    /**
     * Utility to read Access generated AutoNumber ID.
     */
    private int readGeneratedId(PreparedStatement ps, Connection conn) {
        int newId = -1;

        // Attempt #1: JDBC generated keys
        try (ResultSet keys = ps.getGeneratedKeys()) {
            if (keys != null && keys.next()) newId = keys.getInt(1);
        } catch (Exception ignore) {}

        // Attempt #2: Access identity
        if (newId <= 0) {
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT @@IDENTITY")) {
                if (rs.next()) newId = rs.getInt(1);
            } catch (Exception ignore) {}
        }
        return newId;
    }
}