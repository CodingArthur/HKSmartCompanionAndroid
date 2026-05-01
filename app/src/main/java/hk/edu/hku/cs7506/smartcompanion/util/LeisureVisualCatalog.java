package hk.edu.hku.cs7506.smartcompanion.util;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class LeisureVisualCatalog {
    private static final String VISUALS_SOURCE = "Local drawable asset";
    private static final String RESOURCE_PREFIX = "drawable://";
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

    private static String drawable(String fileName) {
        return RESOURCE_PREFIX + fileName;
    }

    private static Map<String, VisualEntry> createNameMatches() {
        Map<String, VisualEntry> visuals = new LinkedHashMap<>();
        visuals.put("hong kong museum of art", new VisualEntry(drawable("leisure_hk_museum_of_art"), VISUALS_SOURCE));
        visuals.put("hong kong museum of history", new VisualEntry(drawable("leisure_hk_museum_of_history"), VISUALS_SOURCE));
        visuals.put("hong kong science museum", new VisualEntry(drawable("leisure_hk_science_museum"), VISUALS_SOURCE));
        visuals.put("hong kong space museum", new VisualEntry(drawable("leisure_hk_space_museum"), VISUALS_SOURCE));
        visuals.put("hong kong heritage museum", new VisualEntry(drawable("leisure_hk_heritage_museum"), VISUALS_SOURCE));
        visuals.put("flagstaff house museum of tea ware", new VisualEntry(drawable("leisure_flagstaff_house"), VISUALS_SOURCE));
        visuals.put("dr sun yat-sen museum", new VisualEntry(drawable("leisure_dr_sun_yat_sen_museum"), VISUALS_SOURCE));
        visuals.put("hong kong visual arts centre", new VisualEntry(drawable("leisure_hk_visual_arts_centre"), VISUALS_SOURCE));
        visuals.put("hong kong film archive", new VisualEntry(drawable("leisure_hk_film_archive"), VISUALS_SOURCE));
        visuals.put("tai kwun", new VisualEntry(drawable("leisure_tai_kwun"), VISUALS_SOURCE));
        return visuals;
    }

    private static Map<String, VisualEntry> createVenueMatches() {
        Map<String, VisualEntry> visuals = new LinkedHashMap<>();
        visuals.put("west kowloon cultural district", new VisualEntry(drawable("leisure_west_kowloon"), VISUALS_SOURCE));
        visuals.put("central harbourfront", new VisualEntry(drawable("leisure_central_harbourfront"), VISUALS_SOURCE));
        visuals.put("victoria harbour", new VisualEntry(drawable("leisure_victoria_harbour"), VISUALS_SOURCE));
        visuals.put("hong kong convention and exhibition centre", new VisualEntry(drawable("leisure_hkcec"), VISUALS_SOURCE));
        visuals.put("kai tak", new VisualEntry(drawable("leisure_kai_tak"), VISUALS_SOURCE));
        visuals.put("harbour city", new VisualEntry(drawable("leisure_harbour_city"), VISUALS_SOURCE));
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
