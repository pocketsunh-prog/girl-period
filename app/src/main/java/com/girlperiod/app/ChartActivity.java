package com.girlperiod.app;

import android.graphics.Color;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.widget.TextView;
import android.widget.Toast;

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
        findViewById(R.id.btnExportPdf).setOnClickListener(v -> exportToPdf());
        findViewById(R.id.btnExportExcel).setOnClickListener(v -> exportToExcel());
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
            avgCycle = 0;
            avgDuration = 0;
            return;
        }

        avgCycle = PeriodPredictor.getAverageCycleLength(records);
        avgDuration = PeriodPredictor.getAveragePeriodDuration(records);

        tvAvgCycle.setText(avgCycle > 0 ? avgCycle + " days" : "28 days");
        tvAvgPeriod.setText(avgDuration + " days");
    }

    private int avgCycle;
    private int avgDuration;

    private void exportToPdf() {
        try {
            PdfDocument document = new PdfDocument();
            int pageWidth = 595; // A4 width in points
            int pageHeight = 842; // A4 height in points
            int yPosition = 50;

            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            android.graphics.Canvas canvas = page.getCanvas();
            android.graphics.Paint paint = new android.graphics.Paint();
            paint.setColor(Color.BLACK);
            paint.setTextSize(24);
            canvas.drawText("Girl Period - Cycle Report", 50, yPosition, paint);
            yPosition += 40;

            paint.setTextSize(16);
            canvas.drawText("Average Cycle: " + (avgCycle > 0 ? avgCycle + " days" : "N/A"), 50, yPosition, paint);
            yPosition += 25;
            canvas.drawText("Average Duration: " + avgDuration + " days", 50, yPosition, paint);
            yPosition += 40;

            paint.setTextSize(14);
            canvas.drawText("Period History:", 50, yPosition, paint);
            yPosition += 25;

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            for (PeriodRecord record : records) {
                if (yPosition > pageHeight - 50) {
                    document.finishPage(page);
                    pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, document.getPages().size() + 1).create();
                    page = document.startPage(pageInfo);
                    canvas = page.getCanvas();
                    yPosition = 50;
                }
                Date startDate = record.getStartDateDate();
                Date endDate = record.getEndDateDate();
                String line = "• " + sdf.format(startDate) + " to " + sdf.format(endDate) +
                        " (" + record.getDuration() + " days)";
                canvas.drawText(line, 50, yPosition, paint);
                yPosition += 20;
            }

            document.finishPage(page);

            // Save to selected folder or default
            String exportUriString = SettingsActivity.getExportFolderUri(this);
            if (exportUriString != null) {
                // Use selected folder via SAF
                android.net.Uri treeUri = android.net.Uri.parse(exportUriString);
                android.net.Uri fileUri = createFileInTree(treeUri, "cycle_report_" + System.currentTimeMillis() + ".pdf", "application/pdf");
                if (fileUri != null) {
                    java.io.OutputStream os = getContentResolver().openOutputStream(fileUri);
                    document.writeTo(os);
                    document.close();
                    os.close();
                    Toast.makeText(this, "PDF exported successfully!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Failed to create file in selected folder", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Use default app directory
                java.io.File dir = new java.io.File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "reports");
                if (!dir.exists()) dir.mkdirs();
                java.io.File file = new java.io.File(dir, "cycle_report_" + System.currentTimeMillis() + ".pdf");
                java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
                document.writeTo(fos);
                document.close();
                fos.close();
                Toast.makeText(this, "PDF exported to: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private android.net.Uri createFileInTree(android.net.Uri treeUri, String fileName, String mimeType) {
        try {
            android.net.Uri docUri = android.provider.DocumentsContract.buildDocumentUriUsingTree(treeUri,
                    android.provider.DocumentsContract.getTreeDocumentId(treeUri));
            android.net.Uri fileUri = android.provider.DocumentsContract.createDocument(getContentResolver(), docUri, mimeType, fileName);
            return fileUri;
        } catch (Exception e) {
            return null;
        }
    }

    private void exportToExcel() {
        try {
            // Create CSV format (can be opened in Excel)
            StringBuilder csv = new StringBuilder();
            csv.append("Girl Period - Cycle Report\n");
            csv.append("Average Cycle:,").append(avgCycle > 0 ? avgCycle + " days" : "N/A").append("\n");
            csv.append("Average Duration:,").append(avgDuration).append(" days\n\n");
            csv.append("Period History\n");
            csv.append("Start Date,End Date,Duration (days),Notes\n");

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            for (PeriodRecord record : records) {
                Date startDate = record.getStartDateDate();
                Date endDate = record.getEndDateDate();
                csv.append(sdf.format(startDate)).append(",");
                csv.append(sdf.format(endDate)).append(",");
                csv.append(record.getDuration()).append(",");
                csv.append(record.getNotes() != null ? record.getNotes() : "").append("\n");
            }

            // Save to selected folder or default
            String exportUriString = SettingsActivity.getExportFolderUri(this);
            if (exportUriString != null) {
                // Use selected folder via SAF
                android.net.Uri treeUri = android.net.Uri.parse(exportUriString);
                android.net.Uri fileUri = createFileInTree(treeUri, "cycle_report_" + System.currentTimeMillis() + ".csv", "text/csv");
                if (fileUri != null) {
                    java.io.OutputStream os = getContentResolver().openOutputStream(fileUri);
                    os.write(csv.toString().getBytes());
                    os.close();
                    Toast.makeText(this, "Excel/CSV exported successfully!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Failed to create file in selected folder", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Use default app directory
                java.io.File dir = new java.io.File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "reports");
                if (!dir.exists()) dir.mkdirs();
                java.io.File file = new java.io.File(dir, "cycle_report_" + System.currentTimeMillis() + ".csv");
                java.io.FileWriter writer = new java.io.FileWriter(file);
                writer.write(csv.toString());
                writer.close();
                Toast.makeText(this, "Excel/CSV exported to: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
