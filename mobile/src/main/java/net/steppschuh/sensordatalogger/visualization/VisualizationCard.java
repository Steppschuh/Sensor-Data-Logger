package net.steppschuh.sensordatalogger.visualization;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;
import android.widget.TextView;

import net.steppschuh.sensordatalogger.R;

public class VisualizationCard extends RelativeLayout {

    private VisualizationCardData data;

    private TextView headingTextView;

    public VisualizationCard(Context context) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.visualization_card, this);

        headingTextView = (TextView) findViewById(R.id.headingText);
    }

    @Override
    public void requestLayout() {
        renderData();
        super.requestLayout();
    }

    private void renderData() {
        if (data == null) {
            return;
        }
        headingTextView.setText(data.getHeading());
    }

    public VisualizationCardData getData() {
        return data;
    }

    public void setData(VisualizationCardData data) {
        this.data = data;
    }

}
