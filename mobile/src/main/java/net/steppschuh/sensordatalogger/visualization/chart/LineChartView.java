package net.steppschuh.sensordatalogger.visualization.chart;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.Log;

import net.steppschuh.datalogger.data.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LineChartView extends ChartView {

    protected float fadePercentage = 0.2f;
    protected long fadeTimestampRange;

    protected boolean fadeInNewData = false;
    protected boolean fadeOutOldData = true;

    protected int horizontalGridLinesCount = 5;

    protected Map<Integer, Path> dataPaths;

    protected float currentMinimumValue;
    protected float currentMaximumValue;
    protected float paddedMaximumValue = Float.MAX_VALUE;
    protected float paddedMinimumValue = Float.MIN_VALUE;
    protected float paddedMinimumRange = 1;
    protected float targetPaddedMaximumValue = paddedMaximumValue;
    protected float targetPaddedMinimumValue = paddedMinimumValue;

    float horizontalRange = endTimestamp - startTimestamp;
    float mappedHorizontalRange = paddedEndX - paddedStartX;

    float verticalRange = paddedMaximumValue - paddedMinimumValue;
    float mappedVerticalRange = paddedEndY - paddedStartY;

    private int debugCount = 0;

    public LineChartView(Context context) {
        super(context);
    }

    public LineChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void updateTimestamps() {
        super.updateTimestamps();
        fadeTimestampRange = Math.round((endTimestamp - startTimestamp) * fadePercentage);
    }

    @Override
    protected void drawGrid(Canvas canvas) {
        super.drawGrid(canvas);

        // horizontal grid
        float gridLineOffset = paddedHeight / (horizontalGridLinesCount - 1);
        for (int gridLineIndex = 0; gridLineIndex < horizontalGridLinesCount; gridLineIndex++) {
            try {
                float y = paddedStartY + (gridLineIndex * gridLineOffset);
                canvas.drawLine(paddedStartX + (2 * padding), y, paddedEndX, y, gridLinePaint);

                float value = getTimestampFromMappedVerticalPosition(y);

                String readableValue = String.format("%.02f", value);
                drawTextCentredLeft(canvas, readableValue, paddedStartX, y, gridLabelPaint, new Rect());
            } catch (Exception ex) {
                Log.v(TAG, "Unable to render grid: " + ex.getMessage());
            }
        }
    }

    @Override
    protected void drawGridLabels(Canvas canvas) {
        super.drawGridLabels(canvas);
    }

    @Override
    protected void drawData(Canvas canvas) {
        super.drawData(canvas);

        try {
            Data newestData = dataBatch.getNewestData();
            if (newestData == null) {
                return;
            }

            horizontalRange = endTimestamp - startTimestamp;
            mappedHorizontalRange = paddedEndX - paddedStartX;

            verticalRange = paddedMaximumValue - paddedMinimumValue;
            mappedVerticalRange = paddedEndY - paddedStartY;

            currentMinimumValue = Float.MAX_VALUE;
            currentMaximumValue = Float.MIN_VALUE;

            dataPaths = new HashMap<>();
            for (int dimension = 0; dimension < newestData.getValues().length; dimension++) {
                dataPaths.put(dimension, new Path());
            }

            // add values from each dimension to path
            for (int dataIndex = 0; dataIndex < dataBatch.getDataList().size(); dataIndex++) {
                try {
                    Data data = dataBatch.getDataList().get(dataIndex);
                    float x = getMappedHorizontalPosition(data.getTimestamp());

                    for (int dimension = 0; dimension < data.getValues().length; dimension++) {
                        if (!shouldRenderDimension(dimension)) {
                            continue;
                        }
                        float value = data.getValues()[dimension];
                        currentMinimumValue = Math.min(currentMinimumValue, value);
                        currentMaximumValue = Math.max(currentMaximumValue, value);
                        float y = getMappedVerticalPosition(value);

                        // add data point to path
                        if (dataIndex == 0) {
                            dataPaths.get(dimension).moveTo(x, y);
                        } else {
                            dataPaths.get(dimension).lineTo(x, y);
                        }
                    }
                } catch (Exception ex) {
                    Log.v(TAG, "Unable to render data point: " + dataBatch.getDataList().get(dataIndex));
                }
            }

            float newestX = getMappedHorizontalPosition(newestData.getTimestamp());

            float fadeOverlayWidth = paddedWidth * fadePercentage;
            for (int dimension = 0; dimension < newestData.getValues().length; dimension++) {
                if (!shouldRenderDimension(dimension)) {
                    continue;
                }

                // draw all data paths
                Paint dataPathPaint = new Paint(dataStrokePaint);
                Shader fadeOutShader = new LinearGradient(paddedStartX + (2 * padding), 0, paddedStartX + (2 * padding) + fadeOverlayWidth, 0, Color.TRANSPARENT, dimensionColors[dimension], Shader.TileMode.CLAMP);
                dataPathPaint.setShader(fadeOutShader);
                canvas.drawPath(dataPaths.get(dimension), dataPathPaint);

                // draw starting points
                Paint dataPointPaint = new Paint(dataFillPaint);
                dataPointPaint.setColor(dimensionColors[dimension]);
                float newestY = getMappedVerticalPosition(newestData.getValues()[dimension]);
                canvas.drawCircle(newestX, newestY, 10, dataPointPaint);
            }

            // adjust minimum & maximum values in order to 'zoom' chart
            float currentPaddedMaximumValue = currentMaximumValue + (currentMaximumValue * 0.2f);
            float currentPaddedMinimumValue = currentMinimumValue - (currentMinimumValue * 0.2f);
            float currentPaddedRange = currentPaddedMaximumValue - currentMinimumValue;

            if (currentPaddedRange < paddedMinimumRange) {
                currentPaddedMaximumValue = currentPaddedMaximumValue + (paddedMinimumRange / 2);
                currentPaddedMinimumValue = currentPaddedMinimumValue - (paddedMinimumRange / 2);
            }

            if (currentPaddedMaximumValue != targetPaddedMaximumValue) {
                fadeMaximumValueTo(currentPaddedMaximumValue);
            }
            if (currentPaddedMinimumValue != targetPaddedMinimumValue) {
                fadeMinimumValueTo(currentPaddedMinimumValue);
            }

            // print debug info
            debugCount++;
            if (debugCount >= 50) {
                debugCount = 0;
                System.out.println("Min: " + currentMinimumValue +  " Max: " + currentMaximumValue);
                System.out.println("Padded Min: " + currentPaddedMinimumValue +  " Padded Max: " + currentPaddedMaximumValue);
                System.out.println("Range: " + verticalRange +  " Mapped: " + mappedVerticalRange);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void drawDataLabels(Canvas canvas) {
        super.drawDataLabels(canvas);
    }



    protected float getDataOpacity(Data data) {
        if (fadeInNewData) {
            long timestampOffset = data.getTimestamp() - (endTimestamp - fadeTimestampRange);
            if (timestampOffset < 0) {
                // value is not new
                return 1;
            } else if (timestampOffset > fadeTimestampRange) {
                // value is too new
                return 0;
            } else {
                return timestampOffset / fadeTimestampRange;
            }
        }

        return 1;
    }

    private void fadeMinimumValueTo(float newMinimumValue) {
        targetPaddedMinimumValue = newMinimumValue;
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(paddedMinimumValue, newMinimumValue);
        valueAnimator.setDuration(ChartView.FADE_DURATION_SLOW);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                paddedMinimumValue = (float) animation.getAnimatedValue();
            }
        });
        valueAnimator.start();

    }

    private void fadeMaximumValueTo(float newMaximumValue) {
        targetPaddedMaximumValue = newMaximumValue;
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(paddedMaximumValue, newMaximumValue);
        valueAnimator.setDuration(ChartView.FADE_DURATION_SLOW);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                paddedMaximumValue = (float) animation.getAnimatedValue();
            }
        });
        valueAnimator.start();
    }

    protected float getMappedHorizontalPosition(long timestamp) {
        try {
            return mapValue(timestamp - startTimestamp, horizontalRange, mappedHorizontalRange);
        } catch (Exception ex) {
            return Float.NaN;
        }
    }

    protected float getMappedVerticalPosition(float value) {
        try {
            return mapValue(paddedMaximumValue - value, verticalRange, mappedVerticalRange);
        } catch (Exception ex) {
            return Float.NaN;
        }
    }

    protected long getTimestampFromMappedHorizontalPosition(float x) {
        try {
            //TODO: implement this
            return 0;
        } catch (Exception ex) {
            return 0;
        }
    }

    protected float getTimestampFromMappedVerticalPosition(float y) {
        try {
            return mapValue(y, paddedHeight, paddedMinimumValue - paddedMaximumValue) + paddedMaximumValue;
        } catch (Exception ex) {
            return Float.NaN;
        }
    }

    public static float mapValue(float value, float start, float end, float mappedStart, float mappedEnd) throws Exception {
        float range = end - start;
        if (range <= 0) {
            throw new Exception("Invalid range");
        }
        if (value > range) {
            throw new Exception("Value not in range");
        }
        float mappedRange = mappedEnd - mappedStart;
        return mapValue(value, range, mappedRange);
    }

    public static float mapValue(float value, float range, float mappedRange) throws Exception {
        return (value * mappedRange) / range;
    }

    public static float getMinimumValue(float[] values) {
        if (values.length == 1) {
            return values[0];
        }
        float minimum = Float.MAX_VALUE;
        for (float value : values) {
            minimum = Math.min(minimum, value);
        }
        return minimum;
    }

    public static float getMaximumValue(float[] values) {
        if (values.length == 1) {
            return values[0];
        }
        float maximum = Float.MIN_VALUE;
        for (float value : values) {
            maximum = Math.max(maximum, value);
        }
        return maximum;
    }


}
