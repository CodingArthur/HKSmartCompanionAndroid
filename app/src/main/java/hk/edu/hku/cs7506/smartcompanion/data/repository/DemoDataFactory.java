package hk.edu.hku.cs7506.smartcompanion.data.repository;

import java.util.ArrayList;
import java.util.List;

import hk.edu.hku.cs7506.smartcompanion.data.model.CommuteCorridor;
import hk.edu.hku.cs7506.smartcompanion.data.model.ParkingDestination;
import hk.edu.hku.cs7506.smartcompanion.data.model.RecommendationItem;
import hk.edu.hku.cs7506.smartcompanion.data.model.RecommendationRequest;
import hk.edu.hku.cs7506.smartcompanion.data.model.RouteStep;
import hk.edu.hku.cs7506.smartcompanion.data.model.SceneType;
import hk.edu.hku.cs7506.smartcompanion.util.PlaceProfileCatalog;

public final class DemoDataFactory {

    private DemoDataFactory() {
    }

    public static List<RecommendationItem> createRecommendations(RecommendationRequest request) {
        List<RecommendationItem> items = new ArrayList<>();
        SceneType sceneType = request.getSceneType();
        String parkingDestinationLabel = resolveParkingDestinationLabel(request.getParkingDestination());
        switch (sceneType) {
            case EMERGENCY:
                items.add(buildEmergencyDemo(
                        "er-1",
                        "Queen Mary Hospital",
                        "Shortest combined time among current demo candidates, balancing travel effort and queue length.",
                        22.2704,
                        114.1310,
                        86.5,
                        18,
                        22
                ));
                items.add(buildEmergencyDemo(
                        "er-2",
                        "Prince of Wales Hospital",
                        "Stronger waiting-time profile with a slightly longer route.",
                        22.3774,
                        114.2007,
                        79.2,
                        24,
                        16
                ));
                items.add(buildEmergencyDemo(
                        "er-3",
                        "Pamela Youde Nethersole Eastern Hospital",
                        "Useful eastern-side fallback with stable arrival time in this mock scenario.",
                        22.2699,
                        114.2361,
                        71.6,
                        19,
                        28
                ));
                break;
            case PARKING:
                items.add(buildParkingDemo(
                        "parking-1",
                        "Central Car Park A",
                        "High vacancy and balanced final walk make this the current top parking choice for " + parkingDestinationLabel + ".",
                        22.2820,
                        114.1588,
                        91.3,
                        9,
                        24,
                        260,
                        4,
                        "Central and Western District",
                        "1 Harbour View Street, Central, Hong Kong",
                        "2868 0000"
                ));
                items.add(buildParkingDemo(
                        "parking-2",
                        "Wan Chai Car Park B",
                        "Slightly farther away but with a healthier vacancy buffer and manageable final walk.",
                        22.2792,
                        114.1736,
                        88.1,
                        11,
                        31,
                        420,
                        6,
                        "Wan Chai District",
                        "2 Expo Drive, Wan Chai, Hong Kong",
                        "2582 8888"
                ));
                items.add(buildParkingDemo(
                        "parking-3",
                        "Kowloon City Car Park",
                        "Reasonable drive time, but the final walk and tighter vacancy make it less attractive.",
                        22.3286,
                        114.1868,
                        73.9,
                        13,
                        8,
                        680,
                        9,
                        "Kowloon City District",
                        "38 Shing Fung Road, Kai Tak, Kowloon",
                        ""
                ));
                break;
            case PLAY:
                items.add(new RecommendationItem("play-1", sceneType, "West Kowloon Art Park",
                        "Excellent balance between trip effort, event potential and favorable environmental conditions.",
                        22.3012, 114.1602, 89.4, 20, null, null, null, null, 0.86, 0.82,
                        "Current event",
                        "Demo cultural shortlist - West Kowloon",
                        "https://www.westk.hk/en",
                        "https://upload.wikimedia.org/wikipedia/commons/e/e8/M%2B%2C_West_Kowloon%2C_Hong_Kong.jpg",
                        "Visuals from Wikimedia Commons",
                        null,
                        "Nearby buses from High Speed Rail West Kowloon Station: W4 5 min, 970 8 min, 281A 11 min."));
                items.add(new RecommendationItem("play-2", sceneType, "Tai Kwun Event Zone",
                        "Convenient city-center option with strong event fit and short route time.",
                        22.2810, 114.1546, 84.6, 12, null, null, null, null, 0.78, 0.80,
                        "Art venue",
                        "Demo cultural shortlist - Central & Western",
                        "https://www.taikwun.hk/en",
                        "https://upload.wikimedia.org/wikipedia/commons/thumb/3/3b/Tai_Kwun_logo.svg/330px-Tai_Kwun_logo.svg.png",
                        "Visuals from Wikimedia Commons",
                        "Live TDAS drive time 12 min over 3.8 km.",
                        "Nearby buses from Central (Observation Wheel): 15C 4 min, 12A 7 min, 40M 9 min."));
                items.add(new RecommendationItem("play-3", sceneType, "Sai Kung Waterfront",
                        "Best environmental quality in the demo set, offset by a longer travel burden.",
                        22.3827, 114.2707, 76.1, 33, null, null, null, null, 0.92, 0.88,
                        "Anytime place",
                        "Demo outdoor shortlist - Sai Kung",
                        "https://www.discoverhongkong.com/eng/explore/great-outdoor/sai-kung.html",
                        null,
                        null,
                        null,
                        null));
                break;
            case COMMUTE:
                CommuteCorridor corridor = request.getCommuteCorridor() == null
                        ? CommuteCorridor.CENTRAL_TO_SHA_TIN
                        : request.getCommuteCorridor();
                items.add(new RecommendationItem(
                        "commute-1",
                        sceneType,
                        "Citybus 182X",
                        "Board in 4 min after a 6 min access walk. Estimated in-vehicle time on the selected corridor is about 30 min.",
                        corridor.getDestinationLatitude(),
                        corridor.getDestinationLongitude(),
                        90.4,
                        4,
                        null,
                        null,
                        420,
                        6,
                        30,
                        null,
                        null,
                        "Commute line",
                        "Demo live corridor · Central terminal to New Territories East",
                        null,
                        null,
                        null,
                        "Sha Tin, Hong Kong",
                        null,
                        null,
                        "Board at Central (Macao Ferry). Demo ETA and access walk shown for stable fallback."
                ));
                items.add(new RecommendationItem(
                        "commute-2",
                        sceneType,
                        "Citybus 182",
                        "Slightly longer access walk, but the direct corridor is still competitive for this demo snapshot.",
                        corridor.getDestinationLatitude(),
                        corridor.getDestinationLongitude(),
                        82.1,
                        7,
                        null,
                        null,
                        600,
                        8,
                        32,
                        null,
                        null,
                        "Commute line",
                        "Demo live corridor · alternate direct departure",
                        null,
                        null,
                        null,
                        "Sha Tin, Hong Kong",
                        null,
                        null,
                        "Board at Central (Macao Ferry). Demo ETA and ride burden are used while live feeds are unavailable."
                ));
                break;
        }
        return items;
    }

