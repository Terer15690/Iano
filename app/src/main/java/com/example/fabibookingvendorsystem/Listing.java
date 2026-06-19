package com.example.fabibookingvendorsystem;

public class Listing {
    public String userId;
    public String name;
    public String category;
    public String description;
    public String price;
    public String location;

    public Listing() {
    }

    public Listing(String userId, String name, String category, String description, String price, String location) {
        this.userId = userId;
        this.name = name;
        this.category = category;
        this.description = description;
        this.price = price;
        this.location = location;
    }
}
