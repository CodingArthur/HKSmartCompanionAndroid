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

public class RecentStore {
    private static final String PREFS_NAME = "smart_companion_recent";
    private static final String KEY_RECENT = "recent_items";
    private static final int MAX_RECENT_ITEMS = 5;

    private final SharedPreferences sharedPreferences;
    private final Gson gson = new Gson();
    private final Type listType = new TypeToken<List<RecommendationItem>>() { }.getType();

    public RecentStore(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public List<RecommendationItem> getAll() {
        String json = sharedPreferences.getString(KEY_RECENT, "[]");
        List<RecommendationItem> items = gson.fromJson(json, listType);
        return items == null ? new ArrayList<>() : items;
    }

    public RecommendationItem getMostRecent() {
        List<RecommendationItem> items = getAll();
        return items.isEmpty() ? null : items.get(0);
    }

    public void recordView(RecommendationItem item) {
        List<RecommendationItem> items = getAll();
        Iterator<RecommendationItem> iterator = items.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getStableId().equals(item.getStableId())) {
                iterator.remove();
                break;
            }
        }
        items.add(0, item);
        while (items.size() > MAX_RECENT_ITEMS) {
            items.remove(items.size() - 1);
        }
        persist(items);
    }

    private void persist(List<RecommendationItem> items) {
        sharedPreferences.edit().putString(KEY_RECENT, gson.toJson(items)).apply();
    }
}
