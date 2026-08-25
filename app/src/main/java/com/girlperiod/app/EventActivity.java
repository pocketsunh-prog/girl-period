package com.girlperiod.app;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.girlperiod.app.data.DatabaseHelper;
import com.girlperiod.app.data.Event;
import com.girlperiod.app.data.User;
import com.girlperiod.app.ui.GhibliDatePickerDialog;
import com.girlperiod.app.ui.GhibliTheme;

import java.util.Calendar;

public class EventActivity extends AppCompatActivity {

    private TextView tvTitle;
    private ImageButton btnBack;
    private EditText etEventTitle;
    private TextView btnEventDate;
    private Spinner spinnerReminder;
    private EditText etEventNotes;
    private TextView btnSave;
    private TextView btnDelete;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private User currentUser;

    private Calendar eventDate;
    private boolean isEditMode = false;
    private long editEventId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GhibliTheme.applyTheme(this);
        setContentView(R.layout.activity_event);

        dbHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        currentUser = sessionManager.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        eventDate = Calendar.getInstance();

        initViews();
        setupReminderSpinner();
        checkEditMode();
    }

    private void initViews() {
        tvTitle = findViewById(R.id.tvTitle);
        btnBack = findViewById(R.id.btnBack);
        etEventTitle = findViewById(R.id.etEventTitle);
        btnEventDate = findViewById(R.id.btnEventDate);
        spinnerReminder = findViewById(R.id.spinnerReminder);
        etEventNotes = findViewById(R.id.etEventNotes);
        btnSave = findViewById(R.id.btnSave);
        btnDelete = findViewById(R.id.btnDelete);

        btnBack.setOnClickListener(v -> finish());

        btnEventDate.setOnClickListener(v -> showDatePicker());

        btnSave.setOnClickListener(v -> saveEvent());

        btnDelete.setOnClickListener(v -> confirmDelete());
    }

    private void setupReminderSpinner() {
        String[] reminders = {"1 day before", "2 days before", "3 days before", "1 week before"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, reminders);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerReminder.setAdapter(adapter);
    }

    private void checkEditMode() {
        if (getIntent().hasExtra("event_id")) {
            isEditMode = true;
            editEventId = getIntent().getLongExtra("event_id", -1);
            tvTitle.setText("Edit Event");
            btnDelete.setVisibility(View.VISIBLE);

            // Load event data
            for (Event event : dbHelper.getEventsByUser(currentUser.getId())) {
                if (event.getId() == editEventId) {
                    etEventTitle.setText(event.getTitle());
                    etEventNotes.setText(event.getNotes());

                    // Parse date
                    try {
                        String[] parts = event.getEventDate().split("-");
                        if (parts.length == 3) {
                            eventDate.set(Calendar.YEAR, Integer.parseInt(parts[0]));
                            eventDate.set(Calendar.MONTH, Integer.parseInt(parts[1]) - 1);
                            eventDate.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parts[2]));
                        }
                    } catch (Exception e) {
                        // Use current date
                    }
                    btnEventDate.setText(event.getEventDate());

                    // Set reminder
                    int reminder = event.getReminderDays();
                    if (reminder >= 1 && reminder <= 4) {
                        spinnerReminder.setSelection(reminder - 1);
                    }
                    break;
                }
            }
        } else {
            isEditMode = false;
            editEventId = -1;
            tvTitle.setText("Add Event");
            btnDelete.setVisibility(View.GONE);

            // Set default date
            String dateStr = String.format("%04d-%02d-%02d",
                    eventDate.get(Calendar.YEAR),
                    eventDate.get(Calendar.MONTH) + 1,
                    eventDate.get(Calendar.DAY_OF_MONTH));
            btnEventDate.setText(dateStr);
        }
    }

    private void showDatePicker() {
        new GhibliDatePickerDialog(this, eventDate, date -> {
            eventDate.setTime(date.getTime());
            String dateStr = String.format("%04d-%02d-%02d",
                    date.get(Calendar.YEAR),
                    date.get(Calendar.MONTH) + 1,
                    date.get(Calendar.DAY_OF_MONTH));
            btnEventDate.setText(dateStr);
        }).show();
    }

    private void saveEvent() {
        String title = etEventTitle.getText().toString().trim();
        String notes = etEventNotes.getText().toString().trim();
        int reminderDays = spinnerReminder.getSelectedItemPosition() + 1;

        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter event title", Toast.LENGTH_SHORT).show();
            return;
        }

        String dateStr = String.format("%04d-%02d-%02d",
                eventDate.get(Calendar.YEAR),
                eventDate.get(Calendar.MONTH) + 1,
                eventDate.get(Calendar.DAY_OF_MONTH));

        Event event = new Event();
        event.setUserId(currentUser.getId());
        event.setTitle(title);
        event.setEventDate(dateStr);
        event.setNotes(notes);
        event.setReminderDays(reminderDays);

        if (isEditMode) {
            event.setId(editEventId);
            int rows = dbHelper.updateEvent(event);
            if (rows > 0) {
                Toast.makeText(this, "Event updated!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Failed to update event", Toast.LENGTH_SHORT).show();
            }
        } else {
            long id = dbHelper.addEvent(event);
            if (id > 0) {
                Toast.makeText(this, "Event saved!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Failed to save event", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Event")
                .setMessage("Are you sure you want to delete this event?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    int rows = dbHelper.deleteEvent(editEventId);
                    if (rows > 0) {
                        Toast.makeText(this, "Event deleted", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, "Failed to delete event", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
