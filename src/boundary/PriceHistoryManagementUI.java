package boundary;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.List;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import control.PriceHistoryManagementController;
import control.PriceListManagementController;
import entity.PriceHistory;
import entity.PriceList;

/**
 * PriceHistoryManagementUI
 *
 * Shows full price history for a selected parking lot.
 * Allows assigning a new PriceList (controller enforces business rules).
 */
public class PriceHistoryManagementUI extends JPanel {

    private final PriceHistoryManagementController historyController;
    private final PriceListManagementController priceListController;

    private Integer parkingLotId = null;

    private final JComboBox<PriceList> priceListCombo = new JComboBox<>();
    private final DefaultTableModel model;
    private final JTable table;

    private final JLabel headerLabel = new JLabel("Price History – ParkingLot: (none)");

    public PriceHistoryManagementUI(PriceHistoryManagementController historyController,
                                    PriceListManagementController priceListController) {
        this.historyController = historyController;
        this.priceListController = priceListController;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel top = new JPanel(new BorderLayout(10, 10));
        top.add(headerLabel, BorderLayout.WEST);

        JPanel assignPanel = new JPanel(new GridLayout(1, 3, 6, 6));
        assignPanel.add(new JLabel("Assign PriceList:"));
        assignPanel.add(priceListCombo);

        JButton assignBtn = new JButton("Assign");
        assignBtn.addActionListener(e -> assignPriceList());
        assignPanel.add(assignBtn);

        top.add(assignPanel, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        model = new DefaultTableModel(
                new Object[] { "PriceList ID", "Effective From", "Effective To" }, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JButton reloadBtn = new JButton("Reload");
        reloadBtn.addActionListener(e -> reloadHistory());
        add(reloadBtn, BorderLayout.SOUTH);

        reloadPriceLists();
    }

    public void setParkingLotId(Integer parkingLotId) {
        this.parkingLotId = parkingLotId;
        headerLabel.setText(
                parkingLotId == null
                    ? "Price History – ParkingLot: (none)"
                    : "Price History – ParkingLot: " + parkingLotId);
        reloadHistory();
    }

    private void reloadPriceLists() {
        priceListCombo.removeAllItems();

        for (PriceList p : priceListController.getAllPriceLists()) {
            priceListCombo.addItem(p);
        }

        priceListCombo.setSelectedIndex(-1);
    }

    private void reloadHistory() {
        model.setRowCount(0);

        if (parkingLotId == null) return;

        List<PriceHistory> list = historyController.getHistoryForParkingLot(parkingLotId);
        for (PriceHistory h : list) {
            LocalDate from = h.getEffectiveFrom();
            LocalDate to = h.getEffectiveTo();
            model.addRow(new Object[] {
                    h.getPriceListId(),
                    from == null ? "" : from.toString(),
                    to == null ? "" : to.toString()
            });
        }
    }

    private void assignPriceList() {
        if (parkingLotId == null) {
            JOptionPane.showMessageDialog(this, "Select a parking lot first.");
            return;
        }

        PriceList selected = (PriceList) priceListCombo.getSelectedItem();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Select a PriceList.");
            return;
        }

        historyController.assignPriceListToParkingLot(
                parkingLotId, selected.getId());

        JOptionPane.showMessageDialog(this, "PriceList assigned successfully.");
        reloadHistory();
    }
}