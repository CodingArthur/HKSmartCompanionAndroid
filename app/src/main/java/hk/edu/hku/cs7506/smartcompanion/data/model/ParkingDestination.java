package hk.edu.hku.cs7506.smartcompanion.data.model;

import hk.edu.hku.cs7506.smartcompanion.R;

public enum ParkingDestination {
    CENTRAL_WATERFRONT("central_waterfront", "Central", R.string.parking_destination_central, 22.2856, 114.1605),
    HKCEC("hkcec", "HKCEC", R.string.parking_destination_hkcec, 22.2821, 114.1753),
    WEST_KOWLOON("west_kowloon", "West Kowloon", R.string.parking_destination_west_kowloon, 22.2948, 114.1598),
    KAI_TAK("kai_tak", "Kai Tak", R.string.parking_destination_kai_tak, 22.3208, 114.2054);

    private final String id;
    private final String displayName;
    private final int titleResId;
    private final double latitude;
    private final double longitude;

    ParkingDestination(String id, String displayName, int titleResId, double latitude, double longitude) {
        this.id = id;
        this.displayName = displayName;
        this.titleResId = titleResId;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getId() {
        return id;
    }

    public int getTitleResId() {
        return titleResId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public static ParkingDestination fromId(String raw) {
        if (raw == null) {
            return CENTRAL_WATERFRONT;
        }
        for (ParkingDestination destination : values()) {
            if (destination.id.equalsIgnoreCase(raw)) {
                return destination;
            }
        }
        return CENTRAL_WATERFRONT;
    }
}
