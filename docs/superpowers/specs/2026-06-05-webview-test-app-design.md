# WebView Test App Design

## Overview

This project will become an Android WebView testing app for repeatedly loading, previewing, and debugging H5 pages on real devices or emulators. The first version is a complete testing-platform prototype, not a simple browser: it supports URL input, internal QR scanning, test cases, history, WebView and Android capability toggles, an in-app debug panel, JSON import/export, and a manual Settings switch that enables WebView debugging even in release builds.

The existing project is a fresh Kotlin + Compose Android app. The implementation should keep that direction and build the app as a single-module Compose application with native Android WebView embedded through `AndroidView`.

## Confirmed Scope

The first version will include:

- A single-session WebView testing workbench.
- Manual URL entry, paste support, internal camera QR scanning, and remote `http`/`https` page loading.
- Test case library with per-case URL and WebView test configuration.
- Automatic local history for visited URLs.
- Local persistence plus JSON import/export for test cases and configurations.
- Settings page with a manual `Enable WebView debugging` switch. The switch is off by default, including release builds.
- WebView and Android capability toggles for common H5 test scenarios.
- In-app common debugging panel.
- Competitive-reference additions: fullscreen view, local HTML file loading, reset menus, view/delete cookie and storage, view source, simplified HTML elements view, downloads, desktop mode, fullscreen video, context menu, and Web media permission testing.
- Emulator-based end-to-end verification as part of implementation acceptance.

The first version will not include:

- Full Chrome DevTools parity.
- Complete network header/body capture.
- JavaScript breakpoints.
- Performance timeline.
- Full Chrome-style Elements inspector with live CSS editing.
- Phone/device identifier permission testing.
- Cloud synchronization.
- Multi-tab or multi-session state.

## Architecture

The app will remain a Kotlin + Compose Android application. The main experience is a single-session debugging workbench:

- Top area: URL input, paste, scan, load, refresh, and fullscreen actions.
- Center area: native `WebView` hosted by Compose through `AndroidView`.
- Bottom panel on phones: tabs for `Config`, `Debug`, `Cases`, and `History`.
- Side panel on large screens or landscape: the same tabs can sit beside the WebView.
- Settings page: global settings such as release WebView debugging and reset actions.

Recommended package boundaries:

- `web/`: WebView factory, settings application, `WebViewClient`, `WebChromeClient`, JavaScript bridge, file chooser, geolocation handling, media permission handling, fullscreen video handling, download handling, context menu integration, and event callbacks.
- `data/`: Room entities/DAOs/repository for test cases and history; DataStore for global app settings.
- `scanner/`: CameraX preview and ML Kit barcode scanning.
- `debug/`: Debug event models for console logs, load errors, page state, cookies, storage snapshots, source snapshots, DOM summaries, and JavaScript execution results.
- `ui/`: Compose screens for Workbench, Cases, History, Settings, Scanner, debug panels, source viewer, elements viewer, and import/export flows.

The WebView layer must not directly read or write the database. UI and ViewModels produce a `WebTestConfig`; the WebView layer applies that config and emits typed events back to ViewModels.

## Main User Flows

### Temporary URL Testing

The user enters or pastes a URL in the Workbench and taps load. The app normalizes the URL, loads it in the WebView, and writes a `HistoryEntry` after the main page commits or finishes loading. Invalid URLs are rejected with an editable error state.

### Internal QR Scanning

The user taps scan, opens an in-app CameraX scanner, and scans a QR code. If the result is a URL, the app returns to the Workbench and either fills or loads it according to the current setting. If the result is not a URL, the app shows the scanned text and lets the user copy it or edit it into a URL.

Camera permission is requested only when the scanner opens.

### Save Current Page As Test Case

The user saves the current URL and current `WebTestConfig` as a named test case with an optional note. The case appears in the Cases panel and can be reopened from that panel.

### Reproduce Test Case

The user opens a test case. The app restores its URL and configuration into the single workbench session, then loads the WebView. Because v1 is single-session, opening a case replaces the current workbench state.

### Switch Test Environment

