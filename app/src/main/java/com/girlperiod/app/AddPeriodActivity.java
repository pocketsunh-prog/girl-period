package com.girlperiod.app;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.girlperiod.app.data.DatabaseHelper;
import com.girlperiod.app.data.PeriodRecord;
import com.girlperiod.app.data.User;
import com.girlperiod.app.ui.GhibliDatePickerDialog;
import com.girlperiod.app.ui.GhibliTheme;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
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
    private Button btnSave;
    private Button btnDelete;

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
        btnSave = findViewById(R.id.btnSave);
        btnDelete = findViewById(R.id.btnDelete);

        updateStartDateDisplay();
        updateEndDateDisplay();
    }

    private void checkEditMode() {
        // Check if editing an existing record
        if (getIntent().hasExtra("record_id")) {
            isEditMode = true;
            editRecordId = getIntent().getLongExtra("record_id", -1);
            tvTitle.setText("Edit Period");

            // Load existing record
            PeriodRecord record = null;
            for (PeriodRecord r : dbHelper.getPeriodRecordsByUser(sessionManager.getCurrentUser().getId())) {
                if (r.getId() == editRecordId) {
                    record = r;
                    break;
                }
            }

            if (record != null) {
                Date startDate = record.getStartDateDate();
                Date endDate = record.getEndDateDate();
                if (startDate != null) {
                    startDateCalendar.setTime(startDate);
                }
                if (endDate != null) {
                    endDateCalendar.setTime(endDate);
                }
                etNotes.setText(record.getNotes());
                updateStartDateDisplay();
                updateEndDateDisplay();
            }

            // Show delete button in edit mode
            btnDelete.setVisibility(View.VISIBLE);
        } else {
            isEditMode = false;
            editRecordId = -1;
            tvTitle.setText("Add Period");
            btnDelete.setVisibility(View.GONE);

            // Check if new record with pre-selected date
            if (getIntent().hasExtra("selected_date")) {
                long selectedDate = getIntent().getLongExtra("selected_date", System.currentTimeMillis());
                startDateCalendar.setTimeInMillis(selectedDate);
                endDateCalendar.setTimeInMillis(selectedDate);
                updateStartDateDisplay();
                updateEndDateDisplay();
            }
        }
    }

    private void setupDatePickers() {
        btnStartDate.setOnClickListener(v -> {
            new GhibliDatePickerDialog(AddPeriodActivity.this, startDateCalendar, date -> {
                startDateCalendar.setTime(date.getTime());
                updateStartDateDisplay();
            }).show();
        });

        btnEndDate.setOnClickListener(v -> {
            new GhibliDatePickerDialog(AddPeriodActivity.this, endDateCalendar, date -> {
                endDateCalendar.setTime(date.getTime());
                updateEndDateDisplay();
            }).show();
        });
    }

    private void setupButtons() {
        btnBack.setOnClickListener(v -> finish());

        btnSave.setOnClickListener(v -> {
            if (isEditMode) {
                updatePeriodRecord();
            } else {
                savePeriodRecord();
            }
        });

        btnDelete.setOnClickListener(v -> confirmDelete());
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
            Date lastStart = lastPeriod.getStartDateDate();
            Date thisStart = record.getStartDateDate();
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

    private void updatePeriodRecord() {
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
        record.setId(editRecordId);
        record.setUserId(currentUser.getId());
        record.setStartDate(startDateStr);
        record.setEndDate(endDateStr);
        record.setNotes(notes);

        int rows = dbHelper.updatePeriodRecord(record);
        if (rows > 0) {
            Toast.makeText(this, "Period record updated!", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Failed to update record", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Period")
                .setMessage("Are you sure you want to delete this period record?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    int rows = dbHelper.deletePeriodRecord(editRecordId);
                    if (rows > 0) {
                        Toast.makeText(this, "Period record deleted", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, "Failed to delete record", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateStartDateDisplay() {
        btnStartDate.setText(dateFormat.format(startDateCalendar.getTime()));
    }

    private void updateEndDateDisplay() {
        btnEndDate.setText(dateFormat.format(endDateCalendar.getTime()));
    }
}
