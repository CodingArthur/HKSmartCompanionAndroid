package hk.edu.hku.cs7506.smartcompanion.data.network;

import android.os.Handler;
import android.os.Looper;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import hk.edu.hku.cs7506.smartcompanion.data.model.RecommendationItem;
import hk.edu.hku.cs7506.smartcompanion.data.model.RecommendationRequest;
import hk.edu.hku.cs7506.smartcompanion.data.model.SceneType;
import hk.edu.hku.cs7506.smartcompanion.data.repository.RepositoryCallback;

public class OfficialDataClient {
    private final ExecutorService executorService;
    private final ExecutorService networkExecutorService;
    private final Executor callbackExecutor;
    private final OfficialOpenDataSource dataSource;
    private final OfficialDataParser parser;
    private final RecommendationScorer scorer;
    private final TransportIntelligenceClient transportIntelligenceClient;
    private final CommuteAssistantClient commuteAssistantClient;

    public OfficialDataClient() {
        this(
                Executors.newSingleThreadExecutor(),
                createCallbackExecutor(),
                new OfficialOpenDataSource(),
                new OfficialDataParser(),
                new RecommendationScorer(),
                new TransportIntelligenceClient(),
                new CommuteAssistantClient()
        );
    }

    public OfficialDataClient(OfficialOpenDataSource dataSource) {
        this(
                Executors.newSingleThreadExecutor(),
                createCallbackExecutor(),
                dataSource,
                new OfficialDataParser(),
                new RecommendationScorer(),
                new TransportIntelligenceClient(),
                new CommuteAssistantClient()
        );
    }

    OfficialDataClient(
            ExecutorService executorService,
            Executor callbackExecutor,
            OfficialOpenDataSource dataSource,
            OfficialDataParser parser,
            RecommendationScorer scorer,
            TransportIntelligenceClient transportIntelligenceClient,
            CommuteAssistantClient commuteAssistantClient
    ) {
        this.executorService = executorService;
        this.networkExecutorService = Executors.newFixedThreadPool(4);
        this.callbackExecutor = callbackExecutor;
        this.dataSource = dataSource;
        this.parser = parser;
        this.scorer = scorer;
        this.transportIntelligenceClient = transportIntelligenceClient;
        this.commuteAssistantClient = commuteAssistantClient;
    }

    public void fetchRecommendations(RecommendationRequest request, RepositoryCallback<List<RecommendationItem>> callback) {
        executorService.execute(() -> {
            try {
                List<RecommendationItem> items;
                Future<String> trafficNewsFuture = submitFetch(dataSource::fetchTrafficNewsFeed);
                if (request.getSceneType() == SceneType.EMERGENCY) {
                    Future<String> emergencyFuture = submitFetch(dataSource::fetchEmergencyWaitFeed);
                    List<RecommendationItem> baseItems = parser.parseEmergencyRecommendations(await(emergencyFuture), request);
                    List<RecommendationItem> rankedItems = scorer.rank(SceneType.EMERGENCY, request, baseItems);
                    items = scorer.rank(
                            SceneType.EMERGENCY,
                            request,
                            transportIntelligenceClient.enrichTopRecommendations(
                                    SceneType.EMERGENCY,
                                    request,
                                    rankedItems,
                                    awaitOptional(trafficNewsFuture)
                            )
                    );
                } else if (request.getSceneType() == SceneType.PARKING) {
                    Future<String> infoFuture = submitFetch(dataSource::fetchCarparkInfoFeed);
                    Future<String> vacancyFuture = submitFetch(dataSource::fetchCarparkVacancyFeed);
                    List<RecommendationItem> baseItems = parser.parseParkingRecommendations(
                            await(infoFuture),
                            await(vacancyFuture),
                            request
                    );
                    List<RecommendationItem> rankedItems = scorer.rank(SceneType.PARKING, request, baseItems);
                    items = scorer.rank(
                            SceneType.PARKING,
                            request,
                            transportIntelligenceClient.enrichTopRecommendations(
                                    SceneType.PARKING,
                                    request,
                                    rankedItems,
                                    awaitOptional(trafficNewsFuture)
                            )
                    );
                } else if (request.getSceneType() == SceneType.PLAY) {
                    Future<OpenDataHttpClient.ResponseData> eventFuture = submitFetch(dataSource::fetchEventFeedResponse);
                    Future<String> museumsFuture = submitFetch(dataSource::fetchMuseumsFeed);
                    Future<String> weatherFuture = submitFetch(dataSource::fetchWeatherFeed);
                    Future<String> aqhiFuture = submitFetch(dataSource::fetchAqhiFeed);
                    OpenDataHttpClient.ResponseData eventFeed = await(eventFuture);
                    List<RecommendationItem> baseItems = parser.parsePlayRecommendations(
                            eventFeed.getBody(),
                            eventFeed.getLastModifiedEpochMillis(),
                            await(museumsFuture),
                            await(weatherFuture),
                            await(aqhiFuture),
                            request
                    );
                    List<RecommendationItem> rankedItems = scorer.rank(SceneType.PLAY, request, baseItems);
                    items = scorer.rank(
                            SceneType.PLAY,
                            request,
                            transportIntelligenceClient.enrichTopRecommendations(
                                    SceneType.PLAY,
                                    request,
                                    rankedItems,
                                    awaitOptional(trafficNewsFuture)
                            )
                    );
                } else {
                    List<RecommendationItem> rankedItems = scorer.rank(
                            SceneType.COMMUTE,
                            request,
                            commuteAssistantClient.fetchRecommendations(request)
                    );
                    items = scorer.rank(
                            SceneType.COMMUTE,
                            request,
                            transportIntelligenceClient.enrichTopRecommendations(
                                    SceneType.COMMUTE,
                                    request,
                                    rankedItems,
                                    awaitOptional(trafficNewsFuture)
                            )
                    );
                }
                callbackExecutor.execute(() -> callback.onSuccess(items));
            } catch (Exception error) {
                String message = error.getMessage() == null ? "Unable to load official data." : error.getMessage();
                callbackExecutor.execute(() -> callback.onError(message));
            }
        });
    }