The user changes options in the Config panel. For options that need a reload, the UI asks the user to reload now or apply on next load. Volatile actions such as clearing cookies, cache, localStorage, sessionStorage, or console logs run immediately after confirmation.

### Debug Current Page

The user opens the Debug panel and can inspect console logs, page errors, page state, cookies, storage, source, simplified DOM elements, and JavaScript execution results. Complex debugging continues through desktop Chrome DevTools after enabling the Settings switch.

### Import And Export

The Cases screen exports test cases and their configs to JSON. History and global Settings are not exported. Import detects conflicts by case name plus URL and offers overwrite, rename imported case, or skip.

## Data Model

### `TestCase`

- `id`
- `name`
- `url`
- `note`
- `configJson`
- `createdAt`
- `updatedAt`
- `lastOpenedAt`

### `HistoryEntry`

- `id`
- `url`
- `title`
- `sourceType`: `REMOTE_URL` or `LOCAL_FILE`
- `visitedAt`

### `AppSettings`

Stored in DataStore:

- `webContentsDebuggingEnabled`
- default `WebTestConfig`
- `autoLoadScannedUrl`
- `startInFullscreen`

### `WebTestConfig`

The first version includes:

- User agent mode: default, custom, desktop.
- Custom user agent string.
- Cookie enabled.
- Third-party cookie enabled.
- Cache mode: default, no cache, clear cache before load.
- Mixed content mode: allow or block.
- Color mode: follow system, force light, force dark where WebView/page support allows it.
- JavaScript enabled.
- DOM Storage enabled.
- Geolocation permission policy: allow, deny, ask every time.
- File chooser policy: allow or deny.
- Camera permission policy for web pages: allow, deny, ask every time.
- Microphone permission policy for web pages: allow, deny, ask every time.
- Back handling: WebView back first, then app default.
- Desktop mode.
- Start fullscreen for this case.

## WebView And Android Feature Support

### HTTP And HTTPS Loading

The app supports loading remote `http` and `https` pages because H5 compatibility testing often needs non-production endpoints. The implementation should include an explicit cleartext traffic policy for this test app instead of relying on Android defaults. SSL errors remain blocked by default and recorded in the Debug panel.

### Fullscreen View

The Workbench provides a fullscreen toggle. In fullscreen mode, browser chrome and debug panels are hidden. The user can leave fullscreen through system back or a visible exit affordance. A global setting and per-case config can start the Workbench in fullscreen.

### Local File Loading

The Workbench can load local HTML files through Android's system file picker. Local file history entries are marked with `sourceType = LOCAL_FILE`. The implementation should avoid broad storage permissions by using the system picker and persisted URI permissions only when needed.

### Reset Menu

Settings exposes reset actions:

- Clear history.
- Reset global WebView defaults.
- Clear current debug logs.
- Clear cookies.
- Clear WebView cache.
- Clear storage for the current page where supported.

Destructive reset actions require confirmation.

### Cookie And Storage Management

The Debug panel can:

- View current URL cookies.
- Delete current URL cookies.
- View localStorage key/value snapshot.
- Delete localStorage keys or clear localStorage for the current page.
- View sessionStorage key/value snapshot.
- Delete sessionStorage keys or clear sessionStorage for the current page.

Storage reads and deletes use `evaluateJavascript` against the current page.

### View Source

The app can capture a current HTML source snapshot through JavaScript and show it in a read-only source viewer. It should be documented as a runtime DOM/source snapshot, not guaranteed to be the original network response body.

### Simplified HTML Elements

The app provides a simplified DOM view:

- Tag tree or flattened node summary.
- Search by tag, id, or class.
- Node attributes and text preview.

It does not provide live style editing or click-to-select inspection in v1.

### Download Handling

The WebView download listener routes downloads to the Android system download/save flow. The debug panel records download start metadata such as URL, MIME type, and content disposition when available.

### Desktop Mode

Desktop mode changes the user agent and related viewport behavior where feasible. It is exposed as a `WebTestConfig` option and can be saved per test case.

### Fullscreen Video

`WebChromeClient.onShowCustomView` and `onHideCustomView` are handled so HTML video fullscreen works inside the test app.

### Context Menu

Long-press context actions cover common resource types:

