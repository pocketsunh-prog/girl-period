package com.girlperiod.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Helper class to show permission dialog and request runtime permissions.
 */
public class PermissionHelper {

    public static final int PERMISSION_REQUEST_CODE = 1001;

    private final Activity activity;

    public PermissionHelper(Activity activity) {
        this.activity = activity;
    }

    /**
     * Check if all required permissions are granted.
     */
    public boolean hasAllPermissions() {
        return hasInternetPermission() && hasNotificationPermission();
    }

    /**
     * Check internet permission (always granted on older Android, but check anyway).
     */
    public boolean hasInternetPermission() {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.INTERNET)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Check notification permission (Android 13+).
     */
    public boolean hasNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Not needed below Android 13
    }

    /**
     * Check fingerprint permission.
     */
    public boolean hasFingerprintPermission() {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.USE_FINGERPRINT)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(activity, Manifest.permission.USE_BIOMETRIC)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Check location permission.
     */
    public boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Show the permission explanation dialog.
     */
    public void showPermissionDialog() {
        android.view.View dialogView = android.view.LayoutInflater.from(activity)
                .inflate(R.layout.dialog_permissions, null);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        dialogView.findViewById(R.id.btnSkip).setOnClickListener(v -> dialog.dismiss());

        dialogView.findViewById(R.id.btnGrant).setOnClickListener(v -> {
            dialog.dismiss();
            requestPermissions();
        });

        dialog.show();
    }

    /**
     * Request all required permissions at runtime.
     */
    public void requestPermissions() {
        java.util.List<String> permissions = new java.util.ArrayList<>();

        // Internet (normal permission, but include for completeness)
        if (!hasInternetPermission()) {
            permissions.add(Manifest.permission.INTERNET);
        }

        // Fingerprint / Biometric
        if (!hasFingerprintPermission()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions.add(Manifest.permission.USE_BIOMETRIC);
            } else {
                permissions.add(Manifest.permission.USE_FINGERPRINT);
            }
        }

        // Location
        if (!hasLocationPermission()) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        // Notifications (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPermission()) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(activity,
                    permissions.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }

    /**
     * Handle permission request results.
     */
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (!allGranted) {
                // Some permissions denied, show settings dialog
                showSettingsDialog();
            }
        }
    }

    /**
     * Show dialog to open app settings when permissions are denied.
     */
    private void showSettingsDialog() {
        new AlertDialog.Builder(activity)
                .setTitle("Permissions Required")
                .setMessage("Some permissions were denied. Please enable them in app settings for full functionality.")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
                    intent.setData(uri);
                    activity.startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Check if we should show permission rationale.
     */
    public boolean shouldShowRationale() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS);
        }
        return false;
    }
}
