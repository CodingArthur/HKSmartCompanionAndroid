package hk.edu.hku.cs7506.smartcompanion.data.model;

import java.io.Serializable;

public class LocationSnapshot implements Serializable {
    private final double latitude;
    private final double longitude;
    private final String label;
    private final String city;
    private final String district;
    private final String sourceLabel;
    private final boolean fallback;

    public LocationSnapshot(
            double latitude,
            double longitude,
            String label,
            String city,
            String district,
            String sourceLabel,
            boolean fallback
    ) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.label = label;
        this.city = city;
        this.district = district;
        this.sourceLabel = sourceLabel;
        this.fallback = fallback;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getLabel() {
        return label;
    }

    public String getCity() {
        return city;
    }

    public String getDistrict() {
        return district;
    }

    public String getSourceLabel() {
        return sourceLabel;
    }

    public boolean isFallback() {
        return fallback;
    }
}
