package com.girlperiod.app;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.girlperiod.app.data.DatabaseHelper;
import com.girlperiod.app.data.PeriodRecord;
import com.girlperiod.app.data.User;
import com.girlperiod.app.ui.GhibliTheme;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Add or edit a period record with start date, end date, and notes.
 */
public class AddPeriodActivity extends AppCompatActivity {

    private TextView tvTitle;
    private TextView btnStartDate;
    private TextView btnEndDate;
    private EditText etNotes;
    private ImageButton btnBack;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private Calendar startDateCalendar;
    private Calendar endDateCalendar;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat dbDateFormat;
    private boolean isEditMode = false;
    private long editRecordId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GhibliTheme.applyTheme(this);
        setContentView(R.layout.activity_add_period);

        dbHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);
        startDateCalendar = Calendar.getInstance();
        endDateCalendar = Calendar.getInstance();
        dateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
        dbDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        initViews();
        checkEditMode();
        setupDatePickers();
        setupButtons();
    }

    private void initViews() {
        tvTitle = findViewById(R.id.tvTitle);
        btnStartDate = findViewById(R.id.btnStartDate);
        btnEndDate = findViewById(R.id.btnEndDate);
        etNotes = findViewById(R.id.etNotes);
        btnBack = findViewById(R.id.btnBack);

        updateStartDateDisplay();
        updateEndDateDisplay();
    }

    private void checkEditMode() {
        if (getIntent().hasExtra("selected_date")) {
            long selectedDate = getIntent().getLongExtra("selected_date", System.currentTimeMillis());
            startDateCalendar.setTimeInMillis(selectedDate);
            endDateCalendar.setTimeInMillis(selectedDate);
            updateStartDateDisplay();
            updateEndDateDisplay();
        }
    }

    private void setupDatePickers() {
        btnStartDate.setOnClickListener(v -> {
            DatePickerDialog dialog = new DatePickerDialog(
                    AddPeriodActivity.this,
                    (view, year, month, dayOfMonth) -> {
                        startDateCalendar.set(Calendar.YEAR, year);
                        startDateCalendar.set(Calendar.MONTH, month);
                        startDateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        updateStartDateDisplay();
                    },
                    startDateCalendar.get(Calendar.YEAR),
                    startDateCalendar.get(Calendar.MONTH),
                    startDateCalendar.get(Calendar.DAY_OF_MONTH)
            );
            dialog.show();
        });

        btnEndDate.setOnClickListener(v -> {
            DatePickerDialog dialog = new DatePickerDialog(
                    AddPeriodActivity.this,
                    (view, year, month, dayOfMonth) -> {
                        endDateCalendar.set(Calendar.YEAR, year);
                        endDateCalendar.set(Calendar.MONTH, month);
                        endDateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        updateEndDateDisplay();
                    },
                    endDateCalendar.get(Calendar.YEAR),
                    endDateCalendar.get(Calendar.MONTH),
                    endDateCalendar.get(Calendar.DAY_OF_MONTH)
            );
            dialog.show();
        });
    }

    private void setupButtons() {
        btnBack.setOnClickListener(v -> finish());

        findViewById(R.id.btnSave).setOnClickListener(v -> savePeriodRecord());
    }

    private void savePeriodRecord() {
        if (endDateCalendar.before(startDateCalendar)) {
            Toast.makeText(this, "End date cannot be before start date", Toast.LENGTH_SHORT).show();
            return;
        }

        String startDateStr = dbDateFormat.format(startDateCalendar.getTime());
        String endDateStr = dbDateFormat.format(endDateCalendar.getTime());
        String notes = etNotes.getText().toString().trim();

        User currentUser = sessionManager.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        PeriodRecord record = new PeriodRecord();
        record.setUserId(currentUser.getId());
        record.setStartDate(startDateStr);
        record.setEndDate(endDateStr);
        record.setNotes(notes);

        // Calculate cycle length if there's a previous record
        long userId = currentUser.getId();
        PeriodRecord lastPeriod = dbHelper.getLastPeriod(userId);
        if (lastPeriod != null) {
            java.util.Date lastStart = lastPeriod.getStartDate();
            java.util.Date thisStart = record.getStartDate();
            if (lastStart != null && thisStart != null) {
                long diff = thisStart.getTime() - lastStart.getTime();
                int cycleDays = (int) (diff / (1000 * 60 * 60 * 24));
                if (cycleDays > 14 && cycleDays < 45) {
                    record.setCycleLength(cycleDays);
                }
            }
        }

        long id = dbHelper.addPeriodRecord(record);
        if (id > 0) {
            Toast.makeText(this, "Period record saved!", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Failed to save record", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateStartDateDisplay() {
        btnStartDate.setText(dateFormat.format(startDateCalendar.getTime()));
    }

    private void updateEndDateDisplay() {
        btnEndDate.setText(dateFormat.format(endDateCalendar.getTime()));
    }
}
