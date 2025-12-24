package boundary;

import control.AccessDb;
import control.CityManagementController;
import control.ConveyorManagementController;
import control.ParkingLotManagementController;
import control.PriceHistoryManagementController;
import control.PriceListManagementController;

import javax.swing.*;
import java.sql.Connection;

public class Main {

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            AccessDb db = new AccessDb("db/parkwise_OfriMagi.accdb");

            // ✅ Check DB connection early (prevents random crashes later)
            try (Connection c = db.open()) {
                // ok
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                        "Database connection failed:\n" + e.getMessage(),
                        "DB Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            CityManagementController cityController = new CityManagementController(db);
            ParkingLotManagementController parkingLotController = new ParkingLotManagementController(db);
            ConveyorManagementController conveyorController = new ConveyorManagementController(db);
            PriceListManagementController priceListController = new PriceListManagementController(db);
            PriceHistoryManagementController priceHistoryController = new PriceHistoryManagementController(db);

            LoginUI loginUI = new LoginUI(() -> {
                ParkingLotDashboardUI dashboard =
                        new ParkingLotDashboardUI(
                                parkingLotController,
                                cityController,
                                conveyorController,
                                priceHistoryController,
                                priceListController
                        );
                dashboard.setVisible(true);
            });

            loginUI.setVisible(true);
        });
    }
}