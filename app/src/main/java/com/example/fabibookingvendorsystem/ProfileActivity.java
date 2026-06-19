package com.example.fabibookingvendorsystem;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileActivity extends AppCompatActivity {
    private TextView tvGreeting, tvBookings, tvTransactions;
    private Button btnLogout;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        tvGreeting = findViewById(R.id.tvProfileGreeting);
        tvBookings = findViewById(R.id.tvBookingsList);
        tvTransactions = findViewById(R.id.tvTransactionsList);
        btnLogout = findViewById(R.id.btnLogout);

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            finish();
        });

        loadUserData();
    }

    private void loadUserData() {
        String userId = mAuth.getCurrentUser().getUid();

        // 1. Fetch Name
        mDatabase.child("users").child(userId).child("name").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    tvGreeting.setText("Hello, " + snapshot.getValue().toString());
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // 2. Fetch Bookings
        mDatabase.child("bookings").orderByChild("userId").equalTo(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                StringBuilder sb = new StringBuilder();
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    Booking b = postSnapshot.getValue(Booking.class);
                    sb.append("📸 ").append(b.photographerName).append("\n")
                      .append("📅 ").append(b.date).append("\n")
                      .append("💰 ").append(b.amount).append("\n\n");
                }
                if (sb.length() > 0) tvBookings.setText(sb.toString());
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // 3. Fetch Transactions
        mDatabase.child("transactions").orderByChild("userId").equalTo(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                StringBuilder sb = new StringBuilder();
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    Transaction t = postSnapshot.getValue(Transaction.class);
                    sb.append("Total: ").append(t.currency).append(" ").append(t.totalAmount).append("\n")
                      .append("Our Commission (10%): ").append(t.devCommission).append("\n")
                      .append("Date: ").append(t.timestamp).append("\n\n");
                }
                if (sb.length() > 0) tvTransactions.setText(sb.toString());
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
