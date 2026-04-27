package hk.edu.hku.cs7506.smartcompanion.util;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class PlaceProfileCatalog {
    private static final String HOSPITAL_HERO_PRIMARY =
            "https://images.unsplash.com/photo-1586773860418-d37222d8fce3?auto=format&fit=crop&w=1200&q=80";
    private static final String HOSPITAL_HERO_SECONDARY =
            "https://images.unsplash.com/photo-1519494026892-80bbd2d6fd0d?auto=format&fit=crop&w=1200&q=80";
    private static final String PARKING_HERO_CENTRAL =
            "https://images.unsplash.com/photo-1506521781263-d8422e82f27a?auto=format&fit=crop&w=1200&q=80";
    private static final String PARKING_HERO_KAI_TAK =
            "https://images.unsplash.com/photo-1503376780353-7e6692767b70?auto=format&fit=crop&w=1200&q=80";
    private static final String PARKING_HERO_DISTRICT =
            "https://images.unsplash.com/photo-1489824904134-891ab64532f1?auto=format&fit=crop&w=1200&q=80";
    private static final String UNSPLASH_ATTRIBUTION = "Hero image via Unsplash";

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
            } else if (loweredDistrict.contains("kowloon city")) {
                heroImage = PARKING_HERO_KAI_TAK;
            }
        }
        String safeDistrict = (district == null || district.trim().isEmpty()) ? "Hong Kong" : district.trim();
        return new PlaceProfile(
                "Live parking option",
                "Public parking recommendation - " + safeDistrict,
                detailsUrl,
                heroImage,
                UNSPLASH_ATTRIBUTION,
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
                HOSPITAL_HERO_PRIMARY,
                UNSPLASH_ATTRIBUTION,
                "102 Pok Fu Lam Road, Pok Fu Lam, Hong Kong",
                null
        ));
        profiles.put(normalizeKey("Queen Elizabeth Hospital"), new PlaceProfile(
                "Emergency unit",
                "Hospital Authority emergency unit - Kowloon Central",
                "https://www.ha.org.hk/visitor/ha_visitor_index.asp?Content_ID=100202&Lang=ENG",
                HOSPITAL_HERO_PRIMARY,
                UNSPLASH_ATTRIBUTION,
                "30 Gascoigne Road, Jordan, Kowloon",
                null
        ));
        profiles.put(normalizeKey("Prince of Wales Hospital"), new PlaceProfile(
                "Emergency unit",
                "Hospital Authority emergency unit - New Territories East",
                "https://www3.ha.org.hk/pwh/en/",
                HOSPITAL_HERO_SECONDARY,
                UNSPLASH_ATTRIBUTION,
                "30-32 Ngan Shing Street, Sha Tin, New Territories",
                null
        ));
        profiles.put(normalizeKey("Pamela Youde Nethersole Eastern Hospital"), new PlaceProfile(
                "Emergency unit",
                "Hospital Authority emergency unit - Hong Kong East",
                "https://www.ha.org.hk/visitor/ha_visitor_index.asp?Content_ID=100214&Lang=ENG",
                HOSPITAL_HERO_PRIMARY,
                UNSPLASH_ATTRIBUTION,
                "3 Lok Man Road, Chai Wan, Hong Kong",
                null
        ));
        profiles.put(normalizeKey("Princess Margaret Hospital"), new PlaceProfile(
                "Emergency unit",
                "Hospital Authority emergency unit - Kowloon West",
                "https://www.ha.org.hk/visitor/ha_visitor_index.asp?Content_ID=100199&Lang=ENG",
                HOSPITAL_HERO_SECONDARY,
                UNSPLASH_ATTRIBUTION,
                "2-10 Princess Margaret Hospital Road, Lai Chi Kok, Kowloon",
                null
        ));
        profiles.put(normalizeKey("North District Hospital"), new PlaceProfile(
                "Emergency unit",
                "Hospital Authority emergency unit - North District",
                "https://www3.ha.org.hk/ndh/en/",
                HOSPITAL_HERO_PRIMARY,
                UNSPLASH_ATTRIBUTION,
                "9 Po Kin Road, Sheung Shui, New Territories",
                null
        ));
        profiles.put(normalizeKey("Tseung Kwan O Hospital"), new PlaceProfile(
                "Emergency unit",
                "Hospital Authority emergency unit - Tseung Kwan O",
                "https://www.ha.org.hk/visitor/ha_visitor_index.asp?Content_ID=100217&Lang=ENG",
                HOSPITAL_HERO_SECONDARY,
                UNSPLASH_ATTRIBUTION,
                "2 Po Ning Lane, Hang Hau, Tseung Kwan O",
                null
        ));
        profiles.put(normalizeKey("Tuen Mun Hospital"), new PlaceProfile(
                "Emergency unit",
                "Hospital Authority emergency unit - Tuen Mun",
                "https://www.ha.org.hk/visitor/ha_visitor_index.asp?Content_ID=100216&Lang=ENG",
                HOSPITAL_HERO_PRIMARY,
                UNSPLASH_ATTRIBUTION,
                "23 Tsing Chung Koon Road, Tuen Mun, New Territories",
                null
        ));
        profiles.put(normalizeKey("United Christian Hospital"), new PlaceProfile(
                "Emergency unit",
                "Hospital Authority emergency unit - Kwun Tong",
                "https://www.ha.org.hk/visitor/ha_visitor_index.asp?Content_ID=100212&Lang=ENG",
                HOSPITAL_HERO_SECONDARY,
                UNSPLASH_ATTRIBUTION,
                "130 Hip Wo Street, Kwun Tong, Kowloon",
                null
        ));
        profiles.put(normalizeKey("St John Hospital"), new PlaceProfile(
                "Emergency unit",
                "Hospital Authority emergency unit - Cheung Chau",
                "https://www.ha.org.hk/visitor/ha_visitor_index.asp?Content_ID=100211&Lang=ENG",
                HOSPITAL_HERO_PRIMARY,
                UNSPLASH_ATTRIBUTION,
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
