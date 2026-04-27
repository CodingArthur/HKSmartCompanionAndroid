package hk.edu.hku.cs7506.smartcompanion.data.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import hk.edu.hku.cs7506.smartcompanion.data.model.CommuteCorridor;
import hk.edu.hku.cs7506.smartcompanion.data.model.RecommendationItem;
import hk.edu.hku.cs7506.smartcompanion.data.model.RecommendationRequest;
import hk.edu.hku.cs7506.smartcompanion.data.model.SceneType;

public class CommuteAssistantClient {
    private static final ZoneId HONG_KONG_ZONE = ZoneId.of("Asia/Hong_Kong");
    private static final String CTB_ETA_TEMPLATE = "https://rt.data.gov.hk/v2/transport/citybus/eta/CTB/%s/%s";
    private static final String CTB_ROUTE_STOP_TEMPLATE = "https://rt.data.gov.hk/v2/transport/citybus/route-stop/ctb/%s/%s";
    private static final String CTB_STOP_TEMPLATE = "https://rt.data.gov.hk/v2/transport/citybus/stop/%s";

    private final OpenDataHttpClient httpClient;
    private final Map<String, StopInfo> stopInfoCache = new HashMap<>();
    private final Map<String, String> firstStopCache = new HashMap<>();
    private final Map<CommuteCorridor, List<ServiceTemplate>> corridorCatalog = createCatalog();

    public CommuteAssistantClient() {
        this(new OpenDataHttpClient(8000, 8000));
    }

