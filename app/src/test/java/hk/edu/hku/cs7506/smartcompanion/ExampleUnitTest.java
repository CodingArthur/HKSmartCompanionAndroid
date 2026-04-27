package hk.edu.hku.cs7506.smartcompanion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

import hk.edu.hku.cs7506.smartcompanion.data.model.CommuteCorridor;
import hk.edu.hku.cs7506.smartcompanion.data.model.DataMode;
import hk.edu.hku.cs7506.smartcompanion.data.model.EmergencySortMode;
import hk.edu.hku.cs7506.smartcompanion.data.model.ParkingDestination;
import hk.edu.hku.cs7506.smartcompanion.data.model.RecommendationItem;
import hk.edu.hku.cs7506.smartcompanion.data.model.RecommendationRequest;
import hk.edu.hku.cs7506.smartcompanion.data.model.SceneType;
import hk.edu.hku.cs7506.smartcompanion.data.repository.DemoDataFactory;

public class ExampleUnitTest {
    @Test
    public void dataModeFallsBackToAuto() {
        assertEquals(DataMode.AUTO, DataMode.fromName("unknown"));
        assertEquals(DataMode.OFFICIAL, DataMode.fromName("official"));
    }

    @Test
    public void emergencyDemoSceneHasItems() {
        List<RecommendationItem> items = DemoDataFactory.createRecommendations(new RecommendationRequest(
                SceneType.EMERGENCY,
                22.3027,
                114.1772,
                EmergencySortMode.TOTAL_TIME,
                ParkingDestination.CENTRAL_WATERFRONT,
                CommuteCorridor.CENTRAL_TO_SHA_TIN
        ));
        assertFalse(items.isEmpty());
        assertTrue(items.get(0).getStableId().startsWith("emergency:"));
    }
}
