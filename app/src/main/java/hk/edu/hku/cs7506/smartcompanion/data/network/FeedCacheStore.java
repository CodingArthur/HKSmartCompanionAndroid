package hk.edu.hku.cs7506.smartcompanion.data.network;

public interface FeedCacheStore {
    Entry get(String key);

    void put(String key, Entry entry);

    void markPrefetchFinished(long epochMillis);

    long getLastPrefetchEpochMillis();

    int getCachedFeedCount();

    final class Entry {
        private final String body;
        private final long lastModifiedEpochMillis;
        private final long fetchedAtEpochMillis;

        public Entry(String body, long lastModifiedEpochMillis, long fetchedAtEpochMillis) {
            this.body = body;
            this.lastModifiedEpochMillis = lastModifiedEpochMillis;
            this.fetchedAtEpochMillis = fetchedAtEpochMillis;
        }

        public String getBody() {
            return body;
        }

        public long getLastModifiedEpochMillis() {
            return lastModifiedEpochMillis;
        }

        public long getFetchedAtEpochMillis() {
            return fetchedAtEpochMillis;
        }

        OpenDataHttpClient.ResponseData toResponseData() {
            return new OpenDataHttpClient.ResponseData(body, lastModifiedEpochMillis, fetchedAtEpochMillis);
        }
    }
}
