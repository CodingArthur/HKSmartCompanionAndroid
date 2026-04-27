package hk.edu.hku.cs7506.smartcompanion.data.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import hk.edu.hku.cs7506.smartcompanion.data.model.CommuteCorridor;
import hk.edu.hku.cs7506.smartcompanion.data.model.EmergencySortMode;
import hk.edu.hku.cs7506.smartcompanion.data.model.ParkingDestination;
import hk.edu.hku.cs7506.smartcompanion.data.model.RecommendationItem;
import hk.edu.hku.cs7506.smartcompanion.data.model.RecommendationRequest;
import hk.edu.hku.cs7506.smartcompanion.data.model.SceneType;

public class OfficialDataClientTest {
    private final OfficialDataClient client = new OfficialDataClient();
    private final RecommendationRequest emergencyRequest = new RecommendationRequest(
            SceneType.EMERGENCY,
            22.3027,
            114.1772,
            EmergencySortMode.TOTAL_TIME,
            ParkingDestination.CENTRAL_WATERFRONT,
            CommuteCorridor.CENTRAL_TO_SHA_TIN
    );
    private final RecommendationRequest parkingRequest = new RecommendationRequest(
            SceneType.PARKING,
            22.3027,
            114.1772,
            EmergencySortMode.TOTAL_TIME,
            ParkingDestination.CENTRAL_WATERFRONT,
            CommuteCorridor.CENTRAL_TO_SHA_TIN
    );
    private final RecommendationRequest playRequest = new RecommendationRequest(
            SceneType.PLAY,
            22.3027,
            114.1772,
            EmergencySortMode.TOTAL_TIME,
            ParkingDestination.CENTRAL_WATERFRONT,
            CommuteCorridor.CENTRAL_TO_SHA_TIN
    );

    @Test
    public void parsesEmergencyWaitFeed() {
        String json = "{\n" +
                "  \"waitTime\": [\n" +
                "    {\"hospName\":\"Queen Mary Hospital\",\"t45p50\":\"3 hours\",\"t3p50\":\"29 minutes\"},\n" +
                "    {\"hospName\":\"Queen Elizabeth Hospital\",\"t45p50\":\"1.5 hours\",\"t3p50\":\"11 minutes\"}\n" +
                "  ]\n" +
                "}";
        List<RecommendationItem> items = client.parseEmergencyRecommendations(json, emergencyRequest);
        assertEquals(2, items.size());
        assertEquals("Queen Mary Hospital", items.get(0).getName());
        assertTrue(items.get(0).getWaitTimeMinutes() > 0);
        assertEquals(Integer.valueOf(90), items.get(1).getWaitTimeMinutes());
        assertEquals("Emergency unit", items.get(0).getContentTag());
        assertNotNull(items.get(0).getImageUrl());
        assertTrue(items.get(0).getAddressLine().contains("Pok Fu Lam"));
    }

    @Test
    public void parsesParkingFeedsAndMergesVacancy() {
        String infoJson = "{\n" +
                "  \"results\": [\n" +
                "    {\"park_Id\":\"p1\",\"name\":\"Test Car Park\",\"opening_status\":\"OPEN\",\"latitude\":22.3,\"longitude\":114.17,\"privateCar\":{\"space\":100}},\n" +
                "    {\"park_Id\":\"p2\",\"name\":\"Closed Car Park\",\"opening_status\":\"CLOSED\",\"latitude\":22.3,\"longitude\":114.17,\"privateCar\":{\"space\":80}}\n" +
                "  ]\n" +
                "}";
        String vacancyJson = "{\n" +
                "  \"results\": [\n" +
                "    {\"park_Id\":\"p1\",\"privateCar\":[{\"vacancy\":35,\"category\":\"HOURLY\"}]},\n" +
                "    {\"park_Id\":\"p2\",\"privateCar\":[{\"vacancy\":20,\"category\":\"HOURLY\"}]}\n" +
                "  ]\n" +
                "}";
        List<RecommendationItem> items = client.parseParkingRecommendations(infoJson, vacancyJson, parkingRequest);
        assertEquals(1, items.size());
        assertEquals("Test Car Park", items.get(0).getName());
        assertEquals(Integer.valueOf(35), items.get(0).getVacancy());
        assertTrue(items.get(0).getWalkMinutes() >= 1);
        assertEquals("Live parking option", items.get(0).getContentTag());
        assertTrue(items.get(0).getMetadataLine() != null && !items.get(0).getMetadataLine().isEmpty());
    }

