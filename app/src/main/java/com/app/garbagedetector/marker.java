package com.app.garbagedetector;

public class marker {
    private double Lat;
    private double lng;
    private String type;

    public marker() {
    }

    public marker(double lat, double lng, String type) {
        Lat = lat;
        this.lng = lng;
        this.type = type;
    }

    public double getLat() {
        return Lat;
    }

    public void setLat(double lat) {
        Lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
