package hk.edu.hku.cs7506.smartcompanion.data.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.time.format.FormatStyle;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import hk.edu.hku.cs7506.smartcompanion.data.model.ParkingDestination;
import hk.edu.hku.cs7506.smartcompanion.data.model.RecommendationItem;
import hk.edu.hku.cs7506.smartcompanion.data.model.RecommendationRequest;
import hk.edu.hku.cs7506.smartcompanion.data.model.SceneType;
import hk.edu.hku.cs7506.smartcompanion.util.LeisureVisualCatalog;
import hk.edu.hku.cs7506.smartcompanion.util.PlaceProfileCatalog;

public class OfficialDataParser {
    private static final ZoneId HONG_KONG_ZONE = ZoneId.of("Asia/Hong_Kong");
    private static final int UPCOMING_EVENT_WINDOW_DAYS = 180;
    private static final long EVENTS_FEED_STALE_DAYS = 120;
    private static final DateTimeFormatter DAY_MONTH_YEAR = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("d MMMM uuuu")
            .toFormatter(Locale.ENGLISH);
    private static final DateTimeFormatter DAY_SHORT_MONTH_YEAR = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("d MMM uuuu")
            .toFormatter(Locale.ENGLISH);
    private static final DateTimeFormatter DAY_MONTH = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("d MMMM")
            .parseDefaulting(ChronoField.YEAR, LocalDate.now(HONG_KONG_ZONE).getYear())
            .toFormatter(Locale.ENGLISH);
    private static final DateTimeFormatter DAY_SHORT_MONTH = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("d MMM")
            .parseDefaulting(ChronoField.YEAR, LocalDate.now(HONG_KONG_ZONE).getYear())
            .toFormatter(Locale.ENGLISH);
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(Locale.ENGLISH);

    private final Map<String, double[]> hospitalCoordinates = createHospitalCoordinates();
    private final Map<String, double[]> aqhiStationCoordinates = createAqhiStationCoordinates();

    public List<RecommendationItem> parseEmergencyRecommendations(String rawJson, RecommendationRequest request) {
        JsonObject root = JsonParser.parseString(sanitizeJson(rawJson)).getAsJsonObject();
        JsonArray waitTime = root.getAsJsonArray("waitTime");
        List<RecommendationItem> items = new ArrayList<>();
        if (waitTime == null) {
            return items;
        }

        int index = 0;
        for (JsonElement element : waitTime) {
            JsonObject object = element.getAsJsonObject();
            String hospitalName = getString(object, "hospName");
            if (hospitalName.isEmpty()) {
                continue;
            }
            int waitMinutes = parseDurationMinutes(getString(object, "t45p50"));
            if (waitMinutes <= 0) {
                waitMinutes = parseDurationMinutes(getString(object, "t3p50"));
            }
            double[] coordinates = hospitalCoordinates.get(hospitalName);
            if (coordinates == null) {
                coordinates = new double[]{
                        request.getUserLatitude() + (index * 0.006),
                        request.getUserLongitude() + (index * 0.005)
                };
            }
            PlaceProfileCatalog.PlaceProfile profile = PlaceProfileCatalog.forHospital(hospitalName);
            int eta = estimateEtaMinutes(
                    request.getUserLatitude(),
                    request.getUserLongitude(),
                    coordinates[0],
                    coordinates[1],
                    28.0
            );
            items.add(new RecommendationItem(
                    "ha-" + hospitalName.replace(" ", "-").toLowerCase(Locale.US),
                    SceneType.EMERGENCY,
                    hospitalName,
                    "",
                    coordinates[0],
                    coordinates[1],
                    0.0,
                    eta,
                    waitMinutes,
                    null,
                    null,
                    null,
                    null,
                    null,
                    profile == null ? null : profile.getContentTag(),
                    profile == null ? null : profile.getMetadataLine(),
                    profile == null ? null : profile.getDetailsUrl(),
                    profile == null ? null : profile.getImageUrl(),
                    profile == null ? null : profile.getImageAttribution(),
                    profile == null ? null : profile.getAddressLine(),
                    profile == null ? null : profile.getContactPhone(),
                    null,
                    null
            ));
            index++;
        }
        return items;
    }