    @Test
    public void parsesOfficialEventCsv() {
        String csv = "Date,Event_Name,Venue\n" +
                "10 February 2024,2024 International Chinese New Year Night Parade,\"Hong Kong Cultural Centre Piazza, Tsim Sha Tsui\"\n" +
                "20 April 2026,2026 Harbour Arts Weekend,West Kowloon Cultural District\n";
        String weatherJson = "{\n" +
                "  \"temperature\": {\"data\": [{\"place\": \"Hong Kong Observatory\", \"value\": 24}]},\n" +
                "  \"rainfall\": {\"data\": [{\"place\": \"Yau Tsim Mong\", \"max\": 0}]}\n" +
                "}";
        String aqhiJson = "[{\"station\":\"Mong Kok\",\"aqhi\":2,\"health_risk\":\"Low\"}]";
        OfficialDataParser parser = new OfficialDataParser();
        List<RecommendationItem> items = parser.parseEventRecommendations(
                csv,
                weatherJson,
                aqhiJson,
                playRequest,
                LocalDate.of(2026, 4, 12)
        );
        assertFalse(items.isEmpty());
        assertEquals("2026 Harbour Arts Weekend", items.get(0).getName());
        assertTrue(items.get(0).getEtaMinutes() >= 0);
        assertTrue(items.get(0).getWeatherScore() > 0);
    }

    @Test
    public void playRecommendationsFallbackToMuseumDirectoryWhenEventFeedIsStale() {
        String staleEventCsv = "Date,Event_Name,Venue\n" +
                "20 April 2026,2026 Harbour Arts Weekend,West Kowloon Cultural District\n";
        String museumsJson = "{\n" +
                "  \"features\": [\n" +
                "    {\n" +
                "      \"attributes\": {\n" +
                "        \"NameEN\": \"Hong Kong Museum of Art\",\n" +
                "        \"DistrictEN\": \"YAU TSIM MONG\",\n" +
                "        \"WebsiteEN\": \"https://hk.art.museum/en_US/web/ma/home.html\"\n" +
                "      },\n" +
                "      \"geometry\": {\"x\": 114.1719737, \"y\": 22.2934641}\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        String weatherJson = "{\n" +
                "  \"temperature\": {\"data\": [{\"place\": \"Hong Kong Observatory\", \"value\": 24}]},\n" +
                "  \"rainfall\": {\"data\": [{\"place\": \"Yau Tsim Mong\", \"max\": 0}]}\n" +
                "}";
        String aqhiJson = "[{\"station\":\"Mong Kok\",\"aqhi\":2,\"health_risk\":\"Low\"}]";
        long staleLastModified = LocalDate.of(2025, 9, 26)
                .atStartOfDay(ZoneId.of("Asia/Hong_Kong"))
                .toInstant()
                .toEpochMilli();

        List<RecommendationItem> items = client.parsePlayRecommendations(
                staleEventCsv,
                staleLastModified,
                museumsJson,
                weatherJson,
                aqhiJson,
                playRequest
        );

        assertEquals(1, items.size());
        assertEquals("Hong Kong Museum of Art", items.get(0).getName());
        assertEquals("Art venue", items.get(0).getContentTag());
        assertTrue(items.get(0).getImageUrl() != null && !items.get(0).getImageUrl().isEmpty());
    }
}
