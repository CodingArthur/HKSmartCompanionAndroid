package hk.edu.hku.cs7506.smartcompanion.ui.navigation;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.Poi;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;
import com.amap.api.navi.AMapNavi;
import com.amap.api.navi.AmapNaviPage;
import com.amap.api.navi.AmapNaviParams;
import com.amap.api.navi.AmapNaviType;
import com.amap.api.navi.AmapPageType;
import com.amap.api.navi.INaviInfoCallback;
import com.amap.api.navi.enums.NaviType;
import com.amap.api.navi.model.AMapNaviLocation;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.route.BusRouteResult;
import com.amap.api.services.route.DriveRouteResult;
import com.amap.api.services.route.Path;
import com.amap.api.services.route.RideRouteResult;
import com.amap.api.services.route.RouteSearch;
import com.amap.api.services.route.WalkRouteResult;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.List;
import java.util.Locale;

import hk.edu.hku.cs7506.smartcompanion.R;
import hk.edu.hku.cs7506.smartcompanion.data.model.LocationSnapshot;
import hk.edu.hku.cs7506.smartcompanion.data.model.RecommendationItem;
import hk.edu.hku.cs7506.smartcompanion.data.model.RouteMode;
import hk.edu.hku.cs7506.smartcompanion.data.model.RouteStep;
import hk.edu.hku.cs7506.smartcompanion.data.repository.AppRepository;
import hk.edu.hku.cs7506.smartcompanion.data.repository.RepositoryCallback;
import hk.edu.hku.cs7506.smartcompanion.ui.adapter.RouteStepAdapter;
import hk.edu.hku.cs7506.smartcompanion.util.AmapRouteSupport;
import hk.edu.hku.cs7506.smartcompanion.util.IntentKeys;

