package boundary;

import control.ConveyorManagementController;
import control.ParkingLotManagementController;
import entity.Conveyor;
import entity.ConveyorStatus;
import entity.ParkingLot;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class ConveyorManagementUI extends JPanel {

    private final ConveyorManagementController controller;
    private final ParkingLotManagementController parkingLotController;

    private Integer parkingLotId = null;

    private JTable table;
    private DefaultTableModel model;

    private JComboBox<ParkingLot> parkingLotBox;

    private JTextField floorField;
    private JTextField xField; // read-only
    private JTextField yField; // read-only
    private JTextField weightField;

    private JLabel selectedInfoLabel;
    private JLabel pendingInfoLabel;

    private JButton refreshBtn;

    // CRUD
    private JButton addBtn;
    private JButton deleteBtn;
    private JButton moveBtn;

    // State machine actions (NO hardware tests, NO pause)
    private JButton decideWeightBtn;
    private JButton confirmWeightBtn;

    private JButton turnOnBtn;
    private JButton restartBtn;
    private JButton turnOffBtn;

    public ConveyorManagementUI(ConveyorManagementController controller,
                               ParkingLotManagementController parkingLotController) {
        this.controller = controller;
        this.parkingLotController = parkingLotController;

        setLayout(new BorderLayout());
        initTable();
        initBottomPanel();
        wireSelection();
    }

    public void setParkingLotId(Integer parkingLotId) {
        this.parkingLotId = parkingLotId;

        SwingUtilities.invokeLater(() -> {
            boolean enabled = (parkingLotId != null);
            setPanelEnabled(enabled);

            if (enabled) {
                loadParkingLots();
                loadConveyors();
            } else {
                model.setRowCount(0);
                updateSelectedInfo();
                updateButtonsEnabled();
            }
        });
    }

    // ========================= UI =========================

    private void initTable() {
        model = new DefaultTableModel(
                new Object[]{"ID", "ParkingLotID", "Floor", "X", "Y", "MaxWeight", "Status"}, 0
        ) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    private void initBottomPanel() {
        JPanel bottom = new JPanel(new BorderLayout());

        JPanel infoPanel = new JPanel(new GridLayout(2, 1));
        selectedInfoLabel = new JLabel("Selected: -");
        pendingInfoLabel = new JLabel("Pending weight: -");
        infoPanel.add(selectedInfoLabel);
        infoPanel.add(pendingInfoLabel);
        bottom.add(infoPanel, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(3, 4, 6, 6));

        parkingLotBox = new JComboBox<>();

        floorField = new JTextField("1");

        xField = new JTextField();
        xField.setEditable(false);
        xField.setBackground(new Color(240, 240, 240));

        yField = new JTextField();
        yField.setEditable(false);
        yField.setBackground(new Color(240, 240, 240));

        weightField = new JTextField();

        refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> loadConveyors());

        addBtn = new JButton("Add (OFF)");
        deleteBtn = new JButton("Delete");
        moveBtn = new JButton("Move to selected ParkingLot");

        addBtn.addActionListener(e -> addConveyor());
        deleteBtn.addActionListener(e -> deleteSelected());
        moveBtn.addActionListener(e -> moveSelected());

        form.add(new JLabel("ParkingLot:"));
        form.add(parkingLotBox);
        form.add(refreshBtn);
        form.add(new JLabel(""));

        form.add(new JLabel("Floor:"));
        form.add(floorField);
        form.add(new JLabel("X (read-only):"));
        form.add(xField);

        form.add(new JLabel("Y (read-only):"));
        form.add(yField);
        form.add(new JLabel("MaxWeight (kg):"));
        form.add(weightField);

        bottom.add(form, BorderLayout.CENTER);

        JPanel actions = new JPanel(new GridLayout(2, 4, 6, 6));

        decideWeightBtn = new JButton("Decide Weight");
        confirmWeightBtn = new JButton("Confirm Weight");

        turnOnBtn = new JButton("Turn ON (-> Testing)");
        turnOffBtn = new JButton("Turn OFF (Operational -> Off)");
        restartBtn = new JButton("Restart (Paused -> Testing)");

        decideWeightBtn.addActionListener(e -> decideWeight());
        confirmWeightBtn.addActionListener(e -> confirmWeight());

        turnOnBtn.addActionListener(e -> turnOn());
        turnOffBtn.addActionListener(e -> turnOff());
        restartBtn.addActionListener(e -> restart());

        actions.add(decideWeightBtn);
        actions.add(confirmWeightBtn);
        actions.add(turnOnBtn);
        actions.add(turnOffBtn);

        actions.add(restartBtn);
        actions.add(addBtn);
        actions.add(deleteBtn);
        actions.add(new JLabel(""));

        bottom.add(actions, BorderLayout.SOUTH);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        right.add(moveBtn);
        bottom.add(right, BorderLayout.EAST);

        add(bottom, BorderLayout.SOUTH);

        updateButtonsEnabled();
    }

    private void wireSelection() {
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateSelectedInfo();
                updateButtonsEnabled();
                updateReadOnlyXYFields();
            }
        });
    }

    private void setPanelEnabled(boolean enabled) {
        table.setEnabled(enabled);
        parkingLotBox.setEnabled(enabled);
        floorField.setEnabled(enabled);
        weightField.setEnabled(enabled);

        addBtn.setEnabled(enabled);
        refreshBtn.setEnabled(enabled);

        xField.setEnabled(enabled);
        yField.setEnabled(enabled);

        updateButtonsEnabled();
    }

    // ========================= Data loading =========================

    private void loadParkingLots() {
        parkingLotBox.removeAllItems();
        if (parkingLotController == null) return;

        // אם אצלך המתודה נקראת אחרת, תחליפי כאן את השורה הזו בלבד.
        for (ParkingLot p : parkingLotController.getAllParkingLots()) {
            parkingLotBox.addItem(p);
        }
    }

    private void loadConveyors() {
        model.setRowCount(0);
        if (parkingLotId == null) return;

        List<Conveyor> list = controller.getConveyorsByParkingLot(parkingLotId);
        for (Conveyor c : list) {
            model.addRow(new Object[]{
                    c.getId(),
                    c.getParkingLotId(),
                    c.getFloorNumber(),
                    c.getX(),
                    c.getY(),
                    c.getMaxVehicleWeightKg(),
                    c.getStatus()
            });
        }

        updateSelectedInfo();
        updateButtonsEnabled();
        updateReadOnlyXYFields();
    }

    // ========================= Selection helpers =========================

    private Integer getSelectedConveyorId() {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        return (Integer) model.getValueAt(row, 0);
    }

    private ConveyorStatus getSelectedStatus() {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        Object st = model.getValueAt(row, 6);
        return (st instanceof ConveyorStatus) ? (ConveyorStatus) st : null;
    }

    private void updateSelectedInfo() {
        Integer id = getSelectedConveyorId();
        if (id == null) {
            selectedInfoLabel.setText("Selected: -");
            pendingInfoLabel.setText("Pending weight: -");
            return;
        }

        ConveyorStatus st = getSelectedStatus();
        Integer pending = controller.getPendingWeight(id);

        selectedInfoLabel.setText("Selected: Conveyor #" + id + " | Status: " + st);
        pendingInfoLabel.setText("Pending weight: " + (pending == null ? "-" : pending));
    }

    private void updateReadOnlyXYFields() {
        int row = table.getSelectedRow();
        if (row < 0) {
            xField.setText("");
            yField.setText("");
            return;
        }
        xField.setText(String.valueOf(model.getValueAt(row, 3)));
        yField.setText(String.valueOf(model.getValueAt(row, 4)));
    }

    private void updateButtonsEnabled() {
        boolean hasSelection = (getSelectedConveyorId() != null);
        ConveyorStatus st = getSelectedStatus();

        deleteBtn.setEnabled(hasSelection);
        moveBtn.setEnabled(hasSelection);

        boolean isOff = hasSelection && st == ConveyorStatus.Off;
        boolean isOperational = hasSelection && st == ConveyorStatus.Operational;
        boolean isPaused = hasSelection && st == ConveyorStatus.Paused;

        decideWeightBtn.setEnabled(isOff);
        confirmWeightBtn.setEnabled(isOff && controller.getPendingWeight(getSelectedConveyorId()) != null);

        turnOnBtn.setEnabled(isOff && controller.getPendingWeight(getSelectedConveyorId()) == null);

        // Turn off only from Operational
        turnOffBtn.setEnabled(isOperational);

        // Restart only from Paused (the only way out)
        restartBtn.setEnabled(isPaused);
    }

    // ========================= Actions =========================

    private int parsePositiveInt(JTextField tf, String name) {
        try {
            int v = Integer.parseInt(tf.getText().trim());
            if (v <= 0) throw new NumberFormatException();
            return v;
        } catch (Exception ex) {
            throw new IllegalArgumentException(name + " must be a positive integer.");
        }
    }

    private void addConveyor() {
        if (parkingLotId == null) {
            JOptionPane.showMessageDialog(this, "You must select a parking lot first.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            ParkingLot lot = (ParkingLot) parkingLotBox.getSelectedItem();
            if (lot == null) {
                JOptionPane.showMessageDialog(this, "Select a ParkingLot from the list.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int floor = parsePositiveInt(floorField, "Floor");
            int weight = parsePositiveInt(weightField, "MaxWeight");

            // X,Y are external — store defaults 1,1 on creation.
            controller.addConveyorToParkingLot(lot.getId(), floor, 1, 1, weight, ConveyorStatus.Off);

            loadConveyors();
            weightField.setText("");

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSelected() {
        Integer id = getSelectedConveyorId();
        if (id == null) return;

        int ok = JOptionPane.showConfirmDialog(this, "Delete Conveyor #" + id + " ?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;

        try {
            controller.deleteConveyor(id);
            loadConveyors();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void moveSelected() {
        Integer id = getSelectedConveyorId();
        if (id == null) return;

        ParkingLot lot = (ParkingLot) parkingLotBox.getSelectedItem();
        if (lot == null) return;

        try {
            controller.moveConveyorToParkingLot(id, lot.getId());
            loadConveyors();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void decideWeight() {
        Integer id = getSelectedConveyorId();
        if (id == null) return;

        try {
            int w = parsePositiveInt(weightField, "MaxWeight");
            controller.decideChangeMaxWeight(id, w);
            updateSelectedInfo();
            updateButtonsEnabled();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void confirmWeight() {
        Integer id = getSelectedConveyorId();
        if (id == null) return;

        try {
            controller.confirmChangeMaxWeight(id);
            loadConveyors();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void turnOn() {
        Integer id = getSelectedConveyorId();
        if (id == null) return;

        try {
            controller.turnOnConveyors(id);
            loadConveyors();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void restart() {
        Integer id = getSelectedConveyorId();
        if (id == null) return;

        try {
            controller.restart(id);
            loadConveyors();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void turnOff() {
        Integer id = getSelectedConveyorId();
        if (id == null) return;

        try {
            controller.turnOffConveyors(id);
            loadConveyors();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
