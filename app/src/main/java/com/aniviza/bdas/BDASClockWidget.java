package com.aniviza.bdas;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.time.LocalTime;

public class BDASClockWidget extends View {

    private double dilationFactor = 1.0; // Default to no dilation (1:1 ratio)
    private LocalTime currentTime = LocalTime.now();
    private final Paint paint = new Paint();

    public BDASClockWidget(Context context) {
        super(context);
    }

    public BDASClockWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BDASClockWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setDilationFactor(double factor) {
        this.dilationFactor = factor;
        invalidate(); // Redraw the clock with updated dilation
    }

    public void setTime(LocalTime time) {
        this.currentTime = time;
        invalidate(); // Redraw the clock with updated time
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        int radius = Math.min(width, height) / 2;
        int centerX = width / 2;
        int centerY = height / 2;

        // Draw clock circle
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(8);
        paint.setColor(0xFF000000);
        canvas.drawCircle(centerX, centerY, radius - 10, paint);

        // Draw hour hand
        paint.setStrokeWidth(12);
        float hourAngle = (float) (currentTime.getHour() % 12 + currentTime.getMinute() / 60.0) * 30;
        drawHand(canvas, centerX, centerY, radius * 0.5, hourAngle);

        // Draw minute hand
        paint.setStrokeWidth(8);
        float minuteAngle = (float) (currentTime.getMinute() + currentTime.getSecond() / 60.0) * 6;
        drawHand(canvas, centerX, centerY, radius * 0.7, minuteAngle);

        // Draw second hand (scaled by dilation factor)
        paint.setStrokeWidth(4);
        paint.setColor(0xFFFF0000); // Red for the second hand
        float scaledSeconds = (float) (currentTime.getSecond() + currentTime.getNano() / 1_000_000_000.0) * (float) dilationFactor;
        float secondAngle = (scaledSeconds / 60) * 360;
        drawHand(canvas, centerX, centerY, radius * 0.9, secondAngle);
    }

    private void drawHand(Canvas canvas, int centerX, int centerY, double length, float angle) {
        double radians = Math.toRadians(angle - 90); // Subtract 90 to start at 12 o'clock
        float endX = (float) (centerX + length * Math.cos(radians));
        float endY = (float) (centerY + length * Math.sin(radians));
        canvas.drawLine(centerX, centerY, endX, endY, paint);
    }
}
