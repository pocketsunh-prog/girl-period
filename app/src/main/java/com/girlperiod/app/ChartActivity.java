package com.girlperiod.app;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.girlperiod.app.data.DatabaseHelper;
import com.girlperiod.app.data.PeriodRecord;
import com.girlperiod.app.data.User;
import com.girlperiod.app.ui.GhibliTheme;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Analysis charts: cycle length, period duration, and phase distribution.
 */
public class ChartActivity extends AppCompatActivity {

    private static final int COLOR_PINK = 0xFFE8A0BF;
    private static final int COLOR_PINK_DARK = 0xFFD46A8E;
    private static final int COLOR_SAGE = 0xFFB8D4A8;
    private static final int COLOR_LAVENDER = 0xFFC8B8E8;
    private static final int COLOR_PEACH = 0xFFF0C8A8;
    private static final int COLOR_SKY = 0xFFA8D4E8;
    private static final int COLOR_TEXT = 0xFF5C4033;

    private LineChart cycleLineChart;
    private BarChart durationBarChart;
    private PieChart phasePieChart;
    private TextView tvAvgCycle;
    private TextView tvAvgPeriod;

    private DatabaseHelper dbHelper;
    private List<PeriodRecord> records;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GhibliTheme.applyTheme(this);
        setContentView(R.layout.activity_chart);

        dbHelper = new DatabaseHelper(this);

        SessionManager session = new SessionManager(this);
        User currentUser = session.getCurrentUser();
        if (currentUser != null) {
            records = dbHelper.getPeriodRecordsByUser(currentUser.getId());
        }
        if (records == null) {
            records = new ArrayList<>();
        }

