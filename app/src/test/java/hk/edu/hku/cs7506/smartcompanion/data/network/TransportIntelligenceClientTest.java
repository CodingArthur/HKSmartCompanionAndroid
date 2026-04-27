package hk.edu.hku.cs7506.smartcompanion.data.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

import hk.edu.hku.cs7506.smartcompanion.data.model.CommuteCorridor;
import hk.edu.hku.cs7506.smartcompanion.data.model.EmergencySortMode;
import hk.edu.hku.cs7506.smartcompanion.data.model.ParkingDestination;
import hk.edu.hku.cs7506.smartcompanion.data.model.RecommendationItem;
import hk.edu.hku.cs7506.smartcompanion.data.model.RecommendationRequest;
import hk.edu.hku.cs7506.smartcompanion.data.model.SceneType;

public class TransportIntelligenceClientTest {
    @Test
    public void enrichesPlayRecommendationsWithDriveAndBusSignals() {
        String etaOne = OffsetDateTime.now(ZoneOffset.ofHours(8)).plusMinutes(5).toString();
        String etaTwo = OffsetDateTime.now(ZoneOffset.ofHours(8)).plusMinutes(9).toString();
        OpenDataHttpClient fakeHttpClient = new OpenDataHttpClient() {
            @Override
            public String get(String url) {
                if (url.contains("kmb/stop-eta/8D804CFD9C7B9042")) {
                    return "{\n" +
                            "  \"data\": [\n" +
                            "    {\"route\":\"1A\",\"dest_en\":\"SAU MAU PING\",\"eta\":\"" + etaOne + "\"},\n" +
                            "    {\"route\":\"5A\",\"dest_en\":\"KAI TAK\",\"eta\":\"" + etaTwo + "\"}\n" +
                            "  ]\n" +
                            "}";
                }
                return "{}";
            }

            @Override
            public String postJson(String url, String body) {
                return "{\n" +
                        "  \"eta\": \"00:07\",\n" +
                        "  \"distU\": \"3.10 km\",\n" +
                        "  \"jSpeed\": \"27 km/h\"\n" +
                        "}";
            }
        };

        TransportIntelligenceClient client = new TransportIntelligenceClient(fakeHttpClient);
        RecommendationRequest request = new RecommendationRequest(
                SceneType.PLAY,
                22.3027,
                114.1772,
                EmergencySortMode.TOTAL_TIME,
                ParkingDestination.CENTRAL_WATERFRONT,
                CommuteCorridor.CENTRAL_TO_SHA_TIN
        );
        RecommendationItem item = new RecommendationItem(
                "museum-1",
                SceneType.PLAY,
                "Hong Kong Museum of Art",
                "",
                22.2935,
                114.1720,
                0.0,
                15,
                null,
                null,
                null,
                null,
                0.84,
                0.82,
                "Museum pick",
                "Official museum directory - Yau Tsim Mong",
                "https://hk.art.museum/en_US/web/ma/home.html",
                "https://upload.wikimedia.org/wikipedia/commons/c/c8/Hong_Kong_Museum_of_Art_renovation_site_201908.jpg",
                "Visuals from Wikimedia Commons",
                null,
                null
        );
        String trafficXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><list><message>"
                + "<INCIDENT_HEADING_EN>Road Incident</INCIDENT_HEADING_EN>"
                + "<LOCATION_EN>Tsim Sha Tsui</LOCATION_EN>"
                + "<CONTENT_EN>Traffic is busy near Salisbury Road.</CONTENT_EN>"
                + "<ANNOUNCEMENT_DATE>2026-04-15T14:40:00</ANNOUNCEMENT_DATE>"
                + "</message></list>";

        List<RecommendationItem> enriched = client.enrichTopRecommendations(
                SceneType.PLAY,
                request,
                Collections.singletonList(item),
                trafficXml
        );

        assertEquals(1, enriched.size());
        assertEquals(Integer.valueOf(7), enriched.get(0).getEtaMinutes());
        assertTrue(enriched.get(0).getTrafficNote().contains("Live TDAS drive time 7 min"));
        assertTrue(enriched.get(0).getTrafficNote().contains("Tsim Sha Tsui"));
        assertTrue(enriched.get(0).getTransitNote().contains("Star Ferry / Harbour City"));
    }
}
