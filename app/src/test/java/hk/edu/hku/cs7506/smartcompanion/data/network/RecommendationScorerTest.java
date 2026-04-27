package hk.edu.hku.cs7506.smartcompanion.data.network;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import hk.edu.hku.cs7506.smartcompanion.data.model.CommuteCorridor;
import hk.edu.hku.cs7506.smartcompanion.data.model.EmergencySortMode;
import hk.edu.hku.cs7506.smartcompanion.data.model.ParkingDestination;
import hk.edu.hku.cs7506.smartcompanion.data.model.RecommendationItem;
import hk.edu.hku.cs7506.smartcompanion.data.model.RecommendationRequest;
import hk.edu.hku.cs7506.smartcompanion.data.model.SceneType;

public class RecommendationScorerTest {
    private final RecommendationScorer scorer = new RecommendationScorer();

    @Test
    public void emergencyRankingSupportsMultipleModes() {
        List<RecommendationItem> items = Arrays.asList(
                new RecommendationItem("a", SceneType.EMERGENCY, "Hospital A", "", 22.30, 114.17, 0.0, 18, 30, null, null, null, null, null),
                new RecommendationItem("b", SceneType.EMERGENCY, "Hospital B", "", 22.31, 114.18, 0.0, 10, 40, null, null, null, null, null),
                new RecommendationItem("c", SceneType.EMERGENCY, "Hospital C", "", 22.32, 114.19, 0.0, 20, 12, null, null, null, null, null)
        );

        List<RecommendationItem> totalTimeRanked = scorer.rankEmergency(items, EmergencySortMode.TOTAL_TIME);
        List<RecommendationItem> nearestRanked = scorer.rankEmergency(items, EmergencySortMode.NEAREST);
        List<RecommendationItem> shortestWaitRanked = scorer.rankEmergency(items, EmergencySortMode.SHORTEST_WAIT);

        assertEquals("Hospital C", totalTimeRanked.get(0).getName());
        assertEquals("Hospital B", nearestRanked.get(0).getName());
        assertEquals("Hospital C", shortestWaitRanked.get(0).getName());
    }

    @Test
    public void parkingRankingRewardsVacancyAndShortFinalWalk() {
        RecommendationRequest request = new RecommendationRequest(
                SceneType.PARKING,
                22.3027,
                114.1772,
                EmergencySortMode.TOTAL_TIME,
                ParkingDestination.CENTRAL_WATERFRONT,
                CommuteCorridor.CENTRAL_TO_SHA_TIN
        );
        List<RecommendationItem> ranked = scorer.rank(
                SceneType.PARKING,
                request,
                Arrays.asList(
                        new RecommendationItem("best", SceneType.PARKING, "Central", "", 22.28, 114.16, 0.0, 10, null, 28, 180, 3, null, null),
                        new RecommendationItem("far", SceneType.PARKING, "Far", "", 22.31, 114.22, 0.0, 12, null, 35, 850, 11, null, null)
                )
        );

        assertEquals("Central", ranked.get(0).getName());
    }

    @Test
    public void parkingRankingStillDifferentiatesLongFinalWalks() {
        List<RecommendationItem> ranked = scorer.rankParking(Arrays.asList(
                new RecommendationItem("west", SceneType.PARKING, "West", "", 22.30, 114.17, 0.0, 15, null, 61, 3000, 79, null, null),
                new RecommendationItem("hkcec", SceneType.PARKING, "HKCEC", "", 22.31, 114.18, 0.0, 15, null, 61, 2800, 75, null, null)
        ));

        assertEquals("HKCEC", ranked.get(0).getName());
    }

    @Test
    public void parkingReasonIncludesSelectedDestination() {
        List<RecommendationItem> ranked = scorer.rankParking(
                Arrays.asList(new RecommendationItem(
                        "central",
                        SceneType.PARKING,
                        "Central",
                        "",
                        22.28,
                        114.16,
                        0.0,
                        12,
                        null,
                        40,
                        900,
                        9,
                        null,
                        null
                )),
                ParkingDestination.HKCEC
        );

        assertEquals(
                "40 spaces left, drive 12 min, then walk 9 min from the car park to HKCEC.",
                ranked.get(0).getReason()
        );
    }

    @Test
    public void commuteRankingPrefersSoonerDepartureAndShorterAccess() {
        RecommendationRequest request = new RecommendationRequest(
                SceneType.COMMUTE,
                22.3027,
                114.1772,
                EmergencySortMode.TOTAL_TIME,
                ParkingDestination.CENTRAL_WATERFRONT,
                CommuteCorridor.CENTRAL_TO_SHA_TIN
        );
        List<RecommendationItem> ranked = scorer.rank(
                SceneType.COMMUTE,
                request,
                Arrays.asList(
                        new RecommendationItem("182", SceneType.COMMUTE, "Citybus 182", "", 22.38, 114.18, 0.0, 9, null, null, 600, 8, 34, null, null),
                        new RecommendationItem("182X", SceneType.COMMUTE, "Citybus 182X", "", 22.38, 114.18, 0.0, 4, null, null, 260, 4, 30, null, null)
                )
        );

        assertEquals("Citybus 182X", ranked.get(0).getName());
    }
}
