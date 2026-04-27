# HK Smart Companion Project Documentation

## 1. Project Summary

HK Smart Companion is a native Android decision-support app developed for the HKU CS 7506 project. The app is positioned as a compact Hong Kong mobility companion that helps the user make fast, practical decisions in real time.

The current version supports four main scenes:

- Emergency Recommendation
- Parking Recommendation
- Leisure Recommendation
- Commute Assistant

The app also includes:

- detail pages
- map preview
- navigation preview
- favorites
- recent history
- settings
- local cache warming and background prefetch

The older split frontend/backend prototype is no longer the main delivery target. This project is the production-facing native Android implementation.

## 2. Product Goals

The app is built around one idea: reduce decision effort when a user needs an answer now.

Each scene answers a different question:

- Emergency: which hospital is the best immediate emergency option?
- Parking: which car park is the most practical for my destination?
- Leisure: where should I go now based on freshness, weather, air quality, and travel burden?
- Commute: which live direct bus corridor is the most practical for this route right now?

The product therefore tries to balance:

- official live data
- travel time
- waiting burden
- walking burden
- environmental conditions
- graceful fallback when a live source is stale or fails

## 3. User Experience Overview

## 3.1 Home

`MainActivity` is the scene launcher.

Home currently exposes:

- Emergency Recommendation
- Parking Recommendation
- Leisure Recommendation
- Commute Assistant
- Favorites
- Settings

It also shows:

- a live cache status card
- the most recently viewed place

## 3.2 Recommendation List

`RecommendationListActivity` is shared across all four scenes.

The list screen renders:

- source-state card only when needed
- current origin card
- compact scene filter controls
- ranked recommendation cards

Current scene filters:

- Emergency: ranking mode
- Parking: destination preset
- Commute: corridor preset
- Leisure: no extra front-row selector at the moment

The filter UI was intentionally refactored away from oversized horizontal controls. The current implementation uses compact selector buttons that open a single-choice dialog.

## 3.3 Place Detail

`PlaceDetailActivity` provides:

- hero image
- scene badge
- title
- metadata line
- decision reason
- metrics
- transport intelligence
- coordinates
- favorite action
- share action
- optional phone-call action when the item has a contact number
- map preview entry
- navigation preview entry
- official source handoff

## 3.4 Map Preview

`MapPreviewActivity` provides:

- destination map
- route summary
- live transport note
- mode switch for `Drive / Walk / Ride / Transit`
- handoff into navigation preview

## 3.5 Navigation Preview

`NavigationPreviewActivity` provides:

- the same four route modes
- route status
- route summary
- step list
- drive-session entry controls
- graceful fallback when a route provider response is missing or degraded

## 3.6 Favorites

`FavoriteListActivity` stores and displays user-saved places locally. The current list reuses recommendation metadata so favorites stay readable even without live reload.

## 3.7 Settings

`SettingsActivity` currently supports:

- data source mode switch
- official source summary
- cache status
- manual cache warming
- preference reset

## 4. Scene Design

## 4.1 Emergency Recommendation

Purpose:

- help the user choose a practical emergency department quickly

Inputs:

- Hospital Authority waiting time
- location-derived travel burden
- optional live TDAS transport enrichment

Ranking modes:

- Best combined time
- Nearest hospital
- Shortest wait

Core logic:

- `Best combined time` ranks by `drive ETA + waiting time`
- `Nearest hospital` emphasizes travel burden first
- `Shortest wait` emphasizes waiting time first

## 4.2 Parking Recommendation

Purpose:

- find the most practical car park for a destination area

Inputs:

- official car park catalog
- official vacancy
- travel burden from origin to car park
- final walking burden from car park to target district

Destination presets:

- Central
- HKCEC
- West Kowloon
- Kai Tak

Scoring intent:

- high vacancy helps
- shorter drive helps
- shorter last-mile walking helps strongly

## 4.3 Leisure Recommendation

Purpose:

- recommend nearby activities or stable cultural venues under current conditions

Inputs:

- HKTB event feed
- museum / venue directory fallback
- weather
- AQHI
- ETA
- nearby bus hints

Important design choice:

- stale or outdated event feeds are not shown just because they are official
- when live event freshness is poor, the app switches to stable venue recommendations instead

This is the reason the leisure scene now looks more mature than the earlier version that surfaced old 2024 activities.

