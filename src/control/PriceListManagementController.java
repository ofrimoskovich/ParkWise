package control;

import java.io.FileReader;
import java.sql.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import entity.PriceList;

/**
 * PriceList:
 * - Import + View only (per requirements)
 * - Writes/reads from Access when configured
 *
 * Access table: PriceList
 * Columns (per your DB):
 *   [ID] (AutoNumber), [year], [firstHourPrice], [additionalHourPrice], [fullDayPrice]
 *
 * IMPORTANT:
 * - Since ID is AutoNumber, we do NOT insert it.
 * - If JSON contains "priceListId" we keep a mapping jsonId -> dbId (in-memory).
 */
public class PriceListManagementController {

    private final Map<Integer, PriceList> priceListsByDbId = new HashMap<>();
    private final Map<Integer, Integer> jsonIdToDbId = new HashMap<>();
    private final AccessDb db;

    public PriceListManagementController(AccessDb db) {
        this.db = db;
    }

    private void ensureDb() {
        if (db == null) throw new IllegalStateException("Access DB is not configured (missing AccessDb)");
    }

    /**
     * Imports price lists from JSON and persists them in Access.
     * JSON schema:
     * { "year": 2025, "priceLists":[{"priceListId":1,"firstHourPrice":..,"additionalHourPrice":..,"fullDayPrice":..}, ...] }
     */
    public void importPriceListsFromJson(String filePath) throws Exception {
        ensureDb();

        Gson gson = new Gson();
        JsonObject root = gson.fromJson(new FileReader(filePath), JsonObject.class);

        int year = root.get("year").getAsInt();
        JsonArray lists = root.getAsJsonArray("priceLists");

        for (JsonElement element : lists) {
            JsonObject obj = element.getAsJsonObject();

            int jsonId = obj.get("priceListId").getAsInt();
            double firstHour = obj.get("firstHourPrice").getAsDouble();
            double additionalHour = obj.get("additionalHourPrice").getAsDouble();
            double fullDay = obj.get("fullDayPrice").getAsDouble();

            Integer existingDbId = jsonIdToDbId.get(jsonId);

            if (existingDbId != null) {
                updateDbRow(existingDbId, year, firstHour, additionalHour, fullDay);
                upsertInMemory(existingDbId, year, firstHour, additionalHour, fullDay);
            } else {
                int newDbId = insertDbRow(year, firstHour, additionalHour, fullDay);
                jsonIdToDbId.put(jsonId, newDbId);
                upsertInMemory(newDbId, year, firstHour, additionalHour, fullDay);
            }
        }
    }

    /** For UI table view (DB IDs). */
    public Collection<PriceList> getAllPriceLists() {
        if (db != null && priceListsByDbId.isEmpty()) {
            try {
                loadAllFromDb();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return priceListsByDbId.values();
    }

    /** Used by PriceHistoryRow join. Expects DB ID (AutoNumber). */
    public PriceList getById(int dbId) {
        PriceList cached = priceListsByDbId.get(dbId);
        if (cached != null) return cached;

        if (db != null) {
            try {
                PriceList fromDb = loadByIdFromDb(dbId);
                if (fromDb != null) priceListsByDbId.put(fromDb.getId(), fromDb);
                return fromDb;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /** Optional: mapping JSON priceListId -> DB ID (only for the current run). */
    public Integer getDbIdForJsonPriceListId(int jsonPriceListId) {
        return jsonIdToDbId.get(jsonPriceListId);
    }

    // ----------------- internal DB helpers -----------------

    private int insertDbRow(int year, double firstHour, double additionalHour, double fullDay) throws SQLException {
        final String insertSql =
                "INSERT INTO PriceList ([year],[firstHourPrice],[additionalHourPrice],[fullDayPrice]) VALUES (?,?,?,?)";

        try (Connection conn = db.open();
             PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, year);
            ps.setDouble(2, firstHour);
            ps.setDouble(3, additionalHour);
            ps.setDouble(4, fullDay);
            ps.executeUpdate();

            return fetchGeneratedId(conn, ps);
        }
    }

    private void updateDbRow(int dbId, int year, double firstHour, double additionalHour, double fullDay) throws SQLException {
        final String updateSql =
                "UPDATE PriceList SET [year]=?, [firstHourPrice]=?, [additionalHourPrice]=?, [fullDayPrice]=? WHERE [ID]=?";

        try (Connection conn = db.open();
             PreparedStatement ps = conn.prepareStatement(updateSql)) {

            ps.setInt(1, year);
            ps.setDouble(2, firstHour);
            ps.setDouble(3, additionalHour);
            ps.setDouble(4, fullDay);
            ps.setInt(5, dbId);

            ps.executeUpdate();
        }
    }

    private void loadAllFromDb() throws SQLException {
        if (db == null) return;

        final String sql =
                "SELECT [ID],[year],[firstHourPrice],[additionalHourPrice],[fullDayPrice] FROM PriceList ORDER BY [year], [ID]";

        try (Connection conn = db.open();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            priceListsByDbId.clear();

            while (rs.next()) {
                int id = rs.getInt("ID");
                int year = rs.getInt("year");
                double first = rs.getDouble("firstHourPrice");
                double add = rs.getDouble("additionalHourPrice");
                double full = rs.getDouble("fullDayPrice");
                upsertInMemory(id, year, first, add, full);
            }
        }
    }

    private PriceList loadByIdFromDb(int id) throws SQLException {
        if (db == null) return null;

        final String sql =
                "SELECT [ID],[year],[firstHourPrice],[additionalHourPrice],[fullDayPrice] FROM PriceList WHERE [ID]=?";

        try (Connection conn = db.open();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                return new PriceList(
                        rs.getInt("ID"),
                        rs.getInt("year"),
                        rs.getDouble("firstHourPrice"),
                        rs.getDouble("additionalHourPrice"),
                        rs.getDouble("fullDayPrice")
                );
            }
        }
    }

    private void upsertInMemory(int dbId, int year, double firstHour, double additionalHour, double fullDay) {
        PriceList existing = priceListsByDbId.get(dbId);
        if (existing != null) {
            existing.setYear(year);
            existing.setFirstHourPrice(firstHour);
            existing.setAdditionalHourPrice(additionalHour);
            existing.setFullDayPrice(fullDay);
        } else {
            priceListsByDbId.put(dbId, new PriceList(dbId, year, firstHour, additionalHour, fullDay));
        }
    }

    private int fetchGeneratedId(Connection conn, PreparedStatement ps) throws SQLException {
        try (ResultSet keys = ps.getGeneratedKeys()) {
            if (keys != null && keys.next()) return keys.getInt(1);
        } catch (SQLException ignored) {}

        try (PreparedStatement ps2 = conn.prepareStatement("SELECT @@IDENTITY");
             ResultSet rs = ps2.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        }

        throw new SQLException("Failed to retrieve generated ID for PriceList insert");
    }
}