package hk.edu.hku.cs7506.smartcompanion.data.location;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.core.content.ContextCompat;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;

import java.util.concurrent.atomic.AtomicBoolean;

import hk.edu.hku.cs7506.smartcompanion.data.local.PreferencesManager;
import hk.edu.hku.cs7506.smartcompanion.data.model.LocationSnapshot;
import hk.edu.hku.cs7506.smartcompanion.data.repository.RepositoryCallback;
import hk.edu.hku.cs7506.smartcompanion.util.GeoBounds;

public class CurrentLocationManager {
    public static final String[] REQUIRED_PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    private static final double FALLBACK_LAT = 22.2830;
    private static final double FALLBACK_LNG = 114.1371;
    private static final String FALLBACK_LABEL = "The University of Hong Kong fallback origin";
    private static final String FALLBACK_CITY = "Hong Kong";
    private static final String FALLBACK_DISTRICT = "Central and Western";
    private static final String OUTSIDE_HONG_KONG_SOURCE =
            "Current location is outside Hong Kong, using The University of Hong Kong fallback origin";

    private final Context appContext;
    private final PreferencesManager preferencesManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public CurrentLocationManager(Context context, PreferencesManager preferencesManager) {
        this.appContext = context.getApplicationContext();
        this.preferencesManager = preferencesManager;
    }

    public boolean hasLocationPermission() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED) {
                return true;
            }
        }
        return false;
    }

    public LocationSnapshot getBestAvailableLocation() {
        LocationSnapshot cached = preferencesManager.getLastLocation();
        if (cached != null && GeoBounds.isLikelyHongKongCoordinate(cached.getLatitude(), cached.getLongitude())) {
            return new LocationSnapshot(
                    cached.getLatitude(),
                    cached.getLongitude(),
                    cached.getLabel(),
                    cached.getCity(),
                    cached.getDistrict(),
                    cached.isFallback()
                            ? cached.getSourceLabel()
                            : "Using cached last-known location",
                    cached.isFallback()
            );
        }
        if (cached != null) {
            preferencesManager.clearLastLocation();
        }
        return getFallbackLocation();
    }

    public LocationSnapshot getFallbackLocation() {
        return new LocationSnapshot(
                FALLBACK_LAT,
                FALLBACK_LNG,
                FALLBACK_LABEL,
                FALLBACK_CITY,
                FALLBACK_DISTRICT,
                "Live location unavailable, using stable fallback origin",
                true
        );
    }

    public void requestSingleLocation(RepositoryCallback<LocationSnapshot> callback) {
        if (!hasLocationPermission()) {
            callback.onError("Location permission has not been granted.");
            return;
        }

        try {
            AMapLocationClient locationClient = new AMapLocationClient(appContext);
            AMapLocationClientOption option = new AMapLocationClientOption();
            option.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            option.setNeedAddress(true);
            option.setOnceLocationLatest(true);
            option.setGpsFirst(true);
            option.setGpsFirstTimeout(8000);
            option.setHttpTimeOut(10000);
            option.setLocationCacheEnable(true);
            option.setMockEnable(isLikelyEmulator());
            option.setSensorEnable(true);
            locationClient.setLocationOption(option);

            AtomicBoolean completed = new AtomicBoolean(false);
            Runnable timeoutRunnable = () -> {
                if (!completed.compareAndSet(false, true)) {
                    return;
                }
                cleanup(locationClient);
                LocationSnapshot cached = getBestAvailableLocation();
                callback.onSuccess(new LocationSnapshot(
                        cached.getLatitude(),
                        cached.getLongitude(),
                        cached.getLabel(),
                        cached.getCity(),
                        cached.getDistrict(),
                        "Timed out while requesting GPS fix, using cached or fallback origin",
                        cached.isFallback()
                ));
            };

            locationClient.setLocationListener(location -> {
                if (!completed.compareAndSet(false, true)) {
                    return;
                }
                mainHandler.removeCallbacks(timeoutRunnable);
                cleanup(locationClient);

                LocationSnapshot snapshot = toSnapshot(location);
                if (snapshot != null) {
                    preferencesManager.saveLastLocation(snapshot);
                    callback.onSuccess(snapshot);
                    return;
                }

                if (isOutsideHongKongFix(location)) {
                    LocationSnapshot fallbackSnapshot = new LocationSnapshot(
                            FALLBACK_LAT,
                            FALLBACK_LNG,
                            FALLBACK_LABEL,
                            FALLBACK_CITY,
                            FALLBACK_DISTRICT,
                            OUTSIDE_HONG_KONG_SOURCE,
                            true
                    );
                    preferencesManager.saveLastLocation(fallbackSnapshot);
                    callback.onSuccess(fallbackSnapshot);
                    return;
                }

                callback.onError(buildLocationErrorMessage(location));
            });

            locationClient.startLocation();
            mainHandler.postDelayed(timeoutRunnable, 12000);
        } catch (Exception error) {
            callback.onError(error.getMessage() == null ? "Unable to start location service." : error.getMessage());
        }
    }

    private LocationSnapshot toSnapshot(AMapLocation location) {
        if (location == null || location.getErrorCode() != AMapLocation.LOCATION_SUCCESS) {
            return null;
        }
        if (!GeoBounds.isLikelyHongKongCoordinate(location.getLatitude(), location.getLongitude())) {
            return null;
        }

        String poiName = sanitize(location.getPoiName());
        String road = sanitize(location.getRoad());
        String district = sanitize(location.getDistrict());
        String city = sanitize(location.getCity());
        String label = !TextUtils.isEmpty(poiName)
                ? poiName
                : (!TextUtils.isEmpty(road) ? road : "Live current location");

        return new LocationSnapshot(
                location.getLatitude(),
                location.getLongitude(),
                label,
                TextUtils.isEmpty(city) ? FALLBACK_CITY : city,
                TextUtils.isEmpty(district) ? FALLBACK_DISTRICT : district,
                "Live GPS/network location fix",
                false
        );
    }

    private boolean isOutsideHongKongFix(AMapLocation location) {
        return location != null
                && location.getErrorCode() == AMapLocation.LOCATION_SUCCESS
                && !GeoBounds.isLikelyHongKongCoordinate(location.getLatitude(), location.getLongitude());
    }

    private String buildLocationErrorMessage(AMapLocation location) {
        if (location == null) {
            return "Location callback returned no data. Using cached or fallback origin.";
        }
        if (location.getErrorCode() == AMapLocation.LOCATION_SUCCESS
                && !GeoBounds.isLikelyHongKongCoordinate(location.getLatitude(), location.getLongitude())) {
            return "Live location fix returned unusable coordinates on this device, using cached or fallback origin.";
        }
        String errorInfo = sanitize(location.getErrorInfo());
        if (!TextUtils.isEmpty(errorInfo)) {
            return "Live location request failed (AMap code "
                    + location.getErrorCode()
                    + ": "
                    + errorInfo
                    + "), using cached or fallback origin.";
        }
        return "Live location request failed (AMap code "
                + location.getErrorCode()
                + "), using cached or fallback origin.";
    }

    private void cleanup(AMapLocationClient client) {
        try {
            client.stopLocation();
        } catch (Exception ignored) {
        }
        try {
            client.onDestroy();
        } catch (Exception ignored) {
        }
    }

    private String sanitize(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isLikelyEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.contains("emulator")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("sdk_gphone")
                || "google_sdk".equals(Build.PRODUCT)
                || Build.HARDWARE.contains("ranchu");
    }
}
