package hk.edu.hku.cs7506.smartcompanion.ui.map;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.route.BusPath;
import com.amap.api.services.route.BusRouteResult;
import com.amap.api.services.route.DrivePath;
import com.amap.api.services.route.DriveRouteResult;
import com.amap.api.services.route.Path;
import com.amap.api.services.route.RidePath;
import com.amap.api.services.route.RideRouteResult;
import com.amap.api.services.route.RouteSearch;
import com.amap.api.services.route.WalkPath;
import com.amap.api.services.route.WalkRouteResult;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.List;

import hk.edu.hku.cs7506.smartcompanion.R;
import hk.edu.hku.cs7506.smartcompanion.data.model.LocationSnapshot;
import hk.edu.hku.cs7506.smartcompanion.data.model.RecommendationItem;
import hk.edu.hku.cs7506.smartcompanion.data.model.RouteMode;
import hk.edu.hku.cs7506.smartcompanion.data.repository.AppRepository;
import hk.edu.hku.cs7506.smartcompanion.data.repository.RepositoryCallback;
import hk.edu.hku.cs7506.smartcompanion.ui.navigation.NavigationPreviewActivity;
import hk.edu.hku.cs7506.smartcompanion.util.AmapRouteSupport;
import hk.edu.hku.cs7506.smartcompanion.util.FormatUtils;
import hk.edu.hku.cs7506.smartcompanion.util.IntentKeys;

public class MapPreviewActivity extends AppCompatActivity implements RouteSearch.OnRouteSearchListener {
    private static final int AMAP_SUCCESS = 1000;
    private static final String TAG = "MapPreviewActivity";

