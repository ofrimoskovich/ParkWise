package boundary;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.GridLayout;
import java.util.List;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import control.ConveyorManagementController;
import control.ParkingLotManagementController;
import control.PriceHistoryManagementController;
import control.PriceListManagementController;
import entity.ParkingLot;

public class ParkingLotDashboardUI extends JFrame {

    private final ParkingLotManagementController parkingLotController;
    private final ConveyorManagementController conveyorController;
    private final PriceListManagementController priceListController;
    private final PriceHistoryManagementController priceHistoryController;

    private JTable lotsTable;
    private DefaultTableModel lotsModel;

    private JTextField nameField, addressField, cityField, spacesField;

    private final CardLayout cards = new CardLayout();
    private final JPanel secondaryArea = new JPanel(cards);

    private final JLabel selectedLotLabel = new JLabel("Selected ParkingLot: (none)");
    private Integer selectedLotId = null;

    private final ConveyorManagementUI conveyorsPanel;
    private final PriceListImportViewUI priceListPanel;
    private final PriceHistoryManagementUI historyPanel;

    public ParkingLotDashboardUI(ParkingLotManagementController pl,
                                ConveyorManagementController cc,
                                PriceListManagementController plc,
                                PriceHistoryManagementController phc) {
        this.parkingLotController = pl;
        this.conveyorController = cc;
        this.priceListController = plc;
        this.priceHistoryController = phc;

        this.conveyorsPanel = new ConveyorManagementUI(conveyorController);
        this.priceListPanel = new PriceListImportViewUI(priceListController);
        this.historyPanel = new PriceHistoryManagementUI(priceHistoryController, priceListController);

        initUI();
        reloadParkingLots();
    }

    private void initUI() {
        setTitle("ParkWise – Parking Lots (Main + Secondary)");
        setSize(1100, 720);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // ===== TOP (Main): ParkingLots table + CRUD =====
        JPanel topPanel = new JPanel(new BorderLayout());

        lotsModel = new DefaultTableModel(new Object[]{"ID","Name","Address","City","AvailableSpaces"}, 0) {
            private static final long serialVersionUID = 1L;
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        lotsTable = new JTable(lotsModel);
        lotsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        lotsTable.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int row = lotsTable.getSelectedRow();
            if (row < 0) {
                setSelectedLot(null);
                return;
            }
            Integer id = Integer.valueOf(lotsModel.getValueAt(row, 0).toString());
            setSelectedLot(id);

            nameField.setText(String.valueOf(lotsModel.getValueAt(row, 1)));
            addressField.setText(String.valueOf(lotsModel.getValueAt(row, 2)));
            cityField.setText(String.valueOf(lotsModel.getValueAt(row, 3)));
            spacesField.setText(String.valueOf(lotsModel.getValueAt(row, 4)));
        });

        topPanel.add(new JScrollPane(lotsTable), BorderLayout.CENTER);

        JPanel lotCrud = new JPanel(new BorderLayout());

        JPanel form = new JPanel(new GridLayout(2, 4, 6, 6));
        nameField = new JTextField();
        addressField = new JTextField();
        cityField = new JTextField();
        spacesField = new JTextField();

        form.add(new JLabel("Name"));
        form.add(nameField);
        form.add(new JLabel("Address"));
        form.add(addressField);
        form.add(new JLabel("City"));
        form.add(cityField);
        form.add(new JLabel("Available Spaces"));
        form.add(spacesField);

        JButton addBtn = new JButton("Add ParkingLot");
        addBtn.addActionListener(e -> runSafely(this::addLot));

        JButton updateBtn = new JButton("Update Selected");
        updateBtn.addActionListener(e -> runSafely(this::updateSelectedLot));

        JButton deleteBtn = new JButton("Delete Selected");
        deleteBtn.addActionListener(e -> runSafely(this::deleteSelectedLot));

        JButton reloadBtn = new JButton("Reload");
        reloadBtn.addActionListener(e -> runSafely(this::reloadParkingLots));

        JPanel buttons = new JPanel(new GridLayout(1, 4, 6, 6));
        buttons.add(addBtn);
        buttons.add(updateBtn);
        buttons.add(deleteBtn);
        buttons.add(reloadBtn);

        lotCrud.add(form, BorderLayout.CENTER);
        lotCrud.add(buttons, BorderLayout.SOUTH);

        topPanel.add(lotCrud, BorderLayout.SOUTH);

        // ===== BOTTOM (Secondary): one area with buttons + cards =====
        JPanel bottomPanel = new JPanel(new BorderLayout());

