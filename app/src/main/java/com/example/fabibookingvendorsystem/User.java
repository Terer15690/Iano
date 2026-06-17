package com.example.fabibookingvendorsystem;

public class User {
    public String email;
    public String uid;

    public User() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public User(String email, String uid) {
        this.email = email;
        this.uid = uid;
    }
}
