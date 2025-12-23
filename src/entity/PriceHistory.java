package entity;

import java.time.LocalDate;

public class PriceHistory {

    private final int id;          // ID ייחודי – לא ניתן לשינוי
    private final int parkingLotId;
    private final int priceListId;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;  

    public PriceHistory(
            int id,
            int parkingLotId,
            int priceListId,
            LocalDate effectiveFrom,
            LocalDate effectiveTo
    ) {
        this.id = id;
        this.parkingLotId = parkingLotId;
        this.priceListId = priceListId;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
    }

    public int getId() {
        return id;
    }

    public int getParkingLotId() {
        return parkingLotId;
    }

    public int getPriceListId() {
        return priceListId;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDate effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public LocalDate getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(LocalDate effectiveTo) {
        this.effectiveTo = effectiveTo;
    }
}