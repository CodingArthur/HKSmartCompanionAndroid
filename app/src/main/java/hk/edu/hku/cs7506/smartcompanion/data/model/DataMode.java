package hk.edu.hku.cs7506.smartcompanion.data.model;

public enum DataMode {
    AUTO,
    DEMO,
    OFFICIAL;

    public static DataMode fromName(String raw) {
        if (raw == null) {
            return AUTO;
        }
        for (DataMode mode : values()) {
            if (mode.name().equalsIgnoreCase(raw)) {
                return mode;
            }
        }
        return AUTO;
    }
}
