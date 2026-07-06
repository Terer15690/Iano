package com.example.fabibookingvendorsystem;

public class User {
    public String email;
    public String uid;
    public String name;
    public String role;
    public String profilePicture; // Base64
    public double latitude;
    public double longitude;

    public User() {
    }

    public User(String email, String uid, String name, String role, String profilePicture, double latitude, double longitude) {
        this.email = email;
        this.uid = uid;
        this.name = name;
        this.role = role;
        this.profilePicture = profilePicture;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
