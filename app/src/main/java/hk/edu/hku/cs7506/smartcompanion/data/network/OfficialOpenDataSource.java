package hk.edu.hku.cs7506.smartcompanion.data.network;

public class OfficialOpenDataSource {
    private static final String CACHE_KEY_EMERGENCY = "emergency";
    private static final String CACHE_KEY_CARPARK_INFO = "carpark_info";
    private static final String CACHE_KEY_CARPARK_VACANCY = "carpark_vacancy";
    private static final String CACHE_KEY_EVENTS = "events";
    private static final String CACHE_KEY_MUSEUMS = "museums";
    private static final String CACHE_KEY_WEATHER = "weather";
    private static final String CACHE_KEY_AQHI = "aqhi";
    private static final String CACHE_KEY_TRAFFIC = "traffic_news";

    private static final long FRESHNESS_EMERGENCY_MILLIS = 5 * 60 * 1000L;
    private static final long FRESHNESS_CARPARK_INFO_MILLIS = 24 * 60 * 60 * 1000L;
    private static final long FRESHNESS_CARPARK_VACANCY_MILLIS = 2 * 60 * 1000L;
    private static final long FRESHNESS_EVENTS_MILLIS = 12 * 60 * 60 * 1000L;
    private static final long FRESHNESS_MUSEUMS_MILLIS = 3 * 24 * 60 * 60 * 1000L;
    private static final long FRESHNESS_WEATHER_MILLIS = 10 * 60 * 1000L;
    private static final long FRESHNESS_AQHI_MILLIS = 10 * 60 * 1000L;
    private static final long FRESHNESS_TRAFFIC_MILLIS = 5 * 60 * 1000L;

    static final String AE_WAIT_URL = "https://www.ha.org.hk/opendata/aed/aedwtdata2-en.json";
    static final String CARPARK_INFO_URL = "https://api.data.gov.hk/v1/carpark-info-vacancy?lang=en_US";
    static final String CARPARK_VACANCY_URL = "https://api.data.gov.hk/v1/carpark-info-vacancy?data=vacancy&vehicleTypes=privateCar&lang=en_US";
    static final String EVENTS_URL = "https://www.tourism.gov.hk/datagovhk/hktb_events/hktb_events_en.csv";
    static final String MUSEUMS_URL = "https://portal.csdi.gov.hk/server/rest/services/common/lcsd_rcd_1629267205214_78787/FeatureServer/0/query?f=json&where=1%3D1&outFields=*&returnGeometry=true&outSR=4326";
    static final String WEATHER_URL = "https://data.weather.gov.hk/weatherAPI/opendata/weather.php?dataType=rhrread&lang=en";
    static final String AQHI_URL = "https://dashboard.data.gov.hk/api/aqhi-individual?format=json";
    static final String TRAFFIC_NEWS_URL = "https://www.td.gov.hk/en/special_news/trafficnews.xml";

    private final OpenDataHttpClient httpClient;
    private final FeedCacheStore feedCacheStore;

    public OfficialOpenDataSource() {
        this(new OpenDataHttpClient(), new NoOpFeedCacheStore());
    }

    OfficialOpenDataSource(OpenDataHttpClient httpClient) {
        this(httpClient, new NoOpFeedCacheStore());
    }

    public OfficialOpenDataSource(OpenDataHttpClient httpClient, FeedCacheStore feedCacheStore) {
        this.httpClient = httpClient;
        this.feedCacheStore = feedCacheStore;
    }

    public String fetchEmergencyWaitFeed() throws Exception {
        return fetchCachedResponse(CACHE_KEY_EMERGENCY, AE_WAIT_URL, FRESHNESS_EMERGENCY_MILLIS).getBody();
    }

    public String fetchCarparkInfoFeed() throws Exception {
        return fetchCachedResponse(CACHE_KEY_CARPARK_INFO, CARPARK_INFO_URL, FRESHNESS_CARPARK_INFO_MILLIS).getBody();
    }

