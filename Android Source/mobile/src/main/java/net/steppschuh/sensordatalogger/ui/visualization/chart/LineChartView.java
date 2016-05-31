package net.steppschuh.sensordatalogger.ui.visualization.chart;

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
    protected float currentPadding;
    protected float currentPaddedMaximumValue;
    protected float currentPaddedMinimumValue;
    protected float currentPaddedRange;

    protected float paddedMaximumValue = 100;
    protected float paddedMinimumValue = 0 - paddedMaximumValue;
    protected float paddedMinimumRange = 2;
    protected float targetPaddedMaximumValue = paddedMaximumValue - 1;
    protected float targetPaddedMinimumValue = paddedMinimumValue + 1;

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
        drawHorizontalGrid(canvas);
    }

    protected void drawHorizontalGrid(Canvas canvas) {
        float horizontalLineOffsetX = 2 * padding;
        float horizontalLineDistance = paddedHeight / (horizontalGridLinesCount - 1);

        // check if we should render the line representing the 0 value
        // if not, lines will be centered vertically
        boolean renderZeroLine = false;
        if (paddedMaximumValue > 1 - paddedMinimumRange && paddedMinimumValue < paddedMinimumRange) {
            renderZeroLine = true;
        }

        // calculate vertical grid line positions
        float horizontalLinesX = paddedStartX + horizontalLineOffsetX;
        float[] horizontalLinesY = new float[horizontalGridLinesCount];
        boolean paddedStartYReached = false;
        boolean paddedEndYReached = false;
        for (int lineIndex = 0; lineIndex < horizontalGridLinesCount; lineIndex++) {
            // first grid line
            if (lineIndex == 0) {
                if (renderZeroLine) {
                    // first grid line represents 0 value
                    horizontalLinesY[lineIndex] = getMappedVerticalPosition(0);
                    continue;
                } else {
                    // first grid line is centered vertically
                    horizontalLinesY[lineIndex] = paddedStartY + (paddedHeight / 2);
                    continue;
                }
            }

            // remaining grid lines are altering above (uneven index) and below (even index)
            // the first grid line
            float nextY;
            int gridLineCountFromStart = (int) Math.ceil((lineIndex + 1) / 2);
            if (paddedStartYReached && paddedEndYReached) {
                break;
            } else if (paddedStartYReached) {
                nextY = horizontalLinesY[lineIndex - 1] + horizontalLineDistance;
            } else if (paddedEndYReached) {
                nextY = horizontalLinesY[lineIndex - 1] - horizontalLineDistance;
            } else {
                if (lineIndex % 2 == 0) {
                    nextY = horizontalLinesY[0] + (gridLineCountFromStart * horizontalLineDistance);
                } else {
                    nextY = horizontalLinesY[0] - (gridLineCountFromStart * horizontalLineDistance);
                }
            }

            // if we run out of space (because first line wasn't centered), make use
            // of remaining space above / below the previous grid line
            if (renderZeroLine) {
                if (nextY < paddedStartY && !paddedStartYReached) {
                    paddedStartYReached = true;
                    if (paddedEndYReached) {
                        break;
                    }
                    nextY = horizontalLinesY[lineIndex - 1] + horizontalLineDistance;
                } else if (nextY > paddedEndY && !paddedEndYReached) {
                    paddedEndYReached = true;
                    if (paddedStartYReached) {
                        break;
                    }
                    nextY = horizontalLinesY[lineIndex - 1] - horizontalLineDistance;
                }
            }

            horizontalLinesY[lineIndex] = nextY;
        }

        // set grid label formatting based on how large the values are
        String readableValueStringFormat;
        if (verticalRange < 10) {
            readableValueStringFormat = "%.2f";
        } else if (verticalRange < 100) {
            readableValueStringFormat = "%.1f";
        } else {
            readableValueStringFormat = "%.0f";
        }

        // draw grid lines & labels
        for (int gridLineIndex = 0; gridLineIndex < horizontalLinesY.length; gridLineIndex++) {
            float horizontalLineY = horizontalLinesY[gridLineIndex];
            if (horizontalLineY < paddedStartY || horizontalLineY > paddedEndY) {
                continue;
            }

            // draw grid line
            canvas.drawLine(horizontalLinesX, horizontalLineY, paddedEndX, horizontalLineY, gridLinePaint);

            // draw grid label
            float value = getTimestampFromMappedVerticalPosition(horizontalLinesY[gridLineIndex]);
            String readableValue = String.format(readableValueStringFormat, value);
            drawTextCentredRight(canvas, readableValue, horizontalLinesX - (padding / 2), horizontalLinesY[gridLineIndex], gridLabelPaint, new Rect());
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
            if (dataBatch == null) {
                return;
            }
            Data newestData = dataBatch.getNewestData();
            if (newestData == null) {
                return;
            }

            // update some values needed for calculations
            horizontalRange = endTimestamp - startTimestamp;
            mappedHorizontalRange = paddedEndX - paddedStartX;

            verticalRange = paddedMaximumValue - paddedMinimumValue;
            mappedVerticalRange = paddedEndY - paddedStartY;

            currentMinimumValue = Float.MAX_VALUE;
            currentMaximumValue = Float.MIN_VALUE;

            // prepare data paths for each dimension
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
            Paint dataPathPaint = new Paint(dataStrokePaint);
            Shader fadeOutShader;

            // iterate over dimensions and draw data paths, as well as highlights
            // for the newest values
            for (int dimension = newestData.getValues().length - 1; dimension >= 0; dimension--) {
                if (!shouldRenderDimension(dimension)) {
                    continue;
                }

                // draw all data paths
                if (fadeOutOldData) {
                    fadeOutShader = new LinearGradient(paddedStartX + (2 * padding), 0, paddedStartX + (2 * padding) + fadeOverlayWidth, 0, Color.TRANSPARENT, dimensionColors[dimension], Shader.TileMode.CLAMP);
                    dataPathPaint.setShader(fadeOutShader);
                }
                canvas.drawPath(dataPaths.get(dimension), dataPathPaint);

                // draw starting points
                Paint dataPointPaint = new Paint(dataFillPaint);
                dataPointPaint.setColor(dimensionColors[dimension]);
                float newestY = getMappedVerticalPosition(newestData.getValues()[dimension]);
                canvas.drawCircle(newestX, newestY, 10, dataPointPaint);
            }

            // adjust minimum & maximum values in order to 'zoom' chart
            currentPadding = Math.abs(currentMaximumValue - currentMinimumValue) * 0.2f;
            currentPaddedMaximumValue = currentMaximumValue + currentPadding;
            currentPaddedMinimumValue = currentMinimumValue - currentPadding;
            currentPaddedRange = currentPaddedMaximumValue - currentMinimumValue;

            // avoid 'zooming in' too much
            if (currentPaddedRange < paddedMinimumRange) {
                if (currentPaddedMaximumValue < 0 + paddedMinimumRange && currentPaddedMinimumValue > 0 - paddedMinimumRange) {
                    // center values in paddedMinimumRange around 0
                    currentPaddedMaximumValue = 0 + paddedMinimumRange;
                    currentPaddedMinimumValue = 0 - paddedMinimumRange;
                } else {
                    // expand minimum & maximum to match paddedMinimumRange
                    currentPaddedMaximumValue = currentPaddedMaximumValue + (paddedMinimumRange / 2);
                    currentPaddedMinimumValue = currentPaddedMinimumValue - (paddedMinimumRange / 2);
                }
            }

            // animate minimum & maximum changes, if any
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
                //System.out.println("Min: " + currentMinimumValue + " Max: " + currentMaximumValue);
                //System.out.println("Padded Min: " + currentPaddedMinimumValue + " Padded Max: " + currentPaddedMaximumValue);
                //System.out.println("Range: " + verticalRange + " Mapped: " + mappedVerticalRange);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void drawDataLabels(Canvas canvas) {
        super.drawDataLabels(canvas);
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
