package boundary;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

import control.ConveyorManagementController;
import entity.Conveyor;
import entity.ConveyorStatus;

/**
 * Conveyor CRUD by ParkingLot.
 * Access DB: Conveyor.ID is AutoNumber -> Add WITHOUT ID.
 */
public class ConveyorManagementUI extends JFrame {

    private final ConveyorManagementController controller;
    private final int parkingLotId;

    private JTable table;
    private DefaultTableModel model;

    private JTextField idField, floorField, xField, yField, weightField;
    private JComboBox<ConveyorStatus> statusBox;

    public ConveyorManagementUI(ConveyorManagementController controller, int parkingLotId) {
        this.controller = controller;
        this.parkingLotId = parkingLotId;
        initUI();
        loadConveyors();
    }

    private void initUI() {
        setTitle("Conveyors (ParkingLot " + parkingLotId + ")");
        setSize(860, 440);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        model = new DefaultTableModel(new Object[] { "ID", "Floor", "X", "Y", "MaxWeightKg", "Status" }, 0) {
            private static final long serialVersionUID = 1L;
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };

        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Fill fields when selecting a row
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) return;
                int row = table.getSelectedRow();
                if (row < 0) return;

                idField.setText(String.valueOf(model.getValueAt(row, 0)));
                floorField.setText(String.valueOf(model.getValueAt(row, 1)));
                xField.setText(String.valueOf(model.getValueAt(row, 2)));
                yField.setText(String.valueOf(model.getValueAt(row, 3)));
                weightField.setText(String.valueOf(model.getValueAt(row, 4)));

                try {
                    Object st = model.getValueAt(row, 5);
                    if (st != null) statusBox.setSelectedItem(ConveyorStatus.valueOf(st.toString()));
                } catch (Exception ignore) {}
            }
        });

        JPanel form = new JPanel(new GridLayout(4, 4, 6, 6));

        idField = new JTextField();
        idField.setEditable(false); // ID comes from DB (AutoNumber)
        floorField = new JTextField();
        xField = new JTextField();
        yField = new JTextField();
        weightField = new JTextField();
        statusBox = new JComboBox<>(ConveyorStatus.values());

        form.add(new JLabel("ID (AutoNumber)"));
        form.add(idField);
        form.add(new JLabel("Floor"));
        form.add(floorField);

        form.add(new JLabel("X"));
        form.add(xField);
        form.add(new JLabel("Y"));
        form.add(yField);

        form.add(new JLabel("Max Weight (Kg)"));
        form.add(weightField);
        form.add(new JLabel("Status"));
        form.add(statusBox);

        JButton addBtn = new JButton("Add (ID auto)");
        addBtn.addActionListener(e -> runSafely(this::addConveyor));

        JButton updateBtn = new JButton("Update");
        updateBtn.addActionListener(e -> runSafely(this::updateConveyor));

        JButton deleteBtn = new JButton("Delete");
        deleteBtn.addActionListener(e -> runSafely(this::deleteConveyor));

        JButton setStatusBtn = new JButton("Set Status");
        setStatusBtn.addActionListener(e -> runSafely(this::setStatus));

        JButton reloadBtn = new JButton("Reload");
        reloadBtn.addActionListener(e -> runSafely(this::loadConveyors));

        JPanel buttons = new JPanel(new GridLayout(1, 5, 6, 6));
        buttons.add(addBtn);
        buttons.add(updateBtn);
        buttons.add(deleteBtn);
        buttons.add(setStatusBtn);
        buttons.add(reloadBtn);

        JPanel south = new JPanel(new BorderLayout());
        south.add(form, BorderLayout.CENTER);
        south.add(buttons, BorderLayout.SOUTH);

        add(south, BorderLayout.SOUTH);
    }

    private void runSafely(Runnable action) {
        try {
            action.run();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    ex.getMessage() == null ? ex.toString() : ex.getMessage(),
                    "Error",
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

    private Integer selectedConveyorIdOrNull() {
        int row = table.getSelectedRow();
        if (row >= 0) {
            Object v = model.getValueAt(row, 0);
            if (v instanceof Integer) return (Integer) v;
            return Integer.valueOf(v.toString());
        }
        // fallback: if user typed an ID (read-only normally, but keep safe)
        String t = idField.getText();
        if (t == null || t.trim().isEmpty()) return null;
        return parseIntOrNull(t, "Conveyor ID");
    }

    private void loadConveyors() {
        model.setRowCount(0);
        List<Conveyor> list = controller.getConveyorsByParkingLot(parkingLotId);
        for (Conveyor c : list) {
            model.addRow(new Object[] {
                    c.getId(),
                    c.getFloorNumber(),
                    c.getX(),
                    c.getY(),
                    c.getMaxVehicleWeightKg(),
                    c.getStatus()
            });
        }
    }

    // Add WITHOUT ID (AutoNumber)
    private void addConveyor() {
        Integer floor = parseIntOrNull(floorField.getText(), "Floor");
        Integer x = parseIntOrNull(xField.getText(), "X");
        Integer y = parseIntOrNull(yField.getText(), "Y");
        Integer weight = parseIntOrNull(weightField.getText(), "Max Weight (Kg)");
        if (floor == null || x == null || y == null || weight == null) return;

        ConveyorStatus st = (ConveyorStatus) statusBox.getSelectedItem();
        Conveyor created = controller.addConveyorToParkingLot(parkingLotId, floor, x, y, weight, st);

        loadConveyors();
        idField.setText(String.valueOf(created.getId()));
        JOptionPane.showMessageDialog(this, "Added conveyor with ID: " + created.getId(), "Success",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void updateConveyor() {
        Integer id = selectedConveyorIdOrNull();
        Integer floor = parseIntOrNull(floorField.getText(), "Floor");
        Integer x = parseIntOrNull(xField.getText(), "X");
        Integer y = parseIntOrNull(yField.getText(), "Y");
        Integer weight = parseIntOrNull(weightField.getText(), "Max Weight (Kg)");
        if (id == null || floor == null || x == null || y == null || weight == null) return;

        controller.updateConveyor(id, floor, x, y, weight);
        loadConveyors();
    }

    private void setStatus() {
        Integer id = selectedConveyorIdOrNull();
        if (id == null) {
            JOptionPane.showMessageDialog(this, "Select a conveyor row first.", "Validation",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        controller.updateConveyorStatus(id, (ConveyorStatus) statusBox.getSelectedItem());
        loadConveyors();
    }

    private void deleteConveyor() {
        Integer id = selectedConveyorIdOrNull();
        if (id == null) {
            JOptionPane.showMessageDialog(this, "Select a conveyor row first.", "Validation",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int ok = JOptionPane.showConfirmDialog(this,
                "Delete Conveyor ID " + id + " ?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION);

        if (ok != JOptionPane.YES_OPTION) return;

        controller.deleteConveyor(id);
        loadConveyors();
    }
}