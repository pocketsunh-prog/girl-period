package com.girlperiod.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.girlperiod.app.data.DatabaseHelper;
import com.girlperiod.app.data.User;
import com.girlperiod.app.ui.GhibliDatePickerDialog;
import com.girlperiod.app.ui.GhibliTheme;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Calendar;

public class ProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 100;

    private ImageView ivProfileImage;
    private ImageButton btnBack;
    private ImageButton btnChangeImage;
    private TextView tvUsername;
    private TextView tvLocation;
    private TextView btnDob;
    private EditText etCurrentPassword;
    private EditText etNewPassword;
    private EditText etConfirmPassword;
    private TextView btnUpdatePassword;
    private TextView btnResetPassword;
    private TextView btnUpdateLocation;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private User currentUser;
    private LocationHelper locationHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GhibliTheme.applyTheme(this);
        setContentView(R.layout.activity_profile);

        dbHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        currentUser = sessionManager.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        loadUserData();
    }

    private void initViews() {
        ivProfileImage = findViewById(R.id.ivProfileImage);
        btnBack = findViewById(R.id.btnBack);
        btnChangeImage = findViewById(R.id.btnChangeImage);
        tvUsername = findViewById(R.id.tvUsername);
        tvLocation = findViewById(R.id.tvLocation);
        btnDob = findViewById(R.id.btnDob);
        etCurrentPassword = findViewById(R.id.etCurrentPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnUpdatePassword = findViewById(R.id.btnUpdatePassword);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        btnUpdateLocation = findViewById(R.id.btnUpdateLocation);

        btnBack.setOnClickListener(v -> finish());

        btnChangeImage.setOnClickListener(v -> openImagePicker());

        btnDob.setOnClickListener(v -> showDobPicker());

        btnUpdatePassword.setOnClickListener(v -> updatePassword());

        btnResetPassword.setOnClickListener(v -> resetPassword());

        btnUpdateLocation.setOnClickListener(v -> updateLocation());

        locationHelper = new LocationHelper(this);
    }

    private void loadUserData() {
        // Load full user data from database
        User fullUser = dbHelper.getUserById(currentUser.getId());
        if (fullUser == null) {
            // Try to get by username
            fullUser = dbHelper.getUserByUsername(currentUser.getUsername());
        }
        
        if (fullUser != null) {
            currentUser = fullUser;
        } else {
            // User not found in database, prompt to log in again
            Toast.makeText(this, "User not found. Please log in again.", Toast.LENGTH_LONG).show();
            sessionManager.logout();
            finish();
            return;
        }

        tvUsername.setText(currentUser.getUsername());

        // Load DOB
        if (currentUser.getDob() != null && !currentUser.getDob().isEmpty()) {
            btnDob.setText(currentUser.getDob());
        }

        // Load location
        updateLocationDisplay();

        // Load profile image
        if (currentUser.getProfileImage() != null && !currentUser.getProfileImage().isEmpty()) {
            try {
                Bitmap bitmap = BitmapFactory.decodeFile(currentUser.getProfileImage());
                if (bitmap != null) {
                    ivProfileImage.setImageBitmap(bitmap);
                }
            } catch (Exception e) {
                // Use default image
            }
        }
    }

    private void updateLocationDisplay() {
        if (currentUser.getCityName() != null && !currentUser.getCityName().isEmpty()) {
            tvLocation.setText(currentUser.getCityName());
        } else if (currentUser.getLatitude() != 0 || currentUser.getLongitude() != 0) {
            tvLocation.setText(String.format("%.4f, %.4f", currentUser.getLatitude(), currentUser.getLongitude()));
        } else {
            tvLocation.setText("Tap to set location");
        }
    }

    private void updateLocation() {
        if (!locationHelper.hasLocationPermission()) {
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Try to get last known location first
        android.location.Location lastLocation = locationHelper.getLastKnownLocation();
        if (lastLocation != null) {
            String cityName = locationHelper.getCityName(lastLocation.getLatitude(), lastLocation.getLongitude());
            saveLocation(lastLocation.getLatitude(), lastLocation.getLongitude(), cityName);
            return;
        }

        // Request fresh location
        Toast.makeText(this, "Getting your location...", Toast.LENGTH_SHORT).show();
        locationHelper.requestSingleLocationUpdate(new LocationHelper.OnLocationListener() {
            @Override
            public void onLocationResult(double latitude, double longitude, String cityName) {
                saveLocation(latitude, longitude, cityName);
            }

            @Override
            public void onLocationError(String error) {
                Toast.makeText(ProfileActivity.this, "Location error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveLocation(double latitude, double longitude, String cityName) {
        // Use provided cityName or get from geocoder
        if (cityName == null || cityName.isEmpty() || "Unknown".equals(cityName)) {
            cityName = locationHelper.getCityName(latitude, longitude);
        }
        
        // Ensure we have a valid user ID
        long userId = currentUser.getId();
        if (userId <= 0) {
            // Try to reload user data from database
            User fullUser = dbHelper.getUserByUsername(currentUser.getUsername());
            if (fullUser != null) {
                userId = fullUser.getId();
                currentUser = fullUser;
            }
        }
        
        if (userId <= 0) {
            Toast.makeText(this, "Error: Invalid user ID. Please log in again.", Toast.LENGTH_LONG).show();
            return;
        }
        
        int rows = dbHelper.updateUserLocation(userId, latitude, longitude, cityName);
        
        if (rows > 0) {
            currentUser.setLatitude(latitude);
            currentUser.setLongitude(longitude);
            currentUser.setCityName(cityName);
            updateLocationDisplay();
            Toast.makeText(this, "Location saved: " + cityName, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to save location (0 rows updated)", Toast.LENGTH_SHORT).show();
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                try {
                    // Copy image to app storage
                    String imagePath = saveImageToStorage(imageUri);
                    if (imagePath != null) {
                        // Update database
                        dbHelper.updateUserProfileImage(currentUser.getId(), imagePath);
                        currentUser.setProfileImage(imagePath);

                        // Display image
                        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                        ivProfileImage.setImageBitmap(bitmap);

                        Toast.makeText(this, "Profile image updated!", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private String saveImageToStorage(Uri imageUri) throws Exception {
        File dir = new File(getFilesDir(), "profile_images");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File file = new File(dir, "profile_" + currentUser.getId() + ".jpg");
        InputStream inputStream = getContentResolver().openInputStream(imageUri);
        FileOutputStream outputStream = new FileOutputStream(file);

        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }

        inputStream.close();
        outputStream.close();

        return file.getAbsolutePath();
    }

    private void showDobPicker() {
        Calendar dobCal = Calendar.getInstance();
        if (currentUser.getDob() != null && !currentUser.getDob().isEmpty()) {
            try {
                String[] parts = currentUser.getDob().split("-");
                if (parts.length == 3) {
                    dobCal.set(Calendar.YEAR, Integer.parseInt(parts[0]));
                    dobCal.set(Calendar.MONTH, Integer.parseInt(parts[1]) - 1);
                    dobCal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parts[2]));
                }
            } catch (Exception e) {
                // Use current date
            }
        }

        new GhibliDatePickerDialog(this, dobCal, date -> {
            String dob = String.format("%04d-%02d-%02d",
                    date.get(Calendar.YEAR),
                    date.get(Calendar.MONTH) + 1,
                    date.get(Calendar.DAY_OF_MONTH));

            // Update database
            dbHelper.updateUserDob(currentUser.getId(), dob);
            currentUser.setDob(dob);

            // Update display
            btnDob.setText(dob);
            Toast.makeText(this, "DOB updated!", Toast.LENGTH_SHORT).show();
        }).show();
    }

    private void updatePassword() {
        String currentPassword = etCurrentPassword.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(this, "New passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!currentUser.getPassword().equals(currentPassword)) {
            Toast.makeText(this, "Current password is incorrect", Toast.LENGTH_SHORT).show();
            return;
        }

        int rows = dbHelper.updateUserPassword(currentUser.getId(), newPassword);
        if (rows > 0) {
            currentUser.setPassword(newPassword);
            etCurrentPassword.setText("");
            etNewPassword.setText("");
            etConfirmPassword.setText("");
            Toast.makeText(this, "Password updated successfully!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to update password", Toast.LENGTH_SHORT).show();
        }
    }

    private void resetPassword() {
        new AlertDialog.Builder(this)
                .setTitle("Reset Password")
                .setMessage("A reset link will be sent to your email address. Continue?")
                .setPositiveButton("Send", (dialog, which) -> {
                    // In a real app, this would send a password reset email
                    Toast.makeText(this, "Password reset link sent to your email!", Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
