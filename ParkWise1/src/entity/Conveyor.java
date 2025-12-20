package entity;

public class Conveyor {

    private final int id;               // ID ייחודי – לא ניתן לשינוי
    private int parkingLotId;
    private int floorNumber;
    private int x;
    private int y;
    private int maxVehicleWeightKg;
    private ConveyorStatus status;

    public Conveyor(
            int id,
            int parkingLotId,
            int floorNumber,
            int x,
            int y,
            int maxVehicleWeightKg,
            ConveyorStatus status
    ) {
        this.id = id;
        this.parkingLotId = parkingLotId;
        this.floorNumber = floorNumber;
        this.x = x;
        this.y = y;
        this.maxVehicleWeightKg = maxVehicleWeightKg;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public int getParkingLotId() {
        return parkingLotId;
    }

    public void setParkingLotId(int parkingLotId) {
        this.parkingLotId = parkingLotId;
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public void setFloorNumber(int floorNumber) {
        this.floorNumber = floorNumber;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getMaxVehicleWeightKg() {
        return maxVehicleWeightKg;
    }

    public void setMaxVehicleWeightKg(int maxVehicleWeightKg) {
        this.maxVehicleWeightKg = maxVehicleWeightKg;
    }

    public ConveyorStatus getStatus() {
        return status;
    }

    public void setStatus(ConveyorStatus status) {
        this.status = status;
    }
}