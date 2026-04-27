package hk.edu.hku.cs7506.smartcompanion.data.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import hk.edu.hku.cs7506.smartcompanion.data.model.CommuteCorridor;
import hk.edu.hku.cs7506.smartcompanion.data.model.EmergencySortMode;
import hk.edu.hku.cs7506.smartcompanion.data.model.ParkingDestination;
import hk.edu.hku.cs7506.smartcompanion.data.model.RecommendationItem;
import hk.edu.hku.cs7506.smartcompanion.data.model.RecommendationRequest;
import hk.edu.hku.cs7506.smartcompanion.data.model.SceneType;

public class CommuteAssistantClientTest {
    @Test
    public void buildsLiveCommuteRecommendationsFromOfficialEta() {
        String nextEta = OffsetDateTime.now(ZoneOffset.ofHours(8)).plusMinutes(4).toString();
        OpenDataHttpClient fakeHttpClient = new OpenDataHttpClient() {
            @Override
            public String get(String url) {
                if (url.contains("route-stop/ctb/182/outbound")) {
                    return "{ \"data\": [ { \"stop\": \"CENTRAL_STOP\" } ] }";
                }
                if (url.contains("route-stop/ctb/182X/outbound")) {
                    return "{ \"data\": [ { \"stop\": \"CENTRAL_STOP_X\" } ] }";
                }
                if (url.contains("stop/CENTRAL_STOP_X")) {
                    return "{ \"data\": { \"name_en\": \"Central (Macao Ferry)\", \"lat\": 22.2869, \"long\": 114.1531 } }";
                }
                if (url.contains("stop/CENTRAL_STOP")) {
                    return "{ \"data\": { \"name_en\": \"Central (Macao Ferry)\", \"lat\": 22.2869, \"long\": 114.1531 } }";
                }
                if (url.contains("eta/CTB/CENTRAL_STOP_X/182X")) {
                    return "{ \"data\": [ { \"dir\": \"O\", \"eta\": \"" + nextEta + "\" } ] }";
                }
                if (url.contains("eta/CTB/CENTRAL_STOP/182")) {
                    return "{ \"data\": [ { \"dir\": \"O\", \"eta\": \"" + OffsetDateTime.now(ZoneOffset.ofHours(8)).plusMinutes(8) + "\" } ] }";
                }
                return "{ \"data\": [] }";
            }
        };

        CommuteAssistantClient client = new CommuteAssistantClient(fakeHttpClient);
        RecommendationRequest request = new RecommendationRequest(
                SceneType.COMMUTE,
                22.2840,
                114.1400,
                EmergencySortMode.TOTAL_TIME,
                ParkingDestination.CENTRAL_WATERFRONT,
                CommuteCorridor.CENTRAL_TO_SHA_TIN
        );

        List<RecommendationItem> items = client.fetchRecommendations(request);

        assertFalse(items.isEmpty());
        assertEquals("Citybus 182", items.get(0).getName());
        assertTrue(items.get(0).getEtaMinutes() >= 1);
        assertTrue(items.get(0).getWalkMinutes() >= 1);
        assertTrue(items.get(0).getInVehicleMinutes() >= 10);
        assertTrue(items.get(0).getTransitNote().contains("Central (Macao Ferry)"));
    }
}
