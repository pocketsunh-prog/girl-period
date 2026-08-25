package com.girlperiod.app.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.TextView;

import com.girlperiod.app.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * A Ghibli-styled DatePicker dialog using a custom GridView calendar.
 * Shows only one month at a time with proper day/month alignment.
 */
public class GhibliDatePickerDialog {

    public interface OnDateSelectedListener {
        void onDateSelected(Calendar date);
    }

    private final Context context;
    private Calendar selectedDate;
    private Calendar currentMonth;
    private OnDateSelectedListener listener;
    private TextView tvMonthYear;
    private GridView gridCalendar;
    private CalendarDayAdapter adapter;

    public GhibliDatePickerDialog(Context context, Calendar selectedDate, OnDateSelectedListener listener) {
        this.context = context;
        this.selectedDate = selectedDate != null ? selectedDate : Calendar.getInstance();
        this.currentMonth = (Calendar) this.selectedDate.clone();
        this.listener = listener;
    }

    public void show() {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_ghibli_date_picker, null);

        gridCalendar = dialogView.findViewById(R.id.gridCalendar);
        tvMonthYear = dialogView.findViewById(R.id.tvMonthYear);
        ImageButton btnPrev = dialogView.findViewById(R.id.btnPrev);
        ImageButton btnNext = dialogView.findViewById(R.id.btnNext);

        // Setup calendar
        adapter = new CalendarDayAdapter();
        gridCalendar.setAdapter(adapter);
        updateMonthYear();

        // Navigation buttons
        btnPrev.setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, -1);
            adapter.notifyDataSetChanged();
            updateMonthYear();
        });

        btnNext.setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, 1);
            adapter.notifyDataSetChanged();
            updateMonthYear();
        });

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .create();

        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btnOk).setOnClickListener(v -> {
            if (listener != null) {
                listener.onDateSelected(selectedDate);
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateMonthYear() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvMonthYear.setText(sdf.format(currentMonth.getTime()));
    }

    private List<Integer> getDaysInMonth() {
        List<Integer> days = new ArrayList<>();
        Calendar cal = (Calendar) currentMonth.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);

        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1; // 0=Sunday
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        // Add empty cells for days before the 1st
        for (int i = 0; i < firstDayOfWeek; i++) {
            days.add(0); // 0 = empty cell
        }

        // Add actual days
        for (int day = 1; day <= daysInMonth; day++) {
            days.add(day);
        }

        return days;
    }

    private class CalendarDayAdapter extends BaseAdapter {
        private List<Integer> days;

        CalendarDayAdapter() {
            days = getDaysInMonth();
        }

        @Override
        public int getCount() {
            return days.size();
        }

        @Override
        public Object getItem(int position) {
            return days.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView textView;
            if (convertView == null) {
                textView = new TextView(context);
                textView.setGravity(android.view.Gravity.CENTER);
                textView.setTextSize(14);
                int size = (int) (40 * context.getResources().getDisplayMetrics().density);
                GridView.LayoutParams params = new GridView.LayoutParams(size, size);
                textView.setLayoutParams(params);
            } else {
                textView = (TextView) convertView;
            }

            int day = days.get(position);
            int selectedColor = GhibliTheme.getDPSelectedColor(context);
            int fontColor = GhibliTheme.getDPFontColor(context);

            if (day == 0) {
                // Empty cell
                textView.setText("");
                textView.setBackgroundColor(Color.TRANSPARENT);
            } else {
                textView.setText(String.valueOf(day));

                // Check if this is the selected day
                Calendar dayCal = (Calendar) currentMonth.clone();
                dayCal.set(Calendar.DAY_OF_MONTH, day);
                boolean isSelected = dayCal.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR)
                        && dayCal.get(Calendar.MONTH) == selectedDate.get(Calendar.MONTH)
                        && dayCal.get(Calendar.DAY_OF_MONTH) == selectedDate.get(Calendar.DAY_OF_MONTH);

                // Check if this is today
                Calendar today = Calendar.getInstance();
                boolean isToday = dayCal.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                        && dayCal.get(Calendar.MONTH) == today.get(Calendar.MONTH)
                        && dayCal.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH);

                if (isSelected) {
                    GradientDrawable bg = new GradientDrawable();
                    bg.setShape(GradientDrawable.OVAL);
                    bg.setColor(selectedColor);
                    textView.setBackground(bg);
                    textView.setTextColor(Color.WHITE);
                } else if (isToday) {
                    GradientDrawable bg = new GradientDrawable();
                    bg.setShape(GradientDrawable.OVAL);
                    bg.setStroke(2, selectedColor);
                    textView.setBackground(bg);
                    textView.setTextColor(fontColor);
                } else {
                    textView.setBackgroundColor(Color.TRANSPARENT);
                    textView.setTextColor(fontColor);
                }

                // Click listener
                textView.setOnClickListener(v -> {
                    selectedDate = (Calendar) currentMonth.clone();
                    selectedDate.set(Calendar.DAY_OF_MONTH, day);
                    notifyDataSetChanged();
                });
            }

            return textView;
        }
    }
}
