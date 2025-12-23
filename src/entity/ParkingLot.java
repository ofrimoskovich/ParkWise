package entity;

public class ParkingLot {

    private final int id;          // ID ייחודי – לא ניתן לשינוי
    private String name;
    private String address;
    private String city;
    private int availableSpaces;

    public ParkingLot(
            int id,
            String name,
            String address,
            String city,
            int availableSpaces
    ) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.city = city;
        this.availableSpaces = availableSpaces;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public int getAvailableSpaces() {
        return availableSpaces;
    }

    public void setAvailableSpaces(int availableSpaces) {
        this.availableSpaces = availableSpaces;
    }
}