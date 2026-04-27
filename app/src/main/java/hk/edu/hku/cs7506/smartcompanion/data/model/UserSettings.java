package hk.edu.hku.cs7506.smartcompanion.data.model;

public class UserSettings {
    private final DataMode dataMode;

    public UserSettings(DataMode dataMode) {
        this.dataMode = dataMode;
    }

    public DataMode getDataMode() {
        return dataMode;
    }
}