    CommuteAssistantClient(OpenDataHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public List<RecommendationItem> fetchRecommendations(RecommendationRequest request) {
        CommuteCorridor corridor = request.getCommuteCorridor() == null
                ? CommuteCorridor.CENTRAL_TO_SHA_TIN
                : request.getCommuteCorridor();
        List<ServiceTemplate> templates = corridorCatalog.get(corridor);
        if (templates == null || templates.isEmpty()) {
            return Collections.emptyList();
        }

        List<RecommendationItem> items = new ArrayList<>();
        for (ServiceTemplate template : templates) {
            RecommendationItem item = buildItem(corridor, template, request);
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }

    private RecommendationItem buildItem(
            CommuteCorridor corridor,
            ServiceTemplate template,
            RecommendationRequest request
    ) {
        try {
            String boardingStopId = resolveFirstStopId(template.routeNumber, template.directionPath);
            if (boardingStopId == null || boardingStopId.isEmpty()) {
                return null;
            }
            StopInfo stopInfo = fetchStopInfo(boardingStopId);
            Integer nextDepartureMinutes = fetchNextDepartureMinutes(boardingStopId, template.routeNumber, template.directionCode);
            if (stopInfo == null || nextDepartureMinutes == null) {
                return null;
            }

            int walkDistanceMeters = estimateDistanceMeters(
                    request.getUserLatitude(),
                    request.getUserLongitude(),
                    stopInfo.latitude,
                    stopInfo.longitude
            );
            int walkMinutes = Math.max(1, (int) Math.round(walkDistanceMeters / 78.0));
            int rideMinutes = estimateRideMinutes(
                    stopInfo.latitude,
                    stopInfo.longitude,
                    corridor.getDestinationLatitude(),
                    corridor.getDestinationLongitude()
            );

            return new RecommendationItem(
                    template.routeNumber.toLowerCase(Locale.US) + "-" + template.directionCode,
                    SceneType.COMMUTE,
                    "Citybus " + template.routeNumber,
                    "",
                    corridor.getDestinationLatitude(),
                    corridor.getDestinationLongitude(),
                    0.0,
                    nextDepartureMinutes,
                    null,
                    null,
                    walkDistanceMeters,
                    walkMinutes,
                    rideMinutes,
                    null,
                    null,
                    "Commute line",
                    "Citybus route " + template.routeNumber + " · " + template.originLabel + " → " + template.destinationLabel,
                    null,
                    null,
                    null,
                    template.destinationAddress,
                    null,
                    null,
                    "Board at " + stopInfo.name + ". Live operator ETA is pulled from the official Citybus feed."
            );
        } catch (Exception ignored) {
            return null;
        }
    }

    private String resolveFirstStopId(String routeNumber, String directionPath) throws Exception {
        String cacheKey = routeNumber + ":" + directionPath;
        String cached = firstStopCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        String response = httpClient.get(String.format(Locale.US, CTB_ROUTE_STOP_TEMPLATE, routeNumber, directionPath));
        JsonArray data = JsonParser.parseString(response).getAsJsonObject().getAsJsonArray("data");
        if (data == null || data.isEmpty()) {
            return null;
        }
        JsonObject first = data.get(0).getAsJsonObject();
        String stopId = getString(first, "stop");
        firstStopCache.put(cacheKey, stopId);
        return stopId;
    }

    private StopInfo fetchStopInfo(String stopId) throws Exception {
        StopInfo cached = stopInfoCache.get(stopId);
        if (cached != null) {
            return cached;
        }
        String response = httpClient.get(String.format(Locale.US, CTB_STOP_TEMPLATE, stopId));
        JsonObject data = JsonParser.parseString(response).getAsJsonObject().getAsJsonObject("data");
        if (data == null) {
            return null;
        }
        StopInfo stopInfo = new StopInfo(
                stopId,
                getString(data, "name_en"),
                getDouble(data, "lat"),
                getDouble(data, "long")
        );
        stopInfoCache.put(stopId, stopInfo);
        return stopInfo;
    }

    private Integer fetchNextDepartureMinutes(String stopId, String routeNumber, String directionCode) throws Exception {
        String response = httpClient.get(String.format(Locale.US, CTB_ETA_TEMPLATE, stopId, routeNumber));
        JsonArray data = JsonParser.parseString(response).getAsJsonObject().getAsJsonArray("data");
        if (data == null || data.isEmpty()) {
            return null;
        }
        Integer best = null;
        for (JsonElement element : data) {
            JsonObject item = element.getAsJsonObject();
            if (!directionCode.equalsIgnoreCase(getString(item, "dir"))) {
                continue;
            }
            Integer minutes = minutesUntil(getString(item, "eta"));
            if (minutes == null) {
                continue;
            }
            if (best == null || minutes < best) {
                best = minutes;
            }
        }
        return best;
    }

    private Integer minutesUntil(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            OffsetDateTime eta = OffsetDateTime.parse(value);
            long minutes = Duration.between(ZonedDateTime.now(HONG_KONG_ZONE).toOffsetDateTime(), eta).toMinutes();
            if (minutes < 0) {
                return null;
            }
            return (int) minutes;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private int estimateRideMinutes(double startLat, double startLng, double endLat, double endLng) {
        double distanceKm = haversineKm(startLat, startLng, endLat, endLng);
        return Math.max(10, (int) Math.round(((distanceKm / 18.0) * 60.0) * 1.4));
    }

    private int estimateDistanceMeters(double lat1, double lng1, double lat2, double lng2) {
        return (int) Math.round(haversineKm(lat1, lng1, lat2, lng2) * 1000.0);
    }

    private double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double radius = 6371.0;
        double p1 = Math.toRadians(lat1);
        double p2 = Math.toRadians(lat2);
        double d1 = Math.toRadians(lat2 - lat1);
        double d2 = Math.toRadians(lng2 - lng1);
        double a = Math.sin(d1 / 2) * Math.sin(d1 / 2)
                + Math.cos(p1) * Math.cos(p2) * Math.sin(d2 / 2) * Math.sin(d2 / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return radius * c;
    }

    private String getString(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        return object.get(key).getAsString();
    }

    private double getDouble(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return 0.0;
        }
        return object.get(key).getAsDouble();
    }

    private Map<CommuteCorridor, List<ServiceTemplate>> createCatalog() {
        Map<CommuteCorridor, List<ServiceTemplate>> catalog = new HashMap<>();
        catalog.put(
                CommuteCorridor.CENTRAL_TO_SHA_TIN,
                Arrays.asList(
                        new ServiceTemplate("182", "outbound", "O", "Central (Macao Ferry)", "Sha Tin"),
                        new ServiceTemplate("182X", "outbound", "O", "Central (Macao Ferry)", "Sha Tin")
                )
        );
        catalog.put(
                CommuteCorridor.CENTRAL_TO_STANLEY,
                Arrays.asList(
                        new ServiceTemplate("6", "outbound", "O", "Central (Exchange Square)", "Stanley Market"),
                        new ServiceTemplate("6A", "outbound", "O", "Central (Exchange Square)", "Stanley Fort"),
                        new ServiceTemplate("6X", "outbound", "O", "Central (Exchange Square)", "Stanley Market"),
                        new ServiceTemplate("66", "outbound", "O", "Central (Exchange Square)", "Stanley Plaza"),
                        new ServiceTemplate("260", "outbound", "O", "Central (Exchange Square)", "Stanley Market")
                )
        );
        catalog.put(
                CommuteCorridor.EXHIBITION_TO_LAI_CHI_KOK,
                Arrays.asList(
                        new ServiceTemplate("905", "outbound", "O", "Exhibition Centre Station", "Lai Chi Kok"),
                        new ServiceTemplate("905A", "outbound", "O", "Exhibition Centre Station", "Lai Chi Kok")
                )
        );
        catalog.put(
                CommuteCorridor.KAI_TAK_TO_AIRPORT,
                Arrays.asList(
                        new ServiceTemplate("A25", "outbound", "O", "Kai Tak", "Airport"),
                        new ServiceTemplate("A25S", "outbound", "O", "Kai Tak Sports Park", "Airport")
                )
        );
        catalog.put(
                CommuteCorridor.WESTERN_TO_EAST_HARBOUR,
                Arrays.asList(
                        new ServiceTemplate("10", "outbound", "O", "Kennedy Town", "North Point Ferry Pier"),
                        new ServiceTemplate("23X", "outbound", "O", "Pokfield Road", "Sai Wan Ho")
                )
        );
        return catalog;
    }

    private static class ServiceTemplate {
        private final String routeNumber;
        private final String directionPath;
        private final String directionCode;
        private final String originLabel;
        private final String destinationLabel;
        private final String destinationAddress;

        private ServiceTemplate(
                String routeNumber,
                String directionPath,
                String directionCode,
                String originLabel,
                String destinationLabel
        ) {
            this.routeNumber = routeNumber;
            this.directionPath = directionPath;
            this.directionCode = directionCode;
            this.originLabel = originLabel;
            this.destinationLabel = destinationLabel;
            this.destinationAddress = destinationLabel + ", Hong Kong";
        }
    }

    private static class StopInfo {
        private final String stopId;
        private final String name;
        private final double latitude;
        private final double longitude;

        private StopInfo(String stopId, String name, double latitude, double longitude) {
            this.stopId = stopId;
            this.name = name == null || name.trim().isEmpty() ? stopId : name;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }
}