    private RecommendationItem item;
    private AppRepository repository;
    private MapView mapView;
    private AMap aMap;
    private RouteSearch routeSearch;
    private TextView textMapOrigin;
    private TextView textMapDestination;
    private TextView textMapSummary;
    private TextView textMapStatus;
    private TextView textMapTransportHeader;
    private TextView textMapTrafficNote;
    private TextView textMapTransitNote;
    private LinearProgressIndicator progressRoute;
    private MaterialButton buttonOpenNavigation;
    private MaterialCardView cardMapTransport;
    private RouteMode routeMode = RouteMode.DRIVE;
    private LocationSnapshot originLocation;
    private boolean hasAmapKeyConfigured;
    private int lastRouteResponseCode = AMAP_SUCCESS;
    private boolean pendingCameraFit;
    private Marker originMarker;
    private Marker destinationMarker;
    private Polyline routePolyline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_preview);

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
        textMapOrigin = findViewById(R.id.textMapOrigin);
        textMapDestination = findViewById(R.id.textMapDestination);
        textMapSummary = findViewById(R.id.textMapSummary);
        textMapStatus = findViewById(R.id.textMapStatus);
        textMapTransportHeader = findViewById(R.id.textMapTransportHeader);
        textMapTrafficNote = findViewById(R.id.textMapTrafficNote);
        textMapTransitNote = findViewById(R.id.textMapTransitNote);
        progressRoute = findViewById(R.id.progressRoute);
        buttonOpenNavigation = findViewById(R.id.buttonOpenNavigation);
        cardMapTransport = findViewById(R.id.cardMapTransport);

        toolbar.setTitle(item.getName());
        toolbar.setNavigationOnClickListener(v -> finish());

        mapView.onCreate(savedInstanceState);
        aMap = mapView.getMap();
        try {
            routeSearch = new RouteSearch(this);
            routeSearch.setRouteSearchListener(this);
        } catch (AMapException exception) {
            textMapStatus.setText(getString(R.string.map_status_error_prefix) + " " + exception.getMessage());
            buttonOpenNavigation.setEnabled(false);
            return;
        }

        routeModeGroup.check(resolveCheckedButtonId(routeMode));
        routeModeGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            routeMode = resolveRouteMode(checkedId);
            if (originLocation != null) {
                if (hasAmapKeyConfigured) {
                    requestRoute();
                } else {
                    renderFallbackRouteState();
                }
            }
        });

        textMapDestination.setText(
                getString(R.string.map_destination_label) + ": " + item.getName() + " - "
                        + FormatUtils.formatCoordinate(this, item)
        );
        textMapSummary.setText(R.string.route_summary_unavailable);
        bindTransportCard();

        buttonOpenNavigation.setOnClickListener(v -> {
            Intent intent = new Intent(this, NavigationPreviewActivity.class);
            intent.putExtra(IntentKeys.EXTRA_ITEM, item);
            intent.putExtra(IntentKeys.EXTRA_ROUTE_MODE, routeMode.getId());
            startActivity(intent);
        });

        configureMap();
        resolveOriginThenRoute();
    }

    private void bindTransportCard() {
        bindOptionalText(textMapTrafficNote, item.getTrafficNote());
        bindOptionalText(textMapTransitNote, item.getTransitNote());
        boolean hasTransport = !TextUtils.isEmpty(item.getTrafficNote()) || !TextUtils.isEmpty(item.getTransitNote());
        cardMapTransport.setVisibility(hasTransport ? View.VISIBLE : View.GONE);
        textMapTransportHeader.setVisibility(hasTransport ? View.VISIBLE : View.GONE);
    }

    private void resolveOriginThenRoute() {
        originLocation = repository.getBestAvailableLocation();
        bindOrigin(originLocation, getString(R.string.map_status_resolving_origin));
        if (!hasAmapKeyConfigured) {
            bindOrigin(originLocation, getString(R.string.navigation_notice_key_missing));
            renderFallbackRouteState();
            return;
        }

        if (!repository.hasLocationPermission()) {
            bindOrigin(originLocation, getString(R.string.location_permission_pending));
            requestRoute();
            return;
        }

        repository.requestCurrentLocation(new RepositoryCallback<LocationSnapshot>() {
            @Override
            public void onSuccess(LocationSnapshot data) {
                originLocation = data;
                bindOrigin(data, data.getSourceLabel());
                requestRoute();
            }

            @Override
            public void onError(String message) {
                originLocation = repository.getBestAvailableLocation();
                bindOrigin(originLocation, message);
                requestRoute();
            }
        });
    }

    private void bindOrigin(LocationSnapshot origin, String message) {
        textMapOrigin.setText(getString(R.string.map_origin_label) + ": " + AmapRouteSupport.getOriginLabel(origin));
        textMapStatus.setText(message);
        redrawBaseMarkers();
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
                .snippet(AmapRouteSupport.getOriginLabel(originLocation)));
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
                + " route from " + originLocation.getLatitude() + "," + originLocation.getLongitude()
                + " to " + item.getLatitude() + "," + item.getLongitude());
        progressRoute.setVisibility(View.VISIBLE);
        textMapSummary.setText(R.string.route_summary_unavailable);
        textMapStatus.setText(AmapRouteSupport.getModeLoadingText(this, routeMode));

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
        progressRoute.setVisibility(View.GONE);
        drawRoute(AmapRouteSupport.buildFallbackPolyline(originLocation, item));
        textMapSummary.setText(AmapRouteSupport.formatFallbackRouteSummary(this, originLocation, item, routeMode));
        textMapStatus.setText(getString(R.string.navigation_notice_key_missing)
                + " "
                + getString(R.string.route_status_fallback_preview));
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

    private void handleRouteResult(Path path, List<LatLng> polylinePoints, int responseCode) {
        progressRoute.setVisibility(View.GONE);
        lastRouteResponseCode = responseCode;
        if (path != null) {
            drawRoute(polylinePoints);
            textMapSummary.setText(AmapRouteSupport.formatRouteSummary(this, path));
            textMapStatus.setText(AmapRouteSupport.getModeReadyText(this, routeMode));
            return;
        }

        drawRoute(AmapRouteSupport.buildFallbackPolyline(originLocation, item));
        textMapSummary.setText(AmapRouteSupport.formatFallbackRouteSummary(this, originLocation, item, routeMode));
        String routeStatus = responseCode == AMAP_SUCCESS
                ? AmapRouteSupport.getEmptyRouteText(this, routeMode)
                : AmapRouteSupport.getRouteErrorText(this, responseCode);
        textMapStatus.setText(routeStatus + " " + getString(R.string.route_status_fallback_preview));
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

    private void bindOptionalText(TextView textView, String value) {
        if (TextUtils.isEmpty(value)) {
            textView.setVisibility(View.GONE);
            textView.setText("");
            return;
        }
        textView.setVisibility(View.VISIBLE);
        textView.setText(value);
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
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}
