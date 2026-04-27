package hk.edu.hku.cs7506.smartcompanion.util;

import android.content.Context;
import android.text.TextUtils;

import java.util.Locale;

import hk.edu.hku.cs7506.smartcompanion.R;
import hk.edu.hku.cs7506.smartcompanion.data.model.DataMode;
import hk.edu.hku.cs7506.smartcompanion.data.model.RecommendationItem;
import hk.edu.hku.cs7506.smartcompanion.data.model.SceneType;

public final class FormatUtils {
    private FormatUtils() {
    }

    public static String getSceneTitle(Context context, SceneType sceneType) {
        return context.getString(sceneType.getTitleResId());
    }

    public static String getDisplayTag(Context context, RecommendationItem item) {
        if (!TextUtils.isEmpty(item.getContentTag())) {
            return item.getContentTag();
        }
        return getSceneTitle(context, item.getSceneType());
    }

    public static String getSceneSubtitle(Context context, SceneType sceneType) {
        return context.getString(sceneType.getSubtitleResId());
    }

    public static String getModeLabel(Context context, DataMode mode) {
        if (mode == DataMode.OFFICIAL) {
            return context.getString(R.string.status_official_mode);
        }
        if (mode == DataMode.DEMO) {
            return context.getString(R.string.status_demo_mode);
        }
        return context.getString(R.string.status_auto_mode);
    }

    public static String getPrimaryMetric(Context context, RecommendationItem item) {
        if (item.getSceneType() == SceneType.EMERGENCY && item.getEtaMinutes() != null && item.getWaitTimeMinutes() != null) {
            return context.getString(R.string.metric_total_time_value, item.getEtaMinutes() + item.getWaitTimeMinutes());
        }
        if (item.getSceneType() == SceneType.PARKING && item.getVacancy() != null) {
            return context.getString(R.string.metric_vacancy_value, item.getVacancy());
        }
        if (item.getSceneType() == SceneType.PLAY && item.getWeatherScore() != null) {
            return String.format(Locale.US, "%s %.2f", context.getString(R.string.metric_weather), item.getWeatherScore());
        }
        if (item.getSceneType() == SceneType.COMMUTE && item.getEtaMinutes() != null) {
            return context.getString(R.string.metric_next_departure_value, item.getEtaMinutes());
        }
        return context.getString(R.string.common_points, item.getTotalScore());
    }

    public static String getSecondaryMetric(Context context, RecommendationItem item) {
        if (item.getSceneType() == SceneType.EMERGENCY && item.getEtaMinutes() != null && item.getWaitTimeMinutes() != null) {
            return context.getString(R.string.metric_drive_wait_value, item.getEtaMinutes(), item.getWaitTimeMinutes());
        }
        if (item.getSceneType() == SceneType.PARKING && item.getEtaMinutes() != null && item.getWalkMinutes() != null) {
            return context.getString(R.string.metric_drive_walk_value, item.getEtaMinutes(), item.getWalkMinutes());
        }
        if (item.getSceneType() == SceneType.PLAY && item.getAqhiScore() != null && item.getEtaMinutes() != null) {
            return String.format(
                    Locale.US,
                    "%s %.2f | %s",
                    context.getString(R.string.metric_aqhi),
                    item.getAqhiScore(),
                    context.getString(R.string.common_minutes, item.getEtaMinutes())
            );
        }
        if (item.getSceneType() == SceneType.COMMUTE
                && item.getInVehicleMinutes() != null
                && item.getWalkMinutes() != null) {
            return context.getString(
                    R.string.metric_ride_walk_value,
                    item.getInVehicleMinutes(),
                    item.getWalkMinutes()
            );
        }
        if (item.getEtaMinutes() != null) {
            return context.getString(R.string.common_minutes, item.getEtaMinutes());
        }
        return context.getString(R.string.common_not_available);
    }

    public static String getMetadataLine(RecommendationItem item) {
        if (item.getSceneType() == SceneType.COMMUTE) {
            return buildCommuteMetadata(item);
        }
        if (!TextUtils.isEmpty(item.getMetadataLine())) {
            return item.getMetadataLine();
        }
        return "";
    }

    public static String getTransportNote(RecommendationItem item) {
        if (!TextUtils.isEmpty(item.getTransitNote())) {
            return item.getTransitNote();
        }
        if (!TextUtils.isEmpty(item.getTrafficNote())) {
            return item.getTrafficNote();
        }
        return "";
    }

    public static String formatCoordinate(Context context, RecommendationItem item) {
        return context.getString(R.string.common_latlng, item.getLatitude(), item.getLongitude());
    }

    public static String formatMetricValue(Context context, String label, Integer value) {
        if (value == null) {
            return label + ": " + context.getString(R.string.common_not_available);
        }
        return label + ": " + context.getString(R.string.common_minutes, value);
    }

    public static String formatPlainMetric(Context context, String label, Integer value) {
        if (value == null) {
            return label + ": " + context.getString(R.string.common_not_available);
        }
        return label + ": " + value;
    }

    public static String formatDistanceMetric(Context context, String label, Integer distanceMeters) {
        if (distanceMeters == null) {
            return label + ": " + context.getString(R.string.common_not_available);
        }
        if (distanceMeters >= 1000) {
            return String.format(Locale.US, "%s: %.1f km", label, distanceMeters / 1000f);
        }
        return String.format(Locale.US, "%s: %d m", label, distanceMeters);
    }

    public static String formatScoreMetric(Context context, String label, Double value) {
        if (value == null) {
            return label + ": " + context.getString(R.string.common_not_available);
        }
        return String.format(Locale.US, "%s: %.2f", label, value);
    }

    private static String buildCommuteMetadata(RecommendationItem item) {
        String destination = item.getAddressLine();
        if (TextUtils.isEmpty(destination)) {
            destination = "Hong Kong";
        }
        return item.getName() + " | Direct bus corridor toward " + destination;
    }
}
