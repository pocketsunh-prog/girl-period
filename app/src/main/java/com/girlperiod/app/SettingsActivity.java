package com.girlperiod.app;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.girlperiod.app.ui.GhibliTheme;

/**
 * Settings screen: theme selection, notification toggle, fingerprint toggle, logout.
 */
public class SettingsActivity extends AppCompatActivity {

    private static final int REQUEST_FINGERPRINT_PERMISSION = 200;
    private static final String PREFS_SETTINGS = "app_settings";
    private static final String KEY_NOTIFICATIONS = "notifications_enabled";
    private static final String KEY_FINGERPRINT = "fingerprint_enabled";

    private SwitchMaterial notificationSwitch;
    private SwitchMaterial fingerprintSwitch;
    private ImageButton btnBack;
    private Button btnLogout;

    private FrameLayout flColorPink, flColorPurple, flColorBlue, flColorGreen, flColorOrange;
    private ImageView ivCheckPink, ivCheckPurple, ivCheckBlue, ivCheckGreen, ivCheckOrange;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GhibliTheme.applyTheme(this);
        setContentView(R.layout.activity_settings);

        initViews();
        setupThemeSelection();
        setupSwitches();
        setupLogout();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        notificationSwitch = findViewById(R.id.switchNotification);
        fingerprintSwitch = findViewById(R.id.switchFingerprint);
        btnLogout = findViewById(R.id.btnLogout);

        flColorPink = findViewById(R.id.flColorPink);
        flColorPurple = findViewById(R.id.flColorPurple);
        flColorBlue = findViewById(R.id.flColorBlue);
        flColorGreen = findViewById(R.id.flColorGreen);
        flColorOrange = findViewById(R.id.flColorOrange);

        ivCheckPink = findViewById(R.id.ivCheckPink);
        ivCheckPurple = findViewById(R.id.ivCheckPurple);
        ivCheckBlue = findViewById(R.id.ivCheckBlue);
        ivCheckGreen = findViewById(R.id.ivCheckGreen);
        ivCheckOrange = findViewById(R.id.ivCheckOrange);

        btnBack.setOnClickListener(v -> finish());

        updateCheckMarks();
    }

    private void setupThemeSelection() {
        flColorPink.setOnClickListener(v -> selectTheme(GhibliTheme.Theme.SAKURA));
        flColorPurple.setOnClickListener(v -> selectTheme(GhibliTheme.Theme.LAVENDER));
        flColorBlue.setOnClickListener(v -> selectTheme(GhibliTheme.Theme.SKY));
        flColorGreen.setOnClickListener(v -> selectTheme(GhibliTheme.Theme.MATCHA));
        flColorOrange.setOnClickListener(v -> selectTheme(GhibliTheme.Theme.PEACH));
    }

    private void selectTheme(GhibliTheme.Theme theme) {
        GhibliTheme.saveTheme(this, theme.name());
        GhibliTheme.applyTheme(this);
        updateCheckMarks();
        recreate();
    }

    private void updateCheckMarks() {
        GhibliTheme.Theme current = GhibliTheme.loadTheme(this);
        ivCheckPink.setVisibility(current == GhibliTheme.Theme.SAKURA ? View.VISIBLE : View.GONE);
        ivCheckPurple.setVisibility(current == GhibliTheme.Theme.LAVENDER ? View.VISIBLE : View.GONE);
        ivCheckBlue.setVisibility(current == GhibliTheme.Theme.SKY ? View.VISIBLE : View.GONE);
        ivCheckGreen.setVisibility(current == GhibliTheme.Theme.MATCHA ? View.VISIBLE : View.GONE);
        ivCheckOrange.setVisibility(current == GhibliTheme.Theme.PEACH ? View.VISIBLE : View.GONE);
    }

    private void setupSwitches() {
        SharedPreferences prefs = getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE);

        boolean notificationsEnabled = prefs.getBoolean(KEY_NOTIFICATIONS, true);
        boolean fingerprintEnabled = prefs.getBoolean(KEY_FINGERPRINT, false);

        notificationSwitch.setChecked(notificationsEnabled);
        fingerprintSwitch.setChecked(fingerprintEnabled);

        notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_NOTIFICATIONS, isChecked).apply();
            if (isChecked) {
                scheduleDailyCheck();
            } else {
                cancelDailyCheck();
            }
        });

        fingerprintSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.USE_FINGERPRINT)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                            this,
                            new String[]{Manifest.permission.USE_FINGERPRINT},
                            REQUEST_FINGERPRINT_PERMISSION
                    );
                    fingerprintSwitch.setChecked(false);
                    return;
                }
            }
            prefs.edit().putBoolean(KEY_FINGERPRINT, isChecked).apply();
        });
    }

    private void setupLogout() {
        btnLogout.setOnClickListener(v -> {
            SessionManager session = new SessionManager(SettingsActivity.this);
            session.logout();
            Toast.makeText(this, "Logged out. See you soon!", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void scheduleDailyCheck() {
        PeriodNotificationService.createNotificationChannel(this);
        Intent intent = new Intent(this, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager != null) {
            long triggerAt = System.currentTimeMillis() + AlarmManager.INTERVAL_DAY;
            alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
            );
        }
    }

    private void cancelDailyCheck() {
        Intent intent = new Intent(this, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_FINGERPRINT_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fingerprintSwitch.setChecked(true);
                getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE)
                        .edit().putBoolean(KEY_FINGERPRINT, true).apply();
            } else {
                Toast.makeText(this, "Fingerprint permission denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
