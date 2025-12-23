package control;

import entity.Conveyor;
import entity.ConveyorStatus;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ConveyorManagementController {

    private final AccessDb db;

    // ========= In-memory state (no DB schema changes) =========
    private final Map<Integer, Integer> attemptCountById = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> pendingWeightById = new ConcurrentHashMap<>();
    private final Set<Integer> mechSuccess = ConcurrentHashMap.newKeySet();
    private final Set<Integer> elecSuccess = ConcurrentHashMap.newKeySet();

    public ConveyorManagementController(AccessDb db) {
        this.db = db;
    }

    private void ensureDb() {
        if (db == null) throw new IllegalStateException("Access DB is not configured");
    }

    // =========================
    // CRUD
    // =========================

    // ===== CREATE =====
    public Conveyor addConveyorToParkingLot(int parkingLotId,
                                           int floorNumber,
                                           int x,
                                           int y,
                                           int maxVehicleWeightKg,
                                           ConveyorStatus status) {
        ensureDb();

        if (parkingLotId <= 0) throw new IllegalArgumentException("ParkingLotID must be positive.");
        if (floorNumber <= 0) throw new IllegalArgumentException("Floor must be positive.");
        if (x <= 0) throw new IllegalArgumentException("X must be positive.");
        if (y <= 0) throw new IllegalArgumentException("Y must be positive.");
        if (maxVehicleWeightKg <= 0) throw new IllegalArgumentException("MaxWeight must be positive.");

        // According to the diagram, it should start in OFF.
        if (status == null) status = ConveyorStatus.Off;

        String sql = "INSERT INTO Conveyor ([ParkingLotID],[Floor],[X],[Y],[MaxWeight],[Status]) VALUES (?,?,?,?,?,?)";

        try (Connection conn = db.open();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, parkingLotId);
            ps.setInt(2, floorNumber);
            ps.setInt(3, x);
            ps.setInt(4, y);
            ps.setInt(5, maxVehicleWeightKg);
            ps.setString(6, status.name());

            ps.executeUpdate();

            int newId = readGeneratedId(ps, conn);

            // init in-memory state
            attemptCountById.put(newId, 0);
            pendingWeightById.remove(newId);
            mechSuccess.remove(newId);
            elecSuccess.remove(newId);

            return new Conveyor(newId, parkingLotId, floorNumber, x, y, maxVehicleWeightKg, status);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to add conveyor: " + e.getMessage(), e);
        }
    }

    // ===== READ (by parking lot) =====
    public List<Conveyor> getConveyorsByParkingLot(int parkingLotId) {
        ensureDb();

        List<Conveyor> list = new ArrayList<>();
        String sql =
                "SELECT [ID],[ParkingLotID],[Floor],[X],[Y],[MaxWeight],[Status] " +
                        "FROM Conveyor WHERE [ParkingLotID]=? ORDER BY [ID]";

        try (Connection conn = db.open();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, parkingLotId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("ID");
                    int lotId = rs.getInt("ParkingLotID");
                    int floor = rs.getInt("Floor");
                    int x = rs.getInt("X");
                    int y = rs.getInt("Y");
                    int maxW = rs.getInt("MaxWeight");

                    ConveyorStatus status = parseStatus(rs.getString("Status"));

                    // default in-memory init if not exist
                    attemptCountById.putIfAbsent(id, 0);

                    list.add(new Conveyor(id, lotId, floor, x, y, maxW, status));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to load conveyors: " + e.getMessage(), e);
        }

        return list;
    }

    // ===== MOVE (reset X,Y,Floor to 1,1,1) =====
    public void moveConveyorToParkingLot(int conveyorId, int newParkingLotId) {
        ensureDb();

        if (conveyorId <= 0) throw new IllegalArgumentException("Conveyor ID must be positive.");
        if (newParkingLotId <= 0) throw new IllegalArgumentException("New ParkingLotID must be positive.");

        String sql = "UPDATE Conveyor SET [ParkingLotID]=?, [X]=?, [Y]=?, [Floor]=? WHERE [ID]=?";

        try (Connection conn = db.open();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, newParkingLotId);
            ps.setInt(2, 1);
            ps.setInt(3, 1);
            ps.setInt(4, 1);
            ps.setInt(5, conveyorId);

            int updated = ps.executeUpdate();
            if (updated == 0) throw new IllegalArgumentException("Conveyor not found: " + conveyorId);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to move conveyor: " + e.getMessage(), e);
        }
    }

    // ===== DELETE =====
    public void deleteConveyor(int conveyorId) {
        ensureDb();

        String sql = "DELETE FROM Conveyor WHERE [ID]=?";

        try (Connection conn = db.open();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, conveyorId);

            int deleted = ps.executeUpdate();
            if (deleted == 0) throw new IllegalArgumentException("Conveyor not found: " + conveyorId);

            // cleanup in-memory state
            attemptCountById.remove(conveyorId);
            pendingWeightById.remove(conveyorId);
            mechSuccess.remove(conveyorId);
            elecSuccess.remove(conveyorId);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete conveyor: " + e.getMessage(), e);
        }
    }

    // =========================
    // Diagram-based events
    // =========================

    // ----- OFF: Weight change (two-step) -----

    // evDecideChangeMaxWeight(newW)
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

    // evConfirmChangeMaxWeight / maxWeight=newW
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

    // ----- OFF -> ON.Testing -----

    // IdleOff --> On : evTurnOnConveyors [no pending weight change] / attemptCount=0
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
        resetTests(conveyorId);

        updateConveyorStatus_DBOnly(conveyorId, ConveyorStatus.Testing);
    }

    // ----- Testing -> Operation (Operational) -----

    // user simulates mechanical branch success
    public void markMechanicalOk(int conveyorId) {
        ensureDb();
        requirePositiveId(conveyorId);
        requireStatus(conveyorId, ConveyorStatus.Testing, "Mechanical OK can be marked only in TESTING.");
        mechSuccess.add(conveyorId);
    }

    // user simulates electronic branch success
    public void markElectronicOk(int conveyorId) {
        ensureDb();
        requirePositiveId(conveyorId);
        requireStatus(conveyorId, ConveyorStatus.Testing, "Electronic OK can be marked only in TESTING.");
        elecSuccess.add(conveyorId);
    }

    // Testing --> Operation : [in(MechSuccess) && in(ElecSuccess)]
    public void attemptFinishTesting(int conveyorId) {
        ensureDb();
        requirePositiveId(conveyorId);
        requireStatus(conveyorId, ConveyorStatus.Testing, "Finishing tests is allowed only in TESTING.");

        if (!mechSuccess.contains(conveyorId) || !elecSuccess.contains(conveyorId)) {
            throw new IllegalStateException("Cannot move to OPERATION: both Mechanical and Electronic tests must succeed.");
        }

        attemptCountById.put(conveyorId, 0);
        updateConveyorStatus_DBOnly(conveyorId, ConveyorStatus.Operational);
    }

    // ----- Timeout / Fail -----

    // evTimeout10m
    public void timeout10m(int conveyorId) {
        ensureDb();
        requirePositiveId(conveyorId);
        requireStatus(conveyorId, ConveyorStatus.Testing, "Timeout can happen only in TESTING.");

        int attempts = attemptCountById.getOrDefault(conveyorId, 0);

        if (attempts < 2) {
            attemptCountById.put(conveyorId, attempts + 1);
            resetTests(conveyorId); // resetTests()
            // remains in Testing
        } else {
            updateConveyorStatus_DBOnly(conveyorId, ConveyorStatus.Paused);
        }
    }

    // evTestFail
    public void testFailed(int conveyorId) {
        ensureDb();
        requirePositiveId(conveyorId);
        requireStatus(conveyorId, ConveyorStatus.Testing, "Test failure can happen only in TESTING.");
        updateConveyorStatus_DBOnly(conveyorId, ConveyorStatus.Paused);
    }

    // ----- Pause / Restart -----

 // ----- Pause (NOT allowed from UI) -----
    public void pause(int conveyorId) {
        ensureDb();
        requirePositiveId(conveyorId);

        // לפי הדרישה שלך: PAUSED הוא מצב שנכנסים אליו רק "חומרתית"/חיצונית
        // ולכן למנהל אין אפשרות להעביר ל-Paused.
        throw new UnsupportedOperationException(
                "Pause is not allowed manually. Paused state is entered by hardware/external events only.");
    }


    // Paused --> H_On : evRestart / startTimer(10min)
    public void restart(int conveyorId) {
        ensureDb();
        requirePositiveId(conveyorId);
        requireStatus(conveyorId, ConveyorStatus.Paused, "Restart is allowed only from PAUSED.");

        attemptCountById.put(conveyorId, 0);
        resetTests(conveyorId);
        updateConveyorStatus_DBOnly(conveyorId, ConveyorStatus.Testing);
    }

    // ----- Turn off (only from Operation.Ready, simplified as Operational) -----

    // Operation.Ready --> Off : evTurnOffConveyors
    public void turnOffConveyors(int conveyorId) {
        ensureDb();
        requirePositiveId(conveyorId);
        requireStatus(conveyorId, ConveyorStatus.Operational, "Turn OFF is allowed only from OPERATION (Operational).");

        updateConveyorStatus_DBOnly(conveyorId, ConveyorStatus.Off);
    }

    public int getAttemptCount(int conveyorId) {
        return attemptCountById.getOrDefault(conveyorId, 0);
    }

    public boolean isMechanicalSuccess(int conveyorId) {
        return mechSuccess.contains(conveyorId);
    }

    public boolean isElectronicSuccess(int conveyorId) {
        return elecSuccess.contains(conveyorId);
    }

    // =========================
    // Existing methods kept but made safer
    // =========================

    // (Kept for internal use; UI should NOT call it freely)
    public void updateConveyorMaxWeight(int conveyorId, int newWeight) {
        ensureDb();
        requirePositiveId(conveyorId);
        requirePositiveWeight(newWeight);

        Conveyor c = getConveyorById(conveyorId);
        if (c.getStatus() != ConveyorStatus.Off)
            throw new IllegalStateException("Max weight can be changed only when conveyor is OFF.");

        updateConveyorMaxWeight_DBOnly(conveyorId, newWeight);
    }

    // (Kept but strongly restricted to prevent bypassing)
    public void updateConveyorStatus(int conveyorId, ConveyorStatus status) {
        ensureDb();
        requirePositiveId(conveyorId);

        // We block generic status setting that can bypass the diagram rules.
        throw new UnsupportedOperationException(
                "Direct status update is not allowed. Use: turnOnConveyors / attemptFinishTesting / pause / restart / turnOffConveyors / timeout10m / testFailed");
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

    private void updateConveyorStatus_DBOnly(int conveyorId, ConveyorStatus status) {
        String sql = "UPDATE Conveyor SET [Status]=? WHERE [ID]=?";

        try (Connection conn = db.open();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status == null ? null : status.name());
            ps.setInt(2, conveyorId);

            int updated = ps.executeUpdate();
            if (updated == 0) throw new IllegalArgumentException("Conveyor not found: " + conveyorId);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update conveyor status: " + e.getMessage(), e);
        }
    }

    // ===== helper: read a single conveyor (for rules) =====
    private Conveyor getConveyorById(int id) {
        ensureDb();

        String sql =
                "SELECT [ID],[ParkingLotID],[Floor],[X],[Y],[MaxWeight],[Status] " +
                        "FROM Conveyor WHERE [ID]=?";

        try (Connection conn = db.open();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Conveyor not found: " + id);

                int lotId = rs.getInt("ParkingLotID");
                int floor = rs.getInt("Floor");
                int x = rs.getInt("X");
                int y = rs.getInt("Y");
                int maxW = rs.getInt("MaxWeight");

                ConveyorStatus status = parseStatus(rs.getString("Status"));

                return new Conveyor(id, lotId, floor, x, y, maxW, status);
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

    private void resetTests(int conveyorId) {
        mechSuccess.remove(conveyorId);
        elecSuccess.remove(conveyorId);
    }

    private void requireStatus(int conveyorId, ConveyorStatus expected, String msg) {
        Conveyor c = getConveyorById(conveyorId);
        if (c.getStatus() != expected) throw new IllegalStateException(msg);
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
}
