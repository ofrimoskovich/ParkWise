package boundary;

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import control.PriceHistoryManagementController;
import control.PriceHistoryRow;

public class PriceHistoryViewUI extends JFrame {

	public PriceHistoryViewUI(PriceHistoryManagementController controller, int parkingLotId) {
		setTitle("Price History – ParkingLot " + parkingLotId);
		setSize(900, 350);
		setLocationRelativeTo(null);
		setLayout(new BorderLayout());

		DefaultTableModel model = new DefaultTableModel(
				new Object[] { "HistoryID", "From", "To", "PriceListID", "Year", "First", "Additional", "FullDay" }, 0);

		JTable table = new JTable(model);
		table.setEnabled(false);
		add(new JScrollPane(table), BorderLayout.CENTER);

		PriceHistoryRow active = controller.getCurrentActiveRow(parkingLotId);
		JLabel activeLbl = new JLabel(active == null ? "Active Price: (none)"
				: "Active Price: PriceListID=" + active.priceListId + " | Year=" + active.year);

		add(activeLbl, BorderLayout.NORTH);

		List<PriceHistoryRow> rows = controller.getHistoryRows(parkingLotId);
		for (PriceHistoryRow r : rows) {
			model.addRow(new Object[] { r.historyId, r.from, r.to, r.priceListId, r.year, r.firstHour, r.additionalHour,
					r.fullDay });
		}
	}
}