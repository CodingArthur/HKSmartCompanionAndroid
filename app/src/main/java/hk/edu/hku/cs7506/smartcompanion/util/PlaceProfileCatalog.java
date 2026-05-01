package hk.edu.hku.cs7506.smartcompanion.util;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class PlaceProfileCatalog {
    private static String commonsImage(String filename) {
        return "https://commons.wikimedia.org/wiki/Special:FilePath/" + filename;
    }

    private static final String QUEEN_MARY_HOSPITAL_HERO =
            commonsImage("Queen_Mary_Hospital_25-11-2023.jpg");
    private static final String QUEEN_ELIZABETH_HOSPITAL_HERO =
            commonsImage("HK_King%27s_Park_%E4%BC%8A%E5%88%A9%E6%B2%99%E4%BC%AF%E9%86%AB%E9%99%A2_Queen_Elizabeth_Hospital_outdoor_entrance_Jan-2014.JPG");
    private static final String PRINCE_OF_WALES_HOSPITAL_HERO =
            commonsImage("Prince_of_Wales_Hospital_Overview_201106.jpg");
    private static final String PAMELA_YOUDE_EASTERN_HOSPITAL_HERO =
            commonsImage("Pamela_Youde_Nethersole_Eastern_Hospital_%28A%26E%29.JPG");
    private static final String PRINCESS_MARGARET_HOSPITAL_HERO =
            commonsImage("Princess_Margaret_Hospital_202101.jpg");
    private static final String NORTH_DISTRICT_HOSPITAL_HERO =
            commonsImage("North_District_Hospital_New_Acute_Block_-_February_2026.jpg");
    private static final String TSEUNG_KWAN_O_HOSPITAL_HERO =
            commonsImage("Tseung_Kwan_O_Hospital.jpg");
    private static final String TUEN_MUN_HOSPITAL_HERO =
            commonsImage("Tuen_Mun_Hospital_%28full_view%29.jpg");
    private static final String UNITED_CHRISTIAN_HOSPITAL_HERO =
            commonsImage("United_Christian_Hospital_2021.jpg");
    private static final String ST_JOHN_HOSPITAL_HERO =
            commonsImage("St._John_Hospital%2C_Cheung_Chau_2026.jpg");

    private static final String PARKING_HERO_CENTRAL =
            commonsImage("HK_Central_Edinburgh_Place_Star_Ferry_Carpark_Building_view_Jardine_House_facade_May-2012.JPG");
    private static final String PARKING_HERO_KAI_TAK =
            commonsImage("Kai_Tak_Cruise_Terminal_carpark_09-04-2016%281%29.jpg");
    private static final String PARKING_HERO_DISTRICT =
            commonsImage("HK_Tsuen_Wan_Town_Hall_%E8%8D%83%E7%81%A3%E5%A4%A7%E6%9C%83%E5%A0%82_outdoor_carpark_red_flagpoles_view_Vision_City_facade_May-2013.JPG");
    private static final String WIKIMEDIA_ATTRIBUTION = "Hero image via Wikimedia Commons";

    private static final Map<String, PlaceProfile> HOSPITAL_PROFILES = createHospitalProfiles();

    private PlaceProfileCatalog() {
    }

    public static PlaceProfile forHospital(String hospitalName) {
        return HOSPITAL_PROFILES.get(normalizeKey(hospitalName));
    }

    public static PlaceProfile createParkingFallback(String district, String detailsUrl, String addressLine, String contactPhone) {
        String heroImage = PARKING_HERO_DISTRICT;
        if (district != null) {
            String loweredDistrict = district.toLowerCase(Locale.US);
            if (loweredDistrict.contains("central")
                    || loweredDistrict.contains("western")
                    || loweredDistrict.contains("wan chai")) {
                heroImage = PARKING_HERO_CENTRAL;
            } else if (loweredDistrict.contains("kowloon city")
                    || loweredDistrict.contains("kai tak")) {
                heroImage = PARKING_HERO_KAI_TAK;
            }
        }
        String safeDistrict = (district == null || district.trim().isEmpty()) ? "Hong Kong" : district.trim();
        return new PlaceProfile(
                "Live parking option",
                "Public parking recommendation - " + safeDistrict,
                detailsUrl,
                heroImage,
                WIKIMEDIA_ATTRIBUTION,
                addressLine,
                contactPhone
        );
    }

    private static Map<String, PlaceProfile> createHospitalProfiles() {
        Map<String, PlaceProfile> profiles = new HashMap<>();
        profiles.put(normalizeKey("Queen Mary Hospital"), new PlaceProfile(
                "Emergency unit",
                "Hospital Authority emergency unit - Hong Kong West",
                "https://www.ha.org.hk/visitor/ha_visitor_index.asp?Content_ID=100220&Lang=ENG",
                QUEEN_MARY_HOSPITAL_HERO,
                WIKIMEDIA_ATTRIBUTION,
                "102 Pok Fu Lam Road, Pok Fu Lam, Hong Kong",
                null
        ));
        profiles.put(normalizeKey("Queen Elizabeth Hospital"), new PlaceProfile(
                "Emergency unit",
                "Hospital Authority emergency unit - Kowloon Central",
                "https://www.ha.org.hk/visitor/ha_visitor_index.asp?Content_ID=100202&Lang=ENG",
                QUEEN_ELIZABETH_HOSPITAL_HERO,
                WIKIMEDIA_ATTRIBUTION,
                "30 Gascoigne Road, Jordan, Kowloon",
                null
        ));
        profiles.put(normalizeKey("Prince of Wales Hospital"), new PlaceProfile(
                "Emergency unit",
                "Hospital Authority emergency unit - New Territories East",
                "https://www3.ha.org.hk/pwh/en/",
                PRINCE_OF_WALES_HOSPITAL_HERO,
                WIKIMEDIA_ATTRIBUTION,
                "30-32 Ngan Shing Street, Sha Tin, New Territories",
                null
        ));
        profiles.put(normalizeKey("Pamela Youde Nethersole Eastern Hospital"), new PlaceProfile(
                "Emergency unit",
                "Hospital Authority emergency unit - Hong Kong East",
                "https://www.ha.org.hk/visitor/ha_visitor_index.asp?Content_ID=100214&Lang=ENG",
                PAMELA_YOUDE_EASTERN_HOSPITAL_HERO,
                WIKIMEDIA_ATTRIBUTION,
                "3 Lok Man Road, Chai Wan, Hong Kong",
                null
        ));
        profiles.put(normalizeKey("Princess Margaret Hospital"), new PlaceProfile(
                "Emergency unit",
                "Hospital Authority emergency unit - Kowloon West",
                "https://www.ha.org.hk/visitor/ha_visitor_index.asp?Content_ID=100199&Lang=ENG",
                PRINCESS_MARGARET_HOSPITAL_HERO,
                WIKIMEDIA_ATTRIBUTION,
                "2-10 Princess Margaret Hospital Road, Lai Chi Kok, Kowloon",
                null
        ));
        profiles.put(normalizeKey("North District Hospital"), new PlaceProfile(
                "Emergency unit",
                "Hospital Authority emergency unit - North District",
                "https://www3.ha.org.hk/ndh/en/",
                NORTH_DISTRICT_HOSPITAL_HERO,
                WIKIMEDIA_ATTRIBUTION,
                "9 Po Kin Road, Sheung Shui, New Territories",
                null
        ));
        profiles.put(normalizeKey("Tseung Kwan O Hospital"), new PlaceProfile(
                "Emergency unit",
                "Hospital Authority emergency unit - Tseung Kwan O",
                "https://www.ha.org.hk/visitor/ha_visitor_index.asp?Content_ID=100217&Lang=ENG",
                TSEUNG_KWAN_O_HOSPITAL_HERO,
                WIKIMEDIA_ATTRIBUTION,
                "2 Po Ning Lane, Hang Hau, Tseung Kwan O",
                null
        ));
        profiles.put(normalizeKey("Tuen Mun Hospital"), new PlaceProfile(
                "Emergency unit",
                "Hospital Authority emergency unit - Tuen Mun",
                "https://www.ha.org.hk/visitor/ha_visitor_index.asp?Content_ID=100216&Lang=ENG",
                TUEN_MUN_HOSPITAL_HERO,
                WIKIMEDIA_ATTRIBUTION,
                "23 Tsing Chung Koon Road, Tuen Mun, New Territories",
                null
        ));
        profiles.put(normalizeKey("United Christian Hospital"), new PlaceProfile(
                "Emergency unit",
                "Hospital Authority emergency unit - Kwun Tong",
                "https://www.ha.org.hk/visitor/ha_visitor_index.asp?Content_ID=100212&Lang=ENG",
                UNITED_CHRISTIAN_HOSPITAL_HERO,
                WIKIMEDIA_ATTRIBUTION,
                "130 Hip Wo Street, Kwun Tong, Kowloon",
                null
        ));
        profiles.put(normalizeKey("St John Hospital"), new PlaceProfile(
                "Emergency unit",
                "Hospital Authority emergency unit - Cheung Chau",
                "https://www.ha.org.hk/visitor/ha_visitor_index.asp?Content_ID=100211&Lang=ENG",
                ST_JOHN_HOSPITAL_HERO,
                WIKIMEDIA_ATTRIBUTION,
                "Cheung Chau Hospital Road, Cheung Chau",
                null
        ));
        return profiles;
    }

    private static String normalizeKey(String input) {
        return input == null ? "" : input.trim().toLowerCase(Locale.US);
    }

    public static final class PlaceProfile {
        private final String contentTag;
        private final String metadataLine;
        private final String detailsUrl;
        private final String imageUrl;
        private final String imageAttribution;
        private final String addressLine;
        private final String contactPhone;

        public PlaceProfile(
                String contentTag,
                String metadataLine,
                String detailsUrl,
                String imageUrl,
                String imageAttribution,
                String addressLine,
                String contactPhone
        ) {
            this.contentTag = contentTag;
            this.metadataLine = metadataLine;
            this.detailsUrl = detailsUrl;
            this.imageUrl = imageUrl;
            this.imageAttribution = imageAttribution;
            this.addressLine = addressLine;
            this.contactPhone = contactPhone;
        }

        public String getContentTag() {
            return contentTag;
        }

        public String getMetadataLine() {
            return metadataLine;
        }

        public String getDetailsUrl() {
            return detailsUrl;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public String getImageAttribution() {
            return imageAttribution;
        }

        public String getAddressLine() {
            return addressLine;
        }

        public String getContactPhone() {
            return contactPhone;
        }
    }
}
