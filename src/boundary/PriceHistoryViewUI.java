package boundary;

import control.PriceHistoryManagementController;
import control.PriceListManagementController;
import entity.PriceHistory;
import entity.PriceList;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Secondary UI for assigning price lists to a parking lot
 * and viewing price history.
 */
public class PriceHistoryViewUI extends JPanel {

    private final int parkingLotId;
    private final PriceHistoryManagementController historyController;
    private final PriceListManagementController priceListController;

    private final JComboBox<PriceList> priceListCombo = new JComboBox<>();
    private final DefaultTableModel tableModel;

    public PriceHistoryViewUI(int parkingLotId,
                                    PriceHistoryManagementController historyController,
                                    PriceListManagementController priceListController) {

        this.parkingLotId = parkingLotId;
        this.historyController = historyController;
        this.priceListController = priceListController;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Assign Price List:"));
        top.add(priceListCombo);

        JButton assignBtn = new JButton("Assign");
        assignBtn.addActionListener(e -> assignPriceList());
        top.add(assignBtn);

        add(top, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
                new Object[]{"Price List", "From Date"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(tableModel);
        add(new JScrollPane(table), BorderLayout.CENTER);

        loadPriceLists();
        loadHistory();
    }

    private void loadPriceLists() {
        priceListCombo.removeAllItems();
        try {
            for (PriceList p : priceListController.getAllPriceLists()) {
                priceListCombo.addItem(p);
            }
        } catch (Exception ex) {
            UiUtil.error(this, ex.getMessage());
        }
    }

    private void loadHistory() {
        tableModel.setRowCount(0);

        if (parkingLotId <=0) {
            return;
        }

        try {
            for (PriceHistory h : historyController.getHistoryForParkingLot(parkingLotId)) {
                tableModel.addRow(new Object[] {
                    h.getPriceListId(),
                    h.getEffectiveFrom(),
                    h.getEffectiveTo()
                });
            }
        } catch (Exception ex) {
            UiUtil.error(this, ex.getMessage());
        }
    }

    private void assignPriceList() {
        PriceList selected = (PriceList) priceListCombo.getSelectedItem();
        if (selected == null) {
            UiUtil.warn(this, "Please select a price list.");
            return;
        }

        try {
            historyController.assignPriceListToParkingLot(
                    parkingLotId, selected.getId());

            UiUtil.info(this, "Price list assigned successfully.");
            loadHistory();

        } catch (Exception ex) {
            UiUtil.error(this, ex.getMessage());
        }
    }
}