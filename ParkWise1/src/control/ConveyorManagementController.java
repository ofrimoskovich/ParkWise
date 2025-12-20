package control;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import entity.Conveyor;
import entity.ConveyorStatus;

public class ConveyorManagementController {

    private final AccessDb db;

    public ConveyorManagementController(AccessDb db) {
        this.db = db;
    }

    private void ensureDb() {
        if (db == null) throw new IllegalStateException("Access DB is not configured");
    }

    // ✅ ADD בלי ID (AutoNumber)
    public Conveyor addConveyorToParkingLot(int parkingLotId,
                                            int floorNumber,
                                            int x,
                                            int y,
                                            int maxVehicleWeightKg,
                                            ConveyorStatus status) {
        ensureDb();

        String sql = "INSERT INTO Conveyor ([ParkingLotID],[Floor],[X],[Y],[MaxWeight],[Status]) VALUES (?,?,?,?,?,?)";

        try (Connection conn = db.open();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, parkingLotId);
            ps.setInt(2, floorNumber);
            ps.setInt(3, x);
            ps.setInt(4, y);
            ps.setInt(5, maxVehicleWeightKg);
            ps.setString(6, status == null ? null : status.name());
            ps.executeUpdate();

            int newId = -1;

            // ניסיון 1: Generated Keys
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys != null && keys.next()) {
                    newId = keys.getInt(1);
                }
            } catch (Exception ignore) {}

            // ניסיון 2 (Access): @@IDENTITY
            if (newId <= 0) {
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT @@IDENTITY")) {
                    if (rs.next()) newId = rs.getInt(1);
                }
            }

            if (newId <= 0) {
                throw new RuntimeException("Insert succeeded but could not read generated ID");
            }

            return new Conveyor(newId, parkingLotId, floorNumber, x, y, maxVehicleWeightKg, status);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to add conveyor: " + e.getMessage(), e);
        }
    }

    public List<Conveyor> getConveyorsByParkingLot(int parkingLotId) {
        ensureDb();

        List<Conveyor> list = new ArrayList<>();
        String sql = "SELECT [ID],[ParkingLotID],[Floor],[X],[Y],[MaxWeight],[Status] FROM Conveyor WHERE [ParkingLotID]=? ORDER BY [ID]";

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

                    String st = rs.getString("Status");
                    ConveyorStatus status = null;
                    try { if (st != null) status = ConveyorStatus.valueOf(st); } catch (Exception ignore) {}

                    list.add(new Conveyor(id, lotId, floor, x, y, maxW, status));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to load conveyors: " + e.getMessage(), e);
        }

        return list;
    }

    public void updateConveyor(int conveyorId, int floorNumber, int x, int y, int maxVehicleWeightKg) {
        ensureDb();

        String sql = "UPDATE Conveyor SET [Floor]=?, [X]=?, [Y]=?, [MaxWeight]=? WHERE [ID]=?";

        try (Connection conn = db.open();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, floorNumber);
            ps.setInt(2, x);
            ps.setInt(3, y);
            ps.setInt(4, maxVehicleWeightKg);
            ps.setInt(5, conveyorId);

            int updated = ps.executeUpdate();
            if (updated == 0) throw new IllegalArgumentException("Conveyor not found: " + conveyorId);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update conveyor: " + e.getMessage(), e);
        }
    }

    public void updateConveyorStatus(int conveyorId, ConveyorStatus status) {
        ensureDb();

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

    public void deleteConveyor(int conveyorId) {
        ensureDb();

        String sql = "DELETE FROM Conveyor WHERE [ID]=?";

        try (Connection conn = db.open();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, conveyorId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete conveyor: " + e.getMessage(), e);
        }
    }
}