    private static String resolveParkingDestinationLabel(ParkingDestination parkingDestination) {
        if (parkingDestination == null) {
            return "Central Waterfront";
        }
        switch (parkingDestination) {
            case HKCEC:
                return "HKCEC";
            case WEST_KOWLOON:
                return "West Kowloon";
            case KAI_TAK:
                return "Kai Tak";
            case CENTRAL_WATERFRONT:
            default:
                return "Central Waterfront";
        }
    }

    public static List<RouteStep> createRouteSteps(RecommendationItem item) {
        List<RouteStep> steps = new ArrayList<>();
        steps.add(new RouteStep("Leave current origin",
                "Start from the current mock origin and head toward " + item.getName() + "."));
        steps.add(new RouteStep("Primary connector road",
                "Follow the main city corridor to keep the route steady and predictable."));
        steps.add(new RouteStep("Local access segment",
                "Use the final district road to enter the target area near the destination."));
        steps.add(new RouteStep("Arrival and handoff",
                "Arrive at " + item.getName() + " and continue with the in-app navigator when available."));
        return steps;
    }

    private static RecommendationItem buildEmergencyDemo(
            String id,
            String name,
            String reason,
            double latitude,
            double longitude,
            double score,
            int etaMinutes,
            int waitMinutes
    ) {
        PlaceProfileCatalog.PlaceProfile profile = PlaceProfileCatalog.forHospital(name);
        return new RecommendationItem(
                id,
                SceneType.EMERGENCY,
                name,
                reason,
                latitude,
                longitude,
                score,
                etaMinutes,
                waitMinutes,
                null,
                null,
                null,
                null,
                null,
                profile == null ? null : profile.getContentTag(),
                profile == null ? null : profile.getMetadataLine(),
                profile == null ? null : profile.getDetailsUrl(),
                profile == null ? null : profile.getImageUrl(),
                profile == null ? null : profile.getImageAttribution(),
                profile == null ? null : profile.getAddressLine(),
                profile == null ? null : profile.getContactPhone(),
                null,
                null
        );
    }

    private static RecommendationItem buildParkingDemo(
            String id,
            String name,
            String reason,
            double latitude,
            double longitude,
            double score,
            int etaMinutes,
            int vacancy,
            int walkDistance,
            int walkMinutes,
            String district,
            String addressLine,
            String contactPhone
    ) {
        PlaceProfileCatalog.PlaceProfile profile = PlaceProfileCatalog.createParkingFallback(
                district,
                "",
                addressLine,
                contactPhone
        );
        return new RecommendationItem(
                id,
                SceneType.PARKING,
                name,
                reason,
                latitude,
                longitude,
                score,
                etaMinutes,
                null,
                vacancy,
                walkDistance,
                walkMinutes,
                null,
                null,
                profile.getContentTag(),
                profile.getMetadataLine(),
                profile.getDetailsUrl(),
                profile.getImageUrl(),
                profile.getImageAttribution(),
                profile.getAddressLine(),
                profile.getContactPhone(),
                null,
                null
        );
    }
}
