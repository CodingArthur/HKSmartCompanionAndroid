package hk.edu.hku.cs7506.smartcompanion.data.model;

import hk.edu.hku.cs7506.smartcompanion.R;

public enum EmergencySortMode {
    TOTAL_TIME("total_time", R.string.emergency_sort_total_time),
    NEAREST("nearest", R.string.emergency_sort_nearest),
    SHORTEST_WAIT("shortest_wait", R.string.emergency_sort_shortest_wait);

    private final String id;
    private final int titleResId;

    EmergencySortMode(String id, int titleResId) {
        this.id = id;
        this.titleResId = titleResId;
    }

    public String getId() {
        return id;
    }

    public int getTitleResId() {
        return titleResId;
    }

    public static EmergencySortMode fromId(String raw) {
        if (raw == null) {
            return TOTAL_TIME;
        }
        for (EmergencySortMode mode : values()) {
            if (mode.id.equalsIgnoreCase(raw)) {
                return mode;
            }
        }
        return TOTAL_TIME;
    }
}
