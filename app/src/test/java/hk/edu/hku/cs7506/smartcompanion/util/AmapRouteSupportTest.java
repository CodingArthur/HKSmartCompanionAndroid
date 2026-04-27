package hk.edu.hku.cs7506.smartcompanion.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.amap.api.services.busline.BusStationItem;
import com.amap.api.services.route.BusPath;
import com.amap.api.services.route.BusStep;
import com.amap.api.services.route.Doorway;
import com.amap.api.services.route.RouteBusLineItem;
import com.amap.api.services.route.RouteBusWalkItem;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

import hk.edu.hku.cs7506.smartcompanion.data.model.LocationSnapshot;
import hk.edu.hku.cs7506.smartcompanion.data.model.RecommendationItem;
import hk.edu.hku.cs7506.smartcompanion.data.model.RouteMode;
import hk.edu.hku.cs7506.smartcompanion.data.model.RouteStep;
import hk.edu.hku.cs7506.smartcompanion.data.model.SceneType;

public class AmapRouteSupportTest {
    @Test
    public void translatesCommonNavigationPromptsIntoEnglish() {
        assertEquals(
                "In 300 meters, turn right.",
                AmapRouteSupport.translateNavigationPromptToEnglish("\u524d\u65b9300\u7c73\u53f3\u8f6c")
        );
        assertEquals(
                "You have arrived at the destination.",
                AmapRouteSupport.translateNavigationPromptToEnglish("\u5df2\u5230\u8fbe\u76ee\u7684\u5730\u9644\u8fd1")
        );
        assertEquals(
                "Recalculating the route.",
                AmapRouteSupport.translateNavigationPromptToEnglish("\u6b63\u5728\u91cd\u65b0\u89c4\u5212\u8def\u7ebf")
        );
    }

    @Test
    public void detectsAmapAuthorizationErrors() {
        assertTrue(AmapRouteSupport.isAmapAuthorizationError(1008));
        assertFalse(AmapRouteSupport.isAmapAuthorizationError(1000));
        assertFalse(AmapRouteSupport.isAmapAuthorizationError(1002));
    }

    @Test
    public void buildsEnglishFallbackRouteSteps() {
        LocationSnapshot origin = new LocationSnapshot(
                22.2830,
                114.1371,
                "The University of Hong Kong fallback origin",
                "Hong Kong",
                "Central and Western",
                "Fallback",
                true
        );
        RecommendationItem item = new RecommendationItem(
                "play-1",
                SceneType.PLAY,
                "West Kowloon Art Park",
                "Reason",
                22.3012,
                114.1602,
                89.4,
                20,
                null,
                null,
                null,
                null,
                0.86,
                0.82
        );

        List<RouteStep> steps = AmapRouteSupport.buildFallbackRouteSteps(origin, item, RouteMode.DRIVE);

        assertEquals(3, steps.size());
        assertEquals("1. Leave origin", steps.get(0).getTitle());
        assertTrue(steps.get(0).getBody().contains("The University of Hong Kong fallback origin"));
        assertTrue(steps.get(1).getBody().contains("Drive toward West Kowloon Art Park"));
        assertEquals("3. Arrive", steps.get(2).getTitle());
    }

    @Test
    public void buildsDetailedEnglishTransitSteps() {
        Doorway entrance = new Doorway();
        entrance.setName("\u9999\u6e2f\u5927\u5b66\u7ad9C1\u53e3");

        Doorway exit = new Doorway();
        exit.setName("\u4e2d\u73af\u7ad9D2\u53e3");

        BusStationItem departure = new BusStationItem();
        departure.setBusStationName("\u9999\u6e2f\u5927\u5b66\u7ad9");

        BusStationItem arrival = new BusStationItem();
        arrival.setBusStationName("\u4e2d\u73af\u7ad9");

        RouteBusLineItem line = new RouteBusLineItem();
        line.setBusLineName("\u6e2f\u5c9b\u7ebf");
        line.setDepartureBusStation(departure);
        line.setArrivalBusStation(arrival);
        line.setPassStationNum(2);
        line.setDistance(1800f);
        line.setDuration(420f);

        RouteBusWalkItem walk = new RouteBusWalkItem();
        walk.setDistance(604f);
        walk.setDuration(540L);

        BusStep step = new BusStep();
        step.setEntrance(entrance);
        step.setExit(exit);
        step.setWalk(walk);
        step.setBusLines(Collections.singletonList(line));

        BusPath path = new BusPath();
        path.setSteps(Collections.singletonList(step));

        List<RouteStep> steps = AmapRouteSupport.toBusRouteSteps(path);

        assertEquals(2, steps.size());
        assertEquals("1. Walk transfer", steps.get(0).getTitle());
        assertTrue(steps.get(0).getBody().contains("HKU Station Exit C1"));
        assertTrue(steps.get(0).getBody().contains("Central Station Exit D2"));
        assertEquals("2. Take MTR", steps.get(1).getTitle());
        assertTrue(steps.get(1).getBody().contains("Island Line"));
        assertTrue(steps.get(1).getBody().contains("HKU Station"));
        assertTrue(steps.get(1).getBody().contains("Central Station"));
        assertTrue(steps.get(1).getBody().contains("2 stops"));
    }
}
