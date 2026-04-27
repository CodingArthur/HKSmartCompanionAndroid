package hk.edu.hku.cs7506.smartcompanion.data.model;

public enum CommuteCorridor {
    CENTRAL_TO_SHA_TIN(
            "central_to_sha_tin",
            "Central to Sha Tin",
            22.2869,
            114.1531,
            22.3826,
            114.1887
    ),
    CENTRAL_TO_STANLEY(
            "central_to_stanley",
            "Central to Stanley",
            22.2869,
            114.1531,
            22.2188,
            114.2147
    ),
    EXHIBITION_TO_LAI_CHI_KOK(
            "exhibition_to_lai_chi_kok",
            "Exhibition Centre to Lai Chi Kok",
            22.2817,
            114.1758,
            22.3373,
            114.1481
    ),
    KAI_TAK_TO_AIRPORT(
            "kai_tak_to_airport",
            "Kai Tak to Airport",
            22.3230,
            114.1930,
            22.3080,
            113.9185
    ),
    WESTERN_TO_EAST_HARBOUR(
            "western_to_east_harbour",
            "Western District to East Harbourfront",
            22.2850,
            114.1322,
            22.2924,
            114.2008
    );

    private final String apiValue;
    private final String plainLabel;
    private final double originLatitude;
    private final double originLongitude;
    private final double destinationLatitude;
    private final double destinationLongitude;

    CommuteCorridor(
            String apiValue,
            String plainLabel,
            double originLatitude,
            double originLongitude,
            double destinationLatitude,
            double destinationLongitude
    ) {
        this.apiValue = apiValue;
        this.plainLabel = plainLabel;
        this.originLatitude = originLatitude;
        this.originLongitude = originLongitude;
        this.destinationLatitude = destinationLatitude;
        this.destinationLongitude = destinationLongitude;
    }

    public String getApiValue() {
        return apiValue;
    }

    public String getPlainLabel() {
        return plainLabel;
    }

    public String getDisplayName(android.content.Context context) {
        return plainLabel;
    }

    public double getOriginLatitude() {
        return originLatitude;
    }

    public double getOriginLongitude() {
        return originLongitude;
    }

    public double getDestinationLatitude() {
        return destinationLatitude;
    }

    public double getDestinationLongitude() {
        return destinationLongitude;
    }

    public static CommuteCorridor fromApiValue(String raw) {
        if (raw == null) {
            return CENTRAL_TO_SHA_TIN;
        }
        for (CommuteCorridor corridor : values()) {
            if (corridor.apiValue.equalsIgnoreCase(raw)) {
                return corridor;
            }
        }
        return CENTRAL_TO_SHA_TIN;
    }

    public static CommuteCorridor nearestTo(double latitude, double longitude) {
        CommuteCorridor best = CENTRAL_TO_SHA_TIN;
        double bestDistance = Double.MAX_VALUE;
        for (CommuteCorridor corridor : values()) {
            double dLat = latitude - corridor.originLatitude;
            double dLng = longitude - corridor.originLongitude;
            double distance = (dLat * dLat) + (dLng * dLng);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = corridor;
            }
        }
        return best;
    }

    @Override
    public String toString() {
        return plainLabel;
    }
}
