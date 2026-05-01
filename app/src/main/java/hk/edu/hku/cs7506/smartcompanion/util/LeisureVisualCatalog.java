package hk.edu.hku.cs7506.smartcompanion.util;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class LeisureVisualCatalog {
    private static final String VISUALS_SOURCE = "Local bundled visual";
    private static final Map<String, VisualEntry> NAME_MATCHES = createNameMatches();
    private static final Map<String, VisualEntry> VENUE_MATCHES = createVenueMatches();

    private LeisureVisualCatalog() {
    }

    public static VisualEntry forNameOrVenue(String name, String venue) {
        VisualEntry directMatch = findMatch(NAME_MATCHES, name);
        if (directMatch != null) {
            return directMatch;
        }
        return findMatch(VENUE_MATCHES, venue);
    }

    private static VisualEntry findMatch(Map<String, VisualEntry> catalog, String rawValue) {
        String value = normalize(rawValue);
        if (value.isEmpty()) {
            return null;
        }
        for (Map.Entry<String, VisualEntry> entry : catalog.entrySet()) {
            if (value.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.US).trim();
    }

    private static Map<String, VisualEntry> createNameMatches() {
        Map<String, VisualEntry> visuals = new LinkedHashMap<>();
        visuals.put("hong kong museum of art", new VisualEntry(
                "file:///android_res/drawable/leisure_rcmd.png",
                VISUALS_SOURCE
        ));
        visuals.put("hong kong museum of history", new VisualEntry(
                "file:///android_res/drawable/leisure_rcmd.png",
                VISUALS_SOURCE
        ));
        visuals.put("hong kong science museum", new VisualEntry(
                "file:///android_res/drawable/leisure_rcmd.png",
                VISUALS_SOURCE
        ));
        visuals.put("hong kong space museum", new VisualEntry(
                "file:///android_res/drawable/leisure_rcmd.png",
                VISUALS_SOURCE
        ));
        visuals.put("hong kong heritage museum", new VisualEntry(
                "file:///android_res/drawable/leisure_rcmd.png",
                VISUALS_SOURCE
        ));
        visuals.put("flagstaff house museum of tea ware", new VisualEntry(
                "file:///android_res/drawable/leisure_rcmd.png",
                VISUALS_SOURCE
        ));
        visuals.put("dr sun yat-sen museum", new VisualEntry(
                "file:///android_res/drawable/leisure_rcmd.png",
                VISUALS_SOURCE
        ));
        visuals.put("hong kong visual arts centre", new VisualEntry(
                "file:///android_res/drawable/leisure_rcmd.png",
                VISUALS_SOURCE
        ));
        visuals.put("hong kong film archive", new VisualEntry(
                "file:///android_res/drawable/leisure_rcmd.png",
                VISUALS_SOURCE
        ));
        visuals.put("tai kwun", new VisualEntry(
                "file:///android_res/drawable/leisure_rcmd.png",
                VISUALS_SOURCE
        ));
        return visuals;
    }

    private static Map<String, VisualEntry> createVenueMatches() {
        Map<String, VisualEntry> visuals = new LinkedHashMap<>();
        visuals.put("west kowloon cultural district", new VisualEntry(
                "file:///android_res/drawable/leisure_rcmd.png",
                VISUALS_SOURCE
        ));
        visuals.put("central harbourfront", new VisualEntry(
                "file:///android_res/drawable/leisure_rcmd.png",
                VISUALS_SOURCE
        ));
        visuals.put("victoria harbour", new VisualEntry(
                "file:///android_res/drawable/leisure_rcmd.png",
                VISUALS_SOURCE
        ));
        visuals.put("hong kong convention and exhibition centre", new VisualEntry(
                "file:///android_res/drawable/leisure_rcmd.png",
                VISUALS_SOURCE
        ));
        visuals.put("kai tak", new VisualEntry(
                "file:///android_res/drawable/leisure_rcmd.png",
                VISUALS_SOURCE
        ));
        visuals.put("harbour city", new VisualEntry(
                "file:///android_res/drawable/leisure_rcmd.png",
                VISUALS_SOURCE
        ));
        return visuals;
    }

    public static final class VisualEntry {
        private final String imageUrl;
        private final String attribution;

        public VisualEntry(String imageUrl, String attribution) {
            this.imageUrl = imageUrl;
            this.attribution = attribution;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public String getAttribution() {
            return attribution;
        }
    }
}
