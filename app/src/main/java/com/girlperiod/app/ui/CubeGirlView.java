package com.girlperiod.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * Custom View that draws a cute cube-shaped girl character in a Ghibli-inspired style.
 * <p>
 * The character has a rounded cube body, simple dot eyes, rosy blush cheeks, and a
 * small smiling mouth. The body colour follows the currently selected Ghibli theme.
 */
public class CubeGirlView extends View {

    private final Paint bodyPaint;
    private final Paint facePaint;
    private final Paint blushPaint;
    private final Paint outlinePaint;
    private final RectF bodyRect;

    private int bodyColor;

    public CubeGirlView(Context context) {
        super(context);
        bodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        facePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        blushPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bodyRect = new RectF();
        bodyColor = GhibliTheme.getPrimaryColor();
        initPaints();
    }

    public CubeGirlView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        bodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        facePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        blushPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bodyRect = new RectF();
        bodyColor = GhibliTheme.getPrimaryColor();
        initPaints();
    }

    public CubeGirlView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        bodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        facePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        blushPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bodyRect = new RectF();
        bodyColor = GhibliTheme.getPrimaryColor();
        initPaints();
    }

    private void initPaints() {
        bodyPaint.setStyle(Paint.Style.FILL);
        bodyPaint.setColor(bodyColor);

        facePaint.setStyle(Paint.Style.FILL);
        facePaint.setColor(Color.parseColor("#3E2723"));

        blushPaint.setStyle(Paint.Style.FILL);
        blushPaint.setColor(Color.parseColor("#F8BBD0"));
        blushPaint.setAlpha(160);

        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setColor(Color.parseColor("#5D4037"));
        outlinePaint.setStrokeWidth(3f);
    }

    /**
     * Updates the body colour to match the given theme and triggers a redraw.
     */
    public void setThemeColor(int color) {
        this.bodyColor = color;
        bodyPaint.setColor(color);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        float padding = w * 0.08f;

        // Body dimensions — a rounded square occupying most of the view
        float bodyLeft = padding;
        float bodyTop = padding;
        float bodyRight = w - padding;
        float bodyBottom = h - padding;
        float cornerRadius = w * 0.18f;

        bodyRect.set(bodyLeft, bodyTop, bodyRight, bodyBottom);

        // Draw shadow
        Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setColor(Color.parseColor("#22000000"));
        canvas.drawRoundRect(
                bodyRect.left + 4, bodyRect.top + 6, bodyRect.right + 4, bodyRect.bottom + 6,
                cornerRadius, cornerRadius, shadowPaint
        );

        // Draw body
        canvas.drawRoundRect(bodyRect, cornerRadius, cornerRadius, bodyPaint);
        canvas.drawRoundRect(bodyRect, cornerRadius, cornerRadius, outlinePaint);

        // Face area — centred horizontally, upper-middle vertically
        float cx = w / 2f;
        float eyeY = bodyTop + (bodyBottom - bodyTop) * 0.38f;
        float eyeSpacing = w * 0.14f;
        float eyeRadius = w * 0.055f;

        // Eyes (simple dots)
        canvas.drawCircle(cx - eyeSpacing, eyeY, eyeRadius, facePaint);
        canvas.drawCircle(cx + eyeSpacing, eyeY, eyeRadius, facePaint);

        // Eye highlights
        Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        highlightPaint.setColor(Color.WHITE);
        float highlightRadius = eyeRadius * 0.4f;
        canvas.drawCircle(cx - eyeSpacing + eyeRadius * 0.3f, eyeY - eyeRadius * 0.3f, highlightRadius, highlightPaint);
        canvas.drawCircle(cx + eyeSpacing + eyeRadius * 0.3f, eyeY - eyeRadius * 0.3f, highlightRadius, highlightPaint);

        // Blush cheeks
        float blushY = eyeY + w * 0.13f;
        float blushSpacing = w * 0.22f;
        float blushRadius = w * 0.07f;
        canvas.drawCircle(cx - blushSpacing, blushY, blushRadius, blushPaint);
        canvas.drawCircle(cx + blushSpacing, blushY, blushRadius, blushPaint);

        // Small smiling mouth
        float mouthY = blushY + w * 0.10f;
        float mouthWidth = w * 0.12f;
        Paint mouthPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mouthPaint.setStyle(Paint.Style.STROKE);
        mouthPaint.setColor(Color.parseColor("#5D4037"));
        mouthPaint.setStrokeWidth(3f);
        mouthPaint.setStrokeCap(Paint.Cap.ROUND);

        RectF mouthRect = new RectF(cx - mouthWidth, mouthY, cx + mouthWidth, mouthY + w * 0.06f);
        canvas.drawArc(mouthRect, 10, 160, false, mouthPaint);
    }
}
