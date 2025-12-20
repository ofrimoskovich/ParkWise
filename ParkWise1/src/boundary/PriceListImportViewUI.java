package boundary;

import java.awt.BorderLayout;
import java.io.File;
import java.util.Collection;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import control.PriceListManagementController;
import entity.PriceList;

/**
 * PriceList: Import from external JSON + view.
 * No manual CRUD here (per requirement).
 */
public class PriceListImportViewUI extends JFrame {

    private final PriceListManagementController controller;
    private final DefaultTableModel model;

    public PriceListImportViewUI(PriceListManagementController controller) {
        this.controller = controller;

        setTitle("Price List – Import / View");
        setSize(780, 360);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        model = new DefaultTableModel(new Object[] { "ID", "Year", "First Hour", "Additional Hour", "Full Day" }, 0) {
            private static final long serialVersionUID = 1L;
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };

        JTable table = new JTable(model);
        table.setEnabled(false);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JButton importBtn = new JButton("Import Price Lists (JSON)...");
        JButton refreshBtn = new JButton("Refresh");

        importBtn.addActionListener(e -> importPriceListsWithChooser());
        refreshBtn.addActionListener(e -> load());

        JPanel top = new JPanel();
        top.add(importBtn);
        top.add(refreshBtn);

        add(top, BorderLayout.NORTH);

        load();
    }

    private void importPriceListsWithChooser() {
        // default: project file
        File defaultFile = new File("pricelist.json");

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select pricelist JSON");
        if (defaultFile.exists()) {
            chooser.setSelectedFile(defaultFile);
        }

        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;

        File f = chooser.getSelectedFile();
        importPriceLists(f.getAbsolutePath());
    }

    private void importPriceLists(String path) {
        try {
            controller.importPriceListsFromJson(path);
            JOptionPane.showMessageDialog(this, "Price lists imported successfully");
            load();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Import failed: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void load() {
        model.setRowCount(0);

        Collection<PriceList> all = controller.getAllPriceLists();
        for (PriceList p : all) {
            model.addRow(new Object[] { p.getId(), p.getYear(), p.getFirstHourPrice(), p.getAdditionalHourPrice(),
                    p.getFullDayPrice() });
        }
    }
}
