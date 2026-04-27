package hk.edu.hku.cs7506.smartcompanion.util;

import android.content.Context;
import android.text.TextUtils;

import com.amap.api.maps.model.LatLng;
import com.amap.api.services.busline.BusStationItem;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.route.BusPath;
import com.amap.api.services.route.BusStep;
import com.amap.api.services.route.Doorway;
import com.amap.api.services.route.DrivePath;
import com.amap.api.services.route.DriveStep;
import com.amap.api.services.route.Path;
import com.amap.api.services.route.RailwayStationItem;
import com.amap.api.services.route.RidePath;
import com.amap.api.services.route.RideStep;
import com.amap.api.services.route.RouteBusLineItem;
import com.amap.api.services.route.RouteBusWalkItem;
import com.amap.api.services.route.RouteRailwayItem;
import com.amap.api.services.route.RouteSearch;
import com.amap.api.services.route.WalkPath;
import com.amap.api.services.route.WalkStep;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import hk.edu.hku.cs7506.smartcompanion.R;
import hk.edu.hku.cs7506.smartcompanion.data.model.LocationSnapshot;
import hk.edu.hku.cs7506.smartcompanion.data.model.RecommendationItem;
import hk.edu.hku.cs7506.smartcompanion.data.model.RouteMode;
import hk.edu.hku.cs7506.smartcompanion.data.model.RouteStep;

public final class AmapRouteSupport {
    private static final Pattern DISTANCE_PROMPT_PATTERN =
            Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(\\u516c\\u91cc|\\u7c73)");
    private static final Pattern EXIT_PATTERN =
            Pattern.compile("(?<![A-Za-z0-9])([A-Za-z]{1,2}\\d*[A-Za-z]?|\\d+[A-Za-z]?)\\s*(?:\\u51fa\\u53e3|\\u53e3)");
    private static final Map<String, String> ACTION_TRANSLATIONS = createActionTranslations();
    private static final Map<String, String> TRANSIT_TEXT_TRANSLATIONS = createTransitTextTranslations();

    private AmapRouteSupport() {
    }

    public static RouteSearch.FromAndTo buildFromAndTo(LocationSnapshot origin, RecommendationItem item) {
        return new RouteSearch.FromAndTo(
                new LatLonPoint(origin.getLatitude(), origin.getLongitude()),
                new LatLonPoint(item.getLatitude(), item.getLongitude())
        );
    }

    public static RouteSearch.DriveRouteQuery buildDriveRouteQuery(LocationSnapshot origin, RecommendationItem item) {
        return new RouteSearch.DriveRouteQuery(
                buildFromAndTo(origin, item),
                RouteSearch.DRIVING_SINGLE_DEFAULT,
                null,
                null,
                ""
        );
    }

    public static RouteSearch.WalkRouteQuery buildWalkRouteQuery(LocationSnapshot origin, RecommendationItem item) {
        return new RouteSearch.WalkRouteQuery(buildFromAndTo(origin, item), RouteSearch.WALK_DEFAULT);
    }

    public static RouteSearch.RideRouteQuery buildRideRouteQuery(LocationSnapshot origin, RecommendationItem item) {
        return new RouteSearch.RideRouteQuery(buildFromAndTo(origin, item), RouteSearch.RIDING_DEFAULT);
    }

    public static RouteSearch.BusRouteQuery buildBusRouteQuery(LocationSnapshot origin, RecommendationItem item) {
        String city = TextUtils.isEmpty(origin.getCity()) ? "Hong Kong" : origin.getCity();
        RouteSearch.BusRouteQuery query = new RouteSearch.BusRouteQuery(
                buildFromAndTo(origin, item),
                RouteSearch.BUS_DEFAULT,
                city,
                0
        );
        query.setCityd(city);
        return query;
    }

    public static LatLng getOriginLatLng(LocationSnapshot origin) {
        return new LatLng(origin.getLatitude(), origin.getLongitude());
    }