        JPanel secHeader = new JPanel(new BorderLayout());
        secHeader.add(selectedLotLabel, BorderLayout.WEST);

        JButton conveyorsBtn = new JButton("Conveyors (selected lot)");
        conveyorsBtn.addActionListener(e -> runSafely(() -> showSecondary("CONVEYORS")));

        JButton priceListBtn = new JButton("PriceList (Import/View)");
        priceListBtn.addActionListener(e -> runSafely(() -> showSecondary("PRICELIST")));

        JButton historyBtn = new JButton("Price History (selected lot)");
        historyBtn.addActionListener(e -> runSafely(() -> showSecondary("HISTORY")));

        JPanel secButtons = new JPanel(new GridLayout(1, 3, 6, 6));
        secButtons.add(conveyorsBtn);
        secButtons.add(priceListBtn);
        secButtons.add(historyBtn);

        secHeader.add(secButtons, BorderLayout.EAST);

        JPanel empty = new JPanel(new BorderLayout());
        empty.add(new JLabel("בחרי חניון מהטבלה למעלה ואז בחרי מסך משני."), BorderLayout.CENTER);

        secondaryArea.add(empty, "EMPTY");
        secondaryArea.add(conveyorsPanel, "CONVEYORS");
        secondaryArea.add(priceListPanel, "PRICELIST");
        secondaryArea.add(historyPanel, "HISTORY");
        cards.show(secondaryArea, "EMPTY");

        bottomPanel.add(secHeader, BorderLayout.NORTH);
        bottomPanel.add(secondaryArea, BorderLayout.CENTER);

        // ===== SPLIT: keeps ParkingLots always visible =====
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topPanel, bottomPanel);
        split.setResizeWeight(0.55);      // 55% top, 45% bottom
        split.setDividerLocation(380);    // initial divider position

        setContentPane(split);
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
            JOptionPane.showMessageDialog(this, "Missing value: " + fieldName, "Validation", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        try { return Integer.parseInt(t); }
        catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Invalid number in " + fieldName + ": " + t, "Validation", JOptionPane.WARNING_MESSAGE);
            return null;
        }
    }

    private void setSelectedLot(Integer lotId) {
        selectedLotId = lotId;
        selectedLotLabel.setText(lotId == null ? "Selected ParkingLot: (none)" : "Selected ParkingLot: " + lotId);

        conveyorsPanel.setParkingLotId(lotId);
        historyPanel.setParkingLotId(lotId);
    }

    private void showSecondary(String card) {
        if ((card.equals("CONVEYORS") || card.equals("HISTORY")) && selectedLotId == null) {
            JOptionPane.showMessageDialog(this, "בחרי חניון קודם מהטבלה למעלה.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        cards.show(secondaryArea, card);
    }

    private void reloadParkingLots() {
        lotsModel.setRowCount(0);
        List<ParkingLot> lots = parkingLotController.getAllParkingLots();
        for (ParkingLot p : lots) {
            lotsModel.addRow(new Object[]{ p.getId(), p.getName(), p.getAddress(), p.getCity(), p.getAvailableSpaces() });
        }
    }

    private void addLot() {
        Integer spaces = parseIntOrNull(spacesField.getText(), "Available Spaces");
        if (spaces == null) return;

        // כאן אני מניח שיש לכם add בלי ID (AutoNumber)
        ParkingLot created = parkingLotController.addParkingLot(
                nameField.getText().trim(),
                addressField.getText().trim(),
                cityField.getText().trim(),
                spaces
        );

        reloadParkingLots();
        JOptionPane.showMessageDialog(this, "Added ParkingLot with ID: " + created.getId());
    }

    private void updateSelectedLot() {
        if (selectedLotId == null) {
            JOptionPane.showMessageDialog(this, "בחרי חניון מהטבלה.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Integer spaces = parseIntOrNull(spacesField.getText(), "Available Spaces");
        if (spaces == null) return;

        parkingLotController.updateParkingLot(
                selectedLotId,
                nameField.getText().trim(),
                addressField.getText().trim(),
                cityField.getText().trim(),
                spaces
        );
        reloadParkingLots();
    }

    private void deleteSelectedLot() {
        if (selectedLotId == null) {
            JOptionPane.showMessageDialog(this, "בחרי חניון מהטבלה.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int ok = JOptionPane.showConfirmDialog(this,
                "Delete ParkingLot ID " + selectedLotId + " ?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION);

        if (ok != JOptionPane.YES_OPTION) return;

        parkingLotController.deleteParkingLot(selectedLotId);
        setSelectedLot(null);
        reloadParkingLots();
    }
}