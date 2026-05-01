package hk.edu.hku.cs7506.smartcompanion;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import hk.edu.hku.cs7506.smartcompanion.data.model.RecommendationItem;
import hk.edu.hku.cs7506.smartcompanion.data.repository.AppRepository;
import hk.edu.hku.cs7506.smartcompanion.ui.map.MapPreviewActivity;
import hk.edu.hku.cs7506.smartcompanion.ui.navigation.NavigationPreviewActivity;
import hk.edu.hku.cs7506.smartcompanion.util.FormatUtils;
import hk.edu.hku.cs7506.smartcompanion.util.IntentKeys;

public class PlaceDetailActivity extends AppCompatActivity {
    private RecommendationItem item;
    private AppRepository repository;
    private MaterialButton buttonFavorite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_detail);

        repository = AppRepository.getInstance(this);
        item = (RecommendationItem) getIntent().getSerializableExtra(IntentKeys.EXTRA_ITEM);
        if (item == null) {
            finish();
            return;
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        TextView textSceneBadge = findViewById(R.id.textSceneBadge);
        TextView textPlaceName = findViewById(R.id.textPlaceName);
        TextView textMetaLine = findViewById(R.id.textMetaLine);
        TextView textReason = findViewById(R.id.textReason);
        TextView textImageAttribution = findViewById(R.id.textImageAttribution);
        TextView textCoordinates = findViewById(R.id.textCoordinates);
        TextView textAddress = findViewById(R.id.textAddress);
        TextView textPhone = findViewById(R.id.textPhone);
        LinearLayout layoutMetrics = findViewById(R.id.layoutMetrics);
        TextView textTransportHeader = findViewById(R.id.textTransportHeader);
        MaterialCardView cardTransport = findViewById(R.id.cardTransport);
        TextView textTrafficNote = findViewById(R.id.textTrafficNote);
        TextView textTransitNote = findViewById(R.id.textTransitNote);
        ImageView imageHero = findViewById(R.id.imageHero);
        android.view.View layoutHero = findViewById(R.id.layoutHero);
        buttonFavorite = findViewById(R.id.buttonFavorite);
        MaterialButton buttonShare = findViewById(R.id.buttonShare);
        MaterialButton buttonCall = findViewById(R.id.buttonCall);
        MaterialButton buttonViewMap = findViewById(R.id.buttonViewMap);
        MaterialButton buttonNavigate = findViewById(R.id.buttonNavigate);
        MaterialButton buttonOpenSource = findViewById(R.id.buttonOpenSource);

        toolbar.setTitle(item.getName());
        toolbar.setNavigationOnClickListener(v -> finish());

        textSceneBadge.setText(FormatUtils.getDisplayTag(this, item));
        textPlaceName.setText(item.getName());
        bindOptionalText(textMetaLine, FormatUtils.getMetadataLine(item));
        textReason.setText(item.getReason());
        bindOptionalText(textImageAttribution, item.getImageAttribution());
        textCoordinates.setText(FormatUtils.formatCoordinate(this, item));
        bindOptionalText(textAddress, formatAddressLine(item.getAddressLine()));
        bindOptionalText(textPhone, formatPhoneLine(item.getContactPhone()));
        bindOptionalText(textTrafficNote, item.getTrafficNote());
        bindOptionalText(textTransitNote, item.getTransitNote());
        boolean hasTransportCard = !TextUtils.isEmpty(item.getTrafficNote()) || !TextUtils.isEmpty(item.getTransitNote());
        cardTransport.setVisibility(hasTransportCard ? android.view.View.VISIBLE : android.view.View.GONE);
        textTransportHeader.setVisibility(hasTransportCard ? android.view.View.VISIBLE : android.view.View.GONE);

        if (TextUtils.isEmpty(item.getImageUrl())) {
            layoutHero.setVisibility(android.view.View.GONE);
        } else {
            layoutHero.setVisibility(android.view.View.VISIBLE);
            Object imageModel = resolveImageModel(imageHero, item.getImageUrl());
            Glide.with(imageHero)
                    .load(imageModel)
                    .centerCrop()
                    .placeholder(R.drawable.bg_hero_panel)
                    .into(imageHero);
        }

        addMetric(layoutMetrics, getString(R.string.metric_total_score) + ": " + getString(R.string.common_points, item.getTotalScore()));
        addMetric(layoutMetrics, FormatUtils.formatMetricValue(this, getString(R.string.metric_eta), item.getEtaMinutes()));
        if (item.getWaitTimeMinutes() != null) {
            addMetric(layoutMetrics, FormatUtils.formatMetricValue(this, getString(R.string.metric_wait_time), item.getWaitTimeMinutes()));
        }
        if (item.getVacancy() != null) {
            addMetric(layoutMetrics, FormatUtils.formatPlainMetric(this, getString(R.string.metric_vacancy), item.getVacancy()));
        }
        if (item.getWalkDistanceMeters() != null) {
            addMetric(layoutMetrics, FormatUtils.formatDistanceMetric(this, getString(R.string.metric_walk_distance), item.getWalkDistanceMeters()));
        }
        if (item.getWalkMinutes() != null) {
            addMetric(layoutMetrics, FormatUtils.formatMetricValue(this, getString(R.string.metric_walk_time), item.getWalkMinutes()));
        }
        if (item.getInVehicleMinutes() != null) {
            addMetric(layoutMetrics, FormatUtils.formatMetricValue(this, getString(R.string.metric_ride_time), item.getInVehicleMinutes()));
        }
        if (item.getWeatherScore() != null) {
            addMetric(layoutMetrics, FormatUtils.formatScoreMetric(this, getString(R.string.metric_weather), item.getWeatherScore()));
        }
        if (item.getAqhiScore() != null) {
            addMetric(layoutMetrics, FormatUtils.formatScoreMetric(this, getString(R.string.metric_aqhi), item.getAqhiScore()));
        }

        repository.recordRecentView(item);
        refreshFavoriteButton();

        buttonFavorite.setOnClickListener(v -> {
            boolean added = repository.toggleFavorite(item);
            refreshFavoriteButton();
            Toast.makeText(this, added ? R.string.detail_favorite_added : R.string.detail_favorite_removed, Toast.LENGTH_SHORT).show();
        });
        buttonShare.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, item.getName());
            shareIntent.putExtra(Intent.EXTRA_TEXT, buildShareMessage());
            startActivity(Intent.createChooser(shareIntent, getString(R.string.action_share_place)));
        });
        if (TextUtils.isEmpty(item.getContactPhone())) {
            buttonCall.setVisibility(android.view.View.GONE);
        } else {
            buttonCall.setVisibility(android.view.View.VISIBLE);
            buttonCall.setOnClickListener(v -> startActivity(
                    new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + item.getContactPhone()))
            ));
        }
        buttonViewMap.setOnClickListener(v -> openTarget(MapPreviewActivity.class));
        buttonNavigate.setOnClickListener(v -> openTarget(NavigationPreviewActivity.class));
        if (TextUtils.isEmpty(item.getDetailsUrl())) {
            buttonOpenSource.setVisibility(android.view.View.GONE);
        } else {
            buttonOpenSource.setVisibility(android.view.View.VISIBLE);
            buttonOpenSource.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(item.getDetailsUrl()));
                startActivity(intent);
            });
        }
    }

    private Object resolveImageModel(ImageView target, String imageUrl) {
        if (imageUrl != null && imageUrl.startsWith("drawable://")) {
            String resourceName = imageUrl.substring("drawable://".length());
            int resourceId = target.getResources().getIdentifier(
                    resourceName,
                    "drawable",
                    target.getContext().getPackageName()
            );
            if (resourceId != 0) {
                return resourceId;
            }
        }
        return imageUrl;
    }

    private void refreshFavoriteButton() {
        boolean favorite = repository.isFavorite(item);
        buttonFavorite.setText(favorite ? R.string.action_remove_favorite : R.string.action_add_favorite);
    }

    private void addMetric(LinearLayout parent, String text) {
        TextView textView = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = (int) (8 * getResources().getDisplayMetrics().density);
        textView.setLayoutParams(params);
        textView.setBackgroundResource(R.drawable.bg_metric_chip);
        int padding = (int) (14 * getResources().getDisplayMetrics().density);
        textView.setPadding(padding, padding, padding, padding);
        textView.setText(text);
        parent.addView(textView);
    }

    private void openTarget(Class<?> targetClass) {
        Intent intent = new Intent(this, targetClass);
        intent.putExtra(IntentKeys.EXTRA_ITEM, item);
        startActivity(intent);
    }

    private void bindOptionalText(TextView textView, String value) {
        if (TextUtils.isEmpty(value)) {
            textView.setVisibility(android.view.View.GONE);
            textView.setText("");
            return;
        }
        textView.setVisibility(android.view.View.VISIBLE);
        textView.setText(value);
    }

    private String buildShareMessage() {
        StringBuilder builder = new StringBuilder();
        builder.append(item.getName());
        String metadataLine = FormatUtils.getMetadataLine(item);
        if (!TextUtils.isEmpty(metadataLine)) {
            builder.append("\n").append(metadataLine);
        }
        builder.append("\n").append(item.getReason());
        if (!TextUtils.isEmpty(item.getAddressLine())) {
            builder.append("\n").append(item.getAddressLine());
        }
        builder.append("\n").append(FormatUtils.formatCoordinate(this, item));
        if (!TextUtils.isEmpty(item.getDetailsUrl())) {
            builder.append("\n").append(item.getDetailsUrl());
        }
        return builder.toString();
    }

    private String formatPhoneLine(String contactPhone) {
        if (TextUtils.isEmpty(contactPhone)) {
            return "";
        }
        return getString(R.string.detail_contact_phone, contactPhone);
    }

    private String formatAddressLine(String addressLine) {
        if (TextUtils.isEmpty(addressLine)) {
            return "";
        }
        return getString(R.string.detail_address_line, addressLine);
    }
}
