package hk.edu.hku.cs7506.smartcompanion.data.model;

import hk.edu.hku.cs7506.smartcompanion.R;

public enum SceneType {
    EMERGENCY("emergency", R.string.scene_emergency_title, R.string.scene_emergency_subtitle),
    PARKING("parking", R.string.scene_parking_title, R.string.scene_parking_subtitle),
    PLAY("play", R.string.scene_play_title, R.string.scene_play_subtitle),
    COMMUTE("commute", R.string.scene_commute_title, R.string.scene_commute_subtitle);

    private final String apiValue;
    private final int titleResId;
    private final int subtitleResId;

    SceneType(String apiValue, int titleResId, int subtitleResId) {
        this.apiValue = apiValue;
        this.titleResId = titleResId;
        this.subtitleResId = subtitleResId;
    }

    public String getApiValue() {
        return apiValue;
    }

    public int getTitleResId() {
        return titleResId;
    }

    public int getSubtitleResId() {
        return subtitleResId;
    }

    public static SceneType fromApiValue(String raw) {
        for (SceneType type : values()) {
            if (type.apiValue.equalsIgnoreCase(raw)) {
                return type;
            }
        }
        return EMERGENCY;
    }
}
