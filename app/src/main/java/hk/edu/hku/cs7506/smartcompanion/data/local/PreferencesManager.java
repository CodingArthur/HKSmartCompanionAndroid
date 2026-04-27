package hk.edu.hku.cs7506.smartcompanion.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import hk.edu.hku.cs7506.smartcompanion.data.model.DataMode;
import hk.edu.hku.cs7506.smartcompanion.data.model.LocationSnapshot;
import hk.edu.hku.cs7506.smartcompanion.data.model.UserSettings;

public class PreferencesManager {
    private static final String PREFS_NAME = "smart_companion_prefs";
    private static final String KEY_DATA_MODE = "data_mode";
    private static final String KEY_LAST_LAT = "last_lat";
    private static final String KEY_LAST_LNG = "last_lng";
    private static final String KEY_LAST_LABEL = "last_label";
    private static final String KEY_LAST_CITY = "last_city";
    private static final String KEY_LAST_DISTRICT = "last_district";
    private static final String KEY_LAST_SOURCE_LABEL = "last_source_label";
    private static final String KEY_LAST_IS_FALLBACK = "last_is_fallback";

    private final SharedPreferences sharedPreferences;

    public PreferencesManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public UserSettings getUserSettings() {
        String modeName = sharedPreferences.getString(KEY_DATA_MODE, DataMode.AUTO.name());
        return new UserSettings(DataMode.fromName(modeName));
    }

    public void save(UserSettings settings) {
        sharedPreferences.edit()
                .putString(KEY_DATA_MODE, settings.getDataMode().name())
                .apply();
    }

    public void reset() {
        save(new UserSettings(DataMode.AUTO));
    }

    public void saveLastLocation(LocationSnapshot snapshot) {
        sharedPreferences.edit()
                .putLong(KEY_LAST_LAT, Double.doubleToRawLongBits(snapshot.getLatitude()))
                .putLong(KEY_LAST_LNG, Double.doubleToRawLongBits(snapshot.getLongitude()))
                .putString(KEY_LAST_LABEL, snapshot.getLabel())
                .putString(KEY_LAST_CITY, snapshot.getCity())
                .putString(KEY_LAST_DISTRICT, snapshot.getDistrict())
                .putString(KEY_LAST_SOURCE_LABEL, snapshot.getSourceLabel())
                .putBoolean(KEY_LAST_IS_FALLBACK, snapshot.isFallback())
                .apply();
    }

    public LocationSnapshot getLastLocation() {
        if (!sharedPreferences.contains(KEY_LAST_LAT) || !sharedPreferences.contains(KEY_LAST_LNG)) {
            return null;
        }
        return new LocationSnapshot(
                Double.longBitsToDouble(sharedPreferences.getLong(KEY_LAST_LAT, 0L)),
                Double.longBitsToDouble(sharedPreferences.getLong(KEY_LAST_LNG, 0L)),
                sharedPreferences.getString(KEY_LAST_LABEL, "Last known location"),
                sharedPreferences.getString(KEY_LAST_CITY, "Hong Kong"),
                sharedPreferences.getString(KEY_LAST_DISTRICT, ""),
                sharedPreferences.getString(KEY_LAST_SOURCE_LABEL, "Cached last-known location"),
                sharedPreferences.getBoolean(KEY_LAST_IS_FALLBACK, false)
        );
    }

    public void clearLastLocation() {
        sharedPreferences.edit()
                .remove(KEY_LAST_LAT)
                .remove(KEY_LAST_LNG)
                .remove(KEY_LAST_LABEL)
                .remove(KEY_LAST_CITY)
                .remove(KEY_LAST_DISTRICT)
                .remove(KEY_LAST_SOURCE_LABEL)
                .remove(KEY_LAST_IS_FALLBACK)
                .apply();
    }
}
