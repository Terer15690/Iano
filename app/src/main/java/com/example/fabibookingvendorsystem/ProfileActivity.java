package com.example.fabibookingvendorsystem;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;

public class ProfileActivity extends AppCompatActivity {
    private TextView tvGreeting, tvBookings, tvTransactions, tvLocation;
    private ImageView ivProfile;
    private Button btnLogout, btnUpdatePhoto, btnViewOnMaps;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private double userLat = 0.0, userLon = 0.0;

    private final ActivityResultLauncher<Void> updatePhotoLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicturePreview(),
            bitmap -> {
                if (bitmap != null) {
                    ivProfile.setImageBitmap(bitmap);
                    savePhotoToDb(bitmap);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        tvGreeting = findViewById(R.id.tvProfileGreeting);
        tvLocation = findViewById(R.id.tvProfileLocation);
        tvBookings = findViewById(R.id.tvBookingsList);
        tvTransactions = findViewById(R.id.tvTransactionsList);
        ivProfile = findViewById(R.id.ivProfileDetail);
        btnUpdatePhoto = findViewById(R.id.btnUpdatePhoto);
        btnViewOnMaps = findViewById(R.id.btnViewOnMaps);
        btnLogout = findViewById(R.id.btnLogout);

        btnUpdatePhoto.setOnClickListener(v -> updatePhotoLauncher.launch(null));
        btnViewOnMaps.setOnClickListener(v -> openMaps());
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            finish();
        });

        loadUserData();
    }

    private void openMaps() {
        if (userLat != 0.0 && userLon != 0.0) {
            String uri = "geo:" + userLat + "," + userLon + "?q=" + userLat + "," + userLon + "(Your Location)";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            intent.setPackage("com.google.android.apps.maps");
            try {
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Google Maps not installed.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadUserData() {
        if (mAuth.getCurrentUser() == null) return;
        String userId = mAuth.getCurrentUser().getUid();

        mDatabase.child("users").child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Robust fetching: try both binding and direct string access
                    String name = "User";
                    String role = "Client";
                    String profilePic = "";
                    
                    if (snapshot.hasChild("name") && snapshot.child("name").getValue() != null) {
                        name = snapshot.child("name").getValue().toString();
                    }
                    if (snapshot.hasChild("role") && snapshot.child("role").getValue() != null) {
                        role = snapshot.child("role").getValue().toString();
                    }
                    if (snapshot.hasChild("profilePicture") && snapshot.child("profilePicture").getValue() != null) {
                        profilePic = snapshot.child("profilePicture").getValue().toString();
                    }
                    
                    userLat = snapshot.hasChild("latitude") && snapshot.child("latitude").getValue() != null ? Double.parseDouble(snapshot.child("latitude").getValue().toString()) : 0.0;
                    userLon = snapshot.hasChild("longitude") && snapshot.child("longitude").getValue() != null ? Double.parseDouble(snapshot.child("longitude").getValue().toString()) : 0.0;

                    tvGreeting.setText(name + " (" + role + ")");
                    
                    if (!name.equals("User")) {
                        Toast.makeText(ProfileActivity.this, "Welcome " + name, Toast.LENGTH_SHORT).show();
                    }
                    
                    if (userLat != 0.0) {
                        tvLocation.setText("Location Captured");
                        btnViewOnMaps.setVisibility(View.VISIBLE);
                    } else {
                        tvLocation.setText("Location not set");
                        btnViewOnMaps.setVisibility(View.GONE);
                    }
                    
                    if (!profilePic.isEmpty()) {
                        ivProfile.setImageBitmap(base64ToBitmap(profilePic));
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Fetch Bookings
        mDatabase.child("bookings").orderByChild("userId").equalTo(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                StringBuilder sb = new StringBuilder();
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    Booking b = postSnapshot.getValue(Booking.class);
                    if (b != null) {
                        sb.append("📸 ").append(b.photographerName).append("\n")
                          .append("📅 ").append(b.date).append("\n")
                          .append("💰 ").append(b.amount).append("\n\n");
                    }
                }
                if (sb.length() > 0) tvBookings.setText(sb.toString());
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void savePhotoToDb(Bitmap bitmap) {
        String base64 = bitmapToBase64(bitmap);
        String uid = mAuth.getCurrentUser().getUid();
        mDatabase.child("users").child(uid).child("profilePicture").setValue(base64)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Profile photo updated!", Toast.LENGTH_SHORT).show());
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream);
        return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT);
    }

    private Bitmap base64ToBitmap(String base64) {
        byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
}