    public List<RecommendationItem> parseParkingRecommendations(
            String infoJson,
            String vacancyJson,
            RecommendationRequest request
    ) {
        Map<String, Integer> vacancyMap = parseVacancyMap(vacancyJson);
        JsonObject root = JsonParser.parseString(sanitizeJson(infoJson)).getAsJsonObject();
        JsonArray results = root.getAsJsonArray("results");
        List<RecommendationItem> items = new ArrayList<>();
        if (results == null) {
            return items;
        }

        ParkingDestination destination = request.getParkingDestination();
        for (JsonElement element : results) {
            JsonObject object = element.getAsJsonObject();
            String parkId = getString(object, "park_Id");
            if (parkId.isEmpty()) {
                parkId = getString(object, "park_id");
            }
            String openingStatus = getString(object, "opening_status");
            JsonObject privateCar = object.has("privateCar") && object.get("privateCar").isJsonObject()
                    ? object.getAsJsonObject("privateCar")
                    : null;
            int totalSpace = privateCar != null ? getInt(privateCar, "space") : 0;
            Integer vacancy = vacancyMap.get(parkId);

            if ("CLOSED".equalsIgnoreCase(openingStatus) || totalSpace <= 0 || vacancy == null || vacancy < 0) {
                continue;
            }

            String name = getString(object, "name");
            double latitude = getDouble(object, "latitude");
            double longitude = getDouble(object, "longitude");
            String displayAddress = getString(object, "displayAddress");
            String district = getString(object, "district");
            String contactNo = getString(object, "contactNo");
            String website = getString(object, "website");
            String nature = getString(object, "nature");
            String carparkType = getString(object, "carpark_Type");
            String imageUrl = resolveParkingImageUrl(object);
            PlaceProfileCatalog.PlaceProfile fallbackProfile = PlaceProfileCatalog.createParkingFallback(
                    district,
                    website,
                    displayAddress,
                    contactNo
            );
            int driveMinutes = estimateEtaMinutes(
                    request.getUserLatitude(),
                    request.getUserLongitude(),
                    latitude,
                    longitude,
                    26.0
            );
            int walkMeters = estimateDistanceMeters(
                    latitude,
                    longitude,
                    destination.getLatitude(),
                    destination.getLongitude()
            );
            int walkMinutes = Math.max(1, (int) Math.round(walkMeters / 78.0));

            items.add(new RecommendationItem(
                    parkId,
                    SceneType.PARKING,
                    name,
                    "",
                    latitude,
                    longitude,
                    0.0,
                    driveMinutes,
                    null,
                    vacancy,
                    walkMeters,
                    walkMinutes,
                    null,
                    null,
                    fallbackProfile.getContentTag(),
                    buildParkingMetadataLine(district, nature, carparkType),
                    website.isEmpty() ? fallbackProfile.getDetailsUrl() : website,
                    imageUrl.isEmpty() ? fallbackProfile.getImageUrl() : imageUrl,
                    imageUrl.isEmpty() ? fallbackProfile.getImageAttribution() : "Parking visuals from data.gov.hk",
                    displayAddress.isEmpty() ? fallbackProfile.getAddressLine() : displayAddress,
                    contactNo.isEmpty() ? fallbackProfile.getContactPhone() : contactNo,
                    null,
                    null
            ));
        }
        return items;
    }

    public List<RecommendationItem> parseEventRecommendations(
            String csvText,
            String weatherJson,
            String aqhiJson,
            RecommendationRequest request
    ) {
        return parseEventRecommendations(
                csvText,
                weatherJson,
                aqhiJson,
                request,
                LocalDate.now(HONG_KONG_ZONE)
        );
    }

    public List<RecommendationItem> parsePlayRecommendations(
            String eventCsvText,
            long eventLastModifiedEpochMillis,
            String museumsJson,
            String weatherJson,
            String aqhiJson,
            RecommendationRequest request
    ) {
        return parsePlayRecommendations(
                eventCsvText,
                eventLastModifiedEpochMillis,
                museumsJson,
                weatherJson,
                aqhiJson,
                request,
                LocalDate.now(HONG_KONG_ZONE)
        );
    }

    List<RecommendationItem> parsePlayRecommendations(
            String eventCsvText,
            long eventLastModifiedEpochMillis,
            String museumsJson,
            String weatherJson,
            String aqhiJson,
            RecommendationRequest request,
            LocalDate today
    ) {
        WeatherSnapshot weather = parseWeather(weatherJson);
        Map<String, AqhiSnapshot> aqhiByStation = parseAqhi(aqhiJson);

        List<RecommendationItem> items = new ArrayList<>();
        if (isEventFeedFresh(eventLastModifiedEpochMillis, today)) {
            items.addAll(parseCurrentEventRecommendations(
                    eventCsvText,
                    weather,
                    aqhiByStation,
                    request,
                    today
            ));
        }
        items.addAll(parseMuseumRecommendations(museumsJson, weather, aqhiByStation, request));
        return items;
    }

    List<RecommendationItem> parseEventRecommendations(
            String csvText,
            String weatherJson,
            String aqhiJson,
            RecommendationRequest request,
            LocalDate today
    ) {
        WeatherSnapshot weather = parseWeather(weatherJson);
        Map<String, AqhiSnapshot> aqhiByStation = parseAqhi(aqhiJson);
        return parseCurrentEventRecommendations(csvText, weather, aqhiByStation, request, today);
    }

    private WeatherSnapshot parseWeather(String weatherJson) {
        JsonObject root = JsonParser.parseString(sanitizeJson(weatherJson)).getAsJsonObject();
        JsonObject temperature = root.getAsJsonObject("temperature");
        JsonObject rainfall = root.getAsJsonObject("rainfall");
        double baseTemperature = 27.0;
        if (temperature != null && temperature.has("data")) {
            JsonArray stations = temperature.getAsJsonArray("data");
            for (JsonElement station : stations) {
                JsonObject item = station.getAsJsonObject();
                String place = getString(item, "place");
                if ("Hong Kong Observatory".equalsIgnoreCase(place) || "King's Park".equalsIgnoreCase(place)) {
                    baseTemperature = getDouble(item, "value");
                    break;
                }
            }
        }

        Map<String, Double> rainfallByDistrict = new HashMap<>();
        if (rainfall != null && rainfall.has("data")) {
            JsonArray districts = rainfall.getAsJsonArray("data");
            for (JsonElement districtElement : districts) {
                JsonObject district = districtElement.getAsJsonObject();
                rainfallByDistrict.put(getString(district, "place"), getDouble(district, "max"));
            }
        }
        return new WeatherSnapshot(baseTemperature, rainfallByDistrict);
    }

    private Map<String, AqhiSnapshot> parseAqhi(String aqhiJson) {
        JsonArray stations = JsonParser.parseString(sanitizeJson(aqhiJson)).getAsJsonArray();
        Map<String, AqhiSnapshot> result = new HashMap<>();
        for (JsonElement element : stations) {
            JsonObject station = element.getAsJsonObject();
            String name = getString(station, "station");
            result.put(name, new AqhiSnapshot(name, getDouble(station, "aqhi")));
        }
        return result;
    }

    private boolean isEventFeedFresh(long eventLastModifiedEpochMillis, LocalDate today) {
        if (eventLastModifiedEpochMillis <= 0) {
            return true;
        }
        LocalDate lastModifiedDate = java.time.Instant.ofEpochMilli(eventLastModifiedEpochMillis)
                .atZone(HONG_KONG_ZONE)
                .toLocalDate();
        long ageDays = java.time.temporal.ChronoUnit.DAYS.between(lastModifiedDate, today);
        return ageDays <= EVENTS_FEED_STALE_DAYS;
    }

    private List<RecommendationItem> parseCurrentEventRecommendations(
            String csvText,
            WeatherSnapshot weather,
            Map<String, AqhiSnapshot> aqhiByStation,
            RecommendationRequest request,
            LocalDate today
    ) {
        String[] lines = csvText.replace("\r", "").split("\n");
        if (lines.length <= 1) {
            return Collections.emptyList();
        }

        String[] header = splitCsvLine(lines[0]);
        int dateIndex = findColumnIndex(header, "Date", "Start Date", "Event Date");
        int nameIndex = findColumnIndex(header, "Event_Name", "Event Name", "Name");
        int venueIndex = findColumnIndex(header, "Venue", "Location", "Event Venue");
        List<EventCandidate> candidates = new ArrayList<>();
        Set<String> seenNames = new HashSet<>();

        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] columns = splitCsvLine(line);
            if (columns.length == 0) {
                continue;
            }
            String eventName = getCsvValue(columns, nameIndex, 1);
            String venue = getCsvValue(columns, venueIndex, 2);
            String rawDate = getCsvValue(columns, dateIndex, 0);
            if (eventName.isEmpty()) {
                continue;
            }

            EventDateRange dateRange = parseEventDateRange(rawDate, today);
            if (dateRange != null) {
                if (dateRange.endDate.isBefore(today)) {
                    continue;
                }
                if (dateRange.startDate.isAfter(today.plusDays(UPCOMING_EVENT_WINDOW_DAYS))) {
                    continue;
                }
            }

            String dedupeKey = eventName.toLowerCase(Locale.ENGLISH).trim();
            if (!seenNames.add(dedupeKey)) {
                continue;
            }

            VenueInfo venueInfo = resolveVenueInfo(venue);
            int eta = estimateEtaMinutes(
                    request.getUserLatitude(),
                    request.getUserLongitude(),
                    venueInfo.latitude,
                    venueInfo.longitude,
                    24.0
            );
            double weatherScore = scoreWeather(weather, venueInfo.district);
            double aqhiScore = scoreAqhi(venueInfo.latitude, venueInfo.longitude, aqhiByStation);
            LeisureVisualCatalog.VisualEntry visualEntry = LeisureVisualCatalog.forNameOrVenue(eventName, venue);

