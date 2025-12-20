package boundary;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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

import control.PriceHistoryManagementController;
import control.PriceHistoryRow;

/**
 * PriceHistory CRUD by ParkingLot.
 *
 * Dates format: yyyy-MM-dd
 * effectiveTo can be empty (NULL = active).
 */
public class PriceHistoryManagementUI extends JFrame {

    private final PriceHistoryManagementController controller;
    private final int parkingLotId;

    private JTable table;
    private DefaultTableModel model;

    private JLabel activeLbl;

    private JTextField historyIdField, priceListIdField, fromField, toField;

    public PriceHistoryManagementUI(PriceHistoryManagementController controller, int parkingLotId) {
        this.controller = controller;
        this.parkingLotId = parkingLotId;

        initUI();
        reload();
    }

    private void initUI() {
        setTitle("Price History (ParkingLot " + parkingLotId + ")");
        setSize(980, 440);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        model = new DefaultTableModel(
                new Object[] { "HistoryID", "From", "To", "PriceListID", "Year", "FirstHour", "AdditionalHour", "FullDay" },
                0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);

        activeLbl = new JLabel("Active Price: (loading...)");
        add(activeLbl, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel form = new JPanel(new GridLayout(3, 4, 6, 6));
        historyIdField = new JTextField();
        priceListIdField = new JTextField();
        fromField = new JTextField();
        toField = new JTextField();

        form.add(new JLabel("History ID"));
        form.add(historyIdField);
        form.add(new JLabel("PriceList ID"));
        form.add(priceListIdField);

        form.add(new JLabel("Effective From (yyyy-MM-dd)"));
        form.add(fromField);
        form.add(new JLabel("Effective To (optional)"));
        form.add(toField);

        JButton addBtn = new JButton("Add");
        addBtn.addActionListener(e -> runSafely(this::addRow));

        JButton updateBtn = new JButton("Update");
        updateBtn.addActionListener(e -> runSafely(this::updateRow));

        JButton deleteBtn = new JButton("Delete");
        deleteBtn.addActionListener(e -> runSafely(this::deleteRow));

        JButton reloadBtn = new JButton("Reload");
        reloadBtn.addActionListener(e -> runSafely(this::reload));

        form.add(addBtn);
        form.add(updateBtn);
        form.add(deleteBtn);
        form.add(reloadBtn);

        add(form, BorderLayout.SOUTH);
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

    private LocalDate parseDateOrNull(String text, String fieldName, boolean allowEmpty) {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) {
            if (allowEmpty) return null;
            JOptionPane.showMessageDialog(this, "Missing value: " + fieldName, "Validation",
                    JOptionPane.WARNING_MESSAGE);
            return null;
        }
        try {
            return LocalDate.parse(t);
        } catch (DateTimeParseException dtpe) {
            JOptionPane.showMessageDialog(this, "Invalid date in " + fieldName + ": " + t + " (use yyyy-MM-dd)",
                    "Validation", JOptionPane.WARNING_MESSAGE);
            return null;
        }
    }

    private Integer selectedHistoryIdOrNull() {
        int row = table.getSelectedRow();
        if (row >= 0) {
            Object v = model.getValueAt(row, 0);
            if (v instanceof Integer) return (Integer) v;
            return Integer.valueOf(v.toString());
        }
        return parseIntOrNull(historyIdField.getText(), "History ID");
    }

    private void reload() {
        model.setRowCount(0);

        PriceHistoryRow active = controller.getCurrentActiveRow(parkingLotId);
        activeLbl.setText(active == null ? "Active Price: (none)"
                : "Active Price: PriceListID=" + active.priceListId + " | Year=" + active.year);

        List<PriceHistoryRow> rows = controller.getHistoryRows(parkingLotId);
        for (PriceHistoryRow r : rows) {
            model.addRow(new Object[] { r.historyId, r.from, r.to, r.priceListId, r.year, r.firstHour, r.additionalHour,
                    r.fullDay });
        }
    }

    private void addRow() {
        Integer plid = parseIntOrNull(priceListIdField.getText(), "PriceList ID");
        LocalDate from = parseDateOrNull(fromField.getText(), "Effective From", false);
        LocalDate to = parseDateOrNull(toField.getText(), "Effective To", true);
        if (plid == null || from == null) return;

      
        controller.addHistoryRow(parkingLotId, plid, from, to);
        reload();
    }

    private void updateRow() {
        Integer hid = selectedHistoryIdOrNull();
        Integer plid = parseIntOrNull(priceListIdField.getText(), "PriceList ID");
        LocalDate from = parseDateOrNull(fromField.getText(), "Effective From", false);
        LocalDate to = parseDateOrNull(toField.getText(), "Effective To", true);
        if (hid == null || plid == null || from == null) return;

        controller.updateHistoryRow(hid, plid, from, to);
        reload();
    }

    private void deleteRow() {
        Integer hid = selectedHistoryIdOrNull();
        if (hid == null) return;
        controller.deleteHistoryRow(hid);
        reload();
    }
}
