package net.steppschuh.sensordatalogger.visualization;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;
import android.widget.TextView;

import net.steppschuh.datalogger.data.Data;
import net.steppschuh.datalogger.data.DataBatch;
import net.steppschuh.sensordatalogger.R;
import net.steppschuh.sensordatalogger.visualization.chart.ChartView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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

        new Thread(new Runnable() {
            @Override
            public void run() {
                long minimumTimestamp = chartView.getStartTimestamp() - TimeUnit.SECONDS.toMillis(1);
                DataBatch processedDataBatch = new DataBatch(data.getDataBatch());
                List<Data> processedData = getProcessedDataList(data.getDataBatch().getDataSince(minimumTimestamp));
                processedDataBatch.setDataList(processedData);
                chartView.setDataBatch(processedDataBatch);
            }
        }).start();
    }

    public static List<Data> getProcessedDataList(List<Data> unprocessedData) {
        List<Data> processedData = new ArrayList<>();

        int currentlySkippedDataCount = 0;
        Data lastAddedData = null;

        int maximumSkipCount = unprocessedData.size() / 5;

        for (int dataIndex = 0; dataIndex < unprocessedData.size(); dataIndex++) {
            Data currentData = unprocessedData.get(dataIndex);

            boolean forceAdding = false;
            if (dataIndex < maximumSkipCount) {
                forceAdding = true;
            } else if (dataIndex > unprocessedData.size() - maximumSkipCount) {
                forceAdding = true;
            } else if (currentlySkippedDataCount > maximumSkipCount) {
                forceAdding = true;
            }

            if (lastAddedData == null || forceAdding) {
                lastAddedData = currentData;
                processedData.add(lastAddedData);
                continue;
            }

            boolean hasEqualValues = true;
            for (int dimension = 0; dimension < currentData.getValues().length; dimension++) {
                float currentValue = currentData.getValues()[dimension];
                float lastAddedValue = lastAddedData.getValues()[dimension];
                float delta = Math.abs(currentValue - lastAddedValue);
                if (delta > 0.1) {
                    hasEqualValues = false;
                    break;
                }
            }

            if (hasEqualValues) {
                // continue skipping
                currentlySkippedDataCount++;
                continue;
            } else {
                if (currentlySkippedDataCount > 0) {
                    // stop skipping and add last data point
                    currentlySkippedDataCount = 0;
                    processedData.add(unprocessedData.get(dataIndex - 1));
                }
                // add current data point
                lastAddedData = currentData;
                processedData.add(lastAddedData);
            }
        }
        return processedData;
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
