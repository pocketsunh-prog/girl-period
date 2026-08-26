package com.girlperiod.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.girlperiod.app.data.DatabaseHelper;
import com.girlperiod.app.data.Event;
import com.girlperiod.app.data.User;
import com.girlperiod.app.ui.GhibliTheme;

import java.util.List;

public class EventListActivity extends AppCompatActivity {

    private LinearLayout layoutEventList;
    private TextView tvEmpty;
    private ImageButton btnBack;
    private TextView btnAddEvent;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private User currentUser;
    private List<Event> events;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GhibliTheme.applyTheme(this);
        setContentView(R.layout.activity_event_list);

        dbHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        currentUser = sessionManager.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Ensure valid user ID
        if (currentUser.getId() <= 0) {
            User fullUser = dbHelper.getUserByUsername(currentUser.getUsername());
            if (fullUser != null) {
                currentUser = fullUser;
            }
        }

        initViews();
        loadEvents();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEvents();
    }

    private void initViews() {
        layoutEventList = findViewById(R.id.layoutEventList);
        tvEmpty = findViewById(R.id.tvEmpty);
        btnBack = findViewById(R.id.btnBack);
        btnAddEvent = findViewById(R.id.btnAddEvent);

        btnBack.setOnClickListener(v -> finish());
        btnAddEvent.setOnClickListener(v -> {
            Intent intent = new Intent(EventListActivity.this, EventActivity.class);
            startActivity(intent);
        });
    }

    private void loadEvents() {
        events = dbHelper.getEventsByUser(currentUser.getId());
        layoutEventList.removeAllViews();

        if (events == null || events.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            return;
        }

        tvEmpty.setVisibility(View.GONE);
        for (Event event : events) {
            View eventView = createEventItem(event);
            layoutEventList.addView(eventView);
        }
    }

    private View createEventItem(Event event) {
        // Card container
        com.google.android.material.card.MaterialCardView card = new com.google.android.material.card.MaterialCardView(this);
        card.setCardBackgroundColor(getResources().getColor(R.color.card_background));
        card.setRadius(16 * getResources().getDisplayMetrics().density);
        card.setCardElevation(2 * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, 12);
        card.setLayoutParams(cardParams);

        // Content layout
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.HORIZONTAL);
        content.setGravity(Gravity.CENTER_VERTICAL);
        content.setPadding(20, 20, 20, 20);

        // Left side - date
        LinearLayout dateLayout = new LinearLayout(this);
        dateLayout.setOrientation(LinearLayout.VERTICAL);
        dateLayout.setGravity(Gravity.CENTER);

        TextView tvDate = new TextView(this);
        tvDate.setText(formatEventDate(event.getEventDate()));
        tvDate.setTextColor(getResources().getColor(R.color.primary_pink));
        tvDate.setTextSize(14);
        tvDate.setTypeface(null, android.graphics.Typeface.BOLD);
        dateLayout.addView(tvDate);

        content.addView(dateLayout);

        // Middle - title and notes
        LinearLayout infoLayout = new LinearLayout(this);
        infoLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        infoParams.setMargins(16, 0, 16, 0);
        infoLayout.setLayoutParams(infoParams);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(event.getTitle());
        tvTitle.setTextColor(getResources().getColor(R.color.text_primary));
        tvTitle.setTextSize(16);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        infoLayout.addView(tvTitle);

        if (event.getNotes() != null && !event.getNotes().isEmpty()) {
            TextView tvNotes = new TextView(this);
            tvNotes.setText(event.getNotes());
            tvNotes.setTextColor(getResources().getColor(R.color.text_secondary));
            tvNotes.setTextSize(13);
            infoLayout.addView(tvNotes);
        }

        TextView tvReminder = new TextView(this);
        tvReminder.setText(getReminderText(event.getReminderDays()));
        tvReminder.setTextColor(getResources().getColor(R.color.text_secondary));
        tvReminder.setTextSize(12);
        infoLayout.addView(tvReminder);

        content.addView(infoLayout);

        // Right side - actions
        LinearLayout actionsLayout = new LinearLayout(this);
        actionsLayout.setOrientation(LinearLayout.HORIZONTAL);

        // Edit button
        ImageView btnEdit = new ImageView(this);
        btnEdit.setImageResource(R.drawable.ic_edit);
        btnEdit.setBackgroundResource(R.drawable.bg_circle_matcha);
        btnEdit.setPadding(8, 8, 8, 8);
        LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(36, 36);
        editParams.setMargins(0, 0, 8, 0);
        btnEdit.setLayoutParams(editParams);
        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(EventListActivity.this, EventActivity.class);
            intent.putExtra("event_id", event.getId());
            startActivity(intent);
        });
        actionsLayout.addView(btnEdit);

        // Delete button
        ImageView btnDelete = new ImageView(this);
        btnDelete.setImageResource(R.drawable.ic_delete);
        btnDelete.setBackgroundResource(R.drawable.bg_circle_period);
        btnDelete.setPadding(8, 8, 8, 8);
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(36, 36);
        btnDelete.setLayoutParams(deleteParams);
        btnDelete.setOnClickListener(v -> confirmDelete(event));
        actionsLayout.addView(btnDelete);

        content.addView(actionsLayout);
        card.addView(content);

        return card;
    }

    private String formatEventDate(String dateStr) {
        try {
            String[] parts = dateStr.split("-");
            if (parts.length == 3) {
                return parts[2] + "/" + parts[1];
            }
        } catch (Exception e) {
            // Ignore
        }
        return dateStr;
    }

    private String getReminderText(int days) {
        switch (days) {
            case 1: return "1 day before";
            case 2: return "2 days before";
            case 3: return "3 days before";
            case 4: return "1 week before";
            default: return days + " days before";
        }
    }

    private void confirmDelete(Event event) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Event")
                .setMessage("Delete \"" + event.getTitle() + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    dbHelper.deleteEvent(event.getId());
                    loadEvents();
                    Toast.makeText(this, "Event deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
