package net.steppschuh.sensordatalogger.visualization.chart;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
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

    protected float maximumValue = Float.MAX_VALUE;
    protected float minimumValue = Float.MIN_VALUE;

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
        float gridLabelOffset = gridLabelPaint.getTextSize() / 2;

        float gridLineOffset = paddedHeight / (horizontalGridLinesCount - 1);
        for (int gridLineIndex = 0; gridLineIndex < horizontalGridLinesCount; gridLineIndex++) {
            try {
                float y = paddedStartY + (gridLineIndex * gridLineOffset);
                canvas.drawLine(paddedStartX + (2 * padding), y, paddedEndX, y, gridLinePaint);

                //float value = mapValue(y, paddedStartY, paddedEndY, maximumValue, minimumValue);
                float value = mapValue(y, paddedStartY, paddedEndY, maximumValue, minimumValue) + maximumValue;
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
            float horizontalRange = endTimestamp - startTimestamp;
            float mappedHorizontalRange = paddedEndX - paddedStartX;

            float verticalRange = maximumValue - minimumValue;
            float mappedVerticalRange = paddedEndY - paddedStartY;

            Data newestData = dataBatch.getNewestData();
            if (newestData == null) {
                return;
            }

            float currentMinimumValue = Float.MAX_VALUE;
            float currentMaximumValue = Float.MIN_VALUE;

            Map<Integer, Path> dataPaths = new HashMap<>();
            for (int dimension = 0; dimension < newestData.getValues().length; dimension++) {
                dataPaths.put(dimension, new Path());
            }

            List<Data> dataList = dataBatch.getDataList();
            for (int dataIndex = 0; dataIndex < dataList.size(); dataIndex++) {
                try {
                    Data data = dataList.get(dataIndex);
                    currentMinimumValue = Math.min(currentMinimumValue, getMinimumValue(data.getValues()));
                    currentMaximumValue = Math.max(currentMaximumValue, getMaximumValue(data.getValues()));

                    float x = mapValue(data.getTimestamp() - startTimestamp, horizontalRange, mappedHorizontalRange);

                    for (int dimension = 0; dimension < data.getValues().length; dimension++) {
                        if (!shouldRenderDimension(dimension)) {
                            continue;
                        }

                        //float y = centerY - (data.getValues()[dimension] * 10);
                        //float y = mapValue(verticalRange - data.getValues()[dimension], verticalRange, mappedVerticalRange);
                        float y = mapValue(maximumValue - data.getValues()[dimension], verticalRange, mappedVerticalRange);

                        // add data point to path
                        if (dataIndex == 0) {
                            dataPaths.get(dimension).moveTo(x, y);
                        } else {
                            dataPaths.get(dimension).lineTo(x, y);
                        }
                    }
                } catch (Exception ex) {
                    Log.v(TAG, "Unable to render data point: " + dataList.get(dataIndex));
                }
            }

            float newestX = mapValue(newestData.getTimestamp() - startTimestamp, horizontalRange, mappedHorizontalRange);

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
                float newestY = mapValue(maximumValue - newestData.getValues()[dimension], verticalRange, mappedVerticalRange);
                canvas.drawCircle(newestX, newestY, 10, dataPointPaint);
            }

            // update vertical range
            //minimumValue = currentMinimumValue + (currentMinimumValue * 0.2f);
            //maximumValue = currentMaximumValue + (currentMaximumValue * 0.2f);

            float newMinimumValue = currentMinimumValue + (currentMinimumValue * 0.2f);
            float newMaximumValue = currentMaximumValue + (currentMaximumValue * 0.2f);

            if (minimumValue != newMinimumValue) {
                fadeMinimumValueTo(newMinimumValue);
            }
            if (maximumValue != newMaximumValue) {
                fadeMaximumValueTo(newMaximumValue);
            }

            debugCount++;
            if (debugCount >= 50) {
                debugCount = 0;

                System.out.println("Min: " + minimumValue +  " Max: " + maximumValue);
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
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(minimumValue, newMinimumValue);
        valueAnimator.setDuration(200);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                minimumValue = (float) animation.getAnimatedValue();
            }
        });
        valueAnimator.start();
    }

    private void fadeMaximumValueTo(float newMaximumValue) {
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(maximumValue, newMaximumValue);
        valueAnimator.setDuration(200);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                maximumValue = (float) animation.getAnimatedValue();
            }
        });
        valueAnimator.start();
    }

    protected float mapTimestamp(long timestamp, float mappedStart, float mappedEnd) throws Exception {
        long trimRange = startTimestamp;
        long trimmedTimestamp = timestamp - trimRange;
        long trimmedStartTimestamp = startTimestamp - trimRange;
        long trimmedEndTimestamp = endTimestamp - trimRange;
        return mapValue(trimmedTimestamp, trimmedStartTimestamp, trimmedEndTimestamp, mappedStart, mappedEnd);
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
