package boundary;

import control.CityManagementController;
import control.ConveyorManagementController;
import control.ParkingLotManagementController;
import control.PriceHistoryManagementController;
import control.PriceListManagementController;
import entity.City;
import entity.ParkingLot;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class ParkingLotDashboardUI extends JFrame {

    private final ParkingLotManagementController parkingLotController;
    private final CityManagementController cityController;
    private final ConveyorManagementController conveyorController;
    private final PriceHistoryManagementController priceHistoryController;
    private final PriceListManagementController priceListController;

    private JTable table;
    private DefaultTableModel model;

    private ParkingLot selectedParkingLot;

    private JTextField idField;
    private JTextField nameField;
    private JTextField addressField;
    private JTextField spacesField;
    private JComboBox<City> cityCombo;

    // search
    private JTextField searchIdField;
    private JButton searchBtn;

    // secondary buttons
    private JButton conveyorBtn;
    private JButton priceHistoryBtn;
    private JButton priceListBtn;

    public ParkingLotDashboardUI(
            ParkingLotManagementController parkingLotController,
            CityManagementController cityController,
            ConveyorManagementController conveyorController,
            PriceHistoryManagementController priceHistoryController,
            PriceListManagementController priceListController
    ) {
        this.parkingLotController = parkingLotController;
        this.cityController = cityController;
        this.conveyorController = conveyorController;
        this.priceHistoryController = priceHistoryController;
        this.priceListController = priceListController;

        setTitle("ParkWise – Parking Lots");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(950, 600);
        setLocationRelativeTo(null);

        initUI();
        loadCities();
        loadParkingLots();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));

        // ================= NORTH =================
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("ParkingLot ID:"));
        searchIdField = new JTextField(6);
        searchPanel.add(searchIdField);

        searchBtn = new JButton("Find");
        searchBtn.addActionListener(e -> findParkingLotById());
        searchPanel.add(searchBtn);

        JPanel secondaryPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        conveyorBtn = new JButton("Conveyors");
        priceHistoryBtn = new JButton("Price History");
        priceListBtn = new JButton("Import Price List");

        conveyorBtn.addActionListener(e -> openConveyorScreen());
        priceHistoryBtn.addActionListener(e -> openPriceHistoryScreen());
        priceListBtn.addActionListener(e -> openPriceListScreen());

        secondaryPanel.add(conveyorBtn);
        secondaryPanel.add(priceHistoryBtn);
        secondaryPanel.add(priceListBtn);

        JPanel north = new JPanel(new BorderLayout());
        north.add(searchPanel, BorderLayout.WEST);
        north.add(secondaryPanel, BorderLayout.EAST);

        add(north, BorderLayout.NORTH);

        // ================= TABLE =================
        model = new DefaultTableModel(
                new Object[]{"ID", "Name", "Address", "City", "Available Spaces"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        table.getSelectionModel().addListSelectionListener((ListSelectionEvent e) -> {
            if (!e.getValueIsAdjusting()) {
                fillFormFromSelection();
            }
        });

        // ================= FORM =================
        JPanel form = new JPanel(new GridLayout(5, 2, 8, 8));

        idField = new JTextField();
        idField.setEditable(false);

        nameField = new JTextField();
        addressField = new JTextField();

        spacesField = new JTextField();
        spacesField.setEditable(false);

        cityCombo = new JComboBox<>();

        form.add(new JLabel("ID"));
        form.add(idField);
        form.add(new JLabel("Name"));
        form.add(nameField);
        form.add(new JLabel("Address"));
        form.add(addressField);
        form.add(new JLabel("City"));
        form.add(cityCombo);
        form.add(new JLabel("Available Spaces"));
        form.add(spacesField);

        JButton addBtn = new JButton("Add");
        addBtn.addActionListener(e -> addParkingLot());

        JButton updateBtn = new JButton("Update");
        updateBtn.addActionListener(e -> updateParkingLot());

        JButton deleteBtn = new JButton("Delete");
        deleteBtn.addActionListener(e -> deleteParkingLot());

        JButton reloadBtn = new JButton("Reload");
        reloadBtn.addActionListener(e -> loadParkingLots());

        JPanel crudButtons = new JPanel(new GridLayout(1, 4, 8, 8));
        crudButtons.add(addBtn);
        crudButtons.add(updateBtn);
        crudButtons.add(deleteBtn);
        crudButtons.add(reloadBtn);

        JPanel south = new JPanel(new BorderLayout(10, 10));
        south.add(form, BorderLayout.CENTER);
        south.add(crudButtons, BorderLayout.SOUTH);

        add(south, BorderLayout.SOUTH);
    }

    // ================= LOGIC =================

    private void loadCities() {
        cityCombo.removeAllItems();
        for (City c : cityController.getAllCities()) {
            cityCombo.addItem(c);
        }
        cityCombo.setSelectedIndex(-1);
    }

    private void loadParkingLots() {
        model.setRowCount(0);
        List<ParkingLot> lots = parkingLotController.getAllParkingLots();
        for (ParkingLot p : lots) {
            model.addRow(new Object[]{
                    p.getId(),
                    p.getName(),
                    p.getAddress(),
                    p.getCity(),
                    p.getAvailableSpaces()
            });
        }
    }

    private void fillFormFromSelection() {
        int row = table.getSelectedRow();
        if (row < 0) return;

        int id = (int) model.getValueAt(row, 0);
        selectedParkingLot = parkingLotController.getParkingLot(id);

        idField.setText(String.valueOf(selectedParkingLot.getId()));
        nameField.setText(selectedParkingLot.getName());
        addressField.setText(selectedParkingLot.getAddress());
        cityCombo.setSelectedItem(selectedParkingLot.getCity());
        spacesField.setText(String.valueOf(selectedParkingLot.getAvailableSpaces()));
    }

    private void findParkingLotById() {
        try {
            int id = Integer.parseInt(searchIdField.getText().trim());
            ParkingLot p = parkingLotController.getParkingLot(id);

            for (int i = 0; i < model.getRowCount(); i++) {
                if ((int) model.getValueAt(i, 0) == id) {
                    table.setRowSelectionInterval(i, i);
                    table.scrollRectToVisible(table.getCellRect(i, 0, true));
                    return;
                }
            }

            selectedParkingLot = p;
            fillFormFromSelection();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Parking lot not found.");
        }
    }

    private void addParkingLot() {
        try {
            parkingLotController.addParkingLot(
                    nameField.getText(),
                    addressField.getText(),
                    (City) cityCombo.getSelectedItem(),
                    Integer.parseInt(spacesField.getText())
            );
            loadParkingLots();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateParkingLot() {
        if (selectedParkingLot == null) {
            JOptionPane.showMessageDialog(this, "Select a parking lot first.");
            return;
        }

        try {
            parkingLotController.updateParkingLot(
                    selectedParkingLot.getId(),
                    nameField.getText(),
                    addressField.getText(),
                    (City) cityCombo.getSelectedItem()
            );
            JOptionPane.showMessageDialog(this, "Parking lot updated successfully.");
            loadParkingLots();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteParkingLot() {
        if (selectedParkingLot == null) return;

        if (JOptionPane.showConfirmDialog(this,
                "Delete this parking lot?",
                "Confirm",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {

            parkingLotController.deleteParkingLot(selectedParkingLot.getId());
            loadParkingLots();
        }
    }

    // ================= SECONDARY WINDOWS =================

    private void openConveyorScreen() {
        if (selectedParkingLot == null) {
            JOptionPane.showMessageDialog(this, "Select a parking lot first.");
            return;
        }

        ConveyorManagementUI ui =
                new ConveyorManagementUI(conveyorController, parkingLotController);

        ui.setParkingLotId(selectedParkingLot.getId());

        JFrame f = new JFrame("Conveyors – ParkingLot " + selectedParkingLot.getId());
        f.setContentPane(ui);
        f.pack();
        f.setLocationRelativeTo(this);
        f.setVisible(true);
    }

    private void openPriceHistoryScreen() {
        if (selectedParkingLot == null) {
            JOptionPane.showMessageDialog(this, "Select a parking lot first.");
            return;
        }

        PriceHistoryManagementUI ui =
                new PriceHistoryManagementUI(priceHistoryController, priceListController);

        ui.setParkingLotId(selectedParkingLot.getId());

        JFrame f = new JFrame("Price History – ParkingLot " + selectedParkingLot.getId());
        f.setContentPane(ui);
        f.pack();
        f.setLocationRelativeTo(this);
        f.setVisible(true);
    }

    private void openPriceListScreen() {
        JFrame f = new JFrame("Import Price List");
        f.setContentPane(new PriceListImportViewUI(priceListController));
        f.pack();
        f.setLocationRelativeTo(this);
        f.setVisible(true);
    }
}