## 4.4 Commute Assistant

Purpose:

- add a fourth scene that is not just another ranking variation of the first three
- provide a real transit-oriented assistant using live bus data

Current corridor presets:

- Central to Sha Tin
- Central to Stanley
- Exhibition Centre to Lai Chi Kok
- Kai Tak to Airport
- Western District to East Harbourfront

Current commute result structure:

- live next departure
- stop / operator metadata
- approximate access walk time
- approximate in-vehicle time
- short explanation of why this route is practical

Commute Assistant is intentionally scoped to fixed direct-bus corridors because that produces a cleaner, more reliable course-project demo than pretending to solve arbitrary full public-transit routing.

## 5. Architecture

## 5.1 Code Organization

Main package:

`app/src/main/java/hk/edu/hku/cs7506/smartcompanion`

Important folders:

- `data/model`: scene types, requests, recommendation items, enums
- `data/local`: preferences, cache store, recent history
- `data/location`: live location handling
- `data/network`: official-data clients, parsers, scoring, transport enrichment
- `data/repository`: application facade and demo data fallback
- `ui/*`: adapters and scene-specific pages
- `util`: formatting, AMap route support, helper catalogs

## 5.2 Repository Layer

`AppRepository` is the stable facade used by the activities.

It owns:

- settings
- favorites
- current location
- cache warming
- official-data fetch orchestration
- demo fallback

The UI therefore does not need to talk to raw parsers or HTTP clients directly.

## 5.3 Data Clients

The network layer is now split more clearly than the earlier monolithic version.

Important responsibilities:

- `OfficialOpenDataSource`: fetch raw official responses
- `OfficialDataParser`: parse and normalize raw payloads
- `RecommendationScorer`: score and rank normalized records
- `TransportIntelligenceClient`: enrich top items with TDAS / traffic / ETA context
- `CommuteAssistantClient`: build commute recommendations from Citybus route-stop, stop, and ETA feeds
- `OfficialDataClient`: orchestrate per-scene recommendation generation

## 5.4 Local State

Current local state includes:

- source mode preference
- favorites
- recent history
- last valid origin snapshot
- cached official feed responses

## 6. Source Modes

The app supports three modes:

- `AUTO`
- `OFFICIAL`
- `DEMO`

Behavior:

- `AUTO`: prefer live official feeds, then fall back
- `OFFICIAL`: do not fall back if live fetch fails
- `DEMO`: use local bundled sample data only

This mode system is important for coursework demos because it allows both stable offline presentation and realistic live behavior.

## 7. Current Official Data Sources

## 7.1 Emergency

- Hospital Authority A&E waiting time

Purpose:

- feed the emergency ranking modes

## 7.2 Parking

- official car park reference data
- official vacancy data

Purpose:

- combine structural parking data with current space availability

## 7.3 Leisure

- HKTB event feed
- LCSD museum / venue directory
- Hong Kong Observatory weather
- AQHI

Purpose:

- produce either fresh event picks or stable fallback venue picks

## 7.4 Commute

- Citybus route-stop
- Citybus stop detail
- Citybus ETA

Purpose:

- show live next-bus corridor recommendations

## 7.5 Shared transport enrichment

- TDAS journey-time data
- Special Traffic News
- nearby bus ETA lookups when relevant

Purpose:

- add explanatory transport intelligence to top-ranked items

## 8. Location Strategy

The app uses live location when it is trustworthy, but it now avoids obviously misleading fixes.

Current behavior:

- valid Hong Kong fix -> use it
- fix outside Hong Kong -> replace with HKU fallback origin
- timeout / provider failure -> use cached valid origin, otherwise HKU fallback

The fallback origin is:

- `The University of Hong Kong`

This was added specifically because emulator and mock-device coordinates can be wildly wrong and create absurd route times if accepted blindly.

## 9. Recommendation and Ranking Logic

## 9.1 Emergency

Important metrics:

- drive minutes
- waiting minutes
- combined minutes

## 9.2 Parking

Important metrics:

- spaces left
- drive minutes
- destination walk minutes

## 9.3 Leisure

Important metrics:

- weather score
- AQHI score
- ETA
- freshness / stability of content

## 9.4 Commute

Important metrics:

- next departure minutes
- access walk minutes
- ride minutes

## 10. Caching and Background Prefetch

The app now uses local feed caching and prefetch to behave more like a mature mobile app.

