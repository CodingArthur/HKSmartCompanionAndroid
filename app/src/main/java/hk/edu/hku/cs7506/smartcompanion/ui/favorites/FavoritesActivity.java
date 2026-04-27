package hk.edu.hku.cs7506.smartcompanion.ui.favorites;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.List;

import hk.edu.hku.cs7506.smartcompanion.PlaceDetailActivity;
import hk.edu.hku.cs7506.smartcompanion.R;
import hk.edu.hku.cs7506.smartcompanion.data.model.RecommendationItem;
import hk.edu.hku.cs7506.smartcompanion.data.repository.AppRepository;
import hk.edu.hku.cs7506.smartcompanion.ui.adapter.FavoriteAdapter;
import hk.edu.hku.cs7506.smartcompanion.util.IntentKeys;

public class FavoritesActivity extends AppCompatActivity {
    private FavoriteAdapter adapter;
    private LinearLayout emptyStateLayout;
    private AppRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        repository = AppRepository.getInstance(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        RecyclerView recyclerView = findViewById(R.id.recyclerFavorites);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);

        toolbar.setNavigationOnClickListener(v -> finish());

        adapter = new FavoriteAdapter(item -> {
            Intent intent = new Intent(this, PlaceDetailActivity.class);
            intent.putExtra(IntentKeys.EXTRA_ITEM, item);
            startActivity(intent);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        List<RecommendationItem> items = repository.getFavorites();
        adapter.submitList(items);
        emptyStateLayout.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }
}

