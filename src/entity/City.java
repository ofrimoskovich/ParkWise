package entity;

import java.util.Objects;

public class City {

    // ===== Fields =====
    private int id;          // AutoNumber from DB
    private String cityName; // City name from DB

    // ===== Constructors =====

    // Constructor לשימוש כשקוראים מה-DB
    public City(int id, String cityName) {
        this.id = id;
        this.cityName = cityName;
    }

    // Constructor לשימוש כשמוסיפים עיר חדשה (לפני insert)
    // ה-ID יווצר בדאטה-בייס
    public City(String cityName) {
        this.cityName = cityName;
    }

    // ===== Getters =====
    public int getId() {
        return id;
    }

    public String getCityName() {
        return cityName;
    }

    // ===== Setters =====
    // אין setId() – AutoNumber!
    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    // ===== Utility =====
    @Override
    public String toString() {
        return cityName; // חשוב ל-ComboBox / UI
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof City)) return false;
        City city = (City) o;
        return id == city.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
}