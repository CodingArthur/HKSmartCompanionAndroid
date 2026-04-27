package hk.edu.hku.cs7506.smartcompanion.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import hk.edu.hku.cs7506.smartcompanion.data.network.FeedCacheStore;

public class FileFeedCacheStore implements FeedCacheStore {
    private static final String PREFS_NAME = "smart_companion_feed_cache";
    private static final String KEY_LAST_PREFETCH = "last_prefetch_epoch";
    private static final String CACHE_DIRECTORY = "official-feed-cache";

    private final File cacheDirectory;
    private final SharedPreferences sharedPreferences;
    private final Gson gson = new Gson();

    public FileFeedCacheStore(Context context) {
        cacheDirectory = new File(context.getCacheDir(), CACHE_DIRECTORY);
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (!cacheDirectory.exists()) {
            //noinspection ResultOfMethodCallIgnored
            cacheDirectory.mkdirs();
        }
    }

    @Override
    public synchronized Entry get(String key) {
        File file = resolveFile(key);
        if (!file.exists()) {
            return null;
        }
        try (FileInputStream inputStream = new FileInputStream(file)) {
            byte[] bytes = new byte[(int) file.length()];
            int read = inputStream.read(bytes);
            if (read <= 0) {
                return null;
            }
            String rawJson = new String(bytes, 0, read, StandardCharsets.UTF_8);
            StoredEntry storedEntry = gson.fromJson(rawJson, StoredEntry.class);
            if (storedEntry == null || storedEntry.body == null) {
                return null;
            }
            return new Entry(
                    storedEntry.body,
                    storedEntry.lastModifiedEpochMillis,
                    storedEntry.fetchedAtEpochMillis
            );
        } catch (IOException ignored) {
            return null;
        }
    }

    @Override
    public synchronized void put(String key, Entry entry) {
        File file = resolveFile(key);
        StoredEntry storedEntry = new StoredEntry(
                entry.getBody(),
                entry.getLastModifiedEpochMillis(),
                entry.getFetchedAtEpochMillis()
        );
        byte[] bytes = gson.toJson(storedEntry).getBytes(StandardCharsets.UTF_8);
        try (FileOutputStream outputStream = new FileOutputStream(file, false)) {
            outputStream.write(bytes);
        } catch (IOException ignored) {
        }
    }

    @Override
    public void markPrefetchFinished(long epochMillis) {
        sharedPreferences.edit().putLong(KEY_LAST_PREFETCH, epochMillis).apply();
    }

    @Override
    public long getLastPrefetchEpochMillis() {
        return sharedPreferences.getLong(KEY_LAST_PREFETCH, 0L);
    }

    @Override
    public int getCachedFeedCount() {
        String[] files = cacheDirectory.list();
        return files == null ? 0 : files.length;
    }

    private File resolveFile(String key) {
        String safeKey = key.replaceAll("[^a-zA-Z0-9._-]", "_");
        return new File(cacheDirectory, safeKey + ".json");
    }

    private static class StoredEntry {
        final String body;
        final long lastModifiedEpochMillis;
        final long fetchedAtEpochMillis;

        StoredEntry(String body, long lastModifiedEpochMillis, long fetchedAtEpochMillis) {
            this.body = body;
            this.lastModifiedEpochMillis = lastModifiedEpochMillis;
            this.fetchedAtEpochMillis = fetchedAtEpochMillis;
        }
    }
}
