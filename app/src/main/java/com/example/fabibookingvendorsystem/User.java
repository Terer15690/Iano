package com.example.fabibookingvendorsystem;

public class User {
    public String email;
    public String uid;
    public String name;
    public String role;
    public String lastLogin;

    public User() {
    }

    public User(String email, String uid, String name, String role) {
        this.email = email;
        this.uid = uid;
        this.name = name;
        this.role = role;
    }
}
