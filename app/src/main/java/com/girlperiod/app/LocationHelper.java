package com.girlperiod.app;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Helper class to get device location using GPS or Network provider.
 */
public class LocationHelper {

    private static final String TAG = "LocationHelper";
    private static final long MIN_TIME_BW_UPDATES = 1000 * 60; // 1 minute
    private static final float MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 meters

    public interface OnLocationListener {
        void onLocationResult(double latitude, double longitude, String cityName);
        void onLocationError(String error);
    }

    private final Context context;
    private LocationManager locationManager;
    private LocationListener locationListener;

    public LocationHelper(Context context) {
        this.context = context;
    }

    /**
     * Check if location permissions are granted.
     */
    public boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Get the last known location (fast, may be null).
     */
    public Location getLastKnownLocation() {
        if (!hasLocationPermission()) return null;

        try {
            locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            Location bestLocation = null;

            // Try GPS provider
            if (locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Location gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (gpsLocation != null) {
                    bestLocation = gpsLocation;
                }
            }

            // Try Network provider
            if (locationManager != null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                Location networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (networkLocation != null && (bestLocation == null || networkLocation.getTime() > bestLocation.getTime())) {
                    bestLocation = networkLocation;
                }
            }

            return bestLocation;
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission denied", e);
            return null;
        }
    }

    /**
     * Request a single location update.
     */
    public void requestSingleLocationUpdate(OnLocationListener listener) {
        if (!hasLocationPermission()) {
            listener.onLocationError("Location permission not granted");
            return;
        }

        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            listener.onLocationError("Location manager not available");
            return;
        }

        // Determine best provider
        String provider = null;
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            provider = LocationManager.GPS_PROVIDER;
        } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            provider = LocationManager.NETWORK_PROVIDER;
        }

        if (provider == null) {
            listener.onLocationError("No location provider available");
            return;
        }

        try {
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    String cityName = getCityName(location.getLatitude(), location.getLongitude());
                    listener.onLocationResult(location.getLatitude(), location.getLongitude(), cityName);
                    // Stop updates after getting one location
                    locationManager.removeUpdates(this);
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {}

                @Override
                public void onProviderEnabled(String provider) {}

                @Override
                public void onProviderDisabled(String provider) {
                    listener.onLocationError("Location provider disabled");
                }
            };

            locationManager.requestSingleUpdate(provider, locationListener, null);
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission denied", e);
            listener.onLocationError("Location permission denied");
        }
    }

    /**
     * Get city name from coordinates using Geocoder.
     */
    public String getCityName(double latitude, double longitude) {
        try {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                // Try to get city name from various fields
                String city = address.getLocality();
                if (city == null) city = address.getSubAdminArea();
                if (city == null) city = address.getAdminArea();
                if (city == null) city = address.getFeatureName();
                return city != null ? city : "Unknown";
            }
        } catch (IOException e) {
            Log.e(TAG, "Geocoder error", e);
        }
        return "Unknown";
    }

    /**
     * Clean up location listener.
     */
    public void cleanup() {
        if (locationManager != null && locationListener != null) {
            try {
                locationManager.removeUpdates(locationListener);
            } catch (SecurityException e) {
                // Ignore
            }
        }
    }
}
