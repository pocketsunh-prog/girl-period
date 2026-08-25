package com.girlperiod.app;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
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

    private View vCalTextColor;
    private View vCalBgColor;
    private FrameLayout flStyleDefault, flStyleCompact, flStyleRounded, flStyleMinimal;
    private ImageView ivCheckStyleDefault, ivCheckStyleCompact, ivCheckStyleRounded, ivCheckStyleMinimal;

    // Ghibli DatePicker style controls
    private com.google.android.material.button.MaterialButton btnFontFamily;
    private com.google.android.material.button.MaterialButton btnFontSize;
    private View vDPFontColor;
    private View vDPBorderColor;
    private View vDPSelectedColor;
    private TextView btnProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GhibliTheme.applyTheme(this);
        setContentView(R.layout.activity_settings);

        initViews();
        setupThemeSelection();
        setupSwitches();
        setupCalendarColorPickers();
        setupCalendarStyle();
        setupDatePickerStyle();
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

        vCalTextColor = findViewById(R.id.vCalTextColor);
        vCalBgColor = findViewById(R.id.vCalBgColor);

        flStyleDefault = findViewById(R.id.flStyleDefault);
        flStyleCompact = findViewById(R.id.flStyleCompact);
        flStyleRounded = findViewById(R.id.flStyleRounded);
        flStyleMinimal = findViewById(R.id.flStyleMinimal);

        ivCheckStyleDefault = findViewById(R.id.ivCheckStyleDefault);
        ivCheckStyleCompact = findViewById(R.id.ivCheckStyleCompact);
        ivCheckStyleRounded = findViewById(R.id.ivCheckStyleRounded);
        ivCheckStyleMinimal = findViewById(R.id.ivCheckStyleMinimal);

        // Ghibli DatePicker style controls
        btnFontFamily = findViewById(R.id.btnFontFamily);
        btnFontSize = findViewById(R.id.btnFontSize);
        vDPFontColor = findViewById(R.id.vDPFontColor);
        vDPBorderColor = findViewById(R.id.vDPBorderColor);
        vDPSelectedColor = findViewById(R.id.vDPSelectedColor);
        btnProfile = findViewById(R.id.btnProfile);

        btnBack.setOnClickListener(v -> finish());
        btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        updateCheckMarks();
        updateColorPreviews();
        updateStyleChecks();
        updateDPColorPreviews();
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

    private void updateColorPreviews() {
        int textColor = GhibliTheme.getCalendarTextColor(this);
        int bgColor = GhibliTheme.getCalendarBackgroundColor(this);
        GradientDrawable textDrawable = (GradientDrawable) vCalTextColor.getBackground();
        textDrawable.setColor(textColor);
        GradientDrawable bgDrawable = (GradientDrawable) vCalBgColor.getBackground();
        bgDrawable.setColor(bgColor);
    }

    private void setupCalendarColorPickers() {
        vCalTextColor.setOnClickListener(v -> showColorPicker("Text Color", 4));
        vCalBgColor.setOnClickListener(v -> showColorPicker("Background Color", 5));
    }

    private void showColorPicker(String title, final int colorType) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_color_picker, null);
        TextView tvTitle = dialogView.findViewById(R.id.tvColorPickerTitle);
        TextView tvCurrent = dialogView.findViewById(R.id.tvCurrentColor);
        GridLayout gridColors = dialogView.findViewById(R.id.gridColors);
        EditText etCustomColor = dialogView.findViewById(R.id.etCustomColor);

        tvTitle.setText("Choose " + title);
        final int currentColor;
        switch (colorType) {
            case 1: currentColor = GhibliTheme.getDPFontColor(this); break;
            case 2: currentColor = GhibliTheme.getDPBorderColor(this); break;
            case 3: currentColor = GhibliTheme.getDPSelectedColor(this); break;
            case 4: currentColor = GhibliTheme.getCalendarTextColor(this); break;
            case 5: currentColor = GhibliTheme.getCalendarBackgroundColor(this); break;
            default: currentColor = Color.BLACK;
        }
        tvCurrent.setText("Current: " + String.format("#%06X", 0xFFFFFF & currentColor));

        // Predefined colors
        int[] colors = {
            Color.parseColor("#000000"), Color.parseColor("#333333"), Color.parseColor("#666666"),
            Color.parseColor("#999999"), Color.parseColor("#CCCCCC"), Color.parseColor("#FFFFFF"),
            Color.parseColor("#FF0000"), Color.parseColor("#FF6600"), Color.parseColor("#FFCC00"),
            Color.parseColor("#00FF00"), Color.parseColor("#0099FF"), Color.parseColor("#0000FF"),
            Color.parseColor("#9900FF"), Color.parseColor("#FF00FF"), Color.parseColor("#FF69B4"),
            Color.parseColor("#8B4513"), Color.parseColor("#FFD700"), Color.parseColor("#00CED1"),
        };

        for (int color : colors) {
            View swatch = new View(this);
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(color);
            drawable.setStroke(2, Color.parseColor("#40000000"));
            swatch.setBackground(drawable);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 40;
            params.height = 40;
            params.setMargins(8, 8, 8, 8);
            swatch.setLayoutParams(params);

            final int selectedColor = color;
            swatch.setOnClickListener(v -> {
                saveColorByType(colorType, selectedColor);
                updateDPColorPreviews();
                updateColorPreviews();
            });

            gridColors.addView(swatch);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        dialogView.findViewById(R.id.btnApplyCustom).setOnClickListener(v -> {
            String hex = etCustomColor.getText().toString().trim();
            try {
                // Validate hex string length
                if (hex.length() < 4 || hex.length() > 9) {
                    Toast.makeText(this, "Invalid color format. Use #RGB, #RRGGBB, or #AARRGGBB", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Ensure it starts with #
                if (!hex.startsWith("#")) {
                    hex = "#" + hex;
                }
                int color = Color.parseColor(hex);
                saveColorByType(colorType, color);
                updateDPColorPreviews();
                updateColorPreviews();
            } catch (IllegalArgumentException e) {
                Toast.makeText(this, "Invalid color format. Use #RRGGBB", Toast.LENGTH_SHORT).show();
            }
        });

        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btnOk).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void saveColorByType(int colorType, int color) {
        switch (colorType) {
            case 1: GhibliTheme.saveDPFontColor(this, color); break;
            case 2: GhibliTheme.saveDPBorderColor(this, color); break;
            case 3: GhibliTheme.saveDPSelectedColor(this, color); break;
            case 4: GhibliTheme.saveCalendarTextColor(this, color); break;
            case 5: GhibliTheme.saveCalendarBackgroundColor(this, color); break;
        }
    }

    private void setupCalendarStyle() {
        flStyleDefault.setOnClickListener(v -> selectCalendarStyle(GhibliTheme.CAL_STYLE_DEFAULT));
        flStyleCompact.setOnClickListener(v -> selectCalendarStyle(GhibliTheme.CAL_STYLE_COMPACT));
        flStyleRounded.setOnClickListener(v -> selectCalendarStyle(GhibliTheme.CAL_STYLE_ROUNDED));
        flStyleMinimal.setOnClickListener(v -> selectCalendarStyle(GhibliTheme.CAL_STYLE_MINIMAL));
    }

    private void selectCalendarStyle(int style) {
        GhibliTheme.saveCalendarStyle(this, style);
        updateStyleChecks();
    }

    private void updateStyleChecks() {
        int current = GhibliTheme.getCalendarStyle(this);
        ivCheckStyleDefault.setVisibility(current == GhibliTheme.CAL_STYLE_DEFAULT ? View.VISIBLE : View.GONE);
        ivCheckStyleCompact.setVisibility(current == GhibliTheme.CAL_STYLE_COMPACT ? View.VISIBLE : View.GONE);
        ivCheckStyleRounded.setVisibility(current == GhibliTheme.CAL_STYLE_ROUNDED ? View.VISIBLE : View.GONE);
        ivCheckStyleMinimal.setVisibility(current == GhibliTheme.CAL_STYLE_MINIMAL ? View.VISIBLE : View.GONE);
    }

    private void setupDatePickerStyle() {
        // Setup font family button
        String[] fonts = {"sans-serif", "sans-serif-light", "sans-serif-medium", "serif", "monospace"};
        String currentFont = GhibliTheme.getDPFont(this);
        btnFontFamily.setText(currentFont);
        btnFontFamily.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Select Font Family")
                    .setItems(fonts, (dialog, which) -> {
                        GhibliTheme.saveDPFont(SettingsActivity.this, fonts[which]);
                        btnFontFamily.setText(fonts[which]);
                    })
                    .show();
        });

        // Setup font size button
        int[] fontSizes = {12, 14, 16, 18, 20, 22, 24};
        int currentSize = GhibliTheme.getDPFontSize(this);
        btnFontSize.setText(String.valueOf(currentSize));
        btnFontSize.setOnClickListener(v -> {
            String[] sizeStrings = new String[fontSizes.length];
            for (int i = 0; i < fontSizes.length; i++) {
                sizeStrings[i] = String.valueOf(fontSizes[i]);
            }
            new AlertDialog.Builder(this)
                    .setTitle("Select Font Size")
                    .setItems(sizeStrings, (dialog, which) -> {
                        GhibliTheme.saveDPFontSize(SettingsActivity.this, fontSizes[which]);
                        btnFontSize.setText(sizeStrings[which]);
                    })
                    .show();
        });

        // Setup color swatches
        updateDPColorPreviews();
        vDPFontColor.setOnClickListener(v -> showColorPicker("Font Color", 1));
        vDPBorderColor.setOnClickListener(v -> showColorPicker("Border Color", 2));
        vDPSelectedColor.setOnClickListener(v -> showColorPicker("Selected Day Color", 3));
    }

    private void updateDPColorPreviews() {
        int fontColor = GhibliTheme.getDPFontColor(this);
        int borderColor = GhibliTheme.getDPBorderColor(this);
        int selectedColor = GhibliTheme.getDPSelectedColor(this);
        GradientDrawable fontDrawable = (GradientDrawable) vDPFontColor.getBackground();
        fontDrawable.setColor(fontColor);
        GradientDrawable borderDrawable = (GradientDrawable) vDPBorderColor.getBackground();
        borderDrawable.setColor(borderColor);
        GradientDrawable selectedDrawable = (GradientDrawable) vDPSelectedColor.getBackground();
        selectedDrawable.setColor(selectedColor);
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
