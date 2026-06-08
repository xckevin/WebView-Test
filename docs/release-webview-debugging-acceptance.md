# Release WebView Debugging Acceptance

Use this checklist when validating the release build on a real Android device.

## Build and Install

```bash
./gradlew :app:assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
```

If the APK is unsigned for the target device flow, build a signed release APK from Android Studio or the configured release signing task, then install that artifact.

## Manual Checks

1. Launch `WebViewTest`.
2. Load `https://example.com` from the Workbench.
3. On desktop Chrome, open `chrome://inspect`.
4. Confirm the app WebView is not inspectable before enabling the setting.
5. In the app, open Settings and enable `Enable WebView debugging`.
6. Return to the Workbench and confirm the current WebView appears in `chrome://inspect`.
7. Open DevTools from Chrome and verify console/page inspection works.
8. Disable `Enable WebView debugging`.
9. Confirm the WebView disappears from `chrome://inspect` after refresh or page reload.

## Optional Connected Smoke Test

```bash
./gradlew :app:connectedDebugAndroidTest
```

The connected test validates the debug APK UI flows. The release debugging switch still needs the manual `chrome://inspect` check above because Chrome inspection visibility is an external desktop/device integration.
