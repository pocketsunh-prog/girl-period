package com.girlperiod.app;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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
import com.girlperiod.app.data.Event;
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
    private TextView tvLocation;
    private TextView tvTemperature;
    private TextView tvUV;
    private TextView tvHumidity;
    private TextView tvRainfall;
    private TextView tvWind;

    private Calendar currentMonth;
    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private List<PeriodRecord> periodRecords;
    private List<Event> events;

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
        tvWind = findViewById(R.id.tvWind);
        tvLocation = findViewById(R.id.tvLocation);

        findViewById(R.id.btnRefreshWeather).setOnClickListener(v -> {
            PermissionHelper permissionHelper = new PermissionHelper(this);
            if (permissionHelper.hasInternetPermission()) {
                // Animate refresh button
                v.animate().rotationBy(360f).setDuration(1000).start();
                updateWeatherSummary();
            } else {
                permissionHelper.requestPermissions();
            }
        });

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

        findViewById(R.id.btnToday).setOnClickListener(v -> {
            currentMonth = Calendar.getInstance();
            setupCalendar();
            updateWeatherSummary();
        });

        // Year selection - click month/year text to pick year
        tvMonthYear.setOnClickListener(v -> showYearPicker());

        com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_calendar) {
                // Calendar icon goes to today
                currentMonth = Calendar.getInstance();
                setupCalendar();
                updateWeatherSummary();
                return true;
            } else if (itemId == R.id.nav_charts) {
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

    private void showYearPicker() {
        int currentYear = currentMonth.get(Calendar.YEAR);
        // Generate year list: 50 years before and after current year
        int startYear = currentYear - 50;
        int endYear = currentYear + 50;
        int yearsCount = endYear - startYear + 1;
        final String[] years = new String[yearsCount];
        for (int i = 0; i < yearsCount; i++) {
            years[i] = String.valueOf(startYear + i);
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Year")
                .setItems(years, (dialog, which) -> {
                    int selectedYear = startYear + which;
                    currentMonth.set(Calendar.YEAR, selectedYear);
                    setupCalendar();
                    updateWeatherSummary();
                })
                .show();
    }

    private void loadPeriodRecords() {
        User currentUser = sessionManager.getCurrentUser();
        if (currentUser != null) {
            periodRecords = dbHelper.getPeriodRecordsByUser(currentUser.getId());
            events = dbHelper.getEventsByUser(currentUser.getId());
        }
        if (periodRecords == null) {
            periodRecords = new ArrayList<>();
        }
        if (events == null) {
            events = new ArrayList<>();
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
            addDayCell(0, false, false, false, false, false, null);
        }

        // Add actual days
        Calendar today = Calendar.getInstance();
        for (int day = 1; day <= daysInMonth; day++) {
            cal.set(Calendar.DAY_OF_MONTH, day);
            boolean isToday = isSameDay(cal, today);
            boolean isPeriod = isPeriodDay(cal);
            boolean isStart = isStartDate(cal);
            boolean isEnd = isEndDate(cal);
            boolean isPredictedDay = isPredictedDay(cal);
            WeatherService.WeatherData weather = getWeatherForDate(cal);
            addDayCell(day, isToday, isPeriod, isStart, isEnd, isPredictedDay, weather);
        }
    }

    private void addDayCell(int day, boolean isToday, boolean isPeriodDay,
                            boolean isStartDate, boolean isEndDate,
                            boolean isPredictedDay, WeatherService.WeatherData weather) {
        View cellView = getLayoutInflater().inflate(R.layout.item_calendar_day, gridCalendar, false);

        TextView tvDayNumber = cellView.findViewById(R.id.tvDayNumber);
        TextView tvLunarDate = cellView.findViewById(R.id.tvLunarDate);
        ImageView ivWeatherIcon = cellView.findViewById(R.id.ivWeatherIcon);
        View vPeriodHighlight = cellView.findViewById(R.id.vPeriodHighlight);
        ImageView vTodayHighlight = cellView.findViewById(R.id.vTodayHighlight);
        View vEventHighlight = cellView.findViewById(R.id.vEventHighlight);

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
            vEventHighlight.setVisibility(View.GONE);
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

        // Check if this day has events
        String dayDateStr = String.format("%04d-%02d-%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                day);
        boolean hasEvent = false;
        for (Event event : events) {
            if (event.getEventDate().equals(dayDateStr)) {
                hasEvent = true;
                break;
            }
        }

        // Apply calendar colors from settings
        int calTextColor = GhibliTheme.getCalendarTextColor(this);
        int calBgColor = GhibliTheme.getCalendarBackgroundColor(this);
        int calStyle = GhibliTheme.getCalendarStyle(this);

        // Apply background color to cell
        GradientDrawable bgDrawable = new GradientDrawable();
        bgDrawable.setShape(GradientDrawable.RECTANGLE);
        bgDrawable.setColor(calBgColor);

        // Apply style (corner radius)
        switch (calStyle) {
            case GhibliTheme.CAL_STYLE_COMPACT:
                bgDrawable.setCornerRadius(4f);
                bgDrawable.setStroke(1, Color.parseColor("#E0E0E0"));
                break;
            case GhibliTheme.CAL_STYLE_ROUNDED:
                bgDrawable.setCornerRadius(16f);
                bgDrawable.setStroke(1, Color.parseColor("#E0E0E0"));
                break;
            case GhibliTheme.CAL_STYLE_MINIMAL:
                bgDrawable.setCornerRadius(0f);
                bgDrawable.setStroke(1, Color.parseColor("#E0E0E0"));
                break;
            default: // CAL_STYLE_DEFAULT
                bgDrawable.setCornerRadius(8f);
                bgDrawable.setStroke(1, Color.parseColor("#E0E0E0"));
                break;
        }
        cellView.setBackground(bgDrawable);

        // Text colors & highlights — event highlight shown when day has events
        if (isStartDate) {
            // Start date: transparent pink highlight, black text, pink border
            vPeriodHighlight.setVisibility(View.VISIBLE);
            vPeriodHighlight.setBackgroundResource(R.drawable.bg_calendar_start_date);
            vEventHighlight.setVisibility(hasEvent ? View.VISIBLE : View.GONE);
            tvDayNumber.setTextColor(getResources().getColor(R.color.black));
            tvLunarDate.setTextColor(getResources().getColor(R.color.black));
        } else if (isEndDate) {
            // End date: transparent purple highlight, black text, purple border
            vPeriodHighlight.setVisibility(View.VISIBLE);
            vPeriodHighlight.setBackgroundResource(R.drawable.bg_calendar_end_date);
            vEventHighlight.setVisibility(hasEvent ? View.VISIBLE : View.GONE);
            tvDayNumber.setTextColor(getResources().getColor(R.color.black));
            tvLunarDate.setTextColor(getResources().getColor(R.color.black));
        } else if (isPeriodDay) {
            // Middle period days: transparent pink highlight, black text
            vPeriodHighlight.setVisibility(View.VISIBLE);
            vPeriodHighlight.setBackgroundResource(R.drawable.bg_circle_period);
            vEventHighlight.setVisibility(hasEvent ? View.VISIBLE : View.GONE);
            tvDayNumber.setTextColor(getResources().getColor(R.color.black));
            tvLunarDate.setTextColor(getResources().getColor(R.color.black));
        } else if (isPredictedDay) {
            vPeriodHighlight.setVisibility(View.VISIBLE);
            vPeriodHighlight.setBackgroundResource(R.drawable.bg_circle_predicted);
            vEventHighlight.setVisibility(hasEvent ? View.VISIBLE : View.GONE);
            tvDayNumber.setTextColor(getResources().getColor(R.color.period_pink_dark));
            tvLunarDate.setTextColor(getResources().getColor(R.color.period_pink_dark));
        } else if (isToday) {
            vPeriodHighlight.setVisibility(View.GONE);
            vTodayHighlight.setVisibility(View.VISIBLE);
            vEventHighlight.setVisibility(hasEvent ? View.VISIBLE : View.GONE);
            tvDayNumber.setTextColor(Color.WHITE);
            tvLunarDate.setTextColor(Color.WHITE);
        } else if (hasEvent) {
            // Event day: orange highlight with black text
            vPeriodHighlight.setVisibility(View.GONE);
            vTodayHighlight.setVisibility(View.GONE);
            vEventHighlight.setVisibility(View.VISIBLE);
            tvDayNumber.setTextColor(getResources().getColor(R.color.black));
            tvLunarDate.setTextColor(getResources().getColor(R.color.black));
        } else {
            vPeriodHighlight.setVisibility(View.GONE);
            vTodayHighlight.setVisibility(View.GONE);
            vEventHighlight.setVisibility(View.GONE);
            tvDayNumber.setTextColor(calTextColor);
            tvLunarDate.setTextColor(getResources().getColor(R.color.text_secondary));
        }

        // Event indicator - show dot if there are events on this day
        View vIndicatorEvent = cellView.findViewById(R.id.vIndicatorEvent);
        vIndicatorEvent.setVisibility(hasEvent ? View.VISIBLE : View.GONE);

        // Click handler
        final int selectedDay = day;
        cellView.setOnClickListener(v -> showDayDetailDialog(selectedDay, isPeriodDay, isStartDate, isEndDate, isPredictedDay, weather));

        gridCalendar.addView(cellView);
    }

    private void showDayDetailDialog(int day, boolean isPeriodDay, boolean isStartDate, boolean isEndDate,
                                     boolean isPredictedDay, WeatherService.WeatherData weather) {
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

        // Find the record ID for start/end dates
        final long[] recordId = {-1};
        if (isStartDate || isEndDate) {
            for (PeriodRecord record : periodRecords) {
                Date startDate = record.getStartDateDate();
                Date endDate = record.getEndDateDate();
                if (startDate != null && endDate != null) {
                    Date checkDate = cal.getTime();
                    if (!checkDate.before(startDate) && !checkDate.after(endDate)) {
                        recordId[0] = record.getId();
                        break;
                    }
                }
            }
        }

        if (isStartDate) {
            message.append("\nStart Date");
        } else if (isEndDate) {
            message.append("\nEnd Date");
        } else if (isPeriodDay) {
            message.append("\nPeriod Day");
        } else if (isPredictedDay) {
            message.append("\nPredicted Period");
        }

        // Show events for this day
        String dayDateStr = String.format("%04d-%02d-%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                day);
        List<Event> dayEvents = new ArrayList<>();
        for (Event event : events) {
            if (event.getEventDate().equals(dayDateStr)) {
                dayEvents.add(event);
            }
        }
        if (!dayEvents.isEmpty()) {
            message.append("\n\nEvents:");
            for (Event event : dayEvents) {
                message.append("\n• ").append(event.getTitle());
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Day Details")
                .setMessage(message.toString());

        if (recordId[0] > 0) {
            // Show Edit/Delete buttons for period dates
            builder.setPositiveButton("Edit Period", (dialog, which) -> {
                Intent intent = new Intent(MainActivity.this, AddPeriodActivity.class);
                intent.putExtra("record_id", recordId[0]);
                startActivity(intent);
            });
            builder.setNegativeButton("Delete Period", (dialog, which) -> {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Delete Period")
                        .setMessage("Are you sure you want to delete this period record?")
                        .setPositiveButton("Delete", (d, w) -> {
                            dbHelper.deletePeriodRecord(recordId[0]);
                            loadPeriodRecords();
                            setupCalendar();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
            if (!dayEvents.isEmpty()) {
                builder.setNeutralButton("Edit Event", (dialog, which) -> {
                    Intent intent = new Intent(MainActivity.this, EventActivity.class);
                    intent.putExtra("event_id", dayEvents.get(0).getId());
                    startActivity(intent);
                });
            } else {
                builder.setNeutralButton("Add Event", (dialog, which) -> {
                    Intent intent = new Intent(MainActivity.this, EventActivity.class);
                    intent.putExtra("selected_date", cal.getTimeInMillis());
                    startActivity(intent);
                });
            }
        } else if (!dayEvents.isEmpty()) {
            // Show Edit/Delete buttons for event dates
            builder.setPositiveButton("Edit Event", (dialog, which) -> {
                Intent intent = new Intent(MainActivity.this, EventActivity.class);
                intent.putExtra("event_id", dayEvents.get(0).getId());
                startActivity(intent);
            });
            builder.setNegativeButton("Delete Event", (dialog, which) -> {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Delete Event")
                        .setMessage("Are you sure you want to delete this event?")
                        .setPositiveButton("Delete", (d, w) -> {
                            dbHelper.deleteEvent(dayEvents.get(0).getId());
                            loadPeriodRecords();
                            setupCalendar();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
            builder.setNeutralButton("Add Period", (dialog, which) -> {
                Intent intent = new Intent(MainActivity.this, AddPeriodActivity.class);
                intent.putExtra("selected_date", cal.getTimeInMillis());
                startActivity(intent);
            });
        } else {
            // Show Add Period and Add Event buttons for non-period dates
            builder.setPositiveButton("Add Event", (dialog, which) -> {
                Intent intent = new Intent(MainActivity.this, EventActivity.class);
                intent.putExtra("selected_date", cal.getTimeInMillis());
                startActivity(intent);
            });
            builder.setNegativeButton("Add Period", (dialog, which) -> {
                Intent intent = new Intent(MainActivity.this, AddPeriodActivity.class);
                intent.putExtra("selected_date", cal.getTimeInMillis());
                startActivity(intent);
            });
            builder.setNeutralButton("OK", null);
        }

        builder.show();
    }

    private void updateWeatherSummary() {
        // Show loading state
        tvTemperature.setText("--°C");
        tvUV.setText("UV --");
        tvHumidity.setText("--%");
        tvRainfall.setText("--mm");
        tvWind.setText("--km/h");

        // Update location display from saved user data
        updateLocationDisplay();

        // Fetch real weather data from HKO API
        HkoWeatherService hkoService = new HkoWeatherService(this);

        if (!hkoService.isNetworkAvailable()) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            // Use mock data
            WeatherService.WeatherData mockData = WeatherService.getCurrentWeather("Hong Kong");
            updateWeatherUI(mockData);
            return;
        }

        hkoService.fetchCurrentWeather(new HkoWeatherService.OnWeatherDataListener() {
            @Override
            public void onSuccess(HkoWeatherService.WeatherData hkoData) {
                WeatherService.WeatherData data = new WeatherService.WeatherData();
                data.setTemperature(hkoData.temperature);
                data.setHumidity(hkoData.humidity);
                data.setRainfall(hkoData.rainfall);
                data.setWindSpeed(hkoData.windSpeed);
                data.setWindDirection(hkoData.windDirection);
                data.setUvIndex(hkoData.uvIndex);
                data.setIconCode(hkoData.iconCode);
                data.setDescription(hkoData.description);
                updateWeatherUI(data);
                Toast.makeText(MainActivity.this, "Weather updated!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(MainActivity.this, "Weather error: " + error, Toast.LENGTH_SHORT).show();
                // Fallback to mock data
                WeatherService.WeatherData mockData = WeatherService.getCurrentWeather("Hong Kong");
                updateWeatherUI(mockData);
            }
        });
    }

    private void updateWeatherUI(WeatherService.WeatherData data) {
        if (data != null) {
            tvTemperature.setText(String.format(Locale.getDefault(), "%.0f°C", data.getTemperature()));
            tvUV.setText("UV " + data.getUvIndex());
            tvHumidity.setText(data.getHumidity() + "%");
            tvRainfall.setText(String.format(Locale.getDefault(), "%.1fmm", data.getRainfall()));
            tvWind.setText(String.format(Locale.getDefault(), "%.0fkm/h", data.getWindSpeed()));
        }
    }

    private void updateLocationDisplay() {
        if (sessionManager != null && sessionManager.isLoggedIn()) {
            User sessionUser = sessionManager.getCurrentUser();
            if (sessionUser != null) {
                // Get full user data from database
                User fullUser = dbHelper.getUserById(sessionUser.getId());
                if (fullUser == null) {
                    // Try to get by username
                    fullUser = dbHelper.getUserByUsername(sessionUser.getUsername());
                }
                if (fullUser != null && fullUser.getCityName() != null && !fullUser.getCityName().isEmpty()) {
                    tvLocation.setText(fullUser.getCityName());
                } else if (fullUser != null && (fullUser.getLatitude() != 0 || fullUser.getLongitude() != 0)) {
                    tvLocation.setText(String.format("%.4f, %.4f", fullUser.getLatitude(), fullUser.getLongitude()));
                } else {
                    tvLocation.setText("Set location");
                }
            } else {
                tvLocation.setText("Set location");
            }
        } else {
            tvLocation.setText("Set location");
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

    private boolean isStartDate(Calendar cal) {
        for (PeriodRecord record : periodRecords) {
            Date startDate = record.getStartDateDate();
            if (startDate != null) {
                Calendar startCal = Calendar.getInstance();
                startCal.setTime(startDate);
                if (isSameDay(startCal, cal)) return true;
            }
        }
        return false;
    }

    private boolean isEndDate(Calendar cal) {
        for (PeriodRecord record : periodRecords) {
            Date endDate = record.getEndDateDate();
            if (endDate != null) {
                Calendar endCal = Calendar.getInstance();
                endCal.setTime(endDate);
                if (isSameDay(endCal, cal)) return true;
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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionHelper permissionHelper = new PermissionHelper(this);
        permissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // If permissions granted, refresh weather
        if (requestCode == PermissionHelper.PERMISSION_REQUEST_CODE && permissionHelper.hasInternetPermission()) {
            updateWeatherSummary();
        }
    }
}
