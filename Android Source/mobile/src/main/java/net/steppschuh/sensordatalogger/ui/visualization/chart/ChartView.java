package net.steppschuh.sensordatalogger.ui.visualization.chart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import net.steppschuh.datalogger.data.DataBatch;
import net.steppschuh.datalogger.logging.TimeTracker;
import net.steppschuh.datalogger.ui.UnitHelper;
import net.steppschuh.sensordatalogger.R;

import java.util.concurrent.TimeUnit;

public abstract class ChartView extends View {

    public static final String TAG = ChartView.class.getSimpleName();

    public static final String TYPE_LINE = LineChartView.class.getSimpleName();

    public static final long UPDATE_INTERVAL_NONE = -1;
    public static final long UPDATE_INTERVAL_DEFAULT = 20;

    public static final int DATA_DIMENSION_ALL = -1;

    public static final long TIMESTAMP_NOT_SET = -1;
    public static final long TIME_RANGE_DEFAULT = TimeUnit.SECONDS.toMillis(20);
    private static final float PADDING_DEFAULT_DP = 16;

    public static final long FADE_DURATION_FAST = 200;
    public static final long FADE_DURATION_NORMAL = 500;
    public static final long FADE_DURATION_SLOW = 1000;

    protected DataBatch dataBatch;
    protected long updateInterval = UPDATE_INTERVAL_DEFAULT;

    protected int dataDimension = DATA_DIMENSION_ALL;

    protected Paint dataStrokePaint;
    protected Paint dataFillPaint;
    protected Paint gridLinePaint;
    protected Paint gridLabelPaint;
    protected Paint clearPaint;

    protected int primaryColor;
    protected int primaryColorLight;
    protected int primaryColorDark;
    protected int secondaryColor;
    protected int tertiaryColor;
    protected int accentColor;
    protected int backgroundColor;
    protected int gridColor;
    protected int[] dimensionColors;

    protected boolean mapEndTimestampToNow = true;
    protected long timeRange = TIME_RANGE_DEFAULT;
    protected long startTimestamp = TIMESTAMP_NOT_SET;
    protected long endTimestamp = TIMESTAMP_NOT_SET;

    protected float padding;
    protected float paddedStartX;
    protected float paddedStartY;
    protected float paddedEndX;
    protected float paddedEndY;
    protected float paddedWidth;
    protected float paddedHeight;

    protected float centerX;
    protected float centerY;

    private Handler updateHandler;
    private Runnable updateRunnable = getUpdateRunnable();

    private TimeTracker renderTimeTracker;

    public ChartView(Context context) {
        super(context);
        initializeView();
    }

