package hk.edu.hku.cs7506.smartcompanion.util;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class LeisureVisualCatalog {
    private static final String VISUALS_SOURCE = "Visuals from Wikimedia Commons";
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
                "https://upload.wikimedia.org/wikipedia/commons/c/c8/Hong_Kong_Museum_of_Art_renovation_site_201908.jpg",
                VISUALS_SOURCE
        ));
        visuals.put("hong kong museum of history", new VisualEntry(
                "https://upload.wikimedia.org/wikipedia/commons/6/67/HongKongMuseumOfHistory.png",
                VISUALS_SOURCE
        ));
        visuals.put("hong kong science museum", new VisualEntry(
                "https://upload.wikimedia.org/wikipedia/commons/f/ff/HKScienceMuseumview.jpg",
                VISUALS_SOURCE
        ));
        visuals.put("hong kong space museum", new VisualEntry(
                "https://upload.wikimedia.org/wikipedia/commons/2/26/Hong_Kong_Space_Museum.jpg",
                VISUALS_SOURCE
        ));
        visuals.put("hong kong heritage museum", new VisualEntry(
                "https://upload.wikimedia.org/wikipedia/commons/5/5f/Hong_Kong_Heritage_Museum_201305.jpg",
                VISUALS_SOURCE
        ));
        visuals.put("flagstaff house museum of tea ware", new VisualEntry(
                "https://upload.wikimedia.org/wikipedia/commons/6/67/%E8%88%8A%E4%B8%89%E8%BB%8D%E5%8F%B8%E4%BB%A4%E5%AE%98%E9%82%B8.jpg",
                VISUALS_SOURCE
        ));
        visuals.put("dr sun yat-sen museum", new VisualEntry(
                "https://upload.wikimedia.org/wikipedia/commons/4/4b/Dr.SunYat-senMuseum.JPG",
                VISUALS_SOURCE
        ));
        visuals.put("hong kong visual arts centre", new VisualEntry(
                "https://upload.wikimedia.org/wikipedia/commons/9/9c/Hong_Kong_Visual_Arts_Center.JPG",
                VISUALS_SOURCE
        ));
        visuals.put("hong kong film archive", new VisualEntry(
                "https://upload.wikimedia.org/wikipedia/commons/2/26/Hong_Kong_Film_Archive02.jpg",
                VISUALS_SOURCE
        ));
        visuals.put("tai kwun", new VisualEntry(
                "https://upload.wikimedia.org/wikipedia/commons/thumb/3/3b/Tai_Kwun_logo.svg/330px-Tai_Kwun_logo.svg.png",
                VISUALS_SOURCE
        ));
        return visuals;
    }

    private static Map<String, VisualEntry> createVenueMatches() {
        Map<String, VisualEntry> visuals = new LinkedHashMap<>();
        visuals.put("west kowloon cultural district", new VisualEntry(
                "https://upload.wikimedia.org/wikipedia/commons/e/e8/M%2B%2C_West_Kowloon%2C_Hong_Kong.jpg",
                VISUALS_SOURCE
        ));
        visuals.put("central harbourfront", new VisualEntry(
                "https://upload.wikimedia.org/wikipedia/commons/f/f5/Central_Harbourfront_Event_Space%2C_Hong_Kong.jpg",
                VISUALS_SOURCE
        ));
        visuals.put("victoria harbour", new VisualEntry(
                "https://upload.wikimedia.org/wikipedia/commons/f/f5/Central_Harbourfront_Event_Space%2C_Hong_Kong.jpg",
                VISUALS_SOURCE
        ));
        visuals.put("hong kong convention and exhibition centre", new VisualEntry(
                "https://upload.wikimedia.org/wikipedia/commons/4/4d/HKCEC.jpg",
                VISUALS_SOURCE
        ));
        visuals.put("kai tak", new VisualEntry(
                "https://upload.wikimedia.org/wikipedia/commons/d/db/2024%E5%B9%B410%E6%9C%88-%E5%95%9F%E5%BE%B7%E9%AB%94%E8%82%B2%E5%9C%92.jpg",
                VISUALS_SOURCE
        ));
        visuals.put("harbour city", new VisualEntry(
                "https://upload.wikimedia.org/wikipedia/commons/c/c8/Hong_Kong_Museum_of_Art_renovation_site_201908.jpg",
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
