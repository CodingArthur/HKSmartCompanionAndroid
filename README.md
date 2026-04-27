# HK Smart Companion

HK Smart Companion is a native Android app for the HKU CS 7506 project. It is designed as a Hong Kong decision-support companion that combines official live data, local caching, route preview, and in-app navigation entry points in one client.

The app now supports four end-user scenes:

- Emergency Recommendation
- Parking Recommendation
- Leisure Recommendation
- Commute Assistant

It also includes:

- detail pages with map and navigation entry
- favorites and recent history
- official / demo / auto-fallback data modes
- live feed caching and background prefetch
- multi-mode route preview for drive, walk, ride, and transit

## What The App Does

### 1. Emergency Recommendation

Ranks Hospital Authority A&E options using current waiting time and travel burden.

Available ranking modes:

- Best combined time
- Nearest hospital
- Shortest wait

### 2. Parking Recommendation

Ranks car parks using current vacancy, drive time, and final walking distance to a chosen destination area.

Available destination presets:

- Central
- HKCEC
- West Kowloon
- Kai Tak

### 3. Leisure Recommendation

Recommends activities and venues using live conditions.

The current logic combines:

- fresh HKTB event data when available
- evergreen museum / venue fallback picks
- Hong Kong Observatory weather
- AQHI
- nearby public transport hints

### 4. Commute Assistant

Provides direct-bus commute recommendations using official Citybus live ETA data plus access walk burden and approximate ride time.

Available commute corridors:

- Central to Sha Tin
- Central to Stanley
- Exhibition Centre to Lai Chi Kok
- Kai Tak to Airport
- Western District to East Harbourfront

## Product Features

- Compact recommendation pages with scene-specific filters
- Detail page with decision metrics, live transport notes, and hero images
- Favorites page for saved places
- Recent-history card on the home screen
- Settings page for source mode selection and cache warming
- Share action from detail page
- Official source handoff in browser
- Route preview map with `Drive / Walk / Ride / Transit`
- Navigation preview page with route summary and step list
- AMap drive-session entry and simulation entry when available

## Data Sources

The app currently integrates these official or public feeds:

- Hospital Authority A&E waiting time
- data.gov.hk car park information
- data.gov.hk parking vacancy
- HKTB events feed
- LCSD museum directory
- Hong Kong Observatory weather
- AQHI
- TDAS journey-time data
- Special Traffic News
- Citybus ETA and stop data

## Key Behavior Notes

### Location fallback

- If the live device fix is in Hong Kong, the app uses it.
- If the live fix is outside Hong Kong, the app falls back to `The University of Hong Kong`.
- If the live fix times out or fails, the app uses cached origin or the same HKU fallback.

### Data modes

- `AUTO`: prefer official feeds, then fall back to demo data
- `OFFICIAL`: require official feeds
- `DEMO`: use bundled deterministic demo data

### Leisure freshness guard

- If the current event feed is stale, already-ended, or otherwise unsuitable, the app suppresses outdated event cards and falls back to stable leisure venues instead of surfacing obviously old content.

### Route modes

- `Drive`, `Walk`, and `Ride` all support preview routing.
- `Transit` is a preview-first route-planning mode.
- The navigation page provides drive-session entry points and still falls back gracefully if a provider route is unavailable.

## Project Structure

```text
app/src/main/java/hk/edu/hku/cs7506/smartcompanion
|- data
|  |- local
|  |- location
|  |- model
|  |- network
|  `- repository
|- ui
|  |- adapter
|  |- favorites
|  |- map
|  |- navigation
|  `- settings
|- util
|- MainActivity.java
|- PlaceDetailActivity.java
`- RecommendationListActivity.java
```

## Setup

### 1. Configure the AMap key

Create `amap.properties` in the project root:

```properties
AMAP_API_KEY=your_amap_key
```

The app reads the key from:

- Gradle project properties
- `amap.properties`
- environment variables

### 2. Build the app

```powershell
cd D:\7506project
$env:JAVA_HOME='D:\JDK'
$env:Path='D:\JDK\bin;'+$env:Path
.\gradlew.bat assembleDebug
```

### 3. Run unit tests

```powershell
cd D:\7506project
$env:JAVA_HOME='D:\JDK'
$env:Path='D:\JDK\bin;'+$env:Path
.\gradlew.bat testDebugUnitTest
```

### 4. Install to emulator or device

```powershell
C:\Users\HF\AppData\Local\Android\Sdk\platform-tools\adb.exe install -r D:\7506project\app\build\outputs\apk\debug\app-debug.apk
```

## Validation Summary

Latest validation was performed on `2026-04-16` on Android emulator `emulator-5554`.

Build verification:

- `testDebugUnitTest` passed
- `assembleDebug` passed

Manual smoke-test coverage:

- Home page scene launchers
- Emergency recommendation load and ranking-mode switch
- Parking recommendation load and destination switch
- Leisure recommendation load
- Commute Assistant load and corridor switch
- Detail page load
- Detail page call action via a phone-backed demo parking record
- Favorites persistence and favorites list
- Settings page load
- Share chooser launch
- Official source browser handoff
- Map preview page
- Navigation preview page
- Simulated AMap drive session launch

## Known Limits

- Some live feeds are slower than others. Parking and commute requests may take longer than emergency recommendations.
- Transit remains a route-planning surface rather than a full turn-by-turn session.
- Final route quality depends on upstream provider responses.
- Leisure content still depends on the freshness of public event feeds, so the app intentionally falls back to stable venues when live event feeds are outdated.

## Documentation

Full project documentation is in:

- `D:\7506project\docs\PROJECT_DOCUMENTATION.md`
