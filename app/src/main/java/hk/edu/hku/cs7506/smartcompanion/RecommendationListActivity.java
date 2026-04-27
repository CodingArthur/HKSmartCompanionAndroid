package hk.edu.hku.cs7506.smartcompanion;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.Locale;

import hk.edu.hku.cs7506.smartcompanion.data.location.CurrentLocationManager;
import hk.edu.hku.cs7506.smartcompanion.data.model.CommuteCorridor;
import hk.edu.hku.cs7506.smartcompanion.data.model.DataMode;
import hk.edu.hku.cs7506.smartcompanion.data.model.EmergencySortMode;
import hk.edu.hku.cs7506.smartcompanion.data.model.LocationSnapshot;
import hk.edu.hku.cs7506.smartcompanion.data.model.ParkingDestination;
import hk.edu.hku.cs7506.smartcompanion.data.model.RecommendationLoadResult;
import hk.edu.hku.cs7506.smartcompanion.data.model.RecommendationRequest;
import hk.edu.hku.cs7506.smartcompanion.data.model.SceneType;
import hk.edu.hku.cs7506.smartcompanion.data.repository.AppRepository;
import hk.edu.hku.cs7506.smartcompanion.data.repository.RepositoryCallback;
import hk.edu.hku.cs7506.smartcompanion.ui.adapter.RecommendationAdapter;
import hk.edu.hku.cs7506.smartcompanion.util.FormatUtils;
import hk.edu.hku.cs7506.smartcompanion.util.IntentKeys;

