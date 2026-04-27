package hk.edu.hku.cs7506.smartcompanion.data.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import hk.edu.hku.cs7506.smartcompanion.data.model.RecommendationItem;
import hk.edu.hku.cs7506.smartcompanion.data.model.RecommendationRequest;
import hk.edu.hku.cs7506.smartcompanion.data.model.SceneType;

public class TransportIntelligenceClient {
    private static final ZoneId HONG_KONG_ZONE = ZoneId.of("Asia/Hong_Kong");
    private static final String TDAS_ROUTE_URL = "https://tdas-api.hkemobility.gov.hk/tdas/api/route";
    private static final String CTB_STOP_ETA_TEMPLATE = "https://rt.data.gov.hk/v1/transport/batch/stop-eta/CTB/%s?lang=en";
    private static final String KMB_STOP_ETA_TEMPLATE = "https://data.etabus.gov.hk/v1/transport/kmb/stop-eta/%s";
    private static final int OPTIONAL_NETWORK_TIMEOUT_MILLIS = 2500;
    private static final int MAX_EMERGENCY_ENRICH_ITEMS = 4;
    private static final int MAX_PARKING_ENRICH_ITEMS = 4;
    private static final int MAX_PLAY_ENRICH_ITEMS = 2;
    private static final int MAX_COMMUTE_ENRICH_ITEMS = 4;

    private final OpenDataHttpClient httpClient;
    private final Map<String, RouteSnapshot> routeSnapshotCache = new HashMap<>();
    private final Map<String, String> transitNoteCache = new HashMap<>();

    public TransportIntelligenceClient() {
        this(new OpenDataHttpClient(OPTIONAL_NETWORK_TIMEOUT_MILLIS, OPTIONAL_NETWORK_TIMEOUT_MILLIS));
    }