    public static String getOriginLabel(LocationSnapshot origin) {
        return origin.getLabel();
    }

    public static List<LatLng> flattenDrivePolyline(DrivePath path) {
        List<LatLng> points = new ArrayList<>();
        if (path == null || path.getSteps() == null) {
            return points;
        }
        for (DriveStep step : path.getSteps()) {
            appendPolyline(points, step.getPolyline());
        }
        return points;
    }

    public static List<LatLng> flattenWalkPolyline(WalkPath path) {
        List<LatLng> points = new ArrayList<>();
        if (path == null || path.getSteps() == null) {
            return points;
        }
        for (WalkStep step : path.getSteps()) {
            appendPolyline(points, step.getPolyline());
        }
        return points;
    }

    public static List<LatLng> flattenRidePolyline(RidePath path) {
        List<LatLng> points = new ArrayList<>();
        if (path == null || path.getSteps() == null) {
            return points;
        }
        for (RideStep step : path.getSteps()) {
            appendPolyline(points, step.getPolyline());
        }
        return points;
    }

    public static List<LatLng> flattenBusPolyline(BusPath path) {
        List<LatLng> points = new ArrayList<>();
        if (path == null || path.getSteps() == null) {
            return points;
        }
        for (BusStep step : path.getSteps()) {
            RouteBusWalkItem walk = step.getWalk();
            if (walk != null) {
                appendPolyline(points, walk.getPolyline());
            }
            List<RouteBusLineItem> lines = step.getBusLines();
            if (lines != null) {
                for (RouteBusLineItem line : lines) {
                    appendPolyline(points, line.getPolyline());
                }
            }
        }
        if (points.isEmpty()) {
            appendPolyline(points, path.getPolyline());
        }
        return points;
    }

    public static List<RouteStep> toDriveRouteSteps(DrivePath path) {
        List<RouteStep> steps = new ArrayList<>();
        if (path == null || path.getSteps() == null) {
            return steps;
        }
        int index = 1;
        for (DriveStep step : path.getSteps()) {
            String title = String.format(
                    Locale.US,
                    "%d. %s",
                    index,
                    translateActionLabel(step.getAction(), step.getInstruction(), "Continue")
            );
            String body = buildEnglishStepBody(RouteMode.DRIVE, step.getDistance(), step.getDuration(), step.getRoad());
            steps.add(new RouteStep(title, body));
            index++;
        }
        return steps;
    }

    public static List<RouteStep> toWalkRouteSteps(WalkPath path) {
        List<RouteStep> steps = new ArrayList<>();
        if (path == null || path.getSteps() == null) {
            return steps;
        }
        int index = 1;
        for (WalkStep step : path.getSteps()) {
            String title = String.format(
                    Locale.US,
                    "%d. %s",
                    index,
                    translateActionLabel(step.getAction(), step.getInstruction(), "Walk")
            );
            String body = buildEnglishStepBody(RouteMode.WALK, step.getDistance(), step.getDuration(), step.getRoad());
            steps.add(new RouteStep(title, body));
            index++;
        }
        return steps;
    }

    public static List<RouteStep> toRideRouteSteps(RidePath path) {
        List<RouteStep> steps = new ArrayList<>();
        if (path == null || path.getSteps() == null) {
            return steps;
        }
        int index = 1;
        for (RideStep step : path.getSteps()) {
            String title = String.format(
                    Locale.US,
                    "%d. %s",
                    index,
                    translateActionLabel(step.getAction(), step.getInstruction(), "Ride")
            );
            String body = buildEnglishStepBody(RouteMode.RIDE, step.getDistance(), step.getDuration(), step.getRoad());
            steps.add(new RouteStep(title, body));
            index++;
        }
        return steps;
    }

