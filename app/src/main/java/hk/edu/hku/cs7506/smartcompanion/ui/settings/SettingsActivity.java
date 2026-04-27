package hk.edu.hku.cs7506.smartcompanion.ui.settings;

import android.os.Bundle;
import android.text.format.DateUtils;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import hk.edu.hku.cs7506.smartcompanion.R;
import hk.edu.hku.cs7506.smartcompanion.data.model.DataMode;
import hk.edu.hku.cs7506.smartcompanion.data.model.UserSettings;
import hk.edu.hku.cs7506.smartcompanion.data.repository.AppRepository;
import hk.edu.hku.cs7506.smartcompanion.data.repository.RepositoryCallback;

public class SettingsActivity extends AppCompatActivity {
    private AppRepository repository;
    private RadioGroup radioGroupMode;
    private TextView textAmapStatus;
    private TextView textCacheStatus;
    private MaterialButton buttonWarmCache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        repository = AppRepository.getInstance(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        radioGroupMode = findViewById(R.id.radioGroupMode);
        textAmapStatus = findViewById(R.id.textAmapStatus);
        textCacheStatus = findViewById(R.id.textCacheStatus);
        MaterialButton buttonSave = findViewById(R.id.buttonSave);
        MaterialButton buttonReset = findViewById(R.id.buttonReset);
        buttonWarmCache = findViewById(R.id.buttonWarmCache);

        toolbar.setNavigationOnClickListener(v -> finish());
        buttonSave.setOnClickListener(v -> saveSettings());
        buttonReset.setOnClickListener(v -> {
            repository.resetSettings();
            bindSettings();
            Toast.makeText(this, R.string.settings_reset, Toast.LENGTH_SHORT).show();
        });
        buttonWarmCache.setOnClickListener(v -> warmCache());

        bindSettings();
    }

    private void bindSettings() {
        UserSettings settings = repository.getSettings();

        if (settings.getDataMode() == DataMode.DEMO) {
            radioGroupMode.check(R.id.radioDemo);
        } else if (settings.getDataMode() == DataMode.OFFICIAL) {
            radioGroupMode.check(R.id.radioOfficial);
        } else {
            radioGroupMode.check(R.id.radioAuto);
        }

        textAmapStatus.setText(repository.hasAmapKeyConfigured() ? R.string.settings_amap_ready : R.string.settings_amap_waiting);
        bindCacheStatus();
    }

    private void saveSettings() {
        DataMode mode;
        int checkedId = radioGroupMode.getCheckedRadioButtonId();
        if (checkedId == R.id.radioDemo) {
            mode = DataMode.DEMO;
        } else if (checkedId == R.id.radioOfficial) {
            mode = DataMode.OFFICIAL;
        } else {
            mode = DataMode.AUTO;
        }
        repository.saveSettings(new UserSettings(mode));
        Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show();
        bindSettings();
    }

    private void warmCache() {
        buttonWarmCache.setEnabled(false);
        textCacheStatus.setText(R.string.home_data_pulse_loading);
        repository.prefetchOfficialData(new RepositoryCallback<Integer>() {
            @Override
            public void onSuccess(Integer data) {
                buttonWarmCache.setEnabled(true);
                bindCacheStatus();
                Toast.makeText(
                        SettingsActivity.this,
                        data != null && data > 0 ? getString(R.string.home_data_pulse_refreshed, data) : getString(R.string.home_data_pulse_skipped),
                        Toast.LENGTH_SHORT
                ).show();
            }

            @Override
            public void onError(String message) {
                buttonWarmCache.setEnabled(true);
                bindCacheStatus();
                Toast.makeText(SettingsActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindCacheStatus() {
        if (repository.getSettings().getDataMode() == DataMode.DEMO) {
            textCacheStatus.setText(R.string.home_data_pulse_demo_body);
            return;
        }

        int cachedFeedCount = repository.getCachedFeedCount();
        long lastPrefetchEpoch = repository.getLastPrefetchEpochMillis();
        if (cachedFeedCount <= 0 || lastPrefetchEpoch <= 0L) {
            textCacheStatus.setText(R.string.settings_cache_empty);
            return;
        }

        CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                lastPrefetchEpoch,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
        );
        textCacheStatus.setText(getString(R.string.settings_cache_ready, cachedFeedCount, relativeTime));
    }
}