    TransportIntelligenceClient(OpenDataHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public List<RecommendationItem> enrichTopRecommendations(
            SceneType sceneType,
            RecommendationRequest request,
            List<RecommendationItem> rankedItems,
            String trafficNewsXml
    ) {
        if (rankedItems == null || rankedItems.isEmpty()) {
            return rankedItems == null ? Collections.emptyList() : rankedItems;
        }

        List<TrafficIncident> incidents = parseTrafficIncidents(trafficNewsXml);
        List<RecommendationItem> enriched = new ArrayList<>(rankedItems.size());
        int maxEnrichItems = resolveMaxEnrichItems(sceneType);
        for (int index = 0; index < rankedItems.size(); index++) {
            RecommendationItem item = rankedItems.get(index);
            if (index >= maxEnrichItems) {
                enriched.add(item);
                continue;
            }

            RouteSnapshot routeSnapshot = fetchRouteSnapshot(
                    request.getUserLatitude(),
                    request.getUserLongitude(),
                    item.getLatitude(),
                    item.getLongitude()
            );
            TrafficIncident incident = findRelevantIncident(item, incidents);
            String trafficNote = buildTrafficNote(routeSnapshot, incident);
            String transitNote = shouldAttachTransit(sceneType)
                    ? fetchTransitNote(item)
                    : item.getTransitNote();
            Integer etaMinutes = routeSnapshot != null ? routeSnapshot.etaMinutes : item.getEtaMinutes();
            enriched.add(item.withTransport(etaMinutes, trafficNote, transitNote));
        }
        return enriched;
    }

    private boolean shouldAttachTransit(SceneType sceneType) {
        return sceneType == SceneType.PLAY || sceneType == SceneType.PARKING;
    }

    private int resolveMaxEnrichItems(SceneType sceneType) {
        if (sceneType == SceneType.EMERGENCY) {
            return MAX_EMERGENCY_ENRICH_ITEMS;
        }
        if (sceneType == SceneType.PARKING) {
            return MAX_PARKING_ENRICH_ITEMS;
        }
        if (sceneType == SceneType.COMMUTE) {
            return MAX_COMMUTE_ENRICH_ITEMS;
        }
        return MAX_PLAY_ENRICH_ITEMS;
    }

    private RouteSnapshot fetchRouteSnapshot(double startLat, double startLng, double endLat, double endLng) {
        String cacheKey = buildRouteCacheKey(startLat, startLng, endLat, endLng);
        RouteSnapshot cachedSnapshot = routeSnapshotCache.get(cacheKey);
        if (cachedSnapshot != null) {
            return cachedSnapshot;
        }
        try {
            String payload = String.format(Locale.US,
                    "{\"start\":{\"lat\":%.6f,\"long\":%.6f},\"end\":{\"lat\":%.6f,\"long\":%.6f},\"departIn\":0,\"lang\":\"en\",\"type\":\"ST\"}",
                    startLat,
                    startLng,
                    endLat,
                    endLng
            );
            String response = httpClient.postJson(TDAS_ROUTE_URL, payload);
            JsonObject root = JsonParser.parseString(response).getAsJsonObject();
            String eta = getString(root, "eta");
            String distanceLabel = getString(root, "distU");
            String speedLabel = getString(root, "jSpeed");
            Integer etaMinutes = parseClockDurationMinutes(eta);
            if (etaMinutes == null) {
                return null;
            }
            RouteSnapshot snapshot = new RouteSnapshot(etaMinutes, distanceLabel, speedLabel);
            routeSnapshotCache.put(cacheKey, snapshot);
            return snapshot;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String fetchTransitNote(RecommendationItem item) {
        TransitStopSpec stopSpec = findNearestTransitStop(item.getLatitude(), item.getLongitude());
        if (stopSpec == null) {
            return null;
        }
        String cached = transitNoteCache.get(stopSpec.cacheKey);
        if (cached != null) {
            return cached;
        }
        try {
            String response = httpClient.get(stopSpec.buildEtaUrl());
            String note = stopSpec.operator == TransitOperator.KMB
                    ? parseKmbStopEta(response, stopSpec.displayName)
                    : parseCtbStopEta(response, stopSpec.displayName);
            if (note != null) {
                transitNoteCache.put(stopSpec.cacheKey, note);
            }
            return note;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String parseKmbStopEta(String json, String stopLabel) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray data = root.getAsJsonArray("data");
        if (data == null || data.isEmpty()) {
            return null;
        }
        List<ArrivalEntry> arrivals = new ArrayList<>();
        for (JsonElement element : data) {
            JsonObject object = element.getAsJsonObject();
            Integer minutes = minutesUntil(getString(object, "eta"));
            if (minutes == null) {
                continue;
            }
            arrivals.add(new ArrivalEntry(
                    getString(object, "route"),
                    getString(object, "dest_en"),
                    minutes
            ));
        }
        return formatArrivalNote(stopLabel, arrivals);
    }

    private String parseCtbStopEta(String json, String stopLabel) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray data = root.getAsJsonArray("data");
        if (data == null || data.isEmpty()) {
            return null;
        }
        List<ArrivalEntry> arrivals = new ArrayList<>();
        for (JsonElement element : data) {
            JsonObject object = element.getAsJsonObject();
            Integer minutes = minutesUntil(getString(object, "eta"));
            if (minutes == null) {
                continue;
            }
            arrivals.add(new ArrivalEntry(
                    getString(object, "route"),
                    getString(object, "dest"),
                    minutes
            ));
        }
        return formatArrivalNote(stopLabel, arrivals);
    }

    private String formatArrivalNote(String stopLabel, List<ArrivalEntry> arrivals) {
        if (arrivals.isEmpty()) {
            return null;
        }
        Map<String, ArrivalEntry> earliestByRoute = new HashMap<>();
        for (ArrivalEntry entry : arrivals) {
            ArrivalEntry current = earliestByRoute.get(entry.route);
            if (current == null || entry.minutes < current.minutes) {
                earliestByRoute.put(entry.route, entry);
            }
        }
        List<ArrivalEntry> uniqueArrivals = new ArrayList<>(earliestByRoute.values());
        uniqueArrivals.sort(Comparator.comparingInt(entry -> entry.minutes));
        StringBuilder builder = new StringBuilder("Nearby buses from ");
        builder.append(stopLabel).append(": ");
        int limit = Math.min(3, uniqueArrivals.size());
        for (int index = 0; index < limit; index++) {
            ArrivalEntry entry = uniqueArrivals.get(index);
            if (index > 0) {
                builder.append(", ");
            }
            builder.append(entry.route).append(" ");
            builder.append(entry.minutes).append(" min");
        }
        return builder.toString();
    }

    private List<TrafficIncident> parseTrafficIncidents(String xml) {
        if (xml == null || xml.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            NodeList messages = document.getElementsByTagName("message");
            List<TrafficIncident> incidents = new ArrayList<>();
            for (int index = 0; index < messages.getLength(); index++) {
                Element element = (Element) messages.item(index);
                String heading = readText(element, "INCIDENT_HEADING_EN");
                String location = readText(element, "LOCATION_EN");
                String content = readText(element, "CONTENT_EN");
                String announcementTime = readText(element, "ANNOUNCEMENT_DATE");
                if (location.isEmpty() && content.isEmpty()) {
                    continue;
                }
                incidents.add(new TrafficIncident(heading, location, content, announcementTime));
            }
            incidents.sort((left, right) -> right.announcementTime.compareTo(left.announcementTime));
            return incidents;
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    private TrafficIncident findRelevantIncident(RecommendationItem item, List<TrafficIncident> incidents) {
        if (incidents.isEmpty()) {
            return null;
        }
        TrafficZone zone = findNearestTrafficZone(item.getLatitude(), item.getLongitude());
        String name = item.getName().toLowerCase(Locale.US);
        for (TrafficIncident incident : incidents) {
            String searchableText = (incident.location + " " + incident.content).toLowerCase(Locale.US);
            if (zone != null) {
                for (String keyword : zone.keywords) {
                    if (searchableText.contains(keyword)) {
                        return incident;
                    }
                }
            }
            for (String token : name.split("[^a-z0-9]+")) {
                if (token.length() >= 4 && searchableText.contains(token)) {
                    return incident;
                }
            }
        }
        return null;
    }

    private TrafficZone findNearestTrafficZone(double latitude, double longitude) {
        TrafficZone best = null;
        double bestDistance = Double.MAX_VALUE;
        for (TrafficZone zone : TrafficZone.values()) {
            double distance = distanceSquared(latitude, longitude, zone.latitude, zone.longitude);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = zone;
            }
        }
        return best;
    }

    private TransitStopSpec findNearestTransitStop(double latitude, double longitude) {
        TransitStopSpec best = null;
        double bestDistance = Double.MAX_VALUE;
        for (TransitStopSpec stop : TransitStopSpec.values()) {
            double distance = distanceSquared(latitude, longitude, stop.latitude, stop.longitude);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = stop;
            }
        }
        return bestDistance <= 0.004 ? best : null;
    }

    private String buildTrafficNote(RouteSnapshot snapshot, TrafficIncident incident) {
        if (snapshot == null && incident == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        if (snapshot != null) {
            builder.append("Live TDAS drive time ");
            builder.append(snapshot.etaMinutes).append(" min");
            if (!snapshot.distanceLabel.isEmpty()) {
                builder.append(" over ").append(snapshot.distanceLabel);
            }
            if (!snapshot.speedLabel.isEmpty()) {
                builder.append(" at ").append(snapshot.speedLabel);
            }
            builder.append(".");
        }
        if (incident != null) {
            if (builder.length() > 0) {
                builder.append(" ");
            }
            builder.append("Traffic alert nearby: ");
            builder.append(incident.location.isEmpty() ? incident.heading : incident.location);
            if (!incident.content.isEmpty()) {
                builder.append(" - ").append(trimSentence(incident.content));
            }
        }
        return builder.toString();
    }

    private String trimSentence(String text) {
        String cleaned = text == null ? "" : text.trim();
        if (cleaned.length() <= 120) {
            return cleaned;
        }
        return cleaned.substring(0, 117).trim() + "...";
    }

    private Integer parseClockDurationMinutes(String value) {
        if (value == null || !value.matches("\\d{2}:\\d{2}")) {
            return null;
        }
        String[] parts = value.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
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

    private String getString(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        return object.get(key).getAsString();
    }

    private String readText(Element element, String tagName) {
        NodeList nodeList = element.getElementsByTagName(tagName);
        if (nodeList == null || nodeList.getLength() == 0 || nodeList.item(0) == null) {
            return "";
        }
        return nodeList.item(0).getTextContent().trim();
    }

    private double distanceSquared(double lat1, double lng1, double lat2, double lng2) {
        double dLat = lat1 - lat2;
        double dLng = lng1 - lng2;
        return (dLat * dLat) + (dLng * dLng);
    }

    private String buildRouteCacheKey(double startLat, double startLng, double endLat, double endLng) {
        return String.format(
                Locale.US,
                "%.3f,%.3f->%.3f,%.3f",
                roundToThreeDecimals(startLat),
                roundToThreeDecimals(startLng),
                roundToThreeDecimals(endLat),
                roundToThreeDecimals(endLng)
        );
    }

    private double roundToThreeDecimals(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private enum TransitOperator {
        CTB,
        KMB
    }

    private enum TransitStopSpec {
        HKU(TransitOperator.CTB, "001115", "Pokfield Road", 22.2816, 114.1323),
        CENTRAL(TransitOperator.CTB, "003339", "Central (Observation Wheel)", 22.2860, 114.1617),
        HKCEC(TransitOperator.CTB, "003415", "Exhibition Centre Station", 22.2817, 114.1758),
        KAI_TAK(TransitOperator.CTB, "003874", "Kai Tak Sports Park", 22.3230, 114.1930),
        TST(TransitOperator.KMB, "8D804CFD9C7B9042", "Star Ferry / Harbour City", 22.2943, 114.1692),
        WEST_KOWLOON(TransitOperator.KMB, "1A0F813116024D92", "High Speed Rail West Kowloon Station", 22.3035, 114.1664);

        private final TransitOperator operator;
        private final String stopId;
        private final String displayName;
        private final double latitude;
        private final double longitude;
        private final String cacheKey;

        TransitStopSpec(TransitOperator operator, String stopId, String displayName, double latitude, double longitude) {
            this.operator = operator;
            this.stopId = stopId;
            this.displayName = displayName;
            this.latitude = latitude;
            this.longitude = longitude;
            this.cacheKey = operator.name() + ":" + stopId;
        }

        private String buildEtaUrl() {
            if (operator == TransitOperator.KMB) {
                return String.format(Locale.US, KMB_STOP_ETA_TEMPLATE, stopId);
            }
            return String.format(Locale.US, CTB_STOP_ETA_TEMPLATE, stopId);
        }
    }

    private enum TrafficZone {
        HKU(22.2816, 114.1323, new String[]{"pokfield", "kennedy town", "western", "bonham", "hk university", "university"}),
        CENTRAL(22.2860, 114.1617, new String[]{"central", "connaught", "admiralty", "harbourfront"}),
        HKCEC(22.2817, 114.1758, new String[]{"wan chai", "exhibition", "gloucester", "convention"}),
        TST(22.2943, 114.1692, new String[]{"tsim sha tsui", "mody", "salisbury", "canton road", "star ferry"}),
        WEST_KOWLOON(22.3035, 114.1664, new String[]{"west kowloon", "austin", "high speed rail", "wkcd"}),
        KAI_TAK(22.3230, 114.1930, new String[]{"kai tak", "sung wong toi", "sports park"}),
        SHA_TIN(22.3774, 114.1852, new String[]{"sha tin", "tai wai", "fotan"});

        private final double latitude;
        private final double longitude;
        private final String[] keywords;

        TrafficZone(double latitude, double longitude, String[] keywords) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.keywords = keywords;
        }
    }

    private static class ArrivalEntry {
        private final String route;
        private final String destination;
        private final int minutes;

        private ArrivalEntry(String route, String destination, int minutes) {
            this.route = route;
            this.destination = destination;
            this.minutes = minutes;
        }
    }

    private static class RouteSnapshot {
        private final int etaMinutes;
        private final String distanceLabel;
        private final String speedLabel;

        private RouteSnapshot(int etaMinutes, String distanceLabel, String speedLabel) {
            this.etaMinutes = etaMinutes;
            this.distanceLabel = distanceLabel == null ? "" : distanceLabel;
            this.speedLabel = speedLabel == null ? "" : speedLabel;
        }
    }

    private static class TrafficIncident {
        private final String heading;
        private final String location;
        private final String content;
        private final String announcementTime;

        private TrafficIncident(String heading, String location, String content, String announcementTime) {
            this.heading = heading == null ? "" : heading;
            this.location = location == null ? "" : location;
            this.content = content == null ? "" : content;
            this.announcementTime = announcementTime == null ? "" : announcementTime;
        }
    }
}