    public static List<RouteStep> toBusRouteSteps(BusPath path) {
        List<RouteStep> steps = new ArrayList<>();
        if (path == null || path.getSteps() == null) {
            return steps;
        }
        int index = 1;
        for (BusStep step : path.getSteps()) {
            RouteBusWalkItem walk = step.getWalk();
            if (walk != null && walk.getDistance() > 0) {
                steps.add(buildTransitWalkStep(index++, walk, step));
            }

            RouteRailwayItem railway = step.getRailway();
            if (railway != null) {
                steps.add(buildTransitRailStep(index++, railway, step));
            }

            List<RouteBusLineItem> lines = step.getBusLines();
            if (lines == null || lines.isEmpty()) {
                continue;
            }
            for (RouteBusLineItem line : lines) {
                steps.add(buildTransitLineStep(index++, line, step));
            }
        }
        return steps;
    }

    public static Path pickBestPath(RouteMode routeMode, DrivePath drivePath, WalkPath walkPath, RidePath ridePath, BusPath busPath) {
        if (routeMode == RouteMode.WALK) {
            return walkPath;
        }
        if (routeMode == RouteMode.RIDE) {
            return ridePath;
        }
        if (routeMode == RouteMode.TRANSIT) {
            return busPath;
        }
        return drivePath;
    }

    public static String formatRouteSummary(Context context, Path path) {
        if (path == null) {
            return context.getString(R.string.route_summary_unavailable);
        }
        return context.getString(
                R.string.route_summary_format,
                formatDistanceMeters(path.getDistance()),
                formatDurationSeconds(path.getDuration())
        );
    }

    public static String getModeReadyText(Context context, RouteMode mode) {
        if (mode == RouteMode.WALK) {
            return context.getString(R.string.route_status_walk_ready);
        }
        if (mode == RouteMode.RIDE) {
            return context.getString(R.string.route_status_ride_ready);
        }
        if (mode == RouteMode.TRANSIT) {
            return context.getString(R.string.route_status_transit_ready);
        }
        return context.getString(R.string.route_status_drive_ready);
    }

    public static String getModeLoadingText(Context context, RouteMode mode) {
        if (mode == RouteMode.WALK) {
            return context.getString(R.string.route_status_walk_loading);
        }
        if (mode == RouteMode.RIDE) {
            return context.getString(R.string.route_status_ride_loading);
        }
        if (mode == RouteMode.TRANSIT) {
            return context.getString(R.string.route_status_transit_loading);
        }
        return context.getString(R.string.route_status_drive_loading);
    }

    public static String getEmptyRouteText(Context context, RouteMode mode) {
        if (mode == RouteMode.WALK) {
            return context.getString(R.string.route_status_walk_empty);
        }
        if (mode == RouteMode.RIDE) {
            return context.getString(R.string.route_status_ride_empty);
        }
        if (mode == RouteMode.TRANSIT) {
            return context.getString(R.string.route_status_transit_empty);
        }
        return context.getString(R.string.route_status_drive_empty);
    }

    public static String getRouteErrorText(Context context, int responseCode) {
        if (responseCode == 1008) {
            return context.getString(R.string.map_status_error_signature_mismatch);
        }
        if (responseCode == 1002) {
            return context.getString(R.string.map_status_error_invalid_key_or_route);
        }
        if (responseCode == 1902) {
            return context.getString(R.string.map_status_error_mode_temporarily_unavailable);
        }
        return context.getString(R.string.map_status_error_generic);
    }

    public static boolean isAmapAuthorizationError(int responseCode) {
        return responseCode == 1008;
    }

    public static List<LatLng> buildFallbackPolyline(LocationSnapshot origin, RecommendationItem item) {
        List<LatLng> points = new ArrayList<>();
        if (origin == null || item == null) {
            return points;
        }
        points.add(getOriginLatLng(origin));
        points.add(new LatLng(item.getLatitude(), item.getLongitude()));
        return points;
    }

    public static String formatFallbackRouteSummary(
            Context context,
            LocationSnapshot origin,
            RecommendationItem item,
            RouteMode routeMode
    ) {
        if (origin == null || item == null) {
            return context.getString(R.string.route_summary_unavailable);
        }
        float distanceMeters = estimateDirectDistanceMeters(origin, item);
        long durationSeconds = estimateFallbackDurationSeconds(item, routeMode, distanceMeters);
        return context.getString(
                R.string.route_summary_format,
                formatDistanceMeters(distanceMeters),
                formatDurationSeconds(durationSeconds)
        );
    }

