package com.example.fabibookingvendorsystem;

public class Booking {
    public String bookingId;
    public String userId;
    public String photographerName;
    public String amount;
    public String location;
    public String date;
    public String timestamp;

    public Booking() {}

    public Booking(String bookingId, String userId, String photographerName, String amount, String location, String date, String timestamp) {
        this.bookingId = bookingId;
        this.userId = userId;
        this.photographerName = photographerName;
        this.amount = amount;
        this.location = location;
        this.date = date;
        this.timestamp = timestamp;
    }
}
