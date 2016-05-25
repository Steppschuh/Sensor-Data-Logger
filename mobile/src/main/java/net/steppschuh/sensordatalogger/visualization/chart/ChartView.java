package net.steppschuh.sensordatalogger.visualization.chart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;

import net.steppschuh.datalogger.data.DataBatch;
import net.steppschuh.datalogger.ui.UnitHelper;
import net.steppschuh.sensordatalogger.R;

public abstract class ChartView extends View {

    public static final String TAG = ChartView.class.getSimpleName();

    public static final String TYPE_LINE = LineChartView.class.getSimpleName();

    public static final long UPDATE_INTERVAL_NONE = -1;
    public static final long UPDATE_INTERVAL_DEFAULT = 20;

    private static final float PADDING_DEFAULT_DP = 16;


    protected DataBatch dataBatch;
    protected long updateInterval = UPDATE_INTERVAL_DEFAULT;

    protected Paint linePaint;
    protected Paint seperatorPaint;
    protected Paint gridLabelPaint;
    protected Paint clearPaint;

    protected int primaryColor;
    protected int primaryColorLight;
    protected int primaryColorDark;
    protected int accentColor;
    protected int backgroundColor;
    protected int seperatorColor;

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

    public ChartView(Context context) {
        super(context);
        initializeView();
    }

    public ChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeView();
    }

    private void initializeView() {
        updateColors();
        updatePaints();
        updateDimensions();
    }

    private void updateColors() {
        primaryColor = ContextCompat.getColor(getContext(), R.color.colorPrimary);
        primaryColorLight = ContextCompat.getColor(getContext(), R.color.colorPrimaryLight);
        primaryColorDark = ContextCompat.getColor(getContext(), R.color.colorPrimaryDark);
        accentColor = ContextCompat.getColor(getContext(), R.color.colorAccent);
        backgroundColor = Color.WHITE;
        seperatorColor = Color.argb(100, 0, 0, 0);
    }

    private void updatePaints() {
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(primaryColor);

        seperatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        seperatorPaint.setColor(seperatorColor);
        seperatorPaint.setStyle(Paint.Style.STROKE);
        seperatorPaint.setStrokeWidth(1);

        gridLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridLabelPaint.setColor(seperatorColor);
        gridLabelPaint.setTextSize(15);

        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(Color.BLACK);

        clearPaint = new Paint();
        clearPaint.setColor(backgroundColor);
        clearPaint.setStyle(Paint.Style.FILL);
    }

    private void updateDimensions() {
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
        super.onDraw(canvas);

        clearCanvas(canvas);
        drawGrid(canvas);
        drawData(canvas);
        drawDataLabels(canvas);
        drawDataLabels(canvas);
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

}