- Copy link URL.
- Open link in the current session.
- Download resource when supported.
- Copy or view image/video resource URL when available.

### Web Permissions

The app supports testing geolocation, camera, and microphone web permissions. Runtime Android permissions are requested only when the H5 page triggers the relevant feature and the current policy allows or asks.

## Debugging Design

The in-app Debug panel is for common device-side investigation:

- `Console`: from `WebChromeClient.onConsoleMessage`, including level, message, source, and line.
- `Errors`: page load errors, HTTP errors, SSL errors, and main failing URL where available.
- `Page`: current URL, title, loading progress, can go back, can go forward.
- `Cookies`: current URL cookie snapshot and clear action.
- `Storage`: localStorage and sessionStorage snapshots plus delete/clear actions.
- `Source`: current HTML snapshot.
- `Elements`: simplified DOM tree/search.
- `JS Exec`: execute JavaScript through `evaluateJavascript` and show the JSON/string result.
- `Simple Requests`: navigation and resource URLs visible through WebView callbacks; this does not promise full request/response headers or bodies.
- `Downloads`: download callback metadata.

Desktop Chrome DevTools remains the path for complete debugging:

- Settings has `Enable WebView debugging`.
- The setting defaults to off, including release builds.
- When turned on, the app calls `WebView.setWebContentsDebuggingEnabled(true)`.
- Settings shows instructions to connect the device and open `chrome://inspect` in desktop Chrome.

## Error Handling

- Invalid URL: show an editable error and do not load.
- QR result is not URL: show recognized text with copy/edit actions.
- Camera permission denied: return to manual URL entry and explain that scanning needs camera permission.
- Geolocation/camera/microphone web permission: follow current `WebTestConfig` policy.
- SSL error: block by default and record the error. V1 does not include a global "ignore all certificate errors" action.
- File chooser denied by config: cancel the request back to the page.
- Local file load failure: show the URI and error message.
- JSON import parse failure: show the file name and parse error.
- JSON import conflict: ask overwrite, rename imported case, or skip.

## Testing Strategy

### Unit Tests

Unit tests should cover:

- URL normalization and validation.
- QR scan result classification.
- `WebTestConfig` serialization and deserialization.
- JSON export and import conflict handling.
- History insertion and deduplication policy.
- Debug event reducers.

### ViewModel Tests

ViewModel tests should cover:

- Loading a URL from manual input.
- Applying a test case to the workbench.
- Saving current workbench state as a test case.
- Clearing history.
- Resetting WebView defaults.
- Toggling `webContentsDebuggingEnabled`.
- Clearing debug logs.
- Importing and exporting cases.

### Emulator End-To-End Tests

Implementation is not complete until an Android emulator is launched and the app is exercised end to end. The acceptance run should include:

- Install and launch the debug build on an emulator.
- Load an `https` page from manual URL input.
- Load an `http` page and confirm the app's cleartext traffic policy supports the test flow.
- Load a local HTML file through the system picker.
- Open the scanner flow and verify camera permission handling. If the emulator cannot scan a real QR image reliably, use an injected or test fixture flow for the scan result and still verify the permission path.
- Save the current URL and config as a test case.
- Reopen the test case and confirm URL/config restoration.
- Change UA, desktop mode, cache, mixed content, JavaScript, DOM Storage, cookie, geolocation, camera, and microphone policies.
- Verify WebView back handling.
- Capture console logs through a test page.
- Execute JavaScript and display the result.
- View and clear cookies.
- View and clear localStorage and sessionStorage.
- View source and simplified elements.
- Trigger download handling.
- Trigger fullscreen WebView and fullscreen video.
- Export test cases to JSON and import them again with conflict handling.
- Open Settings, enable WebView debugging, and verify the setting state.
- Build a release variant, install it on an emulator or device, confirm WebView debugging is off by default, enable it manually in Settings, and verify desktop Chrome can inspect the WebView through `chrome://inspect`.

### Manual Device Smoke Test

At least one physical device smoke test should follow emulator validation for:

- Camera QR scanning with a real QR code.
- USB connection to desktop Chrome `chrome://inspect`.
- File chooser behavior.
- Download behavior.
- Fullscreen video.
