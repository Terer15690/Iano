package com.example.fabibookingvendorsystem;

public class Transaction {
    public String transactionId;
    public String userId;
    public double totalAmount;
    public double devCommission;
    public double vendorEarnings;
    public String currency;
    public String timestamp;

    public Transaction() {}

    public Transaction(String transactionId, String userId, double totalAmount, double devCommission, double vendorEarnings, String currency, String timestamp) {
        this.transactionId = transactionId;
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.devCommission = devCommission;
        this.vendorEarnings = vendorEarnings;
        this.currency = currency;
        this.timestamp = timestamp;
    }
}
