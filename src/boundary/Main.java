package boundary;

import control.AccessDb;
import control.CityManagementController;
import control.ConveyorManagementController;
import control.ParkingLotManagementController;
import control.PriceHistoryManagementController;
import control.PriceListManagementController;

import javax.swing.SwingUtilities;

public class Main {

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            // ===== Initialize DB =====
            AccessDb db = new AccessDb("db/parkwise_OfriMagi.accdb");

            // ===== Initialize Controllers =====
            CityManagementController cityController =
                    new CityManagementController(db);

            ParkingLotManagementController parkingLotController =
                    new ParkingLotManagementController(db);

            ConveyorManagementController conveyorController =
                    new ConveyorManagementController(db);

            PriceListManagementController priceListController =
                    new PriceListManagementController(db);

            PriceHistoryManagementController priceHistoryController =
                    new PriceHistoryManagementController(db);

            // ===== Open Login UI =====
            LoginUI loginUI = new LoginUI(() -> {

                // on login success → open dashboard
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