public class NavigationPreviewActivity extends AppCompatActivity
        implements RouteSearch.OnRouteSearchListener, INaviInfoCallback {
    private static final int AMAP_SUCCESS = 1000;
    private static final String TAG = "NavigationPreview";

    private RecommendationItem item;
    private AppRepository repository;
    private MapView mapView;
    private AMap aMap;
    private RouteSearch routeSearch;
    private TextView textNavigationStatusBody;
    private TextView textNavigationSummary;
    private TextView textDriveSessionBody;
    private TextView textTransportTraffic;
    private TextView textTransportTransit;
    private LinearProgressIndicator progressNavigation;
    private MaterialCardView cardDriveSession;
    private MaterialCardView cardTransportIntel;
    private TextView textDriveSessionTitle;
    private MaterialButton buttonStartLiveNavigation;
    private MaterialButton buttonStartSimulatedNavigation;
    private RouteStepAdapter adapter;
    private RouteMode routeMode = RouteMode.DRIVE;
    private LocationSnapshot originLocation;
    private boolean hasAmapKeyConfigured;
    private TextToSpeech englishTts;
    private boolean englishTtsReady;
    private int lastRouteResponseCode = AMAP_SUCCESS;
    private boolean pendingCameraFit;
    private Marker originMarker;
    private Marker destinationMarker;
    private Polyline routePolyline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation_preview);

        item = (RecommendationItem) getIntent().getSerializableExtra(IntentKeys.EXTRA_ITEM);
        if (item == null) {
            finish();
            return;
        }

        repository = AppRepository.getInstance(this);
        routeMode = RouteMode.fromId(getIntent().getStringExtra(IntentKeys.EXTRA_ROUTE_MODE));
        hasAmapKeyConfigured = repository.hasAmapKeyConfigured();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        MaterialButtonToggleGroup routeModeGroup = findViewById(R.id.routeModeGroup);
        mapView = findViewById(R.id.mapView);
        cardDriveSession = findViewById(R.id.cardDriveSession);
        cardTransportIntel = findViewById(R.id.cardTransportIntel);
        textDriveSessionTitle = findViewById(R.id.textDriveSessionTitle);
        textDriveSessionBody = findViewById(R.id.textDriveSessionBody);
        textTransportTraffic = findViewById(R.id.textTransportTraffic);
        textTransportTransit = findViewById(R.id.textTransportTransit);
        buttonStartLiveNavigation = findViewById(R.id.buttonStartLiveNavigation);
        buttonStartSimulatedNavigation = findViewById(R.id.buttonStartSimulatedNavigation);
        textNavigationStatusBody = findViewById(R.id.textNavigationStatusBody);
        textNavigationSummary = findViewById(R.id.textNavigationSummary);
        progressNavigation = findViewById(R.id.progressNavigation);
        RecyclerView recyclerRouteSteps = findViewById(R.id.recyclerRouteSteps);

        toolbar.setTitle(item.getName());
        toolbar.setNavigationOnClickListener(v -> finish());

        mapView.onCreate(savedInstanceState);
        aMap = mapView.getMap();
        adapter = new RouteStepAdapter();
        recyclerRouteSteps.setLayoutManager(new LinearLayoutManager(this));
        recyclerRouteSteps.setAdapter(adapter);

        buttonStartLiveNavigation.setOnClickListener(v -> launchNavigationSession(NaviType.GPS));
        buttonStartSimulatedNavigation.setOnClickListener(v -> launchNavigationSession(NaviType.EMULATOR));
        initializeEnglishTts();

        try {
            routeSearch = new RouteSearch(this);
            routeSearch.setRouteSearchListener(this);
        } catch (AMapException exception) {
            textNavigationStatusBody.setText(getString(R.string.map_status_error_prefix) + " " + exception.getMessage());
            textNavigationSummary.setText(R.string.route_summary_unavailable);
            adapter.submitList(repository.getRouteSteps(item));
            updateDriveSessionCard();
            return;
        }

        routeModeGroup.check(resolveCheckedButtonId(routeMode));
        routeModeGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            routeMode = resolveRouteMode(checkedId);
            updateDriveSessionCard();
            if (originLocation != null) {
                if (hasAmapKeyConfigured) {
                    requestRoute();
                } else {
                    renderFallbackRouteState();
                }
            }
        });

        configureMap();
        bindTransportIntelCard();
        updateDriveSessionCard();
        resolveOriginThenRoute();
    }

    private void resolveOriginThenRoute() {
        originLocation = repository.getBestAvailableLocation();
        redrawBaseMarkers();
        textNavigationStatusBody.setText(R.string.map_status_resolving_origin);
        updateDriveSessionCard();
        if (!hasAmapKeyConfigured) {
            renderFallbackRouteState();
            return;
        }

        if (!repository.hasLocationPermission()) {
            redrawBaseMarkers();
            textNavigationStatusBody.setText(R.string.location_permission_pending);
            updateDriveSessionCard();
            requestRoute();
            return;
        }

        repository.requestCurrentLocation(new RepositoryCallback<LocationSnapshot>() {
            @Override
            public void onSuccess(LocationSnapshot data) {
                originLocation = data;
                redrawBaseMarkers();
                textNavigationStatusBody.setText(data.getSourceLabel());
                updateDriveSessionCard();
                requestRoute();
            }

            @Override
            public void onError(String message) {
                originLocation = repository.getBestAvailableLocation();
                redrawBaseMarkers();
                textNavigationStatusBody.setText(message);
                updateDriveSessionCard();
                requestRoute();
            }
        });
    }

    private void configureMap() {
        if (aMap == null) {
            return;
        }
        aMap.setMapLanguage(AMap.ENGLISH);
        aMap.showMapText(false);
        aMap.getUiSettings().setZoomControlsEnabled(false);
        aMap.getUiSettings().setCompassEnabled(true);
    }

    private void redrawBaseMarkers() {
        if (aMap == null || originLocation == null) {
            return;
        }
        LatLng origin = AmapRouteSupport.getOriginLatLng(originLocation);
        LatLng destination = new LatLng(item.getLatitude(), item.getLongitude());
        if (routePolyline != null) {
            routePolyline.remove();
            routePolyline = null;
        }
        if (originMarker != null) {
            originMarker.remove();
        }
        if (destinationMarker != null) {
            destinationMarker.remove();
        }
        originMarker = aMap.addMarker(new MarkerOptions()
                .position(origin)
                .title(getString(R.string.map_origin_label))
                .snippet(originLocation.getLabel()));
        destinationMarker = aMap.addMarker(new MarkerOptions()
                .position(destination)
                .title(getString(R.string.map_destination_label))
                .snippet(item.getName()));
        fitCameraToBounds(origin, destination);
    }

    private void fitCameraToBounds(LatLng origin, LatLng destination) {
        if (aMap == null || mapView == null) {
            return;
        }

        LatLngBounds bounds = new LatLngBounds.Builder()
                .include(origin)
                .include(destination)
                .build();
        if (mapView.getWidth() <= 0 || mapView.getHeight() <= 0) {
            if (pendingCameraFit) {
                return;
            }
            pendingCameraFit = true;
            mapView.post(() -> {
                pendingCameraFit = false;
                fitCameraToBounds(origin, destination);
            });
            return;
        }

        aMap.moveCamera(CameraUpdateFactory.newLatLngBoundsRect(
                bounds,
                120,
                120,
                120,
                120
        ));
    }

    private void requestRoute() {
        if (originLocation == null || routeSearch == null) {
            return;
        }
        lastRouteResponseCode = 0;
        Log.d(TAG, "Requesting " + routeMode.getId()
                + " navigation route from " + originLocation.getLatitude() + "," + originLocation.getLongitude()
                + " to " + item.getLatitude() + "," + item.getLongitude());
        progressNavigation.setVisibility(View.VISIBLE);
        textNavigationSummary.setText(R.string.route_summary_unavailable);
        textNavigationStatusBody.setText(AmapRouteSupport.getModeLoadingText(this, routeMode));

        if (routeMode == RouteMode.WALK) {
            routeSearch.calculateWalkRouteAsyn(AmapRouteSupport.buildWalkRouteQuery(originLocation, item));
        } else if (routeMode == RouteMode.RIDE) {
            routeSearch.calculateRideRouteAsyn(AmapRouteSupport.buildRideRouteQuery(originLocation, item));
        } else if (routeMode == RouteMode.TRANSIT) {
            routeSearch.calculateBusRouteAsyn(AmapRouteSupport.buildBusRouteQuery(originLocation, item));
        } else {
            routeSearch.calculateDriveRouteAsyn(AmapRouteSupport.buildDriveRouteQuery(originLocation, item));
        }
    }

    private void renderFallbackRouteState() {
        progressNavigation.setVisibility(View.GONE);
        drawRoute(AmapRouteSupport.buildFallbackPolyline(originLocation, item));
        textNavigationStatusBody.setText(getString(R.string.navigation_notice_key_missing)
                + " "
                + getString(R.string.route_status_fallback_preview));
        textNavigationSummary.setText(AmapRouteSupport.formatFallbackRouteSummary(this, originLocation, item, routeMode));
        adapter.submitList(AmapRouteSupport.buildFallbackRouteSteps(originLocation, item, routeMode));
        updateDriveSessionCard();
    }

    private void updateDriveSessionCard() {
        if (cardDriveSession == null) {
            return;
        }
        boolean supportsLiveGuidance = supportsLiveGuidance(routeMode);
        cardDriveSession.setVisibility(supportsLiveGuidance ? View.VISIBLE : View.GONE);
        if (!supportsLiveGuidance) {
            return;
        }
        textDriveSessionTitle.setText(getSessionTitle(routeMode));
        buttonStartLiveNavigation.setText(getString(
                R.string.action_start_live_navigation_session_format,
                getSessionActionLabel(routeMode)
        ));
        buttonStartSimulatedNavigation.setText(getString(
                R.string.action_start_simulated_navigation_session_format,
                getSessionActionLabel(routeMode)
        ));
        boolean hasAuthorizationError = AmapRouteSupport.isAmapAuthorizationError(lastRouteResponseCode);
        boolean canLaunchSession = supportsLiveGuidance
                && hasAmapKeyConfigured
                && originLocation != null
                && !hasAuthorizationError;
        buttonStartLiveNavigation.setEnabled(canLaunchSession);
        buttonStartSimulatedNavigation.setEnabled(canLaunchSession);
        if (!hasAmapKeyConfigured) {
            textDriveSessionBody.setText("AMap live "
                    + getSessionDescriptor(routeMode)
                    + " is unavailable until a valid key is configured.");
            return;
        }
        if (hasAuthorizationError) {
            textDriveSessionBody.setText(R.string.navigation_session_auth_failed);
            return;
        }
        if (originLocation == null) {
            textDriveSessionBody.setText(getString(
                    R.string.navigation_session_waiting_origin_format,
                    getSessionDescriptor(routeMode)
            ));
            return;
        }

        if (originLocation.isFallback()) {
            textDriveSessionBody.setText(getString(
                    R.string.navigation_session_fallback_notice_format,
                    getSessionDescriptor(routeMode),
                    originLocation.getLabel()
            ) + " " + getString(
                    R.string.navigation_session_ready_format,
                    getSessionDescriptor(routeMode),
                    originLocation.getLabel(),
                    item.getName()
            ));
            return;
        }

        textDriveSessionBody.setText(getString(
                R.string.navigation_session_ready_format,
                getSessionDescriptor(routeMode),
                originLocation.getLabel(),
                item.getName()
        ));
    }

    private void bindTransportIntelCard() {
        boolean hasTraffic = !TextUtils.isEmpty(item.getTrafficNote());
        boolean hasTransit = !TextUtils.isEmpty(item.getTransitNote());
        if (cardTransportIntel == null) {
            return;
        }
        cardTransportIntel.setVisibility((hasTraffic || hasTransit) ? View.VISIBLE : View.GONE);
        if (textTransportTraffic != null) {
            textTransportTraffic.setVisibility(hasTraffic ? View.VISIBLE : View.GONE);
            textTransportTraffic.setText(hasTraffic ? item.getTrafficNote() : "");
        }
        if (textTransportTransit != null) {
            textTransportTransit.setVisibility(hasTransit ? View.VISIBLE : View.GONE);
            textTransportTransit.setText(hasTransit ? item.getTransitNote() : "");
        }
    }

    private void launchNavigationSession(int naviMode) {
        if (!supportsLiveGuidance(routeMode)) {
            updateDriveSessionCard();
            return;
        }
        if (!hasAmapKeyConfigured || originLocation == null
                || AmapRouteSupport.isAmapAuthorizationError(lastRouteResponseCode)) {
            updateDriveSessionCard();
            return;
        }

        AmapNaviType naviType = resolveNaviType(routeMode);
        if (naviType == null) {
            updateDriveSessionCard();
            return;
        }

        Poi startPoi = new Poi(originLocation.getLabel(), AmapRouteSupport.getOriginLatLng(originLocation), "");
        Poi endPoi = new Poi(item.getName(), new LatLng(item.getLatitude(), item.getLongitude()), "");
        AmapNaviParams params = new AmapNaviParams(startPoi, null, endPoi, naviType, AmapPageType.NAVI);
        params.setNaviMode(naviMode);
        params.setUseInnerVoice(false);
        params.setTrafficEnabled(routeMode == RouteMode.DRIVE);
        params.setShowCrossImage(routeMode == RouteMode.DRIVE);
        params.setShowRouteStrategyPreferenceView(routeMode == RouteMode.DRIVE);
        params.setNeedCalculateRouteWhenPresent(true);
        params.setNeedDestroyDriveManagerInstanceWhenNaviExit(true);
        params.setShowExitNaviDialog(true);
        params.setScaleAutoChangeEnable(this, true);
        if (routeMode == RouteMode.DRIVE) {
            params.setCarDirectionMode(this, 2);
            params.setBroadcastMode(this, 2);
        }

        setDriveSessionMessage(naviMode == NaviType.EMULATOR
                ? getString(R.string.navigation_session_simulation_starting_format, getSessionDescriptor(routeMode))
                : getString(R.string.navigation_session_live_starting_format, getSessionDescriptor(routeMode)));

        try {
            AMapNavi.getInstance(getApplicationContext()).setUseInnerVoice(false, true);
            AmapNaviPage.getInstance().showRouteActivity(getApplicationContext(), params, this);
        } catch (Throwable throwable) {
            Log.e(TAG, "Unable to launch AMap drive session", throwable);
            setDriveSessionMessage(getString(
                    R.string.navigation_session_launch_failed_prefix_format,
                    getSessionDescriptor(routeMode)
            ) + " " + throwable.getMessage());
        }
    }

    @Override
    public void onDriveRouteSearched(DriveRouteResult result, int rCode) {
        if (routeMode != RouteMode.DRIVE) {
            return;
        }
        Log.d(TAG, "Drive route callback rCode=" + rCode);
        handleRouteResult(
                rCode == AMAP_SUCCESS && result != null && result.getPaths() != null && !result.getPaths().isEmpty()
                        ? result.getPaths().get(0)
                        : null,
                AmapRouteSupport.toRouteSteps(
                        RouteMode.DRIVE,
                        result != null && result.getPaths() != null && !result.getPaths().isEmpty() ? result.getPaths().get(0) : null,
                        null,
                        null,
                        null
                ),
                AmapRouteSupport.flattenPolyline(
                        RouteMode.DRIVE,
                        result != null && result.getPaths() != null && !result.getPaths().isEmpty() ? result.getPaths().get(0) : null,
                        null,
                        null,
                        null
                ),
                rCode
        );
    }

    @Override
    public void onBusRouteSearched(BusRouteResult result, int rCode) {
        if (routeMode != RouteMode.TRANSIT) {
            return;
        }
        Log.d(TAG, "Transit route callback rCode=" + rCode);
        handleRouteResult(
                rCode == AMAP_SUCCESS && result != null && result.getPaths() != null && !result.getPaths().isEmpty()
                        ? result.getPaths().get(0)
                        : null,
                AmapRouteSupport.toRouteSteps(
                        RouteMode.TRANSIT,
                        null,
                        null,
                        null,
                        result != null && result.getPaths() != null && !result.getPaths().isEmpty() ? result.getPaths().get(0) : null
                ),
                AmapRouteSupport.flattenPolyline(
                        RouteMode.TRANSIT,
                        null,
                        null,
                        null,
                        result != null && result.getPaths() != null && !result.getPaths().isEmpty() ? result.getPaths().get(0) : null
                ),
                rCode
        );
    }

    @Override
    public void onWalkRouteSearched(WalkRouteResult result, int rCode) {
        if (routeMode != RouteMode.WALK) {
            return;
        }
        Log.d(TAG, "Walk route callback rCode=" + rCode);
        handleRouteResult(
                rCode == AMAP_SUCCESS && result != null && result.getPaths() != null && !result.getPaths().isEmpty()
                        ? result.getPaths().get(0)
                        : null,
                AmapRouteSupport.toRouteSteps(
                        RouteMode.WALK,
                        null,
                        result != null && result.getPaths() != null && !result.getPaths().isEmpty() ? result.getPaths().get(0) : null,
                        null,
                        null
                ),
                AmapRouteSupport.flattenPolyline(
                        RouteMode.WALK,
                        null,
                        result != null && result.getPaths() != null && !result.getPaths().isEmpty() ? result.getPaths().get(0) : null,
                        null,
                        null
                ),
                rCode
        );
    }

    @Override
    public void onRideRouteSearched(RideRouteResult result, int rCode) {
        if (routeMode != RouteMode.RIDE) {
            return;
        }
        Log.d(TAG, "Ride route callback rCode=" + rCode);
        handleRouteResult(
                rCode == AMAP_SUCCESS && result != null && result.getPaths() != null && !result.getPaths().isEmpty()
                        ? result.getPaths().get(0)
                        : null,
                AmapRouteSupport.toRouteSteps(
                        RouteMode.RIDE,
                        null,
                        null,
                        result != null && result.getPaths() != null && !result.getPaths().isEmpty() ? result.getPaths().get(0) : null,
                        null
                ),
                AmapRouteSupport.flattenPolyline(
                        RouteMode.RIDE,
                        null,
                        null,
                        result != null && result.getPaths() != null && !result.getPaths().isEmpty() ? result.getPaths().get(0) : null,
                        null
                ),
                rCode
        );
    }

    private void handleRouteResult(Path path, List<RouteStep> routeSteps, List<LatLng> polylinePoints, int responseCode) {
        progressNavigation.setVisibility(View.GONE);
        lastRouteResponseCode = responseCode;
        if (path != null) {
            drawRoute(polylinePoints);
            adapter.submitList(routeSteps);
            if (routeMode == RouteMode.TRANSIT) {
                textNavigationStatusBody.setText(getString(R.string.route_status_transit_ready)
                        + " "
                        + getString(R.string.navigation_session_transit_preview_only));
            } else {
                textNavigationStatusBody.setText(AmapRouteSupport.getModeReadyText(this, routeMode));
            }
            textNavigationSummary.setText(AmapRouteSupport.formatRouteSummary(this, path));
            if (routeSteps.isEmpty()) {
                textNavigationStatusBody.setText(getString(R.string.navigation_empty));
            }
            updateDriveSessionCard();
            return;
        }

        drawRoute(AmapRouteSupport.buildFallbackPolyline(originLocation, item));
        String routeStatus = responseCode == AMAP_SUCCESS
                ? AmapRouteSupport.getEmptyRouteText(this, routeMode)
                : AmapRouteSupport.getRouteErrorText(this, responseCode);
        textNavigationStatusBody.setText(routeStatus + " " + getString(R.string.route_status_fallback_preview));
        textNavigationSummary.setText(AmapRouteSupport.formatFallbackRouteSummary(this, originLocation, item, routeMode));
        adapter.submitList(AmapRouteSupport.buildFallbackRouteSteps(originLocation, item, routeMode));
        updateDriveSessionCard();
    }

    private void drawRoute(List<LatLng> polylinePoints) {
        redrawBaseMarkers();
        if (!polylinePoints.isEmpty()) {
            routePolyline = aMap.addPolyline(new PolylineOptions()
                    .addAll(polylinePoints)
                    .width(18f)
                    .color(getColor(R.color.brand_primary)));
        }
    }

    private int resolveCheckedButtonId(RouteMode mode) {
        if (mode == RouteMode.WALK) {
            return R.id.buttonModeWalk;
        }
        if (mode == RouteMode.RIDE) {
            return R.id.buttonModeRide;
        }
        if (mode == RouteMode.TRANSIT) {
            return R.id.buttonModeTransit;
        }
        return R.id.buttonModeDrive;
    }

    private RouteMode resolveRouteMode(int checkedId) {
        if (checkedId == R.id.buttonModeWalk) {
            return RouteMode.WALK;
        }
        if (checkedId == R.id.buttonModeRide) {
            return RouteMode.RIDE;
        }
        if (checkedId == R.id.buttonModeTransit) {
            return RouteMode.TRANSIT;
        }
        return RouteMode.DRIVE;
    }

    private void setDriveSessionMessage(String message) {
        if (textDriveSessionBody == null) {
            return;
        }
        runOnUiThread(() -> textDriveSessionBody.setText(message));
    }

    @Override
    public void onInitNaviFailure() {
        setDriveSessionMessage(getString(
                R.string.navigation_session_init_failed_format,
                getSessionDescriptor(routeMode)
        ));
    }

    @Override
    public void onGetNavigationText(String text) {
        if (TextUtils.isEmpty(text)) {
            return;
        }
        String translated = AmapRouteSupport.translateNavigationPromptToEnglish(text);
        setDriveSessionMessage(getString(R.string.navigation_session_last_prompt, translated));
        speakNavigationPrompt(translated);
    }

    @Override
    public void onLocationChange(AMapNaviLocation aMapNaviLocation) {
        if (aMapNaviLocation == null || aMapNaviLocation.getCoord() == null) {
            return;
        }
        Log.d(TAG, "Drive session location update "
                + aMapNaviLocation.getCoord().getLatitude() + ","
                + aMapNaviLocation.getCoord().getLongitude());
    }

    @Override
    public void onArriveDestination(boolean arrived) {
        if (arrived) {
            setDriveSessionMessage(getString(R.string.navigation_session_arrived));
        }
    }

    @Override
    public void onStartNavi(int type) {
        setDriveSessionMessage(type == NaviType.EMULATOR
                ? getString(R.string.navigation_session_started_simulated_format, getSessionDescriptor(routeMode))
                : getString(R.string.navigation_session_started_live_format, getSessionDescriptor(routeMode)));
    }

    @Override
    public void onCalculateRouteSuccess(int[] ints) {
        setDriveSessionMessage(getString(
                R.string.navigation_session_route_ready_format,
                getSessionDescriptor(routeMode)
        ));
    }

    @Override
    public void onCalculateRouteFailure(int errorCode) {
        setDriveSessionMessage(getString(R.string.navigation_session_route_failed, errorCode));
    }

    @Override
    public void onStopSpeaking() {
        // No-op.
    }

    @Override
    public void onReCalculateRoute(int reason) {
        setDriveSessionMessage(getString(R.string.navigation_session_recalculating, reason));
    }

    @Override
    public void onExitPage(int pageType) {
        setDriveSessionMessage(getString(R.string.navigation_session_closed));
    }

    @Override
    public void onStrategyChanged(int strategy) {
        Log.d(TAG, "Drive session strategy changed to " + strategy);
    }

    @Override
    public void onArrivedWayPoint(int wayPointIndex) {
        Log.d(TAG, "Arrived waypoint index " + wayPointIndex);
    }

    @Override
    public void onMapTypeChanged(int mapType) {
        Log.d(TAG, "Drive session map type " + mapType);
    }

    @Override
    public void onNaviDirectionChanged(int direction) {
        Log.d(TAG, "Drive session direction changed " + direction);
    }

    @Override
    public void onDayAndNightModeChanged(int mode) {
        Log.d(TAG, "Drive session day/night mode " + mode);
    }

    @Override
    public void onBroadcastModeChanged(int mode) {
        Log.d(TAG, "Drive session broadcast mode " + mode);
    }

    @Override
    public void onScaleAutoChanged(boolean enabled) {
        Log.d(TAG, "Drive session auto-scale " + enabled);
    }

    @Override
    public View getCustomMiddleView() {
        return null;
    }

    @Override
    public View getCustomNaviView() {
        return null;
    }

    @Override
    public View getCustomNaviBottomView() {
        return null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        if (englishTts != null) {
            englishTts.stop();
            englishTts.shutdown();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    private void initializeEnglishTts() {
        englishTts = new TextToSpeech(getApplicationContext(), status -> {
            if (status != TextToSpeech.SUCCESS) {
                englishTtsReady = false;
                return;
            }
            int languageResult = englishTts.setLanguage(Locale.US);
            englishTts.setSpeechRate(1.0f);
            englishTtsReady = languageResult != TextToSpeech.LANG_MISSING_DATA
                    && languageResult != TextToSpeech.LANG_NOT_SUPPORTED;
        });
    }

    private void speakNavigationPrompt(String prompt) {
        if (!englishTtsReady || TextUtils.isEmpty(prompt)) {
            return;
        }
        englishTts.speak(prompt, TextToSpeech.QUEUE_FLUSH, null, "amap-nav-prompt");
    }

    private boolean supportsLiveGuidance(RouteMode mode) {
        return mode == RouteMode.DRIVE || mode == RouteMode.WALK || mode == RouteMode.RIDE;
    }

    private AmapNaviType resolveNaviType(RouteMode mode) {
        if (mode == RouteMode.WALK) {
            return AmapNaviType.WALK;
        }
        if (mode == RouteMode.RIDE) {
            return AmapNaviType.RIDE;
        }
        if (mode == RouteMode.DRIVE) {
            return AmapNaviType.DRIVER;
        }
        return null;
    }

    private String getSessionDescriptor(RouteMode mode) {
        if (mode == RouteMode.WALK) {
            return "walk guidance";
        }
        if (mode == RouteMode.RIDE) {
            return "ride guidance";
        }
        return "drive navigation";
    }

    private String getSessionActionLabel(RouteMode mode) {
        if (mode == RouteMode.WALK) {
            return "walk";
        }
        if (mode == RouteMode.RIDE) {
            return "ride";
        }
        return "drive";
    }

    private String getSessionTitle(RouteMode mode) {
        if (mode == RouteMode.WALK) {
            return getString(R.string.navigation_session_title_walk);
        }
        if (mode == RouteMode.RIDE) {
            return getString(R.string.navigation_session_title_ride);
        }
        return getString(R.string.navigation_session_title);
    }
}
