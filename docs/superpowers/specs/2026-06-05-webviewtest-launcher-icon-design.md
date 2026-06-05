# WebViewTest Launcher Icon Design

## Context

WebViewTest is an Android H5/WebView testing app for loading URLs and local HTML, scanning QR codes, saving reproducible test cases, changing WebView capability settings, and inspecting page state through an in-app debug panel.

The current launcher icon is the default Android template and does not communicate the app's developer-tool purpose.

## Selected Direction

Use the "Debug Console" direction selected by the user.

The icon represents a compact browser/debug console window:

- A dark console surface for developer-tool identity.
- A light top browser bar to keep the WebView/page context visible.
- Small window-control dots in the app theme's blue and teal accents.
- A command prompt chevron and horizontal result lines to suggest console logs, JavaScript execution, and page inspection.

## Android Icon Requirements

The replacement should use Android adaptive icon resources:

- `mipmap-anydpi-v26/ic_launcher.xml`
- `mipmap-anydpi-v26/ic_launcher_round.xml`
- `drawable/ic_launcher_background.xml`
- `drawable/ic_launcher_foreground.xml`

The foreground artwork must stay inside the adaptive icon safe area so launcher masks do not clip the symbol. The background must provide enough contrast in circular, rounded-square, and themed launcher contexts.

For Android 13 themed icons, `monochrome` should point to a single-color foreground drawable rather than relying on the full-color foreground.

## Resource Plan

Replace the template Android robot icon with vector drawables:

- `ic_launcher_background.xml`: flat blue background with a subtle teal accent shape, matching the app's Material theme.
- `ic_launcher_foreground.xml`: full-color console/browser mark.
- `ic_launcher_monochrome.xml`: single-color console/browser mark for themed icons.

Keep the existing manifest references unchanged because they already point at `@mipmap/ic_launcher` and `@mipmap/ic_launcher_round`.

## Acceptance

- The launcher icon no longer uses default Android template artwork.
- The icon reads as a WebView/debugging developer tool at small sizes.
- Adaptive icon XML remains valid and references existing drawable resources.
- The project compiles after replacing icon resources.
