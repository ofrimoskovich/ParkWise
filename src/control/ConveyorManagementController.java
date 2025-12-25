package control;

import entity.Conveyor;
import entity.ConveyorLastStatus;
import entity.ConveyorStatus;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ConveyorManagementController
 *
 * שינוי דרישה:
 * - בשלב ADD או MOVE: Floor/X/Y מוגדרים ל-NULL (לא 1), והמנהל לא קובע אותם.
 * - Floor/X/Y יכולים להיות NULL גם בקריאה (ResultSet) ולכן נשמרים כ-Integer ב-entity.
 *
 * שאר הלוגיקה (state machine + pending weight + lastStatus) נשארת בדיוק כמו שהיה.
 */
public class ConveyorManagementController {

    private final AccessDb db;

    // ========= In-memory state =========
    private final Map<Integer, Integer> attemptCountById = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> pendingWeightById = new ConcurrentHashMap<>();

    public ConveyorManagementController(AccessDb db) {
        this.db = db;
    }

    private void ensureDb() {
        if (db == null) throw new IllegalStateException("Access DB is not configured");
    }

    // =========================
    // CRUD
    // =========================

    public Conveyor addConveyorToParkingLot(int parkingLotId,
                                           int floorNumber, // נשמר חתימה קיימת (לא בשימוש יותר)
                                           int x,          // נשמר חתימה קיימת (לא בשימוש יותר)
                                           int y,          // נשמר חתימה קיימת (לא בשימוש יותר)
                                           int maxVehicleWeightKg,
                                           ConveyorStatus status) {
        ensureDb();

        if (parkingLotId <= 0) throw new IllegalArgumentException("ParkingLotID must be positive.");
        if (maxVehicleWeightKg <= 0) throw new IllegalArgumentException("MaxWeight must be positive.");

        if (status == null) status = ConveyorStatus.Off;

        // ✅ NEW: On creation Floor/X/Y are NULL (manager cannot set them)
        // On creation: Status=Off, LastStatus=NULL
        String sql = "INSERT INTO Conveyor ([ParkingLotID],[Floor],[X],[Y],[MaxWeight],[Status],[LastStatus]) VALUES (?,?,?,?,?,?,?)";

        try (Connection conn = db.open();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, parkingLotId);

            // Floor/X/Y => NULL
            ps.setNull(2, Types.INTEGER);
            ps.setNull(3, Types.INTEGER);
            ps.setNull(4, Types.INTEGER);

            ps.setInt(5, maxVehicleWeightKg);
            ps.setString(6, status.name());
            ps.setString(7, null);

            ps.executeUpdate();

            int newId = readGeneratedId(ps, conn);

            attemptCountById.put(newId, 0);
            pendingWeightById.remove(newId);

            // lastStatus starts as null
            return new Conveyor(newId, parkingLotId, null, null, null, maxVehicleWeightKg, status, null);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to add conveyor: " + e.getMessage(), e);
        }
    }

    public List<Conveyor> getConveyorsByParkingLot(int parkingLotId) {
        ensureDb();

        List<Conveyor> list = new ArrayList<>();
        String sql =
                "SELECT [ID],[ParkingLotID],[Floor],[X],[Y],[MaxWeight],[Status],[LastStatus] " +
                "FROM Conveyor WHERE [ParkingLotID]=? ORDER BY [ID]";

        try (Connection conn = db.open();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, parkingLotId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {

                    int id = rs.getInt("ID");
                    int lotId = rs.getInt("ParkingLotID");

                    Integer floor = getNullableInt(rs, "Floor");
                    Integer x = getNullableInt(rs, "X");
                    Integer y = getNullableInt(rs, "Y");

                    int maxW = rs.getInt("MaxWeight");

                    ConveyorStatus status = parseStatus(rs.getString("Status"));
                    ConveyorLastStatus lastStatus = safeParseLastStatus(rs.getString("LastStatus"));

                    attemptCountById.putIfAbsent(id, 0);

                    list.add(new Conveyor(id, lotId, floor, x, y, maxW, status, lastStatus));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to load conveyors: " + e.getMessage(), e);
        }

        return list;
    }

    public void moveConveyorToParkingLot(int conveyorId, int newParkingLotId) {
        ensureDb();

        requirePositiveId(conveyorId);
        if (newParkingLotId <= 0) throw new IllegalArgumentException("New ParkingLotID must be positive.");

        // ✅ NEW: when moving, Floor/X/Y become NULL (manager cannot set)
        String sql = "UPDATE Conveyor SET [ParkingLotID]=?, [X]=?, [Y]=?, [Floor]=? WHERE [ID]=?";

        try (Connection conn = db.open();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, newParkingLotId);

            ps.setNull(2, Types.INTEGER);
            ps.setNull(3, Types.INTEGER);
            ps.setNull(4, Types.INTEGER);

            ps.setInt(5, conveyorId);

            int updated = ps.executeUpdate();
            if (updated == 0) throw new IllegalArgumentException("Conveyor not found: " + conveyorId);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to move conveyor: " + e.getMessage(), e);
        }
    }

    public void deleteConveyor(int conveyorId) {
        ensureDb();
        requirePositiveId(conveyorId);

        String sql = "DELETE FROM Conveyor WHERE [ID]=?";

        try (Connection conn = db.open();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, conveyorId);

            int deleted = ps.executeUpdate();
            if (deleted == 0) throw new IllegalArgumentException("Conveyor not found: " + conveyorId);

            attemptCountById.remove(conveyorId);
            pendingWeightById.remove(conveyorId);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete conveyor: " + e.getMessage(), e);
        }
    }

    // =========================
    // Diagram-based events
    // =========================

    public void decideChangeMaxWeight(int conveyorId, int newWeight) {
        ensureDb();
        requirePositiveId(conveyorId);
        requirePositiveWeight(newWeight);

        Conveyor c = getConveyorById(conveyorId);
        if (c.getStatus() != ConveyorStatus.Off) {
            throw new IllegalStateException("Max weight change can be decided ONLY when conveyor is OFF.");
        }
        pendingWeightById.put(conveyorId, newWeight);
    }

    public void confirmChangeMaxWeight(int conveyorId) {
        ensureDb();
        requirePositiveId(conveyorId);

        Conveyor c = getConveyorById(conveyorId);
        if (c.getStatus() != ConveyorStatus.Off) {
            throw new IllegalStateException("Max weight can be confirmed ONLY when conveyor is OFF.");
        }

        Integer pending = pendingWeightById.get(conveyorId);
        if (pending == null) {
            throw new IllegalStateException("No pending max weight change for this conveyor.");
        }

        updateConveyorMaxWeight_DBOnly(conveyorId, pending);
        pendingWeightById.remove(conveyorId);
    }

    public Integer getPendingWeight(int conveyorId) {
        return pendingWeightById.get(conveyorId);
    }

    public void turnOnConveyors(int conveyorId) {
        ensureDb();
        requirePositiveId(conveyorId);

        Conveyor c = getConveyorById(conveyorId);
        if (c.getStatus() != ConveyorStatus.Off) {
            throw new IllegalStateException("Turn ON is allowed only from OFF state.");
        }
        if (pendingWeightById.containsKey(conveyorId)) {
            throw new IllegalStateException("Cannot turn ON while there is a pending weight change (confirm it first).");
        }

        attemptCountById.put(conveyorId, 0);
        updateConveyorStatus_WithHistoryRule(conveyorId, ConveyorStatus.Testing);
    }

    public void restart(int conveyorId) {
        ensureDb();
        requirePositiveId(conveyorId);

        Conveyor c = getConveyorById(conveyorId);
        if (c.getStatus() != ConveyorStatus.Paused) {
            throw new IllegalStateException("Restart is allowed only from PAUSED.");
        }

        attemptCountById.put(conveyorId, 0);
        updateConveyorStatus_WithHistoryRule(conveyorId, ConveyorStatus.Testing);
    }

    public void turnOffConveyors(int conveyorId) {
        ensureDb();
        requirePositiveId(conveyorId);

        Conveyor c = getConveyorById(conveyorId);
        if (c.getStatus() != ConveyorStatus.Operational) {
            throw new IllegalStateException("Turn OFF is allowed only from OPERATION (Operational).");
        }

        updateConveyorStatus_WithHistoryRule(conveyorId, ConveyorStatus.Off);
    }

    public void pause(int conveyorId) {
        ensureDb();
        requirePositiveId(conveyorId);
        throw new UnsupportedOperationException(
                "Pause is not allowed manually. Paused state is entered by hardware/external events only.");
    }

    public void updateConveyorStatus(int conveyorId, ConveyorStatus status) {
        ensureDb();
        requirePositiveId(conveyorId);
        throw new UnsupportedOperationException(
                "Direct status update is not allowed. Use: turnOnConveyors / restart / turnOffConveyors");
    }

    // =========================
    // DB-only helpers
    // =========================

    private void updateConveyorMaxWeight_DBOnly(int conveyorId, int newWeight) {
        String sql = "UPDATE Conveyor SET [MaxWeight]=? WHERE [ID]=?";

        try (Connection conn = db.open();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, newWeight);
            ps.setInt(2, conveyorId);

            int updated = ps.executeUpdate();
            if (updated == 0) throw new IllegalArgumentException("Conveyor not found: " + conveyorId);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update conveyor max weight: " + e.getMessage(), e);
        }
    }

    /**
     * Rule:
     * - LastStatus becomes the CURRENT Status (only if current is Testing/Operational)
     * - EXCEPT when switching to Off or Paused -> do NOT change LastStatus
     * - LastStatus never becomes Off/Paused (DB stores only Testing/Operational)
     */
    private void updateConveyorStatus_WithHistoryRule(int conveyorId, ConveyorStatus newStatus) {
        String newText = (newStatus == null ? null : newStatus.name());

        String sql =
                "UPDATE Conveyor " +
                "SET " +
                "  [LastStatus] = IIF( " +
                "       ([Status] IN ('Testing','Operational')) " +
                "       AND (? NOT IN ('Off','Paused')), " +
                "       [Status], " +
                "       [LastStatus] " +
                "  ), " +
                "  [Status] = ? " +
                "WHERE [ID] = ?";

        try (Connection conn = db.open();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newText);
            ps.setString(2, newText);
            ps.setInt(3, conveyorId);

            int updated = ps.executeUpdate();
            if (updated == 0) throw new IllegalArgumentException("Conveyor not found: " + conveyorId);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update conveyor status: " + e.getMessage(), e);
        }
    }

    private Conveyor getConveyorById(int id) {
        ensureDb();

        String sql =
                "SELECT [ID],[ParkingLotID],[Floor],[X],[Y],[MaxWeight],[Status],[LastStatus] " +
                "FROM Conveyor WHERE [ID]=?";

        try (Connection conn = db.open();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Conveyor not found: " + id);

                int lotId = rs.getInt("ParkingLotID");

                Integer floor = getNullableInt(rs, "Floor");
                Integer x = getNullableInt(rs, "X");
                Integer y = getNullableInt(rs, "Y");

                int maxW = rs.getInt("MaxWeight");

                ConveyorStatus status = parseStatus(rs.getString("Status"));
                ConveyorLastStatus lastStatus = safeParseLastStatus(rs.getString("LastStatus"));

                return new Conveyor(id, lotId, floor, x, y, maxW, status, lastStatus);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to read conveyor: " + e.getMessage(), e);
        }
    }

    private ConveyorStatus parseStatus(String st) {
        if (st == null || st.isBlank()) return ConveyorStatus.Off;
        try { return ConveyorStatus.valueOf(st.trim()); }
        catch (Exception ignore) { return ConveyorStatus.Off; }
    }

    private ConveyorLastStatus safeParseLastStatus(String s) {
        if (s == null || s.isBlank()) return null;
        try { return ConveyorLastStatus.valueOf(s.trim()); }
        catch (Exception ignore) { return null; }
    }

    private void requirePositiveId(int id) {
        if (id <= 0) throw new IllegalArgumentException("Conveyor ID must be positive.");
    }

    private void requirePositiveWeight(int w) {
        if (w <= 0) throw new IllegalArgumentException("MaxWeight must be positive.");
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

    /**
     * קריאה בטוחה של INT nullable מתוך ResultSet.
     */
    private Integer getNullableInt(ResultSet rs, String col) throws SQLException {
        Object o = rs.getObject(col);
        if (o == null) return null;
        return ((Number) o).intValue();
    }
}
