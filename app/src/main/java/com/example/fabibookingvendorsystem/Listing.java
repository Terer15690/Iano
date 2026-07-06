package com.example.fabibookingvendorsystem;

public class Listing {
    public String listingId;
    public String userId;
    public String name;
    public String category;
    public String description;
    public String price;
    public String location; // Text location (e.g. Nairobi)
    public String phoneNumber;
    public String imageBase64;
    public double latitude;
    public double longitude;

    public Listing() {
    }

    public Listing(String listingId, String userId, String name, String category, String description, String price, String location, String phoneNumber, String imageBase64, double latitude, double longitude) {
        this.listingId = listingId;
        this.userId = userId;
        this.name = name;
        this.category = category;
        this.description = description;
        this.price = price;
        this.location = location;
        this.phoneNumber = phoneNumber;
        this.imageBase64 = imageBase64;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
