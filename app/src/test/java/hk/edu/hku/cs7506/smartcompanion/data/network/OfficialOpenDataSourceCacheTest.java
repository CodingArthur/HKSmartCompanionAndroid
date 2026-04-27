package hk.edu.hku.cs7506.smartcompanion.data.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class OfficialOpenDataSourceCacheTest {
    @Test
    public void returnsFreshCacheWithoutCallingNetwork() throws Exception {
        InMemoryFeedCacheStore cacheStore = new InMemoryFeedCacheStore();
        cacheStore.put("weather", new FeedCacheStore.Entry("{\"cached\":true}", 0L, System.currentTimeMillis()));

        FakeHttpClient httpClient = new FakeHttpClient();
        OfficialOpenDataSource dataSource = new OfficialOpenDataSource(httpClient, cacheStore);

        assertEquals("{\"cached\":true}", dataSource.fetchWeatherFeed());
        assertEquals(0, httpClient.getResponseCallCount);
    }

    @Test
    public void fallsBackToCachedResponseWhenNetworkFails() throws Exception {
        InMemoryFeedCacheStore cacheStore = new InMemoryFeedCacheStore();
        cacheStore.put("traffic_news", new FeedCacheStore.Entry("<xml>cached</xml>", 0L, 1L));

        FakeHttpClient httpClient = new FakeHttpClient();
        httpClient.throwOnGetResponse = true;
        OfficialOpenDataSource dataSource = new OfficialOpenDataSource(httpClient, cacheStore);

        assertEquals("<xml>cached</xml>", dataSource.fetchTrafficNewsFeed());
        assertTrue(httpClient.getResponseCallCount > 0);
    }

    private static class FakeHttpClient extends OpenDataHttpClient {
        int getResponseCallCount;
        boolean throwOnGetResponse;

        @Override
        public ResponseData getResponse(String url) {
            getResponseCallCount++;
            if (throwOnGetResponse) {
                throw new IllegalStateException("boom");
            }
            return new ResponseData("{\"live\":true}", 0L, System.currentTimeMillis());
        }
    }

    private static class InMemoryFeedCacheStore implements FeedCacheStore {
        private final Map<String, Entry> entries = new HashMap<>();
        private long lastPrefetchEpochMillis;

        @Override
        public Entry get(String key) {
            return entries.get(key);
        }

        @Override
        public void put(String key, Entry entry) {
            entries.put(key, entry);
        }

        @Override
        public void markPrefetchFinished(long epochMillis) {
            lastPrefetchEpochMillis = epochMillis;
        }

        @Override
        public long getLastPrefetchEpochMillis() {
            return lastPrefetchEpochMillis;
        }

        @Override
        public int getCachedFeedCount() {
            return entries.size();
        }
    }
}
