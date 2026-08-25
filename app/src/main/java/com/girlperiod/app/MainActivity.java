package com.girlperiod.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.girlperiod.app.data.DatabaseHelper;
import com.girlperiod.app.data.PeriodRecord;
import com.girlperiod.app.data.User;
import com.girlperiod.app.ui.GhibliTheme;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Main calendar screen showing period tracking with lunar dates and weather.
 */
public class MainActivity extends AppCompatActivity {

    private GridLayout gridCalendar;
    private TextView tvMonthYear;
    private TextView tvTemperature;
    private TextView tvUV;
    private TextView tvHumidity;
    private TextView tvRainfall;

    private Calendar currentMonth;
    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private List<PeriodRecord> periodRecords;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GhibliTheme.applyTheme(this);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);
        currentMonth = Calendar.getInstance();
        periodRecords = new ArrayList<>();

        initViews();
        loadPeriodRecords();
        setupCalendar();
        updateWeatherSummary();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPeriodRecords();
        setupCalendar();
    }

    private void initViews() {
        gridCalendar = findViewById(R.id.gridCalendar);
        tvMonthYear = findViewById(R.id.tvMonthYear);
        tvTemperature = findViewById(R.id.tvTemperature);
        tvUV = findViewById(R.id.tvUV);
        tvHumidity = findViewById(R.id.tvHumidity);
        tvRainfall = findViewById(R.id.tvRainfall);

        findViewById(R.id.btnPrevMonth).setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, -1);
            setupCalendar();
            updateWeatherSummary();
        });

        findViewById(R.id.btnNextMonth).setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, 1);
            setupCalendar();
            updateWeatherSummary();
        });

        findViewById(R.id.fabAddPeriod).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddPeriodActivity.class);
            startActivity(intent);
        });

        com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_charts) {
                Intent intent = new Intent(MainActivity.this, ChartActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_settings) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
                return true;
            }
            return false;
        });
    }

    private void loadPeriodRecords() {
        User currentUser = sessionManager.getCurrentUser();
        if (currentUser != null) {
            periodRecords = dbHelper.getPeriodRecordsByUser(currentUser.getId());
        }
        if (periodRecords == null) {
            periodRecords = new ArrayList<>();
        }
    }

    private void setupCalendar() {
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvMonthYear.setText(monthFormat.format(currentMonth.getTime()));

        Calendar cal = (Calendar) currentMonth.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1;
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        gridCalendar.removeAllViews();
        gridCalendar.setColumnCount(7);
        gridCalendar.setRowCount(6);

        // Add empty cells before the 1st
        for (int i = 0; i < firstDayOfWeek; i++) {
            addDayCell(0, false, false, false, null);
        }

        // Add actual days
        Calendar today = Calendar.getInstance();
        for (int day = 1; day <= daysInMonth; day++) {
            cal.set(Calendar.DAY_OF_MONTH, day);
            boolean isToday = isSameDay(cal, today);
            boolean isPeriodDay = isPeriodDay(cal);
            boolean isPredictedDay = isPredictedDay(cal);
            WeatherService.WeatherData weather = getWeatherForDate(cal);
            addDayCell(day, isToday, isPeriodDay, isPredictedDay, weather);
        }
    }

    private void addDayCell(int day, boolean isToday, boolean isPeriodDay,
                            boolean isPredictedDay, WeatherService.WeatherData weather) {
        View cellView = getLayoutInflater().inflate(R.layout.item_calendar_day, gridCalendar, false);

        TextView tvDayNumber = cellView.findViewById(R.id.tvDayNumber);
        TextView tvLunarDate = cellView.findViewById(R.id.tvLunarDate);
        ImageView ivWeatherIcon = cellView.findViewById(R.id.ivWeatherIcon);
        View vPeriodHighlight = cellView.findViewById(R.id.vPeriodHighlight);
        View vTodayHighlight = cellView.findViewById(R.id.vTodayHighlight);

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(2, 2, 2, 2);
        cellView.setLayoutParams(params);

        if (day == 0) {
            tvDayNumber.setText("");
            tvLunarDate.setText("");
            ivWeatherIcon.setVisibility(View.GONE);
            vPeriodHighlight.setVisibility(View.GONE);
            vTodayHighlight.setVisibility(View.GONE);
            cellView.setOnClickListener(null);
            gridCalendar.addView(cellView);
            return;
        }

        tvDayNumber.setText(String.valueOf(day));

        // Lunar date
        Calendar cal = (Calendar) currentMonth.clone();
        cal.set(Calendar.DAY_OF_MONTH, day);
        String lunar = LunarCalendar.getLunarDate(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH));
        // Show just the day part for brevity
        if (lunar.length() > 2) {
            tvLunarDate.setText(lunar.substring(lunar.length() - 2));
        } else {
            tvLunarDate.setText(lunar);
        }

        // Weather icon
        if (weather != null) {
            int iconRes = getWeatherIconResource(weather.getDescription());
            ivWeatherIcon.setImageResource(iconRes);
        }

        // Highlights
        if (isPeriodDay) {
            vPeriodHighlight.setVisibility(View.VISIBLE);
        } else if (isPredictedDay) {
            vPeriodHighlight.setVisibility(View.VISIBLE);
            vPeriodHighlight.setBackgroundResource(R.drawable.bg_circle_predicted);
        } else {
            vPeriodHighlight.setVisibility(View.GONE);
        }

        if (isToday) {
            vTodayHighlight.setVisibility(View.VISIBLE);
        } else {
            vTodayHighlight.setVisibility(View.GONE);
        }

        // Click handler
        final int selectedDay = day;
        cellView.setOnClickListener(v -> showDayDetailDialog(selectedDay, isPeriodDay, isPredictedDay, weather));

        gridCalendar.addView(cellView);
    }

    private void showDayDetailDialog(int day, boolean isPeriodDay, boolean isPredictedDay,
                                     WeatherService.WeatherData weather) {
        Calendar cal = (Calendar) currentMonth.clone();
        cal.set(Calendar.DAY_OF_MONTH, day);

        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault());
        String dateStr = sdf.format(cal.getTime());
        String lunarDate = LunarCalendar.getLunarDate(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH));

        StringBuilder message = new StringBuilder();
        message.append("Date: ").append(dateStr).append("\n");
        message.append("Lunar: ").append(lunarDate).append("\n\n");

        if (weather != null) {
            message.append("Temperature: ").append(String.format(Locale.getDefault(), "%.1f", weather.getTemperature())).append("°C\n");
            message.append("UV Index: ").append(weather.getUvIndex()).append("\n");
            message.append("Humidity: ").append(weather.getHumidity()).append("%\n");
            message.append("Rainfall: ").append(String.format(Locale.getDefault(), "%.1f", weather.getRainfall())).append("mm\n");
            message.append("Condition: ").append(weather.getDescription()).append("\n");
        }

        if (isPeriodDay) {
            message.append("\nPeriod Day");
        } else if (isPredictedDay) {
            message.append("\nPredicted Period");
        }

        new AlertDialog.Builder(this)
                .setTitle("Day Details")
                .setMessage(message.toString())
                .setPositiveButton("OK", null)
                .setNeutralButton("Add Period", (dialog, which) -> {
                    Intent intent = new Intent(MainActivity.this, AddPeriodActivity.class);
                    intent.putExtra("selected_date", cal.getTimeInMillis());
                    startActivity(intent);
                })
                .show();
    }

    private void updateWeatherSummary() {
        Calendar cal = Calendar.getInstance();
        WeatherService.WeatherData weather = WeatherService.getCurrentWeather("上海");
        if (weather != null) {
            tvTemperature.setText(String.format(Locale.getDefault(), "%.0f°C", weather.getTemperature()));
            tvUV.setText("UV " + weather.getUvIndex());
            tvHumidity.setText(weather.getHumidity() + "%");
            tvRainfall.setText(String.format(Locale.getDefault(), "%.1fmm", weather.getRainfall()));
        }
    }

    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
                && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    private boolean isPeriodDay(Calendar cal) {
        for (PeriodRecord record : periodRecords) {
            Date startDate = record.getStartDateDate();
            Date endDate = record.getEndDateDate();
            if (startDate != null && endDate != null) {
                Date checkDate = cal.getTime();
                if (!checkDate.before(startDate) && !checkDate.after(endDate)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isPredictedDay(Calendar cal) {
        if (periodRecords.isEmpty()) return false;

        int daysUntil = PeriodPredictor.getDaysUntilNextPeriod(periodRecords);
        if (daysUntil < 0) return false;

        Calendar today = Calendar.getInstance();
        Calendar predicted = (Calendar) today.clone();
        predicted.add(Calendar.DAY_OF_MONTH, daysUntil);

        // Mark 3 days around predicted start
        for (int i = -1; i <= 1; i++) {
            Calendar check = (Calendar) predicted.clone();
            check.add(Calendar.DAY_OF_MONTH, i);
            if (isSameDay(check, cal)) return true;
        }
        return false;
    }

    private WeatherService.WeatherData getWeatherForDate(Calendar cal) {
        return WeatherService.getWeatherForDate("上海", cal.getTime());
    }

    private int getWeatherIconResource(String description) {
        if (description == null) return R.drawable.ic_weather_sun;
        switch (description) {
            case "晴朗":
                return R.drawable.ic_weather_sun;
            case "多云":
                return R.drawable.ic_weather_cloud;
            case "小雨":
            case "中雨":
            case "大雨":
            case "雷阵雨":
                return R.drawable.ic_weather_rain;
            default:
                return R.drawable.ic_weather_sun;
        }
    }
}
