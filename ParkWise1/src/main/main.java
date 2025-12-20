package main;

import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;

import boundary.ParkingLotDashboardUI;
import control.AccessDb;
import control.ConveyorManagementController;
import control.ParkingLotManagementController;
import control.PriceHistoryManagementController;
import control.PriceListManagementController;

/**
 * Entry point.
 *
 * To work with the real Access DB:
 *  - Make sure UCanAccess jars are on the classpath at runtime.
 *  - Pick the .accdb file when prompted (defaults to parkwise_OfriMagi.accdb if found).
 */
public class main {
    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            String accdbPath = tryDefaultAccdbPath();
            if (accdbPath == null) {
                accdbPath = chooseAccdbPath();
            }
            if (accdbPath == null) {
                // user cancelled
                return;
            }

            AccessDb db = new AccessDb(accdbPath);

            ParkingLotManagementController parkingLotController = new ParkingLotManagementController(db);
            ConveyorManagementController conveyorController = new ConveyorManagementController(db);
            PriceListManagementController priceListController = new PriceListManagementController(db);
            PriceHistoryManagementController priceHistoryController = new PriceHistoryManagementController(db, priceListController);

            new ParkingLotDashboardUI(parkingLotController, conveyorController, priceListController, priceHistoryController)
                    .setVisible(true);
        });
    }

    private static String tryDefaultAccdbPath() {
        // Try project root (when running from Eclipse: user.dir is project folder or workspace)
        File f1 = new File("parkwise_OfriMagi.accdb");
        if (f1.exists()) return f1.getAbsolutePath();

        // Try one level up
        File f2 = new File("ParkWise1/parkwise_OfriMagi.accdb");
        if (f2.exists()) return f2.getAbsolutePath();

        return null;
    }

    private static String chooseAccdbPath() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select ParkWise .accdb database file");
        int result = chooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile().getAbsolutePath();
        }
        return null;
    }
}