public class RecommendationListActivity extends AppCompatActivity {
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerRecommendations;
    private RecommendationAdapter adapter;
    private LinearLayout emptyStateLayout;
    private TextView textModeLabel;
    private TextView textStatus;
    private TextView textEmptyTitle;
    private TextView textEmptyBody;
    private TextView textLocationTitle;
    private TextView textLocationBody;
    private View cardModeState;
    private View emergencyFilterSection;
    private View parkingDestinationSection;
    private View commuteFilterSection;
    private View filterCardSection;
    private SceneType sceneType;
    private AppRepository repository;
    private EmergencySortMode emergencySortMode = EmergencySortMode.TOTAL_TIME;
    private ParkingDestination parkingDestination = ParkingDestination.CENTRAL_WATERFRONT;
    private CommuteCorridor commuteCorridor = CommuteCorridor.CENTRAL_TO_SHA_TIN;
    private LocationSnapshot currentLocation;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;
    private boolean locationPermissionRequested;
    private boolean commuteCorridorManuallySelected;
    private MaterialButton inputCommuteCorridor;
    private MaterialButton inputEmergencySort;
    private MaterialButton inputParkingDestination;
    private int latestLoadRequestToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recommendation_list);

        repository = AppRepository.getInstance(this);
        sceneType = SceneType.fromApiValue(getIntent().getStringExtra(IntentKeys.EXTRA_SCENE));

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        recyclerRecommendations = findViewById(R.id.recyclerRecommendations);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        cardModeState = findViewById(R.id.cardModeState);
        textModeLabel = findViewById(R.id.textModeLabel);
        textStatus = findViewById(R.id.textStatus);
        textEmptyTitle = findViewById(R.id.textEmptyTitle);
        textEmptyBody = findViewById(R.id.textEmptyBody);
        textLocationTitle = findViewById(R.id.textLocationTitle);
        textLocationBody = findViewById(R.id.textLocationBody);
        emergencyFilterSection = findViewById(R.id.emergencyFilterSection);
        parkingDestinationSection = findViewById(R.id.parkingDestinationSection);
        commuteFilterSection = findViewById(R.id.commuteFilterSection);
        filterCardSection = findViewById(R.id.cardFilterSection);
        inputEmergencySort = findViewById(R.id.inputEmergencySort);
        inputParkingDestination = findViewById(R.id.inputParkingDestination);
        inputCommuteCorridor = findViewById(R.id.inputCommuteCorridor);
        Button buttonRetry = findViewById(R.id.buttonRetry);

        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> loadData()
        );

        toolbar.setTitle(FormatUtils.getSceneTitle(this, sceneType));
        toolbar.setNavigationOnClickListener(v -> finish());

        adapter = new RecommendationAdapter(item -> {
            Intent intent = new Intent(this, PlaceDetailActivity.class);
            intent.putExtra(IntentKeys.EXTRA_ITEM, item);
            startActivity(intent);
        });
        recyclerRecommendations.setLayoutManager(new LinearLayoutManager(this));
        recyclerRecommendations.setAdapter(adapter);

        bindFilterControls();
        swipeRefreshLayout.setOnRefreshListener(this::loadData);
        buttonRetry.setOnClickListener(v -> loadData());

        loadData();
    }

    private void bindFilterControls() {
        emergencyFilterSection.setVisibility(sceneType == SceneType.EMERGENCY ? View.VISIBLE : View.GONE);
        parkingDestinationSection.setVisibility(sceneType == SceneType.PARKING ? View.VISIBLE : View.GONE);
        commuteFilterSection.setVisibility(sceneType == SceneType.COMMUTE ? View.VISIBLE : View.GONE);
        filterCardSection.setVisibility(sceneType == SceneType.PLAY ? View.GONE : View.VISIBLE);

        bindEmergencySortDropdown(inputEmergencySort);
        bindParkingDestinationDropdown(inputParkingDestination);
        bindCommuteCorridorDropdown(inputCommuteCorridor);
    }

    private void loadData() {
        int requestToken = ++latestLoadRequestToken;
        swipeRefreshLayout.setRefreshing(true);
        bindModeStateCard(
                FormatUtils.getModeLabel(this, repository.getSettings().getDataMode()),
                getString(R.string.list_status_loading),
                true
        );

        if (!repository.hasLocationPermission()) {
            if (!locationPermissionRequested) {
                locationPermissionRequested = true;
                locationPermissionLauncher.launch(CurrentLocationManager.REQUIRED_PERMISSIONS);
            }
            currentLocation = repository.getBestAvailableLocation();
            syncCommuteCorridorFromLocation(currentLocation);
            bindLocation(currentLocation, getString(R.string.location_permission_pending));
            requestRecommendations(requestToken);
            return;
        }

        repository.requestCurrentLocation(new RepositoryCallback<LocationSnapshot>() {
            @Override
            public void onSuccess(LocationSnapshot data) {
                if (isStaleRequest(requestToken)) {
                    return;
                }
                currentLocation = data;
                syncCommuteCorridorFromLocation(data);
                bindLocation(data, data.getSourceLabel());
                requestRecommendations(requestToken);
            }

            @Override
            public void onError(String message) {
                if (isStaleRequest(requestToken)) {
                    return;
                }
                currentLocation = repository.getBestAvailableLocation();
                syncCommuteCorridorFromLocation(currentLocation);
                bindLocation(currentLocation, message);
                requestRecommendations(requestToken);
            }
        });
    }

    private void requestRecommendations(int requestToken) {
        RecommendationRequest request = new RecommendationRequest(
                sceneType,
                currentLocation.getLatitude(),
                currentLocation.getLongitude(),
                emergencySortMode,
                parkingDestination,
                commuteCorridor
        );
        repository.loadRecommendations(request, new RepositoryCallback<RecommendationLoadResult>() {
            @Override
            public void onSuccess(RecommendationLoadResult data) {
                if (isStaleRequest(requestToken)) {
                    return;
                }
                swipeRefreshLayout.setRefreshing(false);
                adapter.submitList(data.getItems());
                String modeLabel = FormatUtils.getModeLabel(RecommendationListActivity.this, data.getResolvedMode());
                String statusMessage = resolveStatusMessage(data);
                boolean showModeState = data.getResolvedMode() != DataMode.OFFICIAL || !TextUtils.isEmpty(statusMessage);
                bindModeStateCard(modeLabel, statusMessage, showModeState);

                boolean isEmpty = data.getItems().isEmpty();
                emptyStateLayout.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                textEmptyTitle.setText(R.string.list_status_empty);
                textEmptyBody.setText(FormatUtils.getSceneSubtitle(RecommendationListActivity.this, sceneType));
            }

            @Override
            public void onError(String message) {
                if (isStaleRequest(requestToken)) {
                    return;
                }
                swipeRefreshLayout.setRefreshing(false);
                adapter.submitList(java.util.Collections.emptyList());
                emptyStateLayout.setVisibility(View.VISIBLE);
                bindModeStateCard(
                        FormatUtils.getModeLabel(RecommendationListActivity.this, repository.getSettings().getDataMode()),
                        getString(R.string.list_status_error),
                        true
                );
                textEmptyTitle.setText(R.string.list_status_error);
                textEmptyBody.setText(message);
            }
        });
    }

    private String resolveStatusMessage(RecommendationLoadResult data) {
        String message = data.getStatusMessage();
        if (TextUtils.isEmpty(message)) {
            return "";
        }
        if (getString(R.string.status_message_official_loaded).equals(message)) {
            return "";
        }
        return message;
    }

    private void bindEmergencySortDropdown(MaterialButton button) {
        String[] labels = new String[]{
                getString(R.string.emergency_sort_total_time),
                getString(R.string.emergency_sort_nearest),
                getString(R.string.emergency_sort_shortest_wait)
        };
        button.setText(labels[emergencySortMode.ordinal()]);
        button.setOnClickListener(v -> showChoiceDialog(getString(R.string.emergency_sort_title), labels, emergencySortMode.ordinal(), position -> {
            EmergencySortMode newMode;
            if (position == 1) {
                newMode = EmergencySortMode.NEAREST;
            } else if (position == 2) {
                newMode = EmergencySortMode.SHORTEST_WAIT;
            } else {
                newMode = EmergencySortMode.TOTAL_TIME;
            }
            if (newMode == emergencySortMode) {
                return;
            }
            emergencySortMode = newMode;
            button.setText(labels[position]);
            scrollRecommendationsToTop();
            loadData();
        }));
    }

    private void bindParkingDestinationDropdown(MaterialButton button) {
        ParkingDestination[] destinations = ParkingDestination.values();
        String[] labels = new String[destinations.length];
        for (int index = 0; index < destinations.length; index++) {
            labels[index] = destinations[index].getDisplayName();
        }
        button.setText(labels[parkingDestination.ordinal()]);
        button.setOnClickListener(v -> showChoiceDialog(getString(R.string.parking_destination_title), labels, parkingDestination.ordinal(), position -> {
            ParkingDestination selected = destinations[position];
            if (selected == parkingDestination) {
                return;
            }
            parkingDestination = selected;
            button.setText(labels[position]);
            scrollRecommendationsToTop();
            loadData();
        }));
    }

    private void bindCommuteCorridorDropdown(MaterialButton button) {
        CommuteCorridor[] corridors = CommuteCorridor.values();
        String[] labels = new String[corridors.length];
        for (int index = 0; index < corridors.length; index++) {
            labels[index] = corridors[index].getDisplayName(this);
        }
        button.setText(labels[commuteCorridor.ordinal()]);
        button.setOnClickListener(v -> showChoiceDialog(getString(R.string.commute_corridor_title), labels, commuteCorridor.ordinal(), position -> {
            CommuteCorridor selected = corridors[position];
            if (selected == commuteCorridor) {
                return;
            }
            commuteCorridor = selected;
            commuteCorridorManuallySelected = true;
            button.setText(labels[position]);
            scrollRecommendationsToTop();
            loadData();
        }));
    }

    private void bindLocation(LocationSnapshot snapshot, String message) {
        String districtSuffix = snapshot.getDistrict().isEmpty() ? "" : " | " + snapshot.getDistrict();
        textLocationTitle.setText(snapshot.getLabel() + districtSuffix);
        bindOptionalText(textLocationBody, shouldShowLocationDetail(snapshot, message) ? message : "");
    }

    private void scrollRecommendationsToTop() {
        if (recyclerRecommendations != null) {
            recyclerRecommendations.scrollToPosition(0);
        }
    }

    private void bindOptionalText(TextView view, String message) {
        if (TextUtils.isEmpty(message)) {
            view.setText(null);
            view.setVisibility(View.GONE);
            return;
        }
        view.setText(message);
        view.setVisibility(View.VISIBLE);
    }

    private void bindModeStateCard(String modeLabel, String statusMessage, boolean visible) {
        textModeLabel.setText(modeLabel);
        bindOptionalText(textStatus, statusMessage);
        cardModeState.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private boolean shouldShowLocationDetail(LocationSnapshot snapshot, String message) {
        if (TextUtils.isEmpty(message)) {
            return false;
        }
        String normalized = message.toLowerCase(Locale.US);
        if (!snapshot.isFallback() && message.equals(snapshot.getSourceLabel())) {
            return false;
        }
        if (snapshot.isFallback() && normalized.contains("outside hong kong")) {
            return false;
        }
        if (snapshot.isFallback() && normalized.contains("stable fallback origin")) {
            return false;
        }
        if (normalized.contains("gps/network location fix")) {
            return false;
        }
        return normalized.contains("pending")
                || normalized.contains("permission")
                || normalized.contains("failed")
                || normalized.contains("timed out")
                || normalized.contains("amap code");
    }

    private void syncCommuteCorridorFromLocation(LocationSnapshot snapshot) {
        if (sceneType != SceneType.COMMUTE || snapshot == null || commuteCorridorManuallySelected || inputCommuteCorridor == null) {
            return;
        }
        CommuteCorridor nearest = CommuteCorridor.nearestTo(snapshot.getLatitude(), snapshot.getLongitude());
        if (nearest == commuteCorridor) {
            return;
        }
        commuteCorridor = nearest;
        inputCommuteCorridor.setText(nearest.getDisplayName(this));
    }

    private boolean isStaleRequest(int requestToken) {
        return requestToken != latestLoadRequestToken;
    }

    private void showChoiceDialog(String title, String[] labels, int checkedIndex, MenuSelectionListener listener) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setSingleChoiceItems(labels, checkedIndex, (dialog, which) -> {
                    dialog.dismiss();
                    listener.onSelected(which);
                })
                .show();
    }

    private interface MenuSelectionListener {
        void onSelected(int position);
    }
}
