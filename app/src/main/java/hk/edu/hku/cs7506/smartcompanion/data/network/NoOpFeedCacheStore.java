package hk.edu.hku.cs7506.smartcompanion.data.network;

public class NoOpFeedCacheStore implements FeedCacheStore {
    @Override
    public Entry get(String key) {
        return null;
    }

    @Override
    public void put(String key, Entry entry) {
    }

    @Override
    public void markPrefetchFinished(long epochMillis) {
    }

    @Override
    public long getLastPrefetchEpochMillis() {
        return 0L;
    }

    @Override
    public int getCachedFeedCount() {
        return 0;
    }
}
