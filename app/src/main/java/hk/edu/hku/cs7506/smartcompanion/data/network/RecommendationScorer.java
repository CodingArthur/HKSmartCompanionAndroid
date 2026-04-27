package hk.edu.hku.cs7506.smartcompanion.data.network;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import hk.edu.hku.cs7506.smartcompanion.data.model.EmergencySortMode;
import hk.edu.hku.cs7506.smartcompanion.data.model.ParkingDestination;
import hk.edu.hku.cs7506.smartcompanion.data.model.CommuteCorridor;
import hk.edu.hku.cs7506.smartcompanion.data.model.RecommendationItem;
import hk.edu.hku.cs7506.smartcompanion.data.model.RecommendationRequest;
import hk.edu.hku.cs7506.smartcompanion.data.model.SceneType;

public class RecommendationScorer {
    public List<RecommendationItem> rank(SceneType sceneType, RecommendationRequest request, List<RecommendationItem> items) {
        switch (sceneType) {
            case EMERGENCY:
                return rankEmergency(items, request.getEmergencySortMode());
            case PARKING:
                return rankParking(items, request.getParkingDestination());
            case PLAY:
                return rankPlay(items);
            case COMMUTE:
                return rankCommute(items, request.getCommuteCorridor());
            default:
                return items;
        }
    }

    public List<RecommendationItem> rankEmergency(List<RecommendationItem> items, EmergencySortMode sortMode) {
        List<RecommendationItem> ranked = new ArrayList<>();
        for (RecommendationItem item : items) {
            int eta = safeInt(item.getEtaMinutes());
            int wait = safeInt(item.getWaitTimeMinutes());
            int combined = eta + wait;
            double score;
            String reason;

            if (sortMode == EmergencySortMode.NEAREST) {
                score = Math.max(0, 120 - eta * 3);
                reason = String.format(Locale.US,
                        "Nearest option first: drive %d min, wait %d min, combined %d min.",
                        eta, wait, combined);
            } else if (sortMode == EmergencySortMode.SHORTEST_WAIT) {
                score = Math.max(0, 180 - wait * 2);
                reason = String.format(Locale.US,
                        "Shortest waiting queue first: wait %d min, drive %d min, combined %d min.",
                        wait, eta, combined);
            } else {
                score = Math.max(0, 220 - combined * 2);
                reason = String.format(Locale.US,
                        "Best combined emergency time: drive %d min + wait %d min = %d min.",
                        eta, wait, combined);
            }
            ranked.add(item.withScoreAndReason(score, reason));
        }

        Comparator<RecommendationItem> comparator;
        if (sortMode == EmergencySortMode.NEAREST) {
            comparator = Comparator.comparingInt(item -> safeInt(item.getEtaMinutes()));
        } else if (sortMode == EmergencySortMode.SHORTEST_WAIT) {
            comparator = Comparator.comparingInt(item -> safeInt(item.getWaitTimeMinutes()));
        } else {
            comparator = Comparator.comparingInt(item -> safeInt(item.getEtaMinutes()) + safeInt(item.getWaitTimeMinutes()));
        }
        ranked.sort(comparator);
        return ranked;
    }

    public List<RecommendationItem> rankParking(List<RecommendationItem> items) {
        return rankParking(items, ParkingDestination.CENTRAL_WATERFRONT);
    }

