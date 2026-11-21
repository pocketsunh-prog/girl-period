package com.girlperiod.app.debug;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RemoteViews;

import androidx.appcompat.app.AppCompatActivity;

import com.girlperiod.app.R;
import com.girlperiod.app.widget.CalendarWidgetProvider;

/**
 * Debug-only screen that inflates the home-screen widget layout so it can be
 * inspected (and screenshotted) without placing the widget on the launcher.
 * Not part of the release build.
 */
public class WidgetPreviewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_widget_preview);
        getWindow().getDecorView().setBackgroundColor(Color.rgb(0x0D, 0x0D, 0x0D));

        FrameLayout container = findViewById(R.id.widgetPreviewContainer);
        RemoteViews remoteViews = CalendarWidgetProvider.buildRemoteViews(this, 0);
        View widgetView = remoteViews.apply(this, container);
        container.addView(widgetView,
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
    }
}
