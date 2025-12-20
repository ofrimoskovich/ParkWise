package boundary;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import control.ConveyorManagementController;
import control.ParkingLotManagementController;
import control.PriceHistoryManagementController;
import control.PriceListManagementController;
import entity.ParkingLot;

/**
 * Main form: choose ParkingLot and open the details form.
 */
public class ParkingLotDashboardUI extends JFrame {

    private final ParkingLotManagementController parkingLotController;
    private final ConveyorManagementController conveyorController;
    private final PriceListManagementController priceListController;
    private final PriceHistoryManagementController priceHistoryController;

    private JTable table;
    private DefaultTableModel model;

    private JTextField idField, nameField, addressField, cityField, spacesField;

    public ParkingLotDashboardUI(ParkingLotManagementController pl,
                                ConveyorManagementController cc,
                                PriceListManagementController plc,
                                PriceHistoryManagementController phc) {
        this.parkingLotController = pl;
        this.conveyorController = cc;
        this.priceListController = plc;
        this.priceHistoryController = phc;

        initUI();
        reload();
    }

    private void initUI() {
        setTitle("ParkWise – Parking Lots");
        setSize(980, 520);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        model = new DefaultTableModel(new Object[] { "ID", "Name", "Address", "City", "AvailableSpaces" }, 0) {
            private static final long serialVersionUID = 1L;
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel form = new JPanel(new GridLayout(3, 4, 6, 6));
        idField = new JTextField();
        nameField = new JTextField();
        addressField = new JTextField();
        cityField = new JTextField();
        spacesField = new JTextField();

        form.add(new JLabel("ID"));
        form.add(idField);
        form.add(new JLabel("Name"));
        form.add(nameField);

        form.add(new JLabel("Address"));
        form.add(addressField);
        form.add(new JLabel("City"));
        form.add(cityField);

        form.add(new JLabel("Available Spaces"));
        form.add(spacesField);

        JButton addBtn = new JButton("Add");
        addBtn.addActionListener(e -> runSafely(this::addLot));

        JButton updateBtn = new JButton("Update");
        updateBtn.addActionListener(e -> runSafely(this::updateLot));

        JButton deleteBtn = new JButton("Delete");
        deleteBtn.addActionListener(e -> runSafely(this::deleteLot));

        JButton reloadBtn = new JButton("Reload");
        reloadBtn.addActionListener(e -> runSafely(this::reload));

        JButton openBtn = new JButton("Open Details…");
        openBtn.addActionListener(e -> runSafely(this::openSelectedDetails));

        JPanel buttons = new JPanel(new GridLayout(1, 5, 6, 6));
        buttons.add(addBtn);
        buttons.add(updateBtn);
        buttons.add(deleteBtn);
        buttons.add(reloadBtn);
        buttons.add(openBtn);

        JPanel south = new JPanel(new BorderLayout());
        south.add(form, BorderLayout.CENTER);
        south.add(buttons, BorderLayout.SOUTH);

        add(south, BorderLayout.SOUTH);
    }

    private void runSafely(Runnable action) {
        try {
            action.run();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage() == null ? ex.toString() : ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private Integer parseIntOrNull(String text, String fieldName) {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Missing value: " + fieldName, "Validation",
                    JOptionPane.WARNING_MESSAGE);
            return null;
        }
        try {
            return Integer.parseInt(t);
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Invalid number in " + fieldName + ": " + t, "Validation",
                    JOptionPane.WARNING_MESSAGE);
            return null;
        }
    }

    private Integer selectedLotIdOrNull() {
        int row = table.getSelectedRow();
        if (row >= 0) {
            Object v = model.getValueAt(row, 0);
            if (v instanceof Integer) return (Integer) v;
            return Integer.valueOf(v.toString());
        }
        return parseIntOrNull(idField.getText(), "ParkingLot ID");
    }

    private void reload() {
        model.setRowCount(0);
        List<ParkingLot> lots = parkingLotController.getAllParkingLots();
        for (ParkingLot p : lots) {
            model.addRow(new Object[] { p.getId(), p.getName(), p.getAddress(), p.getCity(), p.getAvailableSpaces() });
        }
    }

    private void addLot() {
        Integer id = parseIntOrNull(idField.getText(), "ID");
        Integer spaces = parseIntOrNull(spacesField.getText(), "Available Spaces");
        if (id == null || spaces == null) return;

        parkingLotController.addParkingLot(id, nameField.getText().trim(), addressField.getText().trim(),
                cityField.getText().trim(), spaces);
        reload();
    }

    private void updateLot() {
        Integer id = selectedLotIdOrNull();
        Integer spaces = parseIntOrNull(spacesField.getText(), "Available Spaces");
        if (id == null || spaces == null) return;

        parkingLotController.updateParkingLot(id, nameField.getText().trim(), addressField.getText().trim(),
                cityField.getText().trim(), spaces);
        reload();
    }

    private void deleteLot() {
        Integer id = selectedLotIdOrNull();
        if (id == null) return;

        parkingLotController.deleteParkingLot(id);
        reload();
    }

    private void openSelectedDetails() {
        Integer id = selectedLotIdOrNull();
        if (id == null) return;

        ParkingLotMainUI ui = new ParkingLotMainUI(parkingLotController, conveyorController, priceListController, priceHistoryController);
        ui.setAndLoadLotId(id);
        ui.setVisible(true);
    }
}
