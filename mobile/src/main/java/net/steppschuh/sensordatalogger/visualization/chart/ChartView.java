package net.steppschuh.sensordatalogger.visualization.chart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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
    public static final long TIME_RANGE_DEFAULT = TimeUnit.SECONDS.toMillis(30);
    private static final float PADDING_DEFAULT_DP = 16;

    protected DataBatch dataBatch;
    protected long updateInterval = UPDATE_INTERVAL_DEFAULT;

    protected int dataDimension = DATA_DIMENSION_ALL;

    protected Paint dataStrokePaint;
    protected Paint dataFillPaint;
    protected Paint seperatorPaint;
    protected Paint gridLabelPaint;
    protected Paint clearPaint;

    protected int primaryColor;
    protected int primaryColorLight;
    protected int primaryColorDark;
    protected int secondaryColor;
    protected int tertiaryColor;
    protected int accentColor;
    protected int backgroundColor;
    protected int seperatorColor;
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
        seperatorColor = Color.argb(100, 0, 0, 0);

        dimensionColors = new int[3];
        dimensionColors[0] = primaryColor;
        dimensionColors[1] = secondaryColor;
        dimensionColors[2] = tertiaryColor;
    }

    protected void updatePaints() {
        dataStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dataStrokePaint.setColor(primaryColor);
        dataStrokePaint.setStrokeWidth(UnitHelper.convertDpToPixel(1.5f, getContext()));
        dataStrokePaint.setStyle(Paint.Style.STROKE);

        dataFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dataFillPaint.setColor(primaryColor);
        dataFillPaint.setStyle(Paint.Style.FILL);

        seperatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        seperatorPaint.setColor(seperatorColor);
        seperatorPaint.setStyle(Paint.Style.STROKE);
        seperatorPaint.setStrokeWidth(UnitHelper.convertDpToPixel(1, getContext()));

        gridLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridLabelPaint.setColor(seperatorColor);
        gridLabelPaint.setTextSize(15);

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
        updateDimensions();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (renderTimeTracker.getTrackingCount() >= 100) {
            long averageRenderingDuration = renderTimeTracker.calculateAverageDuration();
            if (averageRenderingDuration > 3) {
                Log.w(TAG, "Chart rendering for " + dataBatch.getDataList().size() + " data points took " + averageRenderingDuration + "ms!");
            }
            renderTimeTracker = new TimeTracker("Chart Rendering");
        }
        renderTimeTracker.start();

        super.onDraw(canvas);
        updateTimestamps();

        clearCanvas(canvas);
        drawGrid(canvas);
        drawData(canvas);
        drawDataLabels(canvas);
        drawDataLabels(canvas);

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
}
