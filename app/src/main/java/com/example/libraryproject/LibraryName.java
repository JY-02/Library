package com.example.libraryproject;

public class LibraryName {
    private String lbName;
    private double latitude;
    private double longitude;

    public LibraryName(double latitude, double longitude, String name) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.lbName = name;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setName(String name) {
        this.lbName = name;
    }
    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getLbName() {
        return lbName;
    }

}
