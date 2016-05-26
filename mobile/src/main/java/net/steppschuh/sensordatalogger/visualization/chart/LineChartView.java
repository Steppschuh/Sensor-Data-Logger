package net.steppschuh.sensordatalogger.visualization.chart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
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
    }

    @Override
    protected void drawGridLabels(Canvas canvas) {
        super.drawGridLabels(canvas);

        String value = String.valueOf(dataBatch.getNewestData().getTimestamp());
        canvas.drawText(value, paddedStartX, paddedStartY, gridLabelPaint);
    }

    @Override
    protected void drawData(Canvas canvas) {
        super.drawData(canvas);

        try {
            float range = endTimestamp - startTimestamp;
            float mappedRange = paddedEndX - paddedStartX;

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

                    float x = mapValue(data.getTimestamp() - startTimestamp, range, mappedRange);

                    for (int dimension = 0; dimension < data.getValues().length; dimension++) {
                        if (dataDimension != DATA_DIMENSION_ALL && dataDimension != dimension) {
                            continue;
                        }

                        float y = centerY - (data.getValues()[dimension] * 10);

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

            float newestX = mapValue(newestData.getTimestamp() - startTimestamp, range, mappedRange);
            for (int dimension = 0; dimension < newestData.getValues().length; dimension++) {
                // draw all data paths
                Paint dataPathPaint = new Paint(dataStrokePaint);
                dataPathPaint.setColor(dimensionColors[dimension]);
                canvas.drawPath(dataPaths.get(dimension), dataPathPaint);

                // draw starting points
                Paint dataPointPaint = new Paint(dataFillPaint);
                dataPointPaint.setColor(dimensionColors[dimension]);
                float newestY = centerY - (newestData.getValues()[dimension] * 10);
                canvas.drawCircle(newestX, newestY, 10, dataPointPaint);
            }

            drawDataOverlays(canvas);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void drawDataOverlays(Canvas canvas) {
        float fadeOverlayWidth = paddedWidth * fadePercentage;

        if (fadeInNewData) {
            Shader shader = new LinearGradient(paddedEndX - (fadeOverlayWidth), 0, paddedEndX, 0, Color.TRANSPARENT, backgroundColor, Shader.TileMode.CLAMP);
            Paint paint = new Paint();
            paint.setShader(shader);
            canvas.drawRect(new RectF(paddedEndX - (fadeOverlayWidth), paddedStartY, paddedEndX, paddedEndY), paint);
        }

        if (fadeOutOldData) {
            Shader shader = new LinearGradient(paddedStartX, 0, paddedStartX + fadeOverlayWidth, 0, backgroundColor, Color.TRANSPARENT, Shader.TileMode.CLAMP);
            Paint paint = new Paint();
            paint.setShader(shader);
            canvas.drawRect(new RectF(0, paddedStartY, paddedStartX + fadeOverlayWidth, paddedEndY), paint);
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
