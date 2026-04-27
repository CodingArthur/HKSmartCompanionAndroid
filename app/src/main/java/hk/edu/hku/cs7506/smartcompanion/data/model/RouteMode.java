package hk.edu.hku.cs7506.smartcompanion.data.model;

import hk.edu.hku.cs7506.smartcompanion.R;

public enum RouteMode {
    DRIVE("drive", R.string.route_mode_drive),
    WALK("walk", R.string.route_mode_walk),
    RIDE("ride", R.string.route_mode_ride),
    TRANSIT("transit", R.string.route_mode_transit);

    private final String id;
    private final int titleResId;

    RouteMode(String id, int titleResId) {
        this.id = id;
        this.titleResId = titleResId;
    }

    public String getId() {
        return id;
    }

    public int getTitleResId() {
        return titleResId;
    }

    public static RouteMode fromId(String raw) {
        if (raw == null) {
            return DRIVE;
        }
        for (RouteMode mode : values()) {
            if (mode.id.equalsIgnoreCase(raw)) {
                return mode;
            }
        }
        return DRIVE;
    }
}
