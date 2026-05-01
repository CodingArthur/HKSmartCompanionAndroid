package hk.edu.hku.cs7506.smartcompanion.util;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class LeisureVisualCatalog {
    private static final String VISUALS_SOURCE = "Visuals from Wikimedia Commons";
    private static final Map<String, VisualEntry> NAME_MATCHES = createNameMatches();
    private static final Map<String, VisualEntry> VENUE_MATCHES = createVenueMatches();

    private static String commonsImage(String filename) {
        return "https://commons.wikimedia.org/wiki/Special:FilePath/" + filename;
    }

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
                commonsImage("Hong_Kong_Museum_of_Art_20250716.jpg"),
                VISUALS_SOURCE
        ));
        visuals.put("hong kong museum of history", new VisualEntry(
                commonsImage("HK_Hong_Kong_Museum_of_History.JPG"),
                VISUALS_SOURCE
        ));
        visuals.put("hong kong science museum", new VisualEntry(
                commonsImage("HKScienceMuseumview.jpg"),
                VISUALS_SOURCE
        ));
        visuals.put("hong kong space museum", new VisualEntry(
                commonsImage("Hong_Kong_Space_Museum.jpg"),
                VISUALS_SOURCE
        ));
        visuals.put("hong kong heritage museum", new VisualEntry(
                commonsImage("Hong_Kong_Heritage_Museum_201305.jpg"),
                VISUALS_SOURCE
        ));
        visuals.put("flagstaff house museum of tea ware", new VisualEntry(
                commonsImage("Flagstaff_House%2C_Museum_of_Tea_Ware.JPG"),
                VISUALS_SOURCE
        ));
        visuals.put("dr sun yat-sen museum", new VisualEntry(
                commonsImage("Dr_Sun_Yat-sen_Museum.jpg"),
                VISUALS_SOURCE
        ));
        visuals.put("hong kong visual arts centre", new VisualEntry(
                commonsImage("Hong_Kong_Visual_Arts_Centre.jpg"),
                VISUALS_SOURCE
        ));
        visuals.put("hong kong film archive", new VisualEntry(
                commonsImage("Hong_Kong_Film_Archive01.jpg"),
                VISUALS_SOURCE
        ));
        visuals.put("tai kwun", new VisualEntry(
                commonsImage("Tai_Kwun_Parade_Ground_201806.jpg"),
                VISUALS_SOURCE
        ));
        return visuals;
    }

    private static Map<String, VisualEntry> createVenueMatches() {
        Map<String, VisualEntry> visuals = new LinkedHashMap<>();
        visuals.put("west kowloon cultural district", new VisualEntry(
                commonsImage("M%2B%2C_West_Kowloon_Cultural_District_%28Hong_Kong%29.jpg"),
                VISUALS_SOURCE
        ));
        visuals.put("central harbourfront", new VisualEntry(
                commonsImage("Central_Harbourfront_Event_Space%2C_Hong_Kong.jpg"),
                VISUALS_SOURCE
        ));
        visuals.put("victoria harbour", new VisualEntry(
                commonsImage("Victoria_Harbour_%28from_Lugard_Road%29.jpg"),
                VISUALS_SOURCE
        ));
        visuals.put("hong kong convention and exhibition centre", new VisualEntry(
                commonsImage("HKCEC.jpg"),
                VISUALS_SOURCE
        ));
        visuals.put("kai tak", new VisualEntry(
                commonsImage("Kai_Tak_Sports_Park_2025.jpg"),
                VISUALS_SOURCE
        ));
        visuals.put("harbour city", new VisualEntry(
                commonsImage("Harbour_City_Front.JPG"),
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
