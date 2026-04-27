package hk.edu.hku.cs7506.smartcompanion.util;

public final class GeoBounds {
    private static final double HK_LAT_MIN = 22.0;
    private static final double HK_LAT_MAX = 22.65;
    private static final double HK_LNG_MIN = 113.8;
    private static final double HK_LNG_MAX = 114.45;

    private GeoBounds() {
    }

    public static boolean isLikelyHongKongCoordinate(double latitude, double longitude) {
        if (Double.isNaN(latitude) || Double.isNaN(longitude)) {
            return false;
        }
        if (Double.isInfinite(latitude) || Double.isInfinite(longitude)) {
            return false;
        }
        if (Math.abs(latitude) < 0.0001 && Math.abs(longitude) < 0.0001) {
            return false;
        }
        return latitude >= HK_LAT_MIN
                && latitude <= HK_LAT_MAX
                && longitude >= HK_LNG_MIN
                && longitude <= HK_LNG_MAX;
    }
}
