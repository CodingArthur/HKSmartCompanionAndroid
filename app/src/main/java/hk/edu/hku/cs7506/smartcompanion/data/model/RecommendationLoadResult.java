package hk.edu.hku.cs7506.smartcompanion.data.model;

import java.util.List;

public class RecommendationLoadResult {
    private final List<RecommendationItem> items;
    private final DataMode resolvedMode;
    private final String statusMessage;

    public RecommendationLoadResult(List<RecommendationItem> items, DataMode resolvedMode, String statusMessage) {
        this.items = items;
        this.resolvedMode = resolvedMode;
        this.statusMessage = statusMessage;
    }

    public List<RecommendationItem> getItems() {
        return items;
    }

    public DataMode getResolvedMode() {
        return resolvedMode;
    }

    public String getStatusMessage() {
        return statusMessage;
    }
}

