package net.steppschuh.sensordatalogger.visualization;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;
import android.widget.TextView;

import net.steppschuh.sensordatalogger.R;
import net.steppschuh.sensordatalogger.visualization.chart.ChartView;

public class VisualizationCardView extends RelativeLayout {

    private VisualizationCardData data;

    private TextView headingTextView;
    private TextView subHeadingTextView;
    private ChartView chartView;
    private String chartType = ChartView.TYPE_LINE;

    public VisualizationCardView(Context context) {
        super(context);
        initializeLayout(context);
    }

    public VisualizationCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeLayout(context);
    }

    private void initializeLayout(Context context) {
        LayoutInflater.from(context).inflate(R.layout.visualization_card, this);
        headingTextView = (TextView) findViewById(R.id.headingText);
        subHeadingTextView = (TextView) findViewById(R.id.subHeadingText);
        chartView = (ChartView) findViewById(R.id.chartView);
    }

    @Override
    public void requestLayout() {
        renderData();
        super.requestLayout();
    }

    public void renderData() {
        if (data == null) {
            return;
        }
        headingTextView.setText(data.getHeading());
        subHeadingTextView.setText(data.getSubHeading());
        chartView.setDataBatch(data.getDataBatch());
        chartView.invalidate();
    }

    public VisualizationCardData getData() {
        return data;
    }

    public void setData(VisualizationCardData data) {
        this.data = data;
    }

    public String getChartType() {
        return chartType;
    }

    public void setChartType(String chartType) {
        this.chartType = chartType;
    }
}
