package hk.edu.hku.cs7506.smartcompanion.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import hk.edu.hku.cs7506.smartcompanion.data.model.RecommendationItem;

public class FavoriteStore {
    private static final String PREFS_NAME = "smart_companion_favorites";
    private static final String KEY_FAVORITES = "favorite_items";

    private final SharedPreferences sharedPreferences;
    private final Gson gson = new Gson();
    private final Type listType = new TypeToken<List<RecommendationItem>>() { }.getType();

    public FavoriteStore(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public List<RecommendationItem> getAll() {
        String json = sharedPreferences.getString(KEY_FAVORITES, "[]");
        List<RecommendationItem> items = gson.fromJson(json, listType);
        return items == null ? new ArrayList<>() : items;
    }

    public boolean isFavorite(RecommendationItem item) {
        for (RecommendationItem favorite : getAll()) {
            if (favorite.getStableId().equals(item.getStableId())) {
                return true;
            }
        }
        return false;
    }

    public boolean toggle(RecommendationItem item) {
        List<RecommendationItem> items = getAll();
        Iterator<RecommendationItem> iterator = items.iterator();
        while (iterator.hasNext()) {
            RecommendationItem favorite = iterator.next();
            if (favorite.getStableId().equals(item.getStableId())) {
                iterator.remove();
                persist(items);
                return false;
            }
        }
        items.add(0, item);
        persist(items);
        return true;
    }

    private void persist(List<RecommendationItem> items) {
        sharedPreferences.edit().putString(KEY_FAVORITES, gson.toJson(items)).apply();
    }
}

