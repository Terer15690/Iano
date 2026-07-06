package com.example.fabibookingvendorsystem;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayOutputStream;

public class RegisterActivity extends AppCompatActivity {
    private EditText etEmail, etPassword, etFullName;
    private TextView tvLocationDisplay, tvLoginLink;
    private ImageView ivProfilePhoto;
    private Spinner spinnerRole;
    private Button btnRegister, btnTakePhoto, btnGetLocation;
    
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private EventManager eventManager;
    private FusedLocationProviderClient fusedLocationClient;

    private String profilePhotoBase64 = "";
    private double currentLat = 0.0;
    private double currentLon = 0.0;

    private final ActivityResultLauncher<Void> takePhotoLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicturePreview(),
            bitmap -> {
                if (bitmap != null) {
                    ivProfilePhoto.setImageBitmap(bitmap);
                    profilePhotoBase64 = bitmapToBase64(bitmap);
                    Toast.makeText(this, "Photo Captured!", Toast.LENGTH_SHORT).show();
                }
            }
    );

    private final ActivityResultLauncher<String> cameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    takePhotoLauncher.launch(null);
                } else {
                    Toast.makeText(this, "Camera permission required for profile photo", Toast.LENGTH_LONG).show();
                }
            }
    );

    private final ActivityResultLauncher<String[]> locationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                if ((fineLocationGranted != null && fineLocationGranted) || (coarseLocationGranted != null && coarseLocationGranted)) {
                    fetchLocation();
                } else {
                    Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        eventManager = new EventManager(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        ivProfilePhoto = findViewById(R.id.ivProfilePhoto);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        spinnerRole = findViewById(R.id.spinnerRole);
        btnGetLocation = findViewById(R.id.btnGetLocation);
        tvLocationDisplay = findViewById(R.id.tvLocationDisplay);
        btnRegister = findViewById(R.id.btnRegister);
        tvLoginLink = findViewById(R.id.tvLoginLink);

        btnTakePhoto.setOnClickListener(v -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA));
        btnGetLocation.setOnClickListener(v -> requestLocationPermission());
        btnRegister.setOnClickListener(v -> registerUser());
        tvLoginLink.setOnClickListener(v -> startActivity(new Intent(RegisterActivity.this, LoginActivity.class)));
    }

    private void requestLocationPermission() {
        locationPermissionLauncher.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }

    private void fetchLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                currentLat = location.getLatitude();
                currentLon = location.getLongitude();
                tvLocationDisplay.setText("Location Captured Successfully!");
                Toast.makeText(this, "Location captured!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Could not detect location. Please ensure GPS is ON.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void registerUser() {
        String name = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String role = spinnerRole.getSelectedItem().toString();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String userId = mAuth.getCurrentUser().getUid();
                        User user = new User(email, userId, name, role, profilePhotoBase64, currentLat, currentLon);
                        mDatabase.child("users").child(userId).setValue(user).addOnCompleteListener(dbTask -> {
                            if (dbTask.isSuccessful()) {
                                eventManager.logAndNotify("Registered Successfully!");
                                finish();
                            }
                        });
                    } else {
                        handleAuthError(task.getException());
                    }
                });
    }

    private void handleAuthError(Exception exception) {
        String message = exception != null ? exception.getMessage() : "Unknown Error";
        if (message != null && message.contains("already in use")) {
            new AlertDialog.Builder(this)
                .setTitle("Account Exists")
                .setMessage("This email is already registered. Would you like to log in instead?")
                .setPositiveButton("Login", (dialog, which) -> {
                    startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
        } else {
            Toast.makeText(this, "Failed: " + message, Toast.LENGTH_LONG).show();
        }
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream);
        return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT);
    }
}