        initViews();
        setupCharts();
        displayStatistics();
    }

    private void initViews() {
        cycleLineChart = findViewById(R.id.lineChartCycle);
        durationBarChart = findViewById(R.id.barChartDuration);
        phasePieChart = findViewById(R.id.pieChartPhase);
        tvAvgCycle = findViewById(R.id.tvAvgCycle);
        tvAvgPeriod = findViewById(R.id.tvAvgPeriod);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void setupCharts() {
        setupCycleLineChart();
        setupDurationBarChart();
        setupPhasePieChart();
    }

    private void setupCycleLineChart() {
        if (records.size() < 2) {
            cycleLineChart.setNoDataText("Need at least 2 period records");
            return;
        }

        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        for (int i = 1; i < records.size(); i++) {
            Date startDate1 = records.get(i - 1).getStartDateDate();
            Date startDate2 = records.get(i).getStartDateDate();
            if (startDate1 != null && startDate2 != null) {
                long diff = startDate2.getTime() - startDate1.getTime();
                int days = (int) (diff / (1000 * 60 * 60 * 24));
                if (days > 14 && days < 45) {
                    entries.add(new Entry(entries.size(), days));
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM", Locale.getDefault());
                    labels.add(sdf.format(startDate2));
                }
            }
        }

        if (entries.isEmpty()) {
            cycleLineChart.setNoDataText("Not enough data for cycle chart");
            return;
        }

        LineDataSet dataSet = new LineDataSet(entries, "Cycle Length (days)");
        dataSet.setColor(COLOR_PINK_DARK);
        dataSet.setCircleColor(COLOR_PINK);
        dataSet.setCircleRadius(6f);
        dataSet.setLineWidth(2.5f);
        dataSet.setValueTextSize(11f);
        dataSet.setValueTextColor(COLOR_TEXT);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(COLOR_PINK);
        dataSet.setFillAlpha(80);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawValues(true);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        LineData lineData = new LineData(dataSet);
        cycleLineChart.setData(lineData);
        cycleLineChart.getDescription().setEnabled(false);
        cycleLineChart.setDrawGridBackground(false);

        XAxis xAxis = cycleLineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(COLOR_TEXT);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setGranularity(1f);

        cycleLineChart.getAxisLeft().setTextColor(COLOR_TEXT);
        cycleLineChart.getAxisLeft().setAxisMinimum(0);
        cycleLineChart.getAxisRight().setEnabled(false);

        Legend legend = cycleLineChart.getLegend();
        legend.setTextColor(COLOR_TEXT);
        legend.setForm(Legend.LegendForm.CIRCLE);

        cycleLineChart.animateX(1000);
        cycleLineChart.invalidate();
    }

    private void setupDurationBarChart() {
        if (records.isEmpty()) {
            durationBarChart.setNoDataText("No period records yet");
            return;
        }

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        for (int i = 0; i < records.size(); i++) {
            int duration = records.get(i).getDuration();
            if (duration > 0) {
                entries.add(new BarEntry(entries.size(), duration));
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
                Date startDate = records.get(i).getStartDateDate();
                labels.add(startDate != null ? sdf.format(startDate) : "");
            }
        }

        if (entries.isEmpty()) {
            durationBarChart.setNoDataText("No duration data");
            return;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Period Duration (days)");
        dataSet.setColors(COLOR_SAGE, COLOR_LAVENDER, COLOR_PEACH, COLOR_SKY, COLOR_PINK);
        dataSet.setValueTextSize(11f);
        dataSet.setValueTextColor(COLOR_TEXT);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);
        durationBarChart.setData(barData);
        durationBarChart.getDescription().setEnabled(false);
        durationBarChart.setDrawGridBackground(false);
        durationBarChart.setFitBars(true);

        XAxis xAxis = durationBarChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(COLOR_TEXT);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setGranularity(1f);

        durationBarChart.getAxisLeft().setTextColor(COLOR_TEXT);
        durationBarChart.getAxisLeft().setAxisMinimum(0);
        durationBarChart.getAxisRight().setEnabled(false);

        Legend legend = durationBarChart.getLegend();
        legend.setTextColor(COLOR_TEXT);

        durationBarChart.animateY(1000);
        durationBarChart.invalidate();
    }

    private void setupPhasePieChart() {
        int avgCycle = PeriodPredictor.getAverageCycleLength(records);
        if (avgCycle == 0) avgCycle = 28;

        int menstrualDays = 5;
        int follicularDays = avgCycle - 14 - 5;
        int ovulationDays = 3;
        int lutealDays = avgCycle - 5 - follicularDays - ovulationDays;

        if (follicularDays < 3) follicularDays = 4;
        if (lutealDays < 7) lutealDays = 10;

        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(menstrualDays, "Menstrual"));
        entries.add(new PieEntry(follicularDays, "Follicular"));
        entries.add(new PieEntry(ovulationDays, "Ovulation"));
        entries.add(new PieEntry(lutealDays, "Luteal"));

        PieDataSet dataSet = new PieDataSet(entries, "Cycle Phases");
        dataSet.setColors(COLOR_PINK_DARK, COLOR_SAGE, COLOR_LAVENDER, COLOR_PEACH);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(8f);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return (int) value + "d";
            }
        });

        PieData pieData = new PieData(dataSet);
        phasePieChart.setData(pieData);
        phasePieChart.getDescription().setEnabled(false);
        phasePieChart.setHoleRadius(40f);
        phasePieChart.setTransparentCircleRadius(45f);
        phasePieChart.setDrawEntryLabels(false);
        phasePieChart.setCenterText("Cycle\nPhases");
        phasePieChart.setCenterTextColor(COLOR_TEXT);
        phasePieChart.setCenterTextSize(14f);

        Legend legend = phasePieChart.getLegend();
        legend.setTextColor(COLOR_TEXT);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setForm(Legend.LegendForm.CIRCLE);

        phasePieChart.animateY(1000);
        phasePieChart.invalidate();
    }

    private void displayStatistics() {
        if (records.isEmpty()) {
            tvAvgCycle.setText("N/A");
            tvAvgPeriod.setText("N/A");
            return;
        }

        int avgCycle = PeriodPredictor.getAverageCycleLength(records);
        int avgDuration = PeriodPredictor.getAveragePeriodDuration(records);

        tvAvgCycle.setText(avgCycle > 0 ? avgCycle + " days" : "28 days");
        tvAvgPeriod.setText(avgDuration + " days");
    }
}
