package com.girlperiod.app.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.girlperiod.app.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * A Ghibli-styled DatePicker dialog using a custom calendar.
 * <p>
 * The month grid is rebuilt on every navigation / selection so the weekday columns
 * always match the displayed month (the previous GridView adapter cached the days
 * list once, which left the old layout on screen after tapping prev/next).
 */
public class GhibliDatePickerDialog {

    public interface OnDateSelectedListener {
        void onDateSelected(Calendar date);
    }

    private final Context context;
    private Calendar selectedDate;
    private Calendar currentMonth;
    private final OnDateSelectedListener listener;
    private TextView tvMonthYear;
    private LinearLayout calendarContainer;

    public GhibliDatePickerDialog(Context context, Calendar selectedDate, OnDateSelectedListener listener) {
        this.context = context;
        Calendar start = selectedDate != null ? selectedDate : Calendar.getInstance();
        this.selectedDate = (Calendar) start.clone();
        this.currentMonth = (Calendar) start.clone();
        this.listener = listener;
    }

    public void show() {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_ghibli_date_picker, null);

        calendarContainer = dialogView.findViewById(R.id.calendarContainer);
        tvMonthYear = dialogView.findViewById(R.id.tvMonthYear);
        ImageButton btnPrev = dialogView.findViewById(R.id.btnPrev);
        ImageButton btnNext = dialogView.findViewById(R.id.btnNext);

        buildCalendar();
        updateMonthYear();

        // Navigation buttons - rebuild the grid so the weeks match the new month
        btnPrev.setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, -1);
            buildCalendar();
            updateMonthYear();
        });

        btnNext.setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, 1);
            buildCalendar();
            updateMonthYear();
        });

        // Year selection - click month/year text to pick year
        tvMonthYear.setOnClickListener(v -> showYearPicker());

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .create();

        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btnOk).setOnClickListener(v -> {
            if (listener != null) {
                listener.onDateSelected((Calendar) selectedDate.clone());
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateMonthYear() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvMonthYear.setText(sdf.format(currentMonth.getTime()));
    }

    private void showYearPicker() {
        int currentYear = currentMonth.get(Calendar.YEAR);
        int startYear = currentYear - 50;
        int endYear = currentYear + 50;
        int yearsCount = endYear - startYear + 1;
        final String[] years = new String[yearsCount];
        for (int i = 0; i < yearsCount; i++) {
            years[i] = String.valueOf(startYear + i);
        }

        new AlertDialog.Builder(context)
                .setTitle("Select Year")
                .setItems(years, (dialog, which) -> {
                    int selectedYear = startYear + which;
                    // Reset the day first so Feb 29 / 31st never rolls the month over
                    currentMonth.set(Calendar.DAY_OF_MONTH, 1);
                    currentMonth.set(Calendar.YEAR, selectedYear);
                    buildCalendar();
                    updateMonthYear();
                })
                .show();
    }

    /**
     * Rebuilds the whole month grid. Each week is a horizontal row of seven
     * equal-weight cells, so the day numbers line up exactly with the Sun..Sat headers.
     */
    private void buildCalendar() {
        if (calendarContainer == null) {
            return;
        }
        calendarContainer.removeAllViews();

        Calendar first = (Calendar) currentMonth.clone();
        first.set(Calendar.DAY_OF_MONTH, 1);

        // Sunday-first index of the 1st day of the displayed month
        int firstDayIndex = first.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY;
        int daysInMonth = first.getActualMaximum(Calendar.DAY_OF_MONTH);
        int occupiedCells = firstDayIndex + daysInMonth;
        int rows = (occupiedCells + 6) / 7; // 4..6 weeks

        Calendar today = Calendar.getInstance();
        int rowHeight = dp(40);

        int cellIndex = 0;
        for (int row = 0; row < rows; row++) {
            LinearLayout weekRow = new LinearLayout(context);
            weekRow.setOrientation(LinearLayout.HORIZONTAL);
            weekRow.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, rowHeight));

            for (int col = 0; col < 7; col++, cellIndex++) {
                int day = 0;
                if (cellIndex >= firstDayIndex && cellIndex < occupiedCells) {
                    day = cellIndex - firstDayIndex + 1;
                }
                weekRow.addView(createDayCell(day, today));
            }

            calendarContainer.addView(weekRow);
        }
    }

    /**
     * Creates one calendar cell. {@code day == 0} produces an empty leading/trailing cell.
     * The tappable area fills the column while the circle stays square and centred.
     */
    private View createDayCell(int day, Calendar today) {
        FrameLayout cellView = new FrameLayout(context);
        cellView.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT, 1f));

        TextView tv = new TextView(context);
        tv.setLayoutParams(new FrameLayout.LayoutParams(dp(34), dp(34), Gravity.CENTER));
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(14);
        cellView.addView(tv);

        if (day <= 0) {
            tv.setText("");
            tv.setBackgroundColor(Color.TRANSPARENT);
            return cellView;
        }

        tv.setText(String.valueOf(day));

        Calendar dayCal = (Calendar) currentMonth.clone();
        dayCal.set(Calendar.DAY_OF_MONTH, day);

        int selectedColor = GhibliTheme.getDPSelectedColor(context);
        int fontColor = GhibliTheme.getDPFontColor(context);

        if (sameDay(dayCal, selectedDate)) {
            tv.setBackground(oval(selectedColor, 0, 0));
            tv.setTextColor(Color.WHITE);
        } else if (sameDay(dayCal, today)) {
            tv.setBackground(oval(Color.TRANSPARENT, dp(2), selectedColor));
            tv.setTextColor(fontColor);
        } else {
            tv.setBackgroundColor(Color.TRANSPARENT);
            tv.setTextColor(fontColor);
        }

        cellView.setOnClickListener(v -> {
            selectedDate = (Calendar) currentMonth.clone();
            selectedDate.set(Calendar.DAY_OF_MONTH, day);
            buildCalendar();
        });

        return cellView;
    }

    private static boolean sameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.MONTH) == b.get(Calendar.MONTH)
                && a.get(Calendar.DAY_OF_MONTH) == b.get(Calendar.DAY_OF_MONTH);
    }

    private GradientDrawable oval(int fillColor, int strokeWidthPx, int strokeColor) {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(fillColor);
        if (strokeWidthPx > 0) {
            bg.setStroke(strokeWidthPx, strokeColor);
        }
        return bg;
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
