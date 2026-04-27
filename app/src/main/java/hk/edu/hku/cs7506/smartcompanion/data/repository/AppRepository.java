package hk.edu.hku.cs7506.smartcompanion.data.repository;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;

import java.util.List;

import hk.edu.hku.cs7506.smartcompanion.R;
import hk.edu.hku.cs7506.smartcompanion.data.local.FileFeedCacheStore;
import hk.edu.hku.cs7506.smartcompanion.data.local.FavoriteStore;
import hk.edu.hku.cs7506.smartcompanion.data.local.PreferencesManager;
import hk.edu.hku.cs7506.smartcompanion.data.local.RecentStore;
import hk.edu.hku.cs7506.smartcompanion.data.location.CurrentLocationManager;
import hk.edu.hku.cs7506.smartcompanion.data.model.DataMode;
import hk.edu.hku.cs7506.smartcompanion.data.model.LocationSnapshot;
import hk.edu.hku.cs7506.smartcompanion.data.model.ParkingDestination;
import hk.edu.hku.cs7506.smartcompanion.data.model.RecommendationItem;
import hk.edu.hku.cs7506.smartcompanion.data.model.RecommendationLoadResult;
import hk.edu.hku.cs7506.smartcompanion.data.model.RecommendationRequest;
import hk.edu.hku.cs7506.smartcompanion.data.model.RouteStep;
import hk.edu.hku.cs7506.smartcompanion.data.model.SceneType;
import hk.edu.hku.cs7506.smartcompanion.data.model.UserSettings;
import hk.edu.hku.cs7506.smartcompanion.data.network.OfficialDataClient;
import hk.edu.hku.cs7506.smartcompanion.data.network.OfficialOpenDataSource;

public class AppRepository {
    private static AppRepository instance;

    private final PreferencesManager preferencesManager;
    private final FavoriteStore favoriteStore;
    private final RecentStore recentStore;
    private final OfficialDataClient officialDataClient;
    private final CurrentLocationManager currentLocationManager;
    private final Context appContext;

    private AppRepository(Context context) {
        appContext = context.getApplicationContext();
        preferencesManager = new PreferencesManager(appContext);
        favoriteStore = new FavoriteStore(appContext);
        recentStore = new RecentStore(appContext);
        officialDataClient = new OfficialDataClient(
                new OfficialOpenDataSource(
                        new hk.edu.hku.cs7506.smartcompanion.data.network.OpenDataHttpClient(),
                        new FileFeedCacheStore(appContext)
                )
        );
        currentLocationManager = new CurrentLocationManager(appContext, preferencesManager);
    }

    public static synchronized AppRepository getInstance(Context context) {
        if (instance == null) {
            instance = new AppRepository(context);
        }
        return instance;
    }

    public UserSettings getSettings() {
        return preferencesManager.getUserSettings();
    }

    public void saveSettings(UserSettings settings) {
        preferencesManager.save(settings);
    }

    public void resetSettings() {
        preferencesManager.reset();
    }

    public void loadRecommendations(
            RecommendationRequest request,
            RepositoryCallback<RecommendationLoadResult> callback
    ) {
        UserSettings settings = getSettings();
        if (settings.getDataMode() == DataMode.DEMO) {
            callback.onSuccess(new RecommendationLoadResult(
                    DemoDataFactory.createRecommendations(request),
                    DataMode.DEMO,
                    appContext.getString(R.string.status_message_demo)
            ));
            return;
        }

        officialDataClient.fetchRecommendations(request, new RepositoryCallback<List<RecommendationItem>>() {
            @Override
            public void onSuccess(List<RecommendationItem> data) {
                if (data.isEmpty() && settings.getDataMode() == DataMode.AUTO) {
                    callback.onSuccess(new RecommendationLoadResult(
                            DemoDataFactory.createRecommendations(request),
                            DataMode.DEMO,
                            appContext.getString(R.string.status_message_official_empty_fallback)
                    ));
                    return;
                }
                callback.onSuccess(new RecommendationLoadResult(
                        data,
                        DataMode.OFFICIAL,
                        data.isEmpty()
                                ? appContext.getString(R.string.status_message_official_empty)
                                : appContext.getString(R.string.status_message_official_loaded)
                ));
            }

            @Override
            public void onError(String message) {
                if (settings.getDataMode() == DataMode.AUTO) {
                    callback.onSuccess(new RecommendationLoadResult(
                            DemoDataFactory.createRecommendations(request),
                            DataMode.DEMO,
                            appContext.getString(R.string.status_message_official_fallback)
                    ));
                } else {
                    callback.onError(message);
                }
            }
        });
    }

    public List<RecommendationItem> getFavorites() {
        return favoriteStore.getAll();
    }

    public void recordRecentView(RecommendationItem item) {
        recentStore.recordView(item);
    }

    public RecommendationItem getMostRecentItem() {
        return recentStore.getMostRecent();
    }

    public List<RecommendationItem> getRecentItems() {
        return recentStore.getAll();
    }

    public boolean toggleFavorite(RecommendationItem item) {
        return favoriteStore.toggle(item);
    }

    public boolean isFavorite(RecommendationItem item) {
        return favoriteStore.isFavorite(item);
    }

    public List<RouteStep> getRouteSteps(RecommendationItem item) {
        return DemoDataFactory.createRouteSteps(item);
    }

    public boolean hasLocationPermission() {
        return currentLocationManager.hasLocationPermission();
    }

    public void requestCurrentLocation(RepositoryCallback<LocationSnapshot> callback) {
        currentLocationManager.requestSingleLocation(callback);
    }

    public LocationSnapshot getBestAvailableLocation() {
        return currentLocationManager.getBestAvailableLocation();
    }

    public LocationSnapshot getFallbackLocation() {
        return currentLocationManager.getFallbackLocation();
    }

    public boolean hasAmapKeyConfigured() {
        try {
            ApplicationInfo applicationInfo = appContext.getPackageManager()
                    .getApplicationInfo(appContext.getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = applicationInfo.metaData;
            if (bundle == null) {
                return false;
            }
            String key = bundle.getString("com.amap.api.v2.apikey", "");
            return !TextUtils.isEmpty(key);
        } catch (PackageManager.NameNotFoundException ignored) {
            return false;
        }
    }

    public void prefetchOfficialData(RepositoryCallback<Integer> callback) {
        if (!shouldUseLiveData()) {
            callback.onSuccess(0);
            return;
        }
        officialDataClient.prefetchFeeds(callback);
    }

    public void prefetchOfficialDataInBackground() {
        if (!shouldUseLiveData()) {
            return;
        }
        officialDataClient.prefetchFeeds(new RepositoryCallback<Integer>() {
            @Override
            public void onSuccess(Integer data) {
            }

            @Override
            public void onError(String message) {
            }
        });
    }

    public long getLastPrefetchEpochMillis() {
        return officialDataClient.getLastPrefetchEpochMillis();
    }

    public int getCachedFeedCount() {
        return officialDataClient.getCachedFeedCount();
    }

    private boolean shouldUseLiveData() {
        DataMode dataMode = preferencesManager.getUserSettings().getDataMode();
        return dataMode == DataMode.AUTO || dataMode == DataMode.OFFICIAL;
    }
}
