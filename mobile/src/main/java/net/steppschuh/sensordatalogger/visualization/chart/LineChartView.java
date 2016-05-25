package net.steppschuh.sensordatalogger.visualization.chart;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;

import net.steppschuh.datalogger.data.Data;

import java.util.concurrent.TimeUnit;

public class LineChartView extends ChartView {

    public static final long TIMESTAMP_NOT_SET = -1;
    public static final long TIME_RANGE_DEFAULT = TimeUnit.SECONDS.toMillis(10);

    protected boolean mapEndTimestampToNow = true;
    protected long timeRange = TIME_RANGE_DEFAULT;
    protected long startTimestamp = TIMESTAMP_NOT_SET;
    protected long endTimestamp = TIMESTAMP_NOT_SET;

    public LineChartView(Context context) {
        super(context);
    }

    public LineChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private void updateTimestamps() {
        if (mapEndTimestampToNow || endTimestamp == TIMESTAMP_NOT_SET) {
            endTimestamp = System.currentTimeMillis();
        }
        if (mapEndTimestampToNow || startTimestamp == TIMESTAMP_NOT_SET) {
            startTimestamp = endTimestamp - timeRange;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        updateTimestamps();
        super.onDraw(canvas);
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

        for (Data data : dataBatch.getDataSince(startTimestamp)) {
            try {
                float mappedTimestamp = mapTimestamp(data.getTimestamp(), paddedStartX, paddedEndX);
                canvas.drawCircle(mappedTimestamp, centerY - (data.getValues()[0] * 10), 5, linePaint);
            } catch (Exception ex) {
            }
        }
    }

    @Override
    protected void drawDataLabels(Canvas canvas) {
        super.drawDataLabels(canvas);
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
        return (value * mappedRange) / range;
    }
}
