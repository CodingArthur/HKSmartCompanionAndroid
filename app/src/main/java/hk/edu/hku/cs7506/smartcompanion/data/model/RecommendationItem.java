package hk.edu.hku.cs7506.smartcompanion.data.model;

import java.io.Serializable;

public class RecommendationItem implements Serializable {
    private final String id;
    private final SceneType sceneType;
    private final String name;
    private final String reason;
    private final double latitude;
    private final double longitude;
    private final double totalScore;
    private final Integer etaMinutes;
    private final Integer waitTimeMinutes;
    private final Integer vacancy;
    private final Integer walkDistanceMeters;
    private final Integer walkMinutes;
    private final Integer inVehicleMinutes;
    private final Double weatherScore;
    private final Double aqhiScore;
    private final String contentTag;
    private final String metadataLine;
    private final String detailsUrl;
    private final String imageUrl;
    private final String imageAttribution;
    private final String addressLine;
    private final String contactPhone;
    private final String trafficNote;
    private final String transitNote;

    public RecommendationItem(
            String id,
            SceneType sceneType,
            String name,
            String reason,
            double latitude,
            double longitude,
            double totalScore,
            Integer etaMinutes,
            Integer waitTimeMinutes,
            Integer vacancy,
            Integer walkDistanceMeters,
            Integer walkMinutes,
            Integer inVehicleMinutes,
            Double weatherScore,
            Double aqhiScore
    ) {
        this(
                id,
                sceneType,
                name,
                reason,
                latitude,
                longitude,
                totalScore,
                etaMinutes,
                waitTimeMinutes,
                vacancy,
                walkDistanceMeters,
                walkMinutes,
                inVehicleMinutes,
                weatherScore,
                aqhiScore,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    public RecommendationItem(
            String id,
            SceneType sceneType,
            String name,
            String reason,
            double latitude,
            double longitude,
            double totalScore,
            Integer etaMinutes,
            Integer waitTimeMinutes,
            Integer vacancy,
            Integer walkDistanceMeters,
            Integer walkMinutes,
            Integer inVehicleMinutes,
            Double weatherScore,
            Double aqhiScore,
            String contentTag,
            String metadataLine,
            String detailsUrl,
            String imageUrl,
            String imageAttribution,
            String addressLine,
            String contactPhone,
            String trafficNote,
            String transitNote
    ) {
        this.id = id;
        this.sceneType = sceneType;
        this.name = name;
        this.reason = reason;
        this.latitude = latitude;
        this.longitude = longitude;
        this.totalScore = totalScore;
        this.etaMinutes = etaMinutes;
        this.waitTimeMinutes = waitTimeMinutes;
        this.vacancy = vacancy;
        this.walkDistanceMeters = walkDistanceMeters;
        this.walkMinutes = walkMinutes;
        this.inVehicleMinutes = inVehicleMinutes;
        this.weatherScore = weatherScore;
        this.aqhiScore = aqhiScore;
        this.contentTag = contentTag;
        this.metadataLine = metadataLine;
        this.detailsUrl = detailsUrl;
        this.imageUrl = imageUrl;
        this.imageAttribution = imageAttribution;
        this.addressLine = addressLine;
        this.contactPhone = contactPhone;
        this.trafficNote = trafficNote;
        this.transitNote = transitNote;
    }

    public RecommendationItem(
            String id,
            SceneType sceneType,
            String name,
            String reason,
            double latitude,
            double longitude,
            double totalScore,
            Integer etaMinutes,
            Integer waitTimeMinutes,
            Integer vacancy,
            Integer walkDistanceMeters,
            Integer walkMinutes,
            Integer inVehicleMinutes,
            Double weatherScore,
            Double aqhiScore,
            String contentTag,
            String metadataLine,
            String detailsUrl,
            String imageUrl,
            String imageAttribution,
            String trafficNote,
            String transitNote
    ) {
        this(
                id,
                sceneType,
                name,
                reason,
                latitude,
                longitude,
                totalScore,
                etaMinutes,
                waitTimeMinutes,
                vacancy,
                walkDistanceMeters,
                walkMinutes,
                inVehicleMinutes,
                weatherScore,
                aqhiScore,
                contentTag,
                metadataLine,
                detailsUrl,
                imageUrl,
                imageAttribution,
                null,
                null,
                trafficNote,
                transitNote
        );
    }

    public RecommendationItem(
            String id,
            SceneType sceneType,
            String name,
            String reason,
            double latitude,
            double longitude,
            double totalScore,
            Integer etaMinutes,
            Integer waitTimeMinutes,
            Integer vacancy,
            Integer walkDistanceMeters,
            Integer walkMinutes,
            Double weatherScore,
            Double aqhiScore
    ) {
        this(
                id,
                sceneType,
                name,
                reason,
                latitude,
                longitude,
                totalScore,
                etaMinutes,
                waitTimeMinutes,
                vacancy,
                walkDistanceMeters,
                walkMinutes,
                null,
                weatherScore,
                aqhiScore
        );
    }

    public RecommendationItem(
            String id,
            SceneType sceneType,
            String name,
            String reason,
            double latitude,
            double longitude,
            double totalScore,
            Integer etaMinutes,
            Integer waitTimeMinutes,
            Integer vacancy,
            Integer walkDistanceMeters,
            Integer walkMinutes,
            Double weatherScore,
            Double aqhiScore,
            String contentTag,
            String metadataLine,
            String detailsUrl,
            String imageUrl,
            String imageAttribution,
            String addressLine,
            String contactPhone,
            String trafficNote,
            String transitNote
    ) {
        this(
                id,
                sceneType,
                name,
                reason,
                latitude,
                longitude,
                totalScore,
                etaMinutes,
                waitTimeMinutes,
                vacancy,
                walkDistanceMeters,
                walkMinutes,
                null,
                weatherScore,
                aqhiScore,
                contentTag,
                metadataLine,
                detailsUrl,
                imageUrl,
                imageAttribution,
                addressLine,
                contactPhone,
                trafficNote,
                transitNote
        );
    }

    public RecommendationItem(
            String id,
            SceneType sceneType,
            String name,
            String reason,
            double latitude,
            double longitude,
            double totalScore,
            Integer etaMinutes,
            Integer waitTimeMinutes,
            Integer vacancy,
            Integer walkDistanceMeters,
            Integer walkMinutes,
            Double weatherScore,
            Double aqhiScore,
            String contentTag,
            String metadataLine,
            String detailsUrl,
            String imageUrl,
            String imageAttribution,
            String trafficNote,
            String transitNote
    ) {
        this(
                id,
                sceneType,
                name,
                reason,
                latitude,
                longitude,
                totalScore,
                etaMinutes,
                waitTimeMinutes,
                vacancy,
                walkDistanceMeters,
                walkMinutes,
                null,
                weatherScore,
                aqhiScore,
                contentTag,
                metadataLine,
                detailsUrl,
                imageUrl,
                imageAttribution,
                trafficNote,
                transitNote
        );
    }

    public String getId() {
        return id;
    }

    public SceneType getSceneType() {
        return sceneType;
    }

    public String getName() {
        return name;
    }

    public String getReason() {
        return reason;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getTotalScore() {
        return totalScore;
    }

    public Integer getEtaMinutes() {
        return etaMinutes;
    }

    public Integer getWaitTimeMinutes() {
        return waitTimeMinutes;
    }

    public Integer getVacancy() {
        return vacancy;
    }

    public Integer getWalkDistanceMeters() {
        return walkDistanceMeters;
    }

    public Integer getWalkMinutes() {
        return walkMinutes;
    }

    public Integer getInVehicleMinutes() {
        return inVehicleMinutes;
    }

    public Double getWeatherScore() {
        return weatherScore;
    }

    public Double getAqhiScore() {
        return aqhiScore;
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

    public String getTrafficNote() {
        return trafficNote;
    }

    public String getTransitNote() {
        return transitNote;
    }

    public String getStableId() {
        return sceneType.getApiValue() + ":" + id;
    }

    public RecommendationItem withScoreAndReason(double newScore, String newReason) {
        return new RecommendationItem(
                id,
                sceneType,
                name,
                newReason,
                latitude,
                longitude,
                newScore,
                etaMinutes,
                waitTimeMinutes,
                vacancy,
                walkDistanceMeters,
                walkMinutes,
                inVehicleMinutes,
                weatherScore,
                aqhiScore,
                contentTag,
                metadataLine,
                detailsUrl,
                imageUrl,
                imageAttribution,
                addressLine,
                contactPhone,
                trafficNote,
                transitNote
        );
    }

    public RecommendationItem withTransport(Integer newEtaMinutes, String newTrafficNote, String newTransitNote) {
        return new RecommendationItem(
                id,
                sceneType,
                name,
                reason,
                latitude,
                longitude,
                totalScore,
                newEtaMinutes,
                waitTimeMinutes,
                vacancy,
                walkDistanceMeters,
                walkMinutes,
                inVehicleMinutes,
                weatherScore,
                aqhiScore,
                contentTag,
                metadataLine,
                detailsUrl,
                imageUrl,
                imageAttribution,
                addressLine,
                contactPhone,
                newTrafficNote,
                newTransitNote
        );
    }
}
