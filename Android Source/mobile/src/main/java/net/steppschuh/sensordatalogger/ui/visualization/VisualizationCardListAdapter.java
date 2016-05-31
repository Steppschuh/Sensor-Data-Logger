package net.steppschuh.sensordatalogger.ui.visualization;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VisualizationCardListAdapter extends ArrayAdapter<VisualizationCardData> {

    Map<String, VisualizationCardData> visualizationCards;
    Map<String, VisualizationCardView> visualizationCardViews;

    public VisualizationCardListAdapter(Context context, int resource) {
        super(context, resource);
        visualizationCards = new HashMap<>();
        visualizationCardViews = new HashMap<>();
    }

    public VisualizationCardListAdapter(Context context, int resource, List<VisualizationCardData> cardData) {
        super(context, resource, cardData);
        visualizationCards = new HashMap<>();
        visualizationCardViews = new HashMap<>();
        for (VisualizationCardData visualizationCardData : cardData) {
            visualizationCards.put(visualizationCardData.getKey(), visualizationCardData);
        }
    }

    public void invalidateVisualization(String key) {
        VisualizationCardView cardView = visualizationCardViews.get(key);
        if (cardView != null) {
            cardView.setData(visualizationCards.get(key));
            cardView.renderData();
        }
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        VisualizationCardView visualizationCardView;
        if (convertView == null) {
            visualizationCardView = new VisualizationCardView(getContext());
        } else {
            visualizationCardView = (VisualizationCardView) convertView;
        }

        VisualizationCardData visualizationCardData = getItem(position);
        if (visualizationCardData != null) {
            visualizationCardView.setData(visualizationCardData);
            visualizationCardViews.put(visualizationCardData.getKey(), visualizationCardView);
        }

        return visualizationCardView;
    }

    @Override
    public void add(VisualizationCardData visualizationCardData) {
        super.add(visualizationCardData);
        visualizationCards.put(visualizationCardData.getKey(), visualizationCardData);
    }

    @Override
    public void addAll(Collection<? extends VisualizationCardData> visualizationCards) {
        super.addAll(visualizationCards);
        for (VisualizationCardData visualizationCardData : visualizationCards) {
            this.visualizationCards.put(visualizationCardData.getKey(), visualizationCardData);
        }
    }

    @Override
    public void remove(VisualizationCardData visualizationCardData) {
        super.remove(visualizationCardData);
        visualizationCards.remove(visualizationCardData.getKey());
    }

    @Override
    public void insert(VisualizationCardData visualizationCardData, int index) {
        super.insert(visualizationCardData, index);
        visualizationCards.put(visualizationCardData.getKey(), visualizationCardData);
    }

    @Override
    public void clear() {
        super.clear();
        this.visualizationCards = new HashMap<>();
    }

    public void setVisualizationCards(List<VisualizationCardData> visualizationCardData) {
        clear();
        addAll(visualizationCardData);
    }

    public VisualizationCardData getVisualizationCard(String key) {
        return visualizationCards.get(key);
    }

    public Map<String, VisualizationCardData> getVisualizationCards() {
        return visualizationCards;
    }

    public Map<String, VisualizationCardView> getVisualizationCardViews() {
        return visualizationCardViews;
    }

}