    public ChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeView();
    }

    protected void initializeView() {
        if (isInEditMode()) {
            return;
        }
        updateColors();
        updatePaints();
        updateDimensions();
        renderTimeTracker = new TimeTracker("Chart Rendering");
    }

    protected void updateColors() {
        primaryColor = ContextCompat.getColor(getContext(), R.color.colorPrimary);
        primaryColorLight = ContextCompat.getColor(getContext(), R.color.colorPrimaryLight);
        primaryColorDark = ContextCompat.getColor(getContext(), R.color.colorPrimaryDark);
        secondaryColor = ContextCompat.getColor(getContext(), R.color.colorSecondary);
        tertiaryColor = ContextCompat.getColor(getContext(), R.color.colorTertiary);
        accentColor = ContextCompat.getColor(getContext(), R.color.colorAccent);
        backgroundColor = Color.WHITE;
        gridColor = Color.argb(50, 0, 0, 0);

        int dimensionCount = 30;
        dimensionColors = new int[dimensionCount];
        for (int dimension = 0; dimension < dimensionCount; dimension++) {
            int mod = dimension % 3;
            if (mod == 0) {
                dimensionColors[dimension] = primaryColor;
            } else if (mod == 1) {
                dimensionColors[dimension] = secondaryColor;
            } else {
                dimensionColors[dimension] = tertiaryColor;
            }
        }
    }

    protected void updatePaints() {
        dataStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dataStrokePaint.setColor(primaryColor);
        dataStrokePaint.setStrokeWidth(UnitHelper.convertDpToPixel(1.5f, getContext()));
        dataStrokePaint.setStyle(Paint.Style.STROKE);

        dataFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dataFillPaint.setColor(primaryColor);
        dataFillPaint.setStyle(Paint.Style.FILL);

        gridLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridLinePaint.setColor(gridColor);
        gridLinePaint.setStyle(Paint.Style.STROKE);
        gridLinePaint.setStrokeWidth(UnitHelper.convertDpToPixel(0.5f, getContext()));

        gridLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridLabelPaint.setColor(gridColor);
        gridLabelPaint.setTextSize(25);

        clearPaint = new Paint();
        clearPaint.setColor(backgroundColor);
        clearPaint.setStyle(Paint.Style.FILL);
    }

    protected void updateDimensions() {
        padding = UnitHelper.convertDpToPixel(PADDING_DEFAULT_DP, getContext());
        paddedStartX = padding;
        paddedStartY = padding;
        paddedEndX = getWidth() - padding;
        paddedEndY = getHeight() - padding;
        paddedWidth = paddedEndX - paddedStartX;
        paddedHeight = paddedEndY - paddedStartY;

        centerX = getWidth() / 2;
        centerY = getHeight() / 2;
    }

    protected void updateTimestamps() {
        if (mapEndTimestampToNow || endTimestamp == TIMESTAMP_NOT_SET) {
            endTimestamp = System.currentTimeMillis();
        }
        if (mapEndTimestampToNow || startTimestamp == TIMESTAMP_NOT_SET) {
            startTimestamp = endTimestamp - timeRange;
        }
    }

    public void startUpdateThread() {
        if (updateHandler == null) {
            updateHandler = new Handler(Looper.getMainLooper());
            updateHandler.postDelayed(updateRunnable, 1);
        }
    }

    public void stopUpdateThread() {
        if (updateHandler != null) {
            updateHandler.removeCallbacks(updateRunnable);
            updateHandler = null;
        }
    }

    private Runnable getUpdateRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    invalidate();
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    if (updateHandler != null) {
                        updateHandler.postDelayed(updateRunnable, updateInterval);
                    }
                }
            }
        };
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startUpdateThread();
    }

    @Override
    protected void onDetachedFromWindow() {
        stopUpdateThread();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (isInEditMode()) {
            return;
        }
        updateDimensions();
    }

    protected boolean shouldRenderDimension(int dimension) {
        return dataDimension == DATA_DIMENSION_ALL || dataDimension == dimension;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (isInEditMode()) {
            return;
        }

        if (renderTimeTracker.getTrackingCount() >= 100) {
            long averageRenderingDuration = TimeUnit.NANOSECONDS.toMillis(renderTimeTracker.calculateAverageDuration());
            if (averageRenderingDuration > 3 && dataBatch != null) {
                Log.w(TAG, "Chart rendering for " + dataBatch.getDataList().size() + " data points took " + averageRenderingDuration + "ms!");
            }
            renderTimeTracker = new TimeTracker("Chart Rendering");
        }
        renderTimeTracker.start();

        updateTimestamps();

        clearCanvas(canvas);
        drawGrid(canvas);
        drawData(canvas);
        drawDataLabels(canvas);
        drawGridLabels(canvas);

        renderTimeTracker.stop();
    }

    protected void clearCanvas(Canvas canvas) {
        canvas.drawRect(0, 0, getWidth(), getHeight(), clearPaint);
    }

    protected void drawGrid(Canvas canvas) {

    }

    protected void drawGridLabels(Canvas canvas) {

    }

    protected void drawData(Canvas canvas) {

    }

    protected void drawDataLabels(Canvas canvas) {

    }

    public String getCurrentDimensionName() {
        if (dataBatch == null || dataBatch.getNewestData() == null) {
            return "?";
        }
        if (dataDimension == DATA_DIMENSION_ALL) {
            StringBuilder sb = new StringBuilder();
            int dimensionCount = dataBatch.getNewestData().getValues().length;
            for (int dimension = 0; dimension < dimensionCount; dimension++) {
                sb.append(getDimensionName(dimension));
                if (dimension < dimensionCount - 1) {
                    sb.append(", ");
                }
            }
            return sb.toString();
        } else {
            return getDimensionName(dataDimension);
        }
    }

    public static String getDimensionName(int dimension) {
        if (dimension < 3) {
            return UnitHelper.getCharForNumber(dimension + 23);
        } else {
            return UnitHelper.getCharForNumber(dimension - 3);
        }
    }

    public static void drawTextCentred(Canvas canvas, String text, float cx, float cy, Paint paint, Rect textBounds) {
        paint.getTextBounds(text, 0, text.length(), textBounds);
        canvas.drawText(text, cx - textBounds.exactCenterX(), cy - textBounds.exactCenterY(), paint);
    }

    public static void drawTextCentredLeft(Canvas canvas, String text, float cx, float cy, Paint paint, Rect textBounds) {
        paint.getTextBounds(text, 0, text.length(), textBounds);
        canvas.drawText(text, cx, cy - textBounds.exactCenterY(), paint);
    }

    public static void drawTextCentredRight(Canvas canvas, String text, float cx, float cy, Paint paint, Rect textBounds) {
        paint.getTextBounds(text, 0, text.length(), textBounds);
        canvas.drawText(text, cx - textBounds.width(), cy - textBounds.exactCenterY(), paint);
    }

    /**
     * Getter & Setter
     */
    public DataBatch getDataBatch() {
        return dataBatch;
    }

    public void setDataBatch(DataBatch dataBatch) {
        this.dataBatch = dataBatch;
    }

    public boolean isMappingEndTimestampToNow() {
        return mapEndTimestampToNow;
    }

    public void setMapEndTimestampToNow(boolean mapEndTimestampToNow) {
        this.mapEndTimestampToNow = mapEndTimestampToNow;
    }

    public long getTimeRange() {
        return timeRange;
    }

    public void setTimeRange(long timeRange) {
        this.timeRange = timeRange;
    }

    public long getStartTimestamp() {
        return startTimestamp;
    }

    public void setStartTimestamp(long startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    public long getEndTimestamp() {
        return endTimestamp;
    }

    public void setEndTimestamp(long endTimestamp) {
        this.endTimestamp = endTimestamp;
    }

    public int getDataDimension() {
        return dataDimension;
    }

    public int getCurrentDataDimension() {
        int currentIndex = 0;
        if (dataDimension != ChartView.DATA_DIMENSION_ALL) {
            currentIndex = dataDimension;
        }
        return currentIndex;
    }

    public int getNextDataDimension() {
        if (dataBatch == null || dataBatch.getNewestData() == null) {
            return 0;
        }
        if (dataDimension == DATA_DIMENSION_ALL) {
            return 0;
        } else {
            return (getCurrentDataDimension() + 1) % dataBatch.getNewestData().getValues().length;
        }
    }

    public int getPreviousDataDimension() {
        if (dataBatch == null || dataBatch.getNewestData() == null) {
            return 0;
        }
        if (dataDimension == DATA_DIMENSION_ALL) {
            return dataBatch.getNewestData().getValues().length - 1;
        } else {
            int previousIndex = (getCurrentDataDimension() - 1) % dataBatch.getNewestData().getValues().length;
            if (previousIndex < 0) {
                previousIndex += dataBatch.getNewestData().getValues().length;
            }
            return previousIndex;
        }
    }

    public void setDataDimension(int dataDimension) {
        this.dataDimension = dataDimension;
    }
}
