# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Compile debug
./gradlew :app:compileDebugKotlin

# Install debug APK to connected device
./gradlew :app:installDebug

# Unit tests
./gradlew :app:testDebugUnitTest

# Single test class
./gradlew :app:testDebugUnitTest --tests "com.xckevin.android.app.webview.test.util.UrlNormalizerTest"

# Compile instrumentation tests
./gradlew :app:compileDebugAndroidTestKotlin

# Run connected tests (requires device/emulator)
./gradlew :app:connectedDebugAndroidTest
```

## Architecture

Android WebView testing tool. Single-module app (`app/`), package `com.xckevin.android.app.webview.test`.

**DI**: Manual via `AppContainer` (no Hilt). Created as a remembered Composable singleton holding Room DB, repositories, and DataStore.

**State management**: MVI-like with `WorkbenchViewModel` + `MutableStateFlow<WorkbenchState>`. Debug events use a dedicated `DebugReducer` (pure function: `DebugState + DebugAction → DebugState`).

**Navigation**: Navigation Compose with type-safe `@Serializable` route objects (`WorkbenchRoute`, `ScannerRoute`, `SettingsRoute`).

**WebView integration**: `WebViewHost` is a Compose `AndroidView` wrapper. The WebView layer only applies settings and emits `WebPageEvent`s upstream — it never touches the database. The ViewModel is the boundary between UI, persistence, and WebView events.

**Data layer**: Room (test cases, history) + DataStore Preferences (app settings). Repository interfaces with Room implementations. Import/export uses kotlinx.serialization JSON.

**Navigation tracking**: `WebViewNavigationTracker` assigns monotonic IDs to navigations so stale callbacks from previous page loads are ignored.

## Key Design Rules

- WebView layer is event-only: config in, events out. No direct DB reads/writes from `web/` package.
- Web permissions use policy enums (`ALLOW`, `DENY`, `ASK_EVERY_TIME`) configured in `WebTestConfig`.
- Local HTML uses system file chooser (`content://` URIs) — no broad storage permissions.
- Release WebView debugging is OFF by default; must be explicitly enabled via Settings toggle.
- `WebTestConfig` is serializable and stored with test cases — changing its fields affects Room schema migration and JSON import/export compatibility.

## Environment

- JDK 17, Android SDK Platform 36, Build Tools 36.1.0
- AGP 9.x, Kotlin with Compose compiler plugin, KSP for Room
- minSdk 24, targetSdk 36
- `local.properties` must have `sdk.dir`