    public void prefetchFeeds(RepositoryCallback<Integer> callback) {
        executorService.execute(() -> {
            int warmedFeedCount = dataSource.prefetchAllFeeds();
            callbackExecutor.execute(() -> callback.onSuccess(warmedFeedCount));
        });
    }

    public long getLastPrefetchEpochMillis() {
        return dataSource.getLastPrefetchEpochMillis();
    }

    public int getCachedFeedCount() {
        return dataSource.getCachedFeedCount();
    }

    private <T> Future<T> submitFetch(FetchTask<T> task) {
        return networkExecutorService.submit(task::run);
    }

    private <T> T await(Future<T> future) throws Exception {
        try {
            return future.get();
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw error;
        } catch (ExecutionException error) {
            Throwable cause = error.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw new IllegalStateException(cause);
        }
    }

    private <T> T awaitOptional(Future<T> future) {
        try {
            return await(future);
        } catch (Exception ignored) {
            return null;
        }
    }

    List<RecommendationItem> parseEmergencyRecommendations(String rawJson, RecommendationRequest request) {
        return parser.parseEmergencyRecommendations(rawJson, request);
    }

    List<RecommendationItem> parseParkingRecommendations(String infoJson, String vacancyJson, RecommendationRequest request) {
        return parser.parseParkingRecommendations(infoJson, vacancyJson, request);
    }

    List<RecommendationItem> parseEventRecommendations(String csvText, String weatherJson, String aqhiJson, RecommendationRequest request) {
        return parser.parseEventRecommendations(csvText, weatherJson, aqhiJson, request);
    }

    List<RecommendationItem> parsePlayRecommendations(
            String eventCsvText,
            long eventLastModifiedEpochMillis,
            String museumsJson,
            String weatherJson,
            String aqhiJson,
            RecommendationRequest request
    ) {
        return parser.parsePlayRecommendations(
                eventCsvText,
                eventLastModifiedEpochMillis,
                museumsJson,
                weatherJson,
                aqhiJson,
                request
        );
    }

    private static Executor createCallbackExecutor() {
        try {
            Handler handler = new Handler(Looper.getMainLooper());
            return handler::post;
        } catch (RuntimeException ignored) {
            return Runnable::run;
        }
    }

    @FunctionalInterface
    private interface FetchTask<T> {
        T run() throws Exception;
    }
}
