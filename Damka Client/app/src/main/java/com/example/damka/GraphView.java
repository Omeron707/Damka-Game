package com.example.damka;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.Map;
import java.util.TreeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class GraphView extends View {

    private Paint paint;
    private TreeMap<Float, Float> data;

    public GraphView(Context context) {
        super(context);
        init();
    }

    public GraphView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GraphView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setStrokeWidth(5);
        paint.setStyle(Paint.Style.STROKE);

        data = new TreeMap<>();
    }

    public void setData(Map<Float, Float> data) {
        this.data.clear();
        this.data.putAll(data);
        invalidate(); // Request a redraw
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        // Draw x and y axis
        paint.setColor(Color.WHITE);
        canvas.drawLine(100, 50, 100, getHeight() - 100, paint); // Y-axis
        canvas.drawLine(100, getHeight() - 100, getWidth() - 50, getHeight() - 100, paint); // X-axis

        // Draw graph
        if (data != null && data.size() > 1) {
            float[] xValues = new float[data.size()];
            float[] yValues = new float[data.size()];
            int i = 0;
            for (Map.Entry<Float, Float> entry : data.entrySet()) {
                xValues[i] = entry.getKey();
                yValues[i] = entry.getValue();
                i++;
            }

            float xMin = getMin(xValues);
            float xMax = getMax(xValues);
            float yMin = getMin(yValues) - (getMax(yValues) - getMin(yValues)); // put the lowest point in the middle
            float yMax = getMax(yValues);

            float xInterval = (getWidth() - 200) / (xMax - xMin);
            float yInterval = (getHeight() - 200) / (yMax - yMin);

            float prevX = -1;
            float prevY = -1;

            for (Map.Entry<Float, Float> entry : data.entrySet()) {
                float x = 100 + (entry.getKey() - xMin) * xInterval;
                float y = getHeight() - 100 - (entry.getValue() - yMin) * yInterval;

                // Set color based on line direction
                if (prevY != -1 && prevY < y) { // Going down
                    paint.setColor(Color.RED);
                } else if (prevY != -1 && prevY > y) {
                    paint.setColor(Color.GREEN); // Going up
                } else {
                    paint.setColor(Color.BLUE);  // Initial color
                }

                // Draw circle for the data point
                canvas.drawCircle(x, y, 10, paint);

                // Connect with previous point with a line
                if (prevX != -1 && prevY != -1) {
                    canvas.drawLine(prevX, prevY, x, y, paint);
                }

                prevX = x;
                prevY = y;
            }

            // Draw Y-axis labels
            paint.setColor(Color.WHITE);
            paint.setTextSize(30);
            paint.setStrokeWidth(2); // Make text thinner
            for (float j = yMin; j <= yMax; j += (yMax - yMin) / 5) { // number of labels
                float yLabel = getHeight() - 100 - (j - yMin) * yInterval;
                canvas.drawText(String.valueOf((int) j), 20, yLabel, paint);
            }
        }
    }

    private float getMin(float[] array) {
        float min = array[0];
        for (float value : array) {
            if (value < min) {
                min = value;
            }
        }
        return min;
    }

    private float getMax(float[] array) {
        float max = array[0];
        for (float value : array) {
            if (value > max) {
                max = value;
            }
        }
        return max;
    }
}