    public static List<RouteStep> buildFallbackRouteSteps(
            LocationSnapshot origin,
            RecommendationItem item,
            RouteMode routeMode
    ) {
        List<RouteStep> steps = new ArrayList<>();
        if (origin == null || item == null) {
            return steps;
        }
        float distanceMeters = estimateDirectDistanceMeters(origin, item);
        long durationSeconds = estimateFallbackDurationSeconds(item, routeMode, distanceMeters);
        String destinationName = item.getName();
        String travelVerb = resolveModeVerb(routeMode);

        steps.add(new RouteStep(
                "1. Leave origin",
                "Start from " + getOriginLabel(origin) + " and head toward " + destinationName + "."
        ));
        steps.add(new RouteStep(
                "2. Follow the direct preview",
                travelVerb + " toward " + destinationName
                        + " for about "
                        + formatDistanceMeters(distanceMeters)
                        + " - "
                        + formatDurationSeconds(durationSeconds)
                        + "."
        ));
        steps.add(new RouteStep(
                "3. Arrive",
                "Arrive near " + destinationName + " and continue with local access at the destination."
        ));
        return steps;
    }

    public static List<LatLng> flattenPolyline(RouteMode routeMode, DrivePath drivePath, WalkPath walkPath, RidePath ridePath, BusPath busPath) {
        if (routeMode == RouteMode.WALK) {
            return flattenWalkPolyline(walkPath);
        }
        if (routeMode == RouteMode.RIDE) {
            return flattenRidePolyline(ridePath);
        }
        if (routeMode == RouteMode.TRANSIT) {
            return flattenBusPolyline(busPath);
        }
        return flattenDrivePolyline(drivePath);
    }

    public static List<RouteStep> toRouteSteps(RouteMode routeMode, DrivePath drivePath, WalkPath walkPath, RidePath ridePath, BusPath busPath) {
        if (routeMode == RouteMode.WALK) {
            return toWalkRouteSteps(walkPath);
        }
        if (routeMode == RouteMode.RIDE) {
            return toRideRouteSteps(ridePath);
        }
        if (routeMode == RouteMode.TRANSIT) {
            return toBusRouteSteps(busPath);
        }
        return toDriveRouteSteps(drivePath);
    }

    public static String formatDistanceMeters(float distanceMeters) {
        if (distanceMeters >= 1000f) {
            return String.format(Locale.US, "%.1f km", distanceMeters / 1000f);
        }
        return String.format(Locale.US, "%.0f m", distanceMeters);
    }

    public static String formatDurationSeconds(long durationSeconds) {
        long totalMinutes = Math.max(1, Math.round(durationSeconds / 60.0));
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        if (hours > 0) {
            return String.format(Locale.US, "%dh %02dm", hours, minutes);
        }
        return String.format(Locale.US, "%d min", totalMinutes);
    }

    public static String translateNavigationPromptToEnglish(String text) {
        if (isBlank(text)) {
            return "";
        }
        String trimmed = text.trim();
        if (trimmed.contains("\u5230\u8fbe\u76ee\u7684\u5730")) {
            return "You have arrived at the destination.";
        }
        if (trimmed.contains("\u91cd\u65b0\u89c4\u5212")) {
            return "Recalculating the route.";
        }
        if (trimmed.contains("\u524d\u65b9\u9053\u8def\u62e5\u5835")) {
            return "Traffic is heavy ahead.";
        }
        if (trimmed.contains("\u8bf7\u6ce8\u610f")) {
            return "Please pay attention to the road conditions ahead.";
        }

        String action = translateActionLabel(trimmed, trimmed, "Continue");
        String distancePrefix = extractDistancePrefix(trimmed);
        if (distancePrefix.isEmpty()) {
            return action + ".";
        }
        return distancePrefix + ", " + decapitalize(action) + ".";
    }

    private static String buildEnglishStepBody(RouteMode routeMode, float distance, float duration, String road) {
        StringBuilder builder = new StringBuilder();
        builder.append(resolveModeVerb(routeMode))
                .append(" for ")
                .append(formatDistanceMeters(distance))
                .append(" - ")
                .append(formatDurationSeconds(Math.round(duration)));

        String roadLabel = sanitizeRoadLabel(road);
        if (!isBlank(roadLabel)) {
            builder.append(" via ").append(roadLabel);
        }
        return builder.toString();
    }

    private static RouteStep buildTransitWalkStep(int index, RouteBusWalkItem walk, BusStep step) {
        String title = String.format(Locale.US, "%d. Walk transfer", index);
        String from = formatDoorway(step.getEntrance());
        String to = formatDoorway(step.getExit());
        StringBuilder body = new StringBuilder();
        if (!isBlank(from) && !isBlank(to) && !from.equals(to)) {
            body.append("Walk from ").append(from).append(" to ").append(to);
        } else if (!isBlank(to)) {
            body.append("Walk to ").append(to);
        } else if (!isBlank(from)) {
            body.append("Continue on foot from ").append(from);
        } else {
            body.append("Walk transfer");
        }
        body.append(" for ")
                .append(formatDistanceMeters(walk.getDistance()))
                .append(" - ")
                .append(formatDurationSeconds(walk.getDuration()));
        return new RouteStep(title, body.toString());
    }

    private static RouteStep buildTransitRailStep(int index, RouteRailwayItem railway, BusStep step) {
        String title = String.format(Locale.US, "%d. Take MTR", index);
        String lineLabel = describeTransitLine(railway.getTrip());
        if (isBlank(lineLabel)) {
            lineLabel = describeTransitLine(railway.getName());
        }
        String departure = formatRailwayStation(railway.getDeparturestop());
        String arrival = formatRailwayStation(railway.getArrivalstop());
        String entrance = formatDoorway(step.getEntrance());
        String exit = formatDoorway(step.getExit());
        int stopCount = railway.getViastops() == null ? 0 : railway.getViastops().size();

        StringBuilder body = new StringBuilder();
        body.append("Board ")
                .append(isBlank(lineLabel) ? "the rail service" : lineLabel);
        if (!isBlank(departure)) {
            body.append(" at ").append(departure);
        }
        if (!isBlank(arrival)) {
            body.append(" and leave at ").append(arrival);
        }
        if (stopCount > 0) {
            body.append(" (").append(stopCount).append(stopCount == 1 ? " stop" : " stops").append(")");
        }
        if (railway.getDistance() > 0) {
            body.append(" - ").append(formatDistanceMeters(railway.getDistance()));
        }
        if (!isBlank(entrance)) {
            body.append(". Enter via ").append(entrance);
        }
        if (!isBlank(exit)) {
            body.append(". Exit via ").append(exit);
        }
        return new RouteStep(title, body.toString());
    }

    private static RouteStep buildTransitLineStep(int index, RouteBusLineItem line, BusStep step) {
        String lineLabel = describeTransitLine(line.getBusLineName());
        String departure = formatBusStation(line.getDepartureBusStation());
        String arrival = formatBusStation(line.getArrivalBusStation());
        String entrance = formatDoorway(step.getEntrance());
        String exit = formatDoorway(step.getExit());

        StringBuilder body = new StringBuilder();
        body.append("Board ")
                .append(isBlank(lineLabel) ? "the transit service" : lineLabel);
        if (!isBlank(departure)) {
            body.append(" at ").append(departure);
        }
        if (!isBlank(arrival)) {
            body.append(" and leave at ").append(arrival);
        }
        if (line.getPassStationNum() > 0) {
            body.append(" (")
                    .append(line.getPassStationNum())
                    .append(line.getPassStationNum() == 1 ? " stop" : " stops")
                    .append(")");
        }
        body.append(" - ")
                .append(formatDistanceMeters(line.getDistance()))
                .append(" - ")
                .append(formatDurationSeconds(Math.round(line.getDuration())));
        if (!isBlank(entrance)) {
            body.append(". Enter via ").append(entrance);
        }
        if (!isBlank(exit)) {
            body.append(". Exit via ").append(exit);
        }
        return new RouteStep(
                String.format(Locale.US, "%d. %s", index, inferTransitLegTitle(lineLabel)),
                body.toString()
        );
    }

    private static void appendPolyline(List<LatLng> points, List<LatLonPoint> source) {
        if (source == null) {
            return;
        }
        for (LatLonPoint point : source) {
            points.add(new LatLng(point.getLatitude(), point.getLongitude()));
        }
    }

    private static String safe(String text, String fallback) {
        return isBlank(text) ? fallback : text.trim();
    }

    private static String translateActionLabel(String action, String instruction, String fallback) {
        String combined = safe(action, "") + " " + safe(instruction, "");
        for (Map.Entry<String, String> entry : ACTION_TRANSLATIONS.entrySet()) {
            if (combined.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return fallback;
    }

    private static String extractDistancePrefix(String text) {
        Matcher matcher = DISTANCE_PROMPT_PATTERN.matcher(text);
        if (!matcher.find()) {
            return "";
        }
        String amount = matcher.group(1);
        String unit = matcher.group(2);
        return "\u516c\u91cc".equals(unit) ? "In " + amount + " kilometers" : "In " + amount + " meters";
    }

    private static String resolveModeVerb(RouteMode routeMode) {
        if (routeMode == RouteMode.WALK) {
            return "Walk";
        }
        if (routeMode == RouteMode.RIDE) {
            return "Ride";
        }
        if (routeMode == RouteMode.TRANSIT) {
            return "Travel";
        }
        return "Drive";
    }

    private static float estimateDirectDistanceMeters(LocationSnapshot origin, RecommendationItem item) {
        double lat1 = Math.toRadians(origin.getLatitude());
        double lat2 = Math.toRadians(item.getLatitude());
        double deltaLat = lat2 - lat1;
        double deltaLng = Math.toRadians(item.getLongitude() - origin.getLongitude());
        double a = Math.sin(deltaLat / 2d) * Math.sin(deltaLat / 2d)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLng / 2d) * Math.sin(deltaLng / 2d);
        double c = 2d * Math.atan2(Math.sqrt(a), Math.sqrt(1d - a));
        return (float) (6371000d * c);
    }

    private static long estimateFallbackDurationSeconds(
            RecommendationItem item,
            RouteMode routeMode,
            float distanceMeters
    ) {
        int minutes;
        if (routeMode == RouteMode.WALK) {
            minutes = Math.max(1, Math.round(distanceMeters / 83.3f));
        } else if (routeMode == RouteMode.RIDE) {
            minutes = Math.max(1, Math.round(distanceMeters / 250f));
        } else if (routeMode == RouteMode.TRANSIT) {
            minutes = item.getEtaMinutes() != null
                    ? Math.max(2, item.getEtaMinutes())
                    : Math.max(2, Math.round(distanceMeters / 333.3f));
        } else {
            minutes = item.getEtaMinutes() != null
                    ? Math.max(1, item.getEtaMinutes())
                    : Math.max(1, Math.round(distanceMeters / 500f));
        }
        return minutes * 60L;
    }

    private static String sanitizeRoadLabel(String road) {
        if (isBlank(road)) {
            return "";
        }
        String trimmed = translateTransitLabel(road);
        int useful = 0;
        int latin = 0;
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                useful++;
                if (c < 128) {
                    latin++;
                }
            }
        }
        if (useful == 0 || latin < useful) {
            return "";
        }
        return trimmed;
    }

    private static String inferTransitLegTitle(String lineLabel) {
        String normalized = lineLabel == null ? "" : lineLabel.toLowerCase(Locale.US);
        if (normalized.contains("line")
                || normalized.contains("rail")
                || normalized.contains("express")
                || normalized.contains("mtr")) {
            return "Take MTR";
        }
        if (normalized.contains("bus")
                || normalized.matches(".*\\d.*")) {
            return "Take bus";
        }
        return "Take transit";
    }

    private static String describeTransitLine(String lineName) {
        String translated = translateTransitLabel(lineName);
        if (isBlank(translated)) {
            return "";
        }
        translated = translated.replaceAll("[-]{2,}", "-");
        translated = translated.replaceAll("\\s{2,}", " ").trim();
        return translated;
    }

    private static String formatBusStation(BusStationItem stationItem) {
        return stationItem == null ? "" : translateTransitLabel(stationItem.getBusStationName());
    }

    private static String formatRailwayStation(RailwayStationItem stationItem) {
        return stationItem == null ? "" : translateTransitLabel(stationItem.getName());
    }

    private static String formatDoorway(Doorway doorway) {
        return doorway == null ? "" : translateTransitLabel(doorway.getName());
    }

    private static String translateTransitLabel(String text) {
        if (isBlank(text)) {
            return "";
        }
        String translated = text.trim()
                .replace('（', '(')
                .replace('）', ')')
                .replace('【', '[')
                .replace('】', ']')
                .replace('－', '-')
                .replace('—', '-');
        for (Map.Entry<String, String> entry : TRANSIT_TEXT_TRANSLATIONS.entrySet()) {
            translated = translated.replace(entry.getKey(), entry.getValue());
        }
        translated = translated.replaceAll("(Station)([A-Za-z]{1,2}\\d*[A-Za-z]?)(?:\\u51fa\\u53e3|\\u53e3)", "$1 Exit $2");
        translated = translated.replaceAll("(Centre)([A-Za-z]{1,2}\\d*[A-Za-z]?)(?:\\u51fa\\u53e3|\\u53e3)", "$1 Exit $2");

        Matcher matcher = EXIT_PATTERN.matcher(translated);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, "Exit " + matcher.group(1));
        }
        matcher.appendTail(buffer);
        translated = buffer.toString();

        translated = translated.replace("地铁站", " Station");
        translated = translated.replace("地铁", "MTR");
        translated = translated.replace("车站", " Station");
        translated = translated.replace("总站", " Terminus");
        translated = translated.replace("换乘", " transfer");
        translated = translated.replace("往", " toward ");
        translated = translated.replace("开往", " toward ");
        translated = translated.replace("方向", " bound");
        translated = translated.replace("出入口", " Exit");
        translated = translated.replace("-Exit", " Exit");
        translated = translated.replace("- Exit", " Exit");
        translated = translated.replace("StationExit", "Station Exit");
        translated = translated.replace("Station Station", "Station");
        translated = translated.replace("Exit Exit", "Exit");
        translated = translated.replaceAll("[\\u4e00-\\u9fff]+", " ");
        translated = translated.replaceAll("\\s{2,}", " ").trim();
        translated = translated.replaceAll("\\s+\\)", ")");
        translated = translated.replaceAll("\\(\\s+", "(");
        translated = translated.replace(" ,", ",");
        translated = translated.replace(" - ", " - ");
        return translated.trim();
    }

    private static String decapitalize(String text) {
        if (isBlank(text)) {
            return "";
        }
        if (text.length() == 1) {
            return text.toLowerCase(Locale.US);
        }
        return Character.toLowerCase(text.charAt(0)) + text.substring(1);
    }

    private static Map<String, String> createActionTranslations() {
        Map<String, String> translations = new LinkedHashMap<>();
        translations.put("\u5230\u8fbe\u76ee\u7684\u5730", "Arrive at the destination");
        translations.put("\u8fdb\u5165\u73af\u5c9b", "Enter the roundabout");
        translations.put("\u9a76\u51fa\u73af\u5c9b", "Exit the roundabout");
        translations.put("\u79bb\u5f00\u73af\u5c9b", "Exit the roundabout");
        translations.put("\u8c03\u5934", "Make a U-turn");
        translations.put("\u53f3\u524d\u65b9", "Bear right");
        translations.put("\u5de6\u524d\u65b9", "Bear left");
        translations.put("\u53f3\u540e\u65b9", "Keep right");
        translations.put("\u5de6\u540e\u65b9", "Keep left");
        translations.put("\u9760\u53f3", "Keep right");
        translations.put("\u9760\u5de6", "Keep left");
        translations.put("\u53f3\u8f6c", "Turn right");
        translations.put("\u5de6\u8f6c", "Turn left");
        translations.put("\u76f4\u884c", "Go straight");
        translations.put("\u901a\u8fc7\u4eba\u884c\u6a2a\u9053", "Cross the road");
        translations.put("\u901a\u8fc7", "Pass through");
        return translations;
    }

    private static Map<String, String> createTransitTextTranslations() {
        Map<String, String> translations = new LinkedHashMap<>();
        translations.put("香港大学地铁站", "HKU Station");
        translations.put("香港大学站", "HKU Station");
        translations.put("西营盘地铁站", "Sai Ying Pun Station");
        translations.put("西营盘站", "Sai Ying Pun Station");
        translations.put("上环站", "Sheung Wan Station");
        translations.put("中环站", "Central Station");
        translations.put("金钟站", "Admiralty Station");
        translations.put("湾仔站", "Wan Chai Station");
        translations.put("会展站", "Exhibition Centre Station");
        translations.put("尖沙咀站", "Tsim Sha Tsui Station");
        translations.put("尖东站", "East Tsim Sha Tsui Station");
        translations.put("柯士甸站", "Austin Station");
        translations.put("九龙站", "Kowloon Station");
        translations.put("香港站", "Hong Kong Station");
        translations.put("启德站", "Kai Tak Station");
        translations.put("宋皇台站", "Sung Wong Toi Station");
        translations.put("东涌线", "Tung Chung Line");
        translations.put("港岛线", "Island Line");
        translations.put("荃湾线", "Tsuen Wan Line");
        translations.put("观塘线", "Kwun Tong Line");
        translations.put("东铁线", "East Rail Line");
        translations.put("屯马线", "Tuen Ma Line");
        translations.put("机场快线", "Airport Express");
        translations.put("南港岛线", "South Island Line");
        translations.put("将军澳线", "Tseung Kwan O Line");
        translations.put("迪士尼线", "Disneyland Resort Line");
        translations.put("德辅道西", "Des Voeux Road West");
        translations.put("皇后大道西", "Queen's Road West");
        translations.put("干诺道西", "Connaught Road West");
        translations.put("般咸道", "Bonham Road");
        translations.put("薄扶林道", "Pok Fu Lam Road");
        translations.put("荷李活道", "Hollywood Road");
        translations.put("奥卑利街", "Old Bailey Street");
        translations.put("歌赋街", "Gough Street");
        translations.put("士丹顿街", "Staunton Street");
        translations.put("卑路乍街", "Belcher's Street");
        translations.put("皇后大道中", "Queen's Road Central");
        translations.put("中环", "Central");
        translations.put("香港大学", "HKU");
        translations.put("西营盘", "Sai Ying Pun");
        translations.put("会展", "Exhibition Centre");
        translations.put("湾仔", "Wan Chai");
        translations.put("金钟", "Admiralty");
        translations.put("上环", "Sheung Wan");
        translations.put("尖沙咀", "Tsim Sha Tsui");
        translations.put("尖东", "East Tsim Sha Tsui");
        translations.put("柯士甸", "Austin");
        translations.put("九龙", "Kowloon");
        translations.put("启德", "Kai Tak");
        translations.put("宋皇台", "Sung Wong Toi");
        return translations;
    }

    private static boolean isBlank(String text) {
        return text == null || text.isEmpty();
    }
}
