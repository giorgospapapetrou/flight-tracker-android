# Flight Tracker — Android

A Jetpack Compose Android app that visualises live and historical aircraft positions served by the [flight-tracker-backend](https://github.com/giorgospapapetrou/flight-tracker-backend).

## What it does

- **Map tab** — live aircraft positions with altitude-coloured silhouettes, tap an aircraft for telemetry
- **Aircraft tab** — list of every currently tracked aircraft with callsign, ICAO, altitude, speed, heading
- **History tab** — flight summaries within the backend's 7-day retention window, with date prefixes for older entries
- **Flight detail** — drawn flight path with start and end markers, plus a replay scrubber that interpolates position and heading along the route

## Stack

- Kotlin, Jetpack Compose (Material 3)
- Hilt for dependency injection, KSP for code generation
- Retrofit + OkHttp + kotlinx.serialization for HTTP and WebSocket
- [osmdroid](https://github.com/osmdroid/osmdroid) for map rendering
- Timber for logging
- Min SDK 26 (Android 8), tested on Android 14

## Requirements

- Android Studio (any recent version with AGP 8.x support)
- A running [flight-tracker-backend](https://github.com/giorgospapapetrou/flight-tracker-backend) reachable from the device

## Setup

```bash
git clone https://github.com/giorgospapapetrou/flight-tracker-android.git
cd flight-tracker-android
```

Create a `local.properties` file at the project root (it's git-ignored) with the API base URL and key:

```properties
sdk.dir=/path/to/your/Android/Sdk
API_BASE_URL=http://192.168.1.5:8000/
API_KEY=the-bearer-token-from-your-backends-.env
```

Then **Build → Clean Project → Rebuild → Run** in Android Studio. A clean rebuild is required after every `local.properties` change so `BuildConfig` regenerates.

## Connecting to the backend

The app supports two modes:

**Local network.** Set `API_BASE_URL` to `http://<laptop-ip>:8000/` and ensure `android:usesCleartextTraffic="true"` in `AndroidManifest.xml`. The phone and laptop must be on the same wifi.

**Public tunnel (ngrok).** Set `API_BASE_URL` to `https://<your-tunnel>.ngrok-free.dev/` and disable cleartext (`android:usesCleartextTraffic="false"`). The phone reaches the laptop from anywhere, including cellular.

> ⚠️ Free ngrok URLs change every time the tunnel restarts. Keep the tunnel running between development sessions, or upgrade to a reserved domain for stable demos.

## Architecture

```
ui/             # Compose screens and theme
  splash/       # connection check screen
  map/          # live map with telemetry sheet
  aircraftlist/ # active aircraft list
  history/      # flight summaries list
  flightdetail/ # path replay screen
data/
  api/          # Retrofit interfaces, OkHttp config
  repository/   # data sources for aircraft, flights, health
  websocket/    # WebSocket client with auto-reconnect
domain/
  model/        # plain data classes
di/             # Hilt modules
```

The WebSocket client maintains a persistent connection with exponential backoff (1s → 30s) and pushes events into a flow consumed by the map and aircraft list ViewModels. The history list refreshes every 15 seconds via REST polling.

## Map and assets

The plane silhouette on the map is drawn programmatically as a bitmap, with colours based on altitude:

| Altitude              | Colour        |
|-----------------------|---------------|
| Below 10,000 ft       | Amber         |
| 10,000 – 30,000 ft    | Teal          |
| Above 30,000 ft       | Deep blue     |
| Unknown               | Grey          |

The launcher icon and the in-app airplane icon use the same blue gradient design (`#1E88E5` → `#0D47A1`).

## Known limitations

- **Live data depends on the backend.** When the backend is unreachable, the splash shows an error and the user is prompted to retry. There is no offline cache.
- **Position interpolation is great-circle.** Replay headings are computed along great circles between known positions. Real flight paths are slightly different, but the difference is invisible at typical demo zoom levels.
- **Map tiles need internet.** osmdroid fetches OpenStreetMap tiles on demand; an offline map cache is not bundled.
- **No deep links or external sharing.** Aircraft and flight URLs are not exposed.

## Companion backend

The backend lives at [flight-tracker-backend](https://github.com/giorgospapapetrou/flight-tracker-backend).