            candidates.add(new EventCandidate(
                    eventName,
                    venueInfo,
                    eta,
                    weatherScore,
                    aqhiScore,
                    dateRange,
                    "Current event",
                    buildEventMetadataLine(dateRange, venueInfo),
                    null,
                    visualEntry == null ? null : visualEntry.getImageUrl(),
                    visualEntry == null ? null : visualEntry.getAttribution()
            ));
        }

        candidates.sort((left, right) -> compareEventCandidates(left, right, today));

        List<RecommendationItem> items = new ArrayList<>();
        int index = 1;
        for (EventCandidate candidate : candidates) {
            items.add(new RecommendationItem(
                    "event-" + index,
                    SceneType.PLAY,
                    candidate.name,
                    "",
                    candidate.venueInfo.latitude,
                    candidate.venueInfo.longitude,
                    0.0,
                    candidate.etaMinutes,
                    null,
                    null,
                    null,
                    null,
                    candidate.weatherScore,
                    candidate.aqhiScore,
                    candidate.contentTag,
                    candidate.metadataLine,
                    candidate.detailsUrl,
                    candidate.imageUrl,
                    candidate.imageAttribution,
                    null,
                    null
            ));
            index++;
        }
        return items;
    }

    private List<RecommendationItem> parseMuseumRecommendations(
            String museumsJson,
            WeatherSnapshot weather,
            Map<String, AqhiSnapshot> aqhiByStation,
            RecommendationRequest request
    ) {
        JsonObject root = JsonParser.parseString(sanitizeJson(museumsJson)).getAsJsonObject();
        JsonArray features = root.getAsJsonArray("features");
        if (features == null || features.isEmpty()) {
            return Collections.emptyList();
        }

        List<RecommendationItem> items = new ArrayList<>();
        int index = 1;
        for (JsonElement featureElement : features) {
            JsonObject feature = featureElement.getAsJsonObject();
            JsonObject attributes = feature.getAsJsonObject("attributes");
            JsonObject geometry = feature.getAsJsonObject("geometry");
            if (attributes == null || geometry == null) {
                continue;
            }

            String name = getString(attributes, "NameEN");
            double latitude = geometry.has("y") ? geometry.get("y").getAsDouble() : getDouble(attributes, "LATITUDE");
            double longitude = geometry.has("x") ? geometry.get("x").getAsDouble() : getDouble(attributes, "LONGITUDE");
            String district = normalizeDistrict(getString(attributes, "DistrictEN"));
            String website = getString(attributes, "WebsiteEN");
            if (name.isEmpty() || latitude == 0 || longitude == 0) {
                continue;
            }

            int eta = estimateEtaMinutes(
                    request.getUserLatitude(),
                    request.getUserLongitude(),
                    latitude,
                    longitude,
                    24.0
            );
            double weatherScore = scoreWeather(weather, district);
            double aqhiScore = scoreAqhi(latitude, longitude, aqhiByStation);
            LeisureVisualCatalog.VisualEntry visualEntry = LeisureVisualCatalog.forNameOrVenue(name, district);

            items.add(new RecommendationItem(
                    "museum-" + index,
                    SceneType.PLAY,
                    name,
                    "",
                    latitude,
                    longitude,
                    0.0,
                    eta,
                    null,
                    null,
                    null,
                    null,
                    weatherScore,
                    aqhiScore,
                    determineMuseumTag(name),
                    "Official museum directory - " + district,
                    website,
                    visualEntry == null ? null : visualEntry.getImageUrl(),
                    visualEntry == null ? null : visualEntry.getAttribution(),
                    null,
                    null
            ));
            index++;
        }
        return items;
    }

    private double scoreWeather(WeatherSnapshot weather, String district) {
        double temperatureScore = 1.0 - Math.min(1.0, Math.abs(weather.temperature - 24.0) / 10.0);
        double rainfall = weather.rainfallByDistrict.containsKey(district)
                ? weather.rainfallByDistrict.get(district)
                : 0.0;
        double rainfallScore = rainfall <= 0.2 ? 1.0 : Math.max(0.15, 1.0 - (rainfall / 20.0));
        return Math.max(0.15, Math.min(1.0, (temperatureScore * 0.55) + (rainfallScore * 0.45)));
    }

    private double scoreAqhi(double latitude, double longitude, Map<String, AqhiSnapshot> aqhiByStation) {
        String nearestStation = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Map.Entry<String, double[]> entry : aqhiStationCoordinates.entrySet()) {
            double distance = haversineKm(latitude, longitude, entry.getValue()[0], entry.getValue()[1]);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestStation = entry.getKey();
            }
        }
        AqhiSnapshot aqhi = nearestStation == null ? null : aqhiByStation.get(nearestStation);
        if (aqhi == null) {
            return 0.75;
        }
        return Math.max(0.2, 1.05 - (aqhi.value / 10.0));
    }

    private String buildParkingMetadataLine(String district, String nature, String carparkType) {
        List<String> parts = new ArrayList<>();
        if (!nature.isEmpty()) {
            parts.add(capitalizeWords(nature.replace('-', ' ')));
        }
        if (!carparkType.isEmpty()) {
            parts.add(capitalizeWords(carparkType.replace('-', ' ')));
        }
        if (!district.isEmpty()) {
            parts.add(district);
        }
        if (parts.isEmpty()) {
            return "Public parking recommendation";
        }
        return String.join(" | ", parts);
    }

    private Map<String, Integer> parseVacancyMap(String rawJson) {
        Map<String, Integer> vacancyMap = new HashMap<>();
        JsonObject root = JsonParser.parseString(sanitizeJson(rawJson)).getAsJsonObject();

        JsonArray oneStopResults = root.getAsJsonArray("results");
        if (oneStopResults != null) {
            for (JsonElement element : oneStopResults) {
                JsonObject result = element.getAsJsonObject();
                String parkId = getString(result, "park_Id");
                JsonArray privateCar = result.has("privateCar") && result.get("privateCar").isJsonArray()
                        ? result.getAsJsonArray("privateCar")
                        : null;
                if (parkId.isEmpty() || privateCar == null) {
                    continue;
                }
                Integer vacancy = extractOneStopVacancy(privateCar);
                if (vacancy != null) {
                    vacancyMap.put(parkId, vacancy);
                }
            }
        }

        JsonArray carParks = root.getAsJsonArray("car_park");
        if (carParks == null) {
            return vacancyMap;
        }

        for (JsonElement element : carParks) {
            JsonObject carPark = element.getAsJsonObject();
            String parkId = getString(carPark, "park_id");
            JsonArray vehicleTypes = carPark.getAsJsonArray("vehicle_type");
            if (parkId.isEmpty() || vehicleTypes == null) {
                continue;
            }
            for (JsonElement vehicleTypeElement : vehicleTypes) {
                JsonObject vehicleType = vehicleTypeElement.getAsJsonObject();
                String type = getString(vehicleType, "type");
                if (!type.startsWith("P")) {
                    continue;
                }
                JsonArray categories = vehicleType.getAsJsonArray("service_category");
                if (categories == null) {
                    continue;
                }
                for (JsonElement categoryElement : categories) {
                    JsonObject category = categoryElement.getAsJsonObject();
                    String hourly = getString(category, "category");
                    if (!"HOURLY".equalsIgnoreCase(hourly)) {
                        continue;
                    }
                    int vacancy = getInt(category, "vacancy");
                    if (!vacancyMap.containsKey(parkId) || vacancy > vacancyMap.get(parkId)) {
                        vacancyMap.put(parkId, vacancy);
                    }
                }
            }
        }
        return vacancyMap;
    }

    private String resolveParkingImageUrl(JsonObject object) {
        JsonObject renditionUrls = object.has("renditionUrls") && object.get("renditionUrls").isJsonObject()
                ? object.getAsJsonObject("renditionUrls")
                : null;
        if (renditionUrls != null) {
            String directPhoto = getString(renditionUrls, "carpark_photo");
            if (!directPhoto.isEmpty()) {
                return directPhoto;
            }
            String banner = getString(renditionUrls, "banner");
            if (!banner.isEmpty()) {
                return banner;
            }
            String thumbnail = getString(renditionUrls, "thumbnail");
            if (!thumbnail.isEmpty()) {
                return thumbnail;
            }
            String square = getString(renditionUrls, "square");
            if (!square.isEmpty()) {
                return square;
            }
        }
        return "";
    }

    private Integer extractOneStopVacancy(JsonArray privateCarEntries) {
        Integer bestVacancy = null;
        for (JsonElement entryElement : privateCarEntries) {
            JsonObject entry = entryElement.getAsJsonObject();
            String category = getString(entry, "category");
            if (!category.isEmpty() && !"HOURLY".equalsIgnoreCase(category)) {
                continue;
            }
            int vacancy = getInt(entry, "vacancy");
            if (bestVacancy == null || vacancy > bestVacancy) {
                bestVacancy = vacancy;
            }
        }
        return bestVacancy;
    }

    private String buildEventMetadataLine(EventDateRange dateRange, VenueInfo venueInfo) {
        if (dateRange == null) {
            return "HKTB event feed - " + venueInfo.district;
        }
        if (dateRange.startDate.equals(dateRange.endDate)) {
            return "HKTB event feed - " + DISPLAY_DATE.format(dateRange.startDate);
        }
        return "HKTB event feed - " + DISPLAY_DATE.format(dateRange.startDate)
                + " to "
                + DISPLAY_DATE.format(dateRange.endDate);
    }

    private String determineMuseumTag(String name) {
        String lower = name.toLowerCase(Locale.US);
        if (lower.contains("art") || lower.contains("visual")) {
            return "Art venue";
        }
        if (lower.contains("museum")) {
            return "Museum pick";
        }
        return "Anytime place";
    }

    private String normalizeDistrict(String district) {
        if (district == null || district.trim().isEmpty()) {
            return "Hong Kong";
        }
        String normalized = district.trim().replace('_', ' ');
        if ("CENTRAL & WESTERN".equalsIgnoreCase(normalized)) {
            return "Central & Western";
        }
        if ("YAU TSIM MONG".equalsIgnoreCase(normalized)) {
            return "Yau Tsim Mong";
        }
        if ("SHA TIN".equalsIgnoreCase(normalized)) {
            return "Sha Tin";
        }
        if ("SHAM SHUI PO".equalsIgnoreCase(normalized)) {
            return "Sham Shui Po";
        }
        return toTitleCase(normalized);
    }

    private String toTitleCase(String value) {
        String[] parts = value.toLowerCase(Locale.US).split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.toString();
    }

    private VenueInfo resolveVenueInfo(String venue) {
        if (venue.contains("Hong Kong Cultural Centre")) {
            return new VenueInfo(22.2947, 114.1741, "Yau Tsim Mong");
        }
        if (venue.contains("Tsim Sha Tsui East Waterfront")) {
            return new VenueInfo(22.2964, 114.1778, "Yau Tsim Mong");
        }
        if (venue.contains("Central Harbourfront")) {
            return new VenueInfo(22.2849, 114.1600, "Central & Western District");
        }
        if (venue.contains("West Kowloon Cultural District")) {
            return new VenueInfo(22.2936, 114.1591, "Yau Tsim Mong");
        }
        if (venue.contains("Victoria Harbour")) {
            return new VenueInfo(22.2870, 114.1683, "Central & Western District");
        }
        if (venue.contains("Kai Tak")) {
            return new VenueInfo(22.3076, 114.2135, "Kowloon City");
        }
        if (venue.contains("Hong Kong Convention and Exhibition Centre") || venue.contains("HKCEC")) {
            return new VenueInfo(22.2822, 114.1743, "Wan Chai");
        }
        if (venue.contains("Hong Kong Observation Wheel") || venue.contains("Central")) {
            return new VenueInfo(22.2860, 114.1617, "Central & Western");
        }
        return new VenueInfo(22.3027, 114.1772, "Yau Tsim Mong");
    }

    private int findColumnIndex(String[] header, String... candidates) {
        for (int i = 0; i < header.length; i++) {
            String column = stripQuotes(header[i]).trim();
            for (String candidate : candidates) {
                if (candidate.equalsIgnoreCase(column)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private String getCsvValue(String[] columns, int preferredIndex, int fallbackIndex) {
        if (preferredIndex >= 0 && preferredIndex < columns.length) {
            return stripQuotes(columns[preferredIndex]);
        }
        if (fallbackIndex >= 0 && fallbackIndex < columns.length) {
            return stripQuotes(columns[fallbackIndex]);
        }
        return "";
    }

    private EventDateRange parseEventDateRange(String rawDate, LocalDate today) {
        String normalized = rawDate == null ? "" : rawDate.trim()
                .replace('\u2013', '-')
                .replace('\u2014', '-')
                .replaceAll("\\s+", " ");
        if (normalized.isEmpty()) {
            return null;
        }

        if (normalized.contains(" to ")) {
            String[] parts = normalized.split("\\s+to\\s+");
            if (parts.length == 2) {
                LocalDate end = parseFlexibleDate(parts[1], today.getYear(), null);
                if (end == null) {
                    return null;
                }
                LocalDate start = parseFlexibleDate(parts[0], end.getYear(), end.getMonth());
                if (start == null) {
                    start = parseFlexibleDate(parts[0], today.getYear(), null);
                }
                if (start == null) {
                    return null;
                }
                if (start.isAfter(end)) {
                    if (parts[0].matches("\\d{1,2}")) {
                        start = start.minusYears(1);
                    } else {
                        LocalDate adjusted = parseFlexibleDate(parts[0], end.getYear() - 1, null);
                        if (adjusted != null) {
                            start = adjusted;
                        }
                    }
                }
                return new EventDateRange(start, end);
            }
        }

        LocalDate singleDate = parseFlexibleDate(normalized, today.getYear(), null);
        return singleDate == null ? null : new EventDateRange(singleDate, singleDate);
    }

    private LocalDate parseFlexibleDate(String raw, int fallbackYear, Month fallbackMonth) {
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        List<LocalDate> candidates = new ArrayList<>();
        candidates.add(tryParseDate(trimmed, DAY_MONTH_YEAR));
        candidates.add(tryParseDate(trimmed, DAY_SHORT_MONTH_YEAR));
        if (fallbackMonth != null) {
            LocalDate parsedDayOnly = tryParseDayOnly(trimmed, fallbackYear, fallbackMonth);
            if (parsedDayOnly != null) {
                candidates.add(parsedDayOnly);
            }
        }
        candidates.add(tryParseDate(trimmed + " " + fallbackYear, DAY_MONTH_YEAR));
        candidates.add(tryParseDate(trimmed + " " + fallbackYear, DAY_SHORT_MONTH_YEAR));
        candidates.add(tryParseDate(trimmed, DAY_MONTH));
        candidates.add(tryParseDate(trimmed, DAY_SHORT_MONTH));
        for (LocalDate candidate : candidates) {
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    private LocalDate tryParseDate(String raw, DateTimeFormatter formatter) {
        try {
            return LocalDate.parse(raw, formatter);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private LocalDate tryParseDayOnly(String raw, int year, Month month) {
        if (!raw.matches("\\d{1,2}")) {
            return null;
        }
        try {
            return LocalDate.of(year, month, Integer.parseInt(raw));
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private int compareEventCandidates(EventCandidate left, EventCandidate right, LocalDate today) {
        if (left.dateRange != null && right.dateRange != null) {
            boolean leftCurrent = !left.dateRange.startDate.isAfter(today);
            boolean rightCurrent = !right.dateRange.startDate.isAfter(today);
            if (leftCurrent != rightCurrent) {
                return leftCurrent ? -1 : 1;
            }
            int startCompare = left.dateRange.startDate.compareTo(right.dateRange.startDate);
            if (startCompare != 0) {
                return startCompare;
            }
        } else if (left.dateRange != null || right.dateRange != null) {
            return left.dateRange != null ? -1 : 1;
        }
        return Integer.compare(left.etaMinutes, right.etaMinutes);
    }

    private int parseDurationMinutes(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return 0;
        }
        String lower = raw.toLowerCase(Locale.US).trim();
        Matcher matcher = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)").matcher(lower);
        if (!matcher.find()) {
            return 0;
        }
        double value = Double.parseDouble(matcher.group(1));
        if (lower.contains("hour")) {
            return (int) Math.round(value * 60);
        }
        return (int) Math.round(value);
    }

    private String sanitizeJson(String rawJson) {
        if (rawJson == null) {
            return "{}";
        }
        String trimmed = rawJson.trim();
        int objectIndex = trimmed.indexOf('{');
        int arrayIndex = trimmed.indexOf('[');
        int startIndex;
        if (objectIndex == -1) {
            startIndex = arrayIndex;
        } else if (arrayIndex == -1) {
            startIndex = objectIndex;
        } else {
            startIndex = Math.min(objectIndex, arrayIndex);
        }
        if (startIndex > 0) {
            return trimmed.substring(startIndex);
        }
        return trimmed;
    }

    private String[] splitCsvLine(String line) {
        return line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
    }

    private String stripQuotes(String text) {
        String cleaned = text == null ? "" : text.trim();
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"") && cleaned.length() >= 2) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }
        return cleaned.replace("\"\"", "\"");
    }

    private int estimateEtaMinutes(double userLat, double userLng, double targetLat, double targetLng, double speedKmh) {
        double distanceKm = haversineKm(userLat, userLng, targetLat, targetLng);
        return Math.max(3, (int) Math.round((distanceKm / speedKmh) * 60));
    }

    private int estimateDistanceMeters(double lat1, double lng1, double lat2, double lng2) {
        return (int) Math.round(haversineKm(lat1, lng1, lat2, lng2) * 1000);
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

    private String capitalizeWords(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "";
        }
        String[] parts = input.trim().split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1).toLowerCase(Locale.US));
            }
        }
        return builder.toString();
    }

    private String getString(JsonObject object, String key) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        return object.get(key).getAsString();
    }

    private int getInt(JsonObject object, String key) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            return 0;
        }
        return object.get(key).getAsInt();
    }

    private double getDouble(JsonObject object, String key) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            return 0;
        }
        return object.get(key).getAsDouble();
    }

    private Map<String, double[]> createHospitalCoordinates() {
        Map<String, double[]> coordinates = new HashMap<>();
        coordinates.put("Alice Ho Miu Ling Nethersole Hospital", new double[]{22.4587, 114.1741});
        coordinates.put("Caritas Medical Centre", new double[]{22.3407, 114.1535});
        coordinates.put("Kwong Wah Hospital", new double[]{22.3132, 114.1729});
        coordinates.put("North District Hospital", new double[]{22.4963, 114.1289});
        coordinates.put("North Lantau Hospital", new double[]{22.2852, 113.9404});
        coordinates.put("Pamela Youde Nethersole Eastern Hospital", new double[]{22.2699, 114.2361});
        coordinates.put("Pok Oi Hospital", new double[]{22.4455, 114.0385});
        coordinates.put("Prince of Wales Hospital", new double[]{22.3774, 114.2007});
        coordinates.put("Princess Margaret Hospital", new double[]{22.3417, 114.1358});
        coordinates.put("Queen Elizabeth Hospital", new double[]{22.3085, 114.1747});
        coordinates.put("Queen Mary Hospital", new double[]{22.2704, 114.1310});
        coordinates.put("Ruttonjee Hospital", new double[]{22.2762, 114.1768});
        coordinates.put("St John Hospital", new double[]{22.2085, 114.0280});
        coordinates.put("Tin Shui Wai Hospital", new double[]{22.4593, 114.0046});
        coordinates.put("Tseung Kwan O Hospital", new double[]{22.3156, 114.2707});
        coordinates.put("Tuen Mun Hospital", new double[]{22.4070, 113.9753});
        coordinates.put("United Christian Hospital", new double[]{22.3227, 114.2272});
        coordinates.put("Yan Chai Hospital", new double[]{22.3690, 114.1207});
        return coordinates;
    }

    private Map<String, double[]> createAqhiStationCoordinates() {
        Map<String, double[]> coordinates = new HashMap<>();
        coordinates.put("Central/Western", new double[]{22.2855, 114.1544});
        coordinates.put("Central", new double[]{22.2821, 114.1588});
        coordinates.put("Causeway Bay", new double[]{22.2803, 114.1849});
        coordinates.put("Eastern", new double[]{22.2836, 114.2248});
        coordinates.put("Southern", new double[]{22.2470, 114.1607});
        coordinates.put("Kwun Tong", new double[]{22.3133, 114.2250});
        coordinates.put("Sham Shui Po", new double[]{22.3303, 114.1595});
        coordinates.put("Kwai Chung", new double[]{22.3600, 114.1290});
        coordinates.put("Tsuen Wan", new double[]{22.3731, 114.1173});
        coordinates.put("Tseung Kwan O", new double[]{22.3156, 114.2649});
        coordinates.put("Yuen Long", new double[]{22.4440, 114.0222});
        coordinates.put("Tuen Mun", new double[]{22.3915, 113.9771});
        coordinates.put("Tung Chung", new double[]{22.2890, 113.9414});
        coordinates.put("Tai Po", new double[]{22.4500, 114.1670});
        coordinates.put("Sha Tin", new double[]{22.3810, 114.1880});
        coordinates.put("North", new double[]{22.4960, 114.1280});
        coordinates.put("Tap Mun", new double[]{22.4722, 114.3634});
        coordinates.put("Mong Kok", new double[]{22.3193, 114.1694});
        return coordinates;
    }

    private static class WeatherSnapshot {
        private final double temperature;
        private final Map<String, Double> rainfallByDistrict;

        private WeatherSnapshot(double temperature, Map<String, Double> rainfallByDistrict) {
            this.temperature = temperature;
            this.rainfallByDistrict = rainfallByDistrict;
        }
    }

    private static class AqhiSnapshot {
        private final double value;

        private AqhiSnapshot(String station, double value) {
            this.value = value;
        }
    }

    private static class VenueInfo {
        private final double latitude;
        private final double longitude;
        private final String district;

        private VenueInfo(double latitude, double longitude, String district) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.district = district;
        }
    }

    private static class EventDateRange {
        private final LocalDate startDate;
        private final LocalDate endDate;

        private EventDateRange(LocalDate startDate, LocalDate endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }
    }

    private static class EventCandidate {
        private final String name;
        private final VenueInfo venueInfo;
        private final int etaMinutes;
        private final double weatherScore;
        private final double aqhiScore;
        private final EventDateRange dateRange;
        private final String contentTag;
        private final String metadataLine;
        private final String detailsUrl;
        private final String imageUrl;
        private final String imageAttribution;

        private EventCandidate(
                String name,
                VenueInfo venueInfo,
                int etaMinutes,
                double weatherScore,
                double aqhiScore,
                EventDateRange dateRange,
                String contentTag,
                String metadataLine,
                String detailsUrl,
                String imageUrl,
                String imageAttribution
        ) {
            this.name = name;
            this.venueInfo = venueInfo;
            this.etaMinutes = etaMinutes;
            this.weatherScore = weatherScore;
            this.aqhiScore = aqhiScore;
            this.dateRange = dateRange;
            this.contentTag = contentTag;
            this.metadataLine = metadataLine;
            this.detailsUrl = detailsUrl;
            this.imageUrl = imageUrl;
            this.imageAttribution = imageAttribution;
        }
    }
}
