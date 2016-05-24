package net.steppschuh.sensordatalogger.visualization;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.List;

public class VisualizationCardListAdapter extends ArrayAdapter<VisualizationCardData> {

    public VisualizationCardListAdapter(Context context, int resource) {
        super(context, resource);
    }

    public VisualizationCardListAdapter(Context context, int resource, List<VisualizationCardData> objects) {
        super(context, resource, objects);
    }

    public void setVisualizationCardData(VisualizationCardData visualizationCardData) {
        clear();
        addAll(visualizationCardData);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        VisualizationCard visualizationCard;
        if (convertView == null) {
            visualizationCard = new VisualizationCard(getContext());
        } else {
            visualizationCard = (VisualizationCard) convertView;
        }

        VisualizationCardData visualizationCardData = getItem(position);
        if (visualizationCardData != null) {
            visualizationCard.setData(visualizationCardData);
        }

        return visualizationCard;
    }
}
