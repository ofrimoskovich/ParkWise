package boundary;

import control.PriceListManagementController;
import entity.PriceList;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Collection;
import java.util.List;

public class PriceListImportViewUI extends JPanel {

    private final PriceListManagementController controller;
    private JTable table;
    private DefaultTableModel model;

    public PriceListImportViewUI(PriceListManagementController controller) {
        this.controller = controller;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        initUI();
    }

    private void initUI() {
        JButton loadBtn = new JButton("Load Price List (from system)");
        loadBtn.addActionListener(e -> loadFromJson());

        add(loadBtn, BorderLayout.NORTH);

        model = new DefaultTableModel(
                new Object[]{"ID", "Year", "First Hour", "Additional Hour", "Full Day"}, 0
        ) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        loadTable();
    }

    private void loadFromJson() {
        try {
            controller.importPriceListsFromDefaultJson();
            JOptionPane.showMessageDialog(this, "Price list loaded successfully.");
            loadTable();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadTable() {
        model.setRowCount(0);
       Collection<PriceList> list = controller.getAllPriceLists();

        for (PriceList p : list) {
            model.addRow(new Object[]{
                    p.getId(),
                    p.getYear(),
                    p.getFirstHourPrice(),
                    p.getAdditionalHourPrice(),
                    p.getFullDayPrice()
            });
        }
    }
}