package control;

import java.time.LocalDate;

import entity.PriceList;

/**
 * UI-friendly row for PriceHistory joined with PriceList values.
 * (Does not change entity classes.)
 */
public class PriceHistoryRow {
    public final int historyId;
    public final LocalDate from;
    public final LocalDate to;
    public final int priceListId;
    public final int year;
    public final double firstHour;
    public final double additionalHour;
    public final double fullDay;

    public PriceHistoryRow(int historyId, LocalDate from, LocalDate to, int priceListId, int year, double firstHour,
            double additionalHour, double fullDay) {
        this.historyId = historyId;
        this.from = from;
        this.to = to;
        this.priceListId = priceListId;
        this.year = year;
        this.firstHour = firstHour;
        this.additionalHour = additionalHour;
        this.fullDay = fullDay;
    }

    public static PriceHistoryRow fromHistoryRow(int historyId,
                                                 int parkingLotId,
                                                 int priceListId,
                                                 LocalDate from,
                                                 LocalDate to,
                                                 PriceListManagementController priceListController) {

        int year = (from != null) ? from.getYear() : 0;
        double firstHour = 0;
        double additionalHour = 0;
        double fullDay = 0;

        if (priceListController != null) {
            try {
                PriceList p = priceListController.getById(priceListId);
                if (p != null) {
                    year = p.getYear();
                    firstHour = p.getFirstHourPrice();
                    additionalHour = p.getAdditionalHourPrice();
                    fullDay = p.getFullDayPrice();
                }
            } catch (Exception ignore) {
                // keep zeros; UI should still render history row
            }
        }

        return new PriceHistoryRow(historyId, from, to, priceListId, year, firstHour, additionalHour, fullDay);
    }
}
