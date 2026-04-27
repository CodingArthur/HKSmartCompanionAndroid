package hk.edu.hku.cs7506.smartcompanion;

import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.button.MaterialButton;

import hk.edu.hku.cs7506.smartcompanion.data.location.CurrentLocationManager;
import hk.edu.hku.cs7506.smartcompanion.data.model.DataMode;
import hk.edu.hku.cs7506.smartcompanion.data.model.RecommendationItem;
import hk.edu.hku.cs7506.smartcompanion.data.repository.AppRepository;
import hk.edu.hku.cs7506.smartcompanion.data.repository.RepositoryCallback;
import hk.edu.hku.cs7506.smartcompanion.data.model.SceneType;
import hk.edu.hku.cs7506.smartcompanion.ui.favorites.FavoritesActivity;
import hk.edu.hku.cs7506.smartcompanion.ui.settings.SettingsActivity;
import hk.edu.hku.cs7506.smartcompanion.util.FormatUtils;
import hk.edu.hku.cs7506.smartcompanion.util.IntentKeys;

public class MainActivity extends AppCompatActivity {
    private ActivityResultLauncher<String[]> locationPermissionLauncher;
    private AppRepository repository;
    private TextView textDataPulseTitle;
    private TextView textDataPulseBody;
    private MaterialButton buttonRefreshLiveData;
    private MaterialCardView cardRecentPlace;
    private TextView textRecentTitle;
    private TextView textRecentBody;
    private RecommendationItem recentItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        repository = AppRepository.getInstance(this);
        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                }
        );

        MaterialCardView cardEmergency = findViewById(R.id.cardEmergency);
        MaterialCardView cardParking = findViewById(R.id.cardParking);
        MaterialCardView cardPlay = findViewById(R.id.cardPlay);
        MaterialCardView cardCommute = findViewById(R.id.cardCommute);
        MaterialButton buttonFavorites = findViewById(R.id.buttonFavorites);
        MaterialButton buttonSettings = findViewById(R.id.buttonSettings);
        textDataPulseTitle = findViewById(R.id.textDataPulseTitle);
        textDataPulseBody = findViewById(R.id.textDataPulseBody);
        buttonRefreshLiveData = findViewById(R.id.buttonRefreshLiveData);
        cardRecentPlace = findViewById(R.id.cardRecentPlace);
        textRecentTitle = findViewById(R.id.textRecentTitle);
        textRecentBody = findViewById(R.id.textRecentBody);
        MaterialButton buttonResumeRecent = findViewById(R.id.buttonResumeRecent);

        cardEmergency.setOnClickListener(v -> openScene(SceneType.EMERGENCY));
        cardParking.setOnClickListener(v -> openScene(SceneType.PARKING));
        cardPlay.setOnClickListener(v -> openScene(SceneType.PLAY));
        cardCommute.setOnClickListener(v -> openScene(SceneType.COMMUTE));
        buttonFavorites.setOnClickListener(v -> startActivity(new Intent(this, FavoritesActivity.class)));
        buttonSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        buttonRefreshLiveData.setOnClickListener(v -> refreshLiveData());
        buttonResumeRecent.setOnClickListener(v -> {
            if (recentItem != null) {
                Intent intent = new Intent(this, PlaceDetailActivity.class);
                intent.putExtra(IntentKeys.EXTRA_ITEM, recentItem);
                startActivity(intent);
            }
        });

        if (!repository.hasLocationPermission()) {
            locationPermissionLauncher.launch(CurrentLocationManager.REQUIRED_PERMISSIONS);
        }

        bindDataPulse(false);
        bindRecentCard();
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindDataPulse(false);
        bindRecentCard();
    }

    private void openScene(SceneType sceneType) {
        Intent intent = new Intent(this, RecommendationListActivity.class);
        intent.putExtra(IntentKeys.EXTRA_SCENE, sceneType.getApiValue());
        startActivity(intent);
    }

    private void refreshLiveData() {
        bindDataPulse(true);
        buttonRefreshLiveData.setEnabled(false);
        repository.prefetchOfficialData(new RepositoryCallback<Integer>() {
            @Override
            public void onSuccess(Integer data) {
                buttonRefreshLiveData.setEnabled(true);
                bindDataPulse(false);
                Toast.makeText(
                        MainActivity.this,
                        data != null && data > 0 ? getString(R.string.home_data_pulse_refreshed, data) : getString(R.string.home_data_pulse_skipped),
                        Toast.LENGTH_SHORT
                ).show();
            }

            @Override
            public void onError(String message) {
                buttonRefreshLiveData.setEnabled(true);
                bindDataPulse(false);
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindDataPulse(boolean loading) {
        DataMode dataMode = repository.getSettings().getDataMode();
        if (loading) {
            textDataPulseTitle.setText(R.string.home_data_pulse_loading);
            textDataPulseBody.setText(R.string.home_data_pulse_loading_body);
            return;
        }

        if (dataMode == DataMode.DEMO) {
            textDataPulseTitle.setText(R.string.home_data_pulse_demo);
            textDataPulseBody.setText(R.string.home_data_pulse_demo_body);
            return;
        }

        int cachedFeedCount = repository.getCachedFeedCount();
        long lastPrefetchEpoch = repository.getLastPrefetchEpochMillis();
        if (cachedFeedCount <= 0 || lastPrefetchEpoch <= 0L) {
            textDataPulseTitle.setText(R.string.home_data_pulse_cold);
            textDataPulseBody.setText(R.string.home_data_pulse_cold_body);
            return;
        }

        CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                lastPrefetchEpoch,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
        );
        textDataPulseTitle.setText(getString(R.string.home_data_pulse_ready, cachedFeedCount));
        textDataPulseBody.setText(getString(
                R.string.home_data_pulse_ready_body,
                relativeTime,
                repository.hasAmapKeyConfigured()
                        ? getString(R.string.settings_amap_ready)
                        : getString(R.string.settings_amap_waiting)
        ));
    }

    private void bindRecentCard() {
        recentItem = repository.getMostRecentItem();
        if (recentItem == null) {
            cardRecentPlace.setVisibility(android.view.View.GONE);
            return;
        }
        cardRecentPlace.setVisibility(android.view.View.VISIBLE);
        textRecentTitle.setText(recentItem.getName());
        String metadataLine = recentItem.getMetadataLine();
        if (metadataLine == null || metadataLine.trim().isEmpty()) {
            metadataLine = FormatUtils.getDisplayTag(this, recentItem);
        }
        textRecentBody.setText(metadataLine);
    }
}