    public List<RecommendationItem> rankParking(List<RecommendationItem> items, ParkingDestination destination) {
        List<RecommendationItem> ranked = new ArrayList<>();
        String destinationLabel = destination == null
                ? ParkingDestination.CENTRAL_WATERFRONT.getDisplayName()
                : destination.getDisplayName();
        for (RecommendationItem item : items) {
            int driveMinutes = safeInt(item.getEtaMinutes());
            int walkMinutes = safeInt(item.getWalkMinutes());
            int vacancy = safeInt(item.getVacancy());
            double vacancyScore = Math.min(100.0, vacancy * 1.4);
            double driveScore = Math.max(0.0, 100.0 - driveMinutes * 2.2);
            double walkScore = Math.max(0.0, 100.0 - walkMinutes * 1.1);
            double score = (vacancyScore * 0.35) + (driveScore * 0.25) + (walkScore * 0.40);
            String reason = String.format(Locale.US,
                    "%d spaces left, drive %d min, then walk %d min from the car park to %s.",
                    vacancy, driveMinutes, walkMinutes, destinationLabel);
            ranked.add(item.withScoreAndReason(score, reason));
        }

        ranked.sort(Comparator
                .comparingDouble(RecommendationItem::getTotalScore).reversed()
                .thenComparingInt(item -> safeInt(item.getWalkMinutes()))
                .thenComparingInt(item -> safeInt(item.getEtaMinutes()))
                .thenComparing((left, right) -> Integer.compare(safeInt(right.getVacancy()), safeInt(left.getVacancy()))));
        return ranked.size() > 20 ? new ArrayList<>(ranked.subList(0, 20)) : ranked;
    }

    public List<RecommendationItem> rankPlay(List<RecommendationItem> items) {
        List<RecommendationItem> ranked = new ArrayList<>();
        for (RecommendationItem item : items) {
            double weatherScore = safeDouble(item.getWeatherScore());
            double aqhiScore = safeDouble(item.getAqhiScore());
            double etaScore = Math.max(0, 1.0 - (safeInt(item.getEtaMinutes()) / 60.0));
            double freshnessBoost = resolveFreshnessBoost(item);
            double total = 100.0 * ((weatherScore * 0.35) + (aqhiScore * 0.25) + (etaScore * 0.25) + freshnessBoost);
            String reason = String.format(Locale.US,
                    "Weather %.2f, AQHI %.2f, and ETA %d min make this a stronger leisure option.",
                    weatherScore, aqhiScore, safeInt(item.getEtaMinutes()));
            ranked.add(item.withScoreAndReason(total, reason));
        }

        ranked.sort((left, right) -> Double.compare(right.getTotalScore(), left.getTotalScore()));
        return ranked;
    }

    public List<RecommendationItem> rankCommute(List<RecommendationItem> items, CommuteCorridor corridor) {
        List<RecommendationItem> ranked = new ArrayList<>();
        String corridorLabel = corridor == null
                ? "the selected corridor"
                : corridor.getPlainLabel();
        for (RecommendationItem item : items) {
            int nextDeparture = safeInt(item.getEtaMinutes());
            int accessWalk = safeInt(item.getWalkMinutes());
            int inVehicle = safeInt(item.getInVehicleMinutes());
            int totalBurden = nextDeparture + accessWalk + inVehicle;
            double departureScore = Math.max(0.0, 100.0 - nextDeparture * 5.0);
            double accessScore = Math.max(0.0, 100.0 - accessWalk * 3.0);
            double rideScore = Math.max(0.0, 100.0 - inVehicle * 1.5);
            double score = (departureScore * 0.40) + (accessScore * 0.20) + (rideScore * 0.40);
            String reason = String.format(
                    Locale.US,
                    "Board in %d min after a %d min access walk. Estimated in-vehicle time on %s is about %d min.",
                    nextDeparture,
                    accessWalk,
                    corridorLabel,
                    inVehicle
            );
            ranked.add(item.withScoreAndReason(score, reason));
        }

        ranked.sort(Comparator
                .comparingDouble(RecommendationItem::getTotalScore).reversed()
                .thenComparingInt(item -> safeInt(item.getEtaMinutes()))
                .thenComparingInt(item -> safeInt(item.getWalkMinutes()))
                .thenComparingInt(item -> safeInt(item.getInVehicleMinutes())));
        return ranked;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private double safeDouble(Double value) {
        return value == null ? 0.0 : value;
    }

    private double resolveFreshnessBoost(RecommendationItem item) {
        if (item.getContentTag() == null) {
            return 0.0;
        }
        if ("Current event".equalsIgnoreCase(item.getContentTag())) {
            return 0.15;
        }
        if ("Art venue".equalsIgnoreCase(item.getContentTag())) {
            return 0.10;
        }
        return 0.08;
    }
}