    public String fetchCarparkVacancyFeed() throws Exception {
        return fetchCachedResponse(CACHE_KEY_CARPARK_VACANCY, CARPARK_VACANCY_URL, FRESHNESS_CARPARK_VACANCY_MILLIS).getBody();
    }

    public String fetchEventFeed() throws Exception {
        return fetchEventFeedResponse().getBody();
    }

    public OpenDataHttpClient.ResponseData fetchEventFeedResponse() throws Exception {
        return fetchCachedResponse(CACHE_KEY_EVENTS, EVENTS_URL, FRESHNESS_EVENTS_MILLIS);
    }

    public String fetchMuseumsFeed() throws Exception {
        return fetchCachedResponse(CACHE_KEY_MUSEUMS, MUSEUMS_URL, FRESHNESS_MUSEUMS_MILLIS).getBody();
    }

    public String fetchWeatherFeed() throws Exception {
        return fetchCachedResponse(CACHE_KEY_WEATHER, WEATHER_URL, FRESHNESS_WEATHER_MILLIS).getBody();
    }

    public String fetchAqhiFeed() throws Exception {
        return fetchCachedResponse(CACHE_KEY_AQHI, AQHI_URL, FRESHNESS_AQHI_MILLIS).getBody();
    }

    public String fetchTrafficNewsFeed() throws Exception {
        return fetchCachedResponse(CACHE_KEY_TRAFFIC, TRAFFIC_NEWS_URL, FRESHNESS_TRAFFIC_MILLIS).getBody();
    }

    public int prefetchAllFeeds() {
        int successCount = 0;
        successCount += prefetchQuietly(this::fetchEmergencyWaitFeed);
        successCount += prefetchQuietly(this::fetchCarparkInfoFeed);
        successCount += prefetchQuietly(this::fetchCarparkVacancyFeed);
        successCount += prefetchQuietly(this::fetchEventFeed);
        successCount += prefetchQuietly(this::fetchMuseumsFeed);
        successCount += prefetchQuietly(this::fetchWeatherFeed);
        successCount += prefetchQuietly(this::fetchAqhiFeed);
        successCount += prefetchQuietly(this::fetchTrafficNewsFeed);
        if (successCount > 0) {
            feedCacheStore.markPrefetchFinished(System.currentTimeMillis());
        }
        return successCount;
    }

    public long getLastPrefetchEpochMillis() {
        return feedCacheStore.getLastPrefetchEpochMillis();
    }

    public int getCachedFeedCount() {
        return feedCacheStore.getCachedFeedCount();
    }

    private OpenDataHttpClient.ResponseData fetchCachedResponse(
            String cacheKey,
            String url,
            long freshnessMillis
    ) throws Exception {
        FeedCacheStore.Entry cachedEntry = feedCacheStore.get(cacheKey);
        long now = System.currentTimeMillis();
        if (cachedEntry != null && freshnessMillis > 0
                && (now - cachedEntry.getFetchedAtEpochMillis()) <= freshnessMillis) {
            return cachedEntry.toResponseData();
        }

        try {
            OpenDataHttpClient.ResponseData responseData = httpClient.getResponse(url);
            feedCacheStore.put(cacheKey, new FeedCacheStore.Entry(
                    responseData.getBody(),
                    responseData.getLastModifiedEpochMillis(),
                    responseData.getFetchedAtEpochMillis()
            ));
            return responseData;
        } catch (Exception error) {
            if (cachedEntry != null) {
                return cachedEntry.toResponseData();
            }
            throw error;
        }
    }

    private int prefetchQuietly(FetchOperation fetchOperation) {
        try {
            fetchOperation.run();
            return 1;
        } catch (Exception ignored) {
            return 0;
        }
    }

    @FunctionalInterface
    private interface FetchOperation {
        void run() throws Exception;
    }
}
