package hk.edu.hku.cs7506.smartcompanion.data.model;

public class RecommendationRequest {
    private final SceneType sceneType;
    private final double userLatitude;
    private final double userLongitude;
    private final EmergencySortMode emergencySortMode;
    private final ParkingDestination parkingDestination;
    private final CommuteCorridor commuteCorridor;

    public RecommendationRequest(
            SceneType sceneType,
            double userLatitude,
            double userLongitude,
            EmergencySortMode emergencySortMode,
            ParkingDestination parkingDestination,
            CommuteCorridor commuteCorridor
    ) {
        this.sceneType = sceneType;
        this.userLatitude = userLatitude;
        this.userLongitude = userLongitude;
        this.emergencySortMode = emergencySortMode;
        this.parkingDestination = parkingDestination;
        this.commuteCorridor = commuteCorridor;
    }

    public SceneType getSceneType() {
        return sceneType;
    }

    public double getUserLatitude() {
        return userLatitude;
    }

    public double getUserLongitude() {
        return userLongitude;
    }

    public EmergencySortMode getEmergencySortMode() {
        return emergencySortMode;
    }

    public ParkingDestination getParkingDestination() {
        return parkingDestination;
    }

    public CommuteCorridor getCommuteCorridor() {
        return commuteCorridor;
    }
}
