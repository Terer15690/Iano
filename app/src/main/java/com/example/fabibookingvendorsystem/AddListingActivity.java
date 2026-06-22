package com.example.fabibookingvendorsystem;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class AddListingActivity extends AppCompatActivity {
    private EditText etName, etPrice, etLocation, etDescription, etPhoneNumber;
    private Spinner spinnerCategory;
    private ImageView ivSelectedImage;
    private Button btnSelectImage, btnSubmit;
    private Uri imageUri;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    private final ActivityResultLauncher<String> mGetContent = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    imageUri = uri;
                    ivSelectedImage.setImageURI(uri);
                    ivSelectedImage.setVisibility(View.VISIBLE);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_listing);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        etName = findViewById(R.id.etServiceName);
        etPrice = findViewById(R.id.etPrice);
        etLocation = findViewById(R.id.etLocation);
        etDescription = findViewById(R.id.etDescription);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        ivSelectedImage = findViewById(R.id.ivSelectedImage);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnSubmit = findViewById(R.id.btnSubmitListing);

        btnSelectImage.setOnClickListener(v -> mGetContent.launch("image/*"));
        btnSubmit.setOnClickListener(v -> submitListing());
    }

    private void submitListing() {
        String name = etName.getText().toString().trim();
        String price = etPrice.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String phone = etPhoneNumber.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(price) || TextUtils.isEmpty(location) || TextUtils.isEmpty(phone)) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmit.setEnabled(false);
        btnSubmit.setText("Publishing...");

        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "Anonymous";
        String listingId = mDatabase.child("listings").push().getKey();

        // Convert image to Base64 String
        String imageString = "";
        if (imageUri != null) {
            imageString = uriToBase64(imageUri);
        }

        Listing listing = new Listing(listingId, userId, name, category, description, price, location, phone, imageString);

        if (listingId != null) {
            mDatabase.child("listings").child(listingId).setValue(listing)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(AddListingActivity.this, "Published to Database!", Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        btnSubmit.setEnabled(true);
                        btnSubmit.setText("Upload Portfolio & Price");
                        Toast.makeText(AddListingActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
        }
    }

    private String uriToBase64(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            // Compress image to keep database size small
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream);
            byte[] byteArray = outputStream.toByteArray();
            return Base64.encodeToString(byteArray, Base64.DEFAULT);
        } catch (Exception e) {
            return "";
        }
    }
}