Current caching behavior:

- official responses are cached on-device
- app startup triggers background warming
- the home screen exposes cache status
- settings exposes manual cache warming

Benefits:

- repeat opens are faster
- source flakiness hurts less
- demo sessions are more stable

## 11. Map and Navigation Behavior

## 11.1 Map Preview

Supported modes:

- Drive
- Walk
- Ride
- Transit

The page shows:

- origin
- destination
- route summary
- status message
- transport note

## 11.2 Navigation Preview

The navigation page reuses the same route-mode model and renders:

- route status
- summary
- step list
- drive-session controls

The app also supports simulation entry from the drive session card for demonstrations.

## 11.3 Graceful degradation

If a provider route cannot be built, the app does not fail silently. It keeps the page usable by surfacing a fallback summary and simplified step list rather than exposing raw provider internals.

## 12. UI and Interaction Improvements

This iteration improved the app in several user-facing ways.

### 12.1 Compact filters

The older oversized list filters were replaced with smaller selector buttons plus modal single-choice dialogs.

Benefits:

- less vertical waste
- easier mobile tapping
- clearer current value display

### 12.2 Cleaner state messaging

The recommendation page now hides top source-state cards in normal successful cases instead of repeating noisy explanatory text on every screen.

### 12.3 Better visuals

The app now uses:

- hero images
- scene badges
- compact metric chips
- stronger card hierarchy

### 12.4 Place actions

The detail page now supports:

- favorite toggle
- share
- map preview
- navigation preview
- official source browser handoff

## 13. Testing and Validation

## 13.1 Automated build checks

Validated with:

```powershell
cd D:\7506project
$env:JAVA_HOME='D:\JDK'
$env:Path='D:\JDK\bin;'+$env:Path
.\gradlew.bat testDebugUnitTest assembleDebug
```

Result:

- passed on `2026-04-16`

## 13.2 Manual smoke test on emulator

Validated on:

- Android emulator `emulator-5554`

Manually verified flows:

1. Home page opens and shows all four scene launchers
2. Emergency scene loads and ranking switch updates to `Nearest hospital`
3. Parking scene loads and destination switch updates to `HKCEC`
4. Leisure scene loads and shows current venue recommendations
5. Commute Assistant loads and corridor switch updates to `Kai Tak to Airport`
6. Detail page opens from recent history
7. Demo parking detail shows `Call place` and launches the Android dialer
8. Favorites list contains saved places
9. Settings page opens and shows mode selection plus cache warm control
10. Share action launches Android share chooser
11. Official page action opens the source page in Chrome
12. Map preview opens and shows route summary plus mode switch
13. Navigation preview opens and shows route summary, step plan, and drive-session controls
14. Simulated drive launches the embedded AMap route activity

## 13.3 What was explicitly rechecked after the latest UI and commute changes

Because filter selection used asynchronous loading, regression testing specifically rechecked:

- emergency selector actually changing the ranked result
- parking selector actually changing the destination-specific reasoning text
- commute selector actually changing the line and corridor-specific explanation

This regression mattered because older async requests could overwrite newer choices. The current implementation guards against stale callbacks.

## 14. Known Limitations

- Parking and commute scenes can take noticeably longer to settle because they depend on slower live feeds.
- Transit remains preview-oriented rather than a full native turn-by-turn session.
- Commute Assistant currently focuses on curated direct-bus corridors instead of arbitrary public-transit journey planning.
- Leisure still depends on the freshness of public event feeds, so venue fallback remains necessary.
- Upstream provider outages can still degrade route quality even though the app now falls back more gracefully.

## 15. Future Extension Ideas

The current app is already course-ready, but the most practical next steps would be:

- expand Commute Assistant to more operators and corridors
- add side-by-side compare mode for top candidates
- add alerting for saved parking / commute items
- add a stronger offline snapshot page
- add open-hours intelligence for leisure and facility scenes

## 16. Conclusion

HK Smart Companion has evolved from a basic three-scene demo into a broader Android application with:

- four real user scenarios
- official-data integration
- local resilience features
- route preview and navigation entry
- cleaner mobile UI
- a tested end-to-end user flow

For a course project, the current version is much closer to a mature Android app than the original prototype because it now has:

- clearer product scope
- stronger scene separation
- practical live-data usage
- better fallback behavior
- better state persistence
- much more complete regression coverage
