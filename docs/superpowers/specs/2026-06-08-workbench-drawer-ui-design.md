# Workbench Drawer UI Design

## Overview

This design improves the WebViewTest Workbench UI for phone-sized screens. The current compact layout uses a bottom navigation bar plus a modal bottom sheet for `Config`, `Debug`, `Cases`, and `History`. That creates three practical issues:

- The bottom bar is visually heavy for a tool whose primary surface should be the WebView.
- Opening a bottom sheet can leave a visible bottom gap and still reveals underlying chrome.
- Debug and panel content are fragmented, with inconsistent density and repeated card styles.

The new compact-phone layout removes the bottom bar and main-panel bottom sheet. Workbench tools move into a sliding side drawer that overlays the WebView only when the user needs tools. The default state remains browser-first.

## Confirmed Scope

This first UI optimization pass includes:

- Replace compact-width bottom navigation with a hidden sliding drawer.
- Add a clear drawer entry in the compact URL bar.
- Keep the WebView full width when the drawer is closed.
- Remove the compact main-panel `ModalBottomSheet` path, fixing the bottom blank gap by design.
- Rework panel presentation so `Config`, `Debug`, `Cases`, and `History` share common dense UI primitives.
- Reduce Debug navigation from ten narrow tabs into fewer grouped modes.
- Keep fullscreen and video fullscreen behavior intact.
- Update Compose UI tests that depend on bottom navigation or panel visibility.

This pass does not include:

- A full app-wide visual redesign.
- A new Settings information architecture.
- A command palette.
- Multi-session or multi-tab browser behavior.
- Complete Chrome DevTools parity.

## Layout Design

### Compact Phone Default State

On compact-width screens, the Workbench default state is:

- Top URL bar.
- Full-width WebView.
- No bottom navigation bar.
- No persistent panel strip.

The URL bar gains a leading drawer button. The compact phone URL bar should show only:

- Drawer button.
- URL input.
- Load/reload action.
- Overflow action.

Secondary actions move into the overflow menu: scan, open local HTML, settings, and fullscreen. This keeps the bar to one row and prevents the current two-row toolbar from consuming WebView height.

The intended hierarchy is:

1. WebView content.
2. URL input and load/reload controls.
3. Tool drawer entry.
4. Secondary actions such as scan, local HTML, settings, and fullscreen.

### Sliding Drawer Open State

When the user opens the drawer:

- The drawer slides in from the left.
- The drawer overlays the WebView rather than resizing it.
- A scrim covers the remaining WebView area.
- Tapping the scrim closes the drawer.
- System back closes the drawer before normal Workbench back handling continues.

The drawer is structured as:

- A narrow navigation rail with four icon entries: `Config`, `Debug`, `Cases`, `History`.
- A content pane for the selected panel.
- A compact title/action row at the top of the content pane.

The drawer should target a useful phone width, roughly 280-320 dp depending on available screen width. It should not require the user to horizontally scroll normal panel controls.

### Wide Screens And Landscape

The first implementation keeps the existing persistent side-panel behavior for wide screens. Wide screens are not the primary problem in this pass.

If the persistent side panel remains, it should reuse the same panel content primitives introduced for the drawer so compact and wide layouts do not drift apart again.

## Interaction Design

### Opening And Closing Tools

The compact phone Workbench should support:

- Tap the leading drawer button to open tools.
- Tap a tool icon inside the drawer rail to switch panels.
- Tap the scrim to close tools.
- Press system back to close tools before leaving fullscreen, navigating WebView history, or leaving the screen.

Edge-swipe open is not part of the first implementation. The explicit drawer button avoids conflicts with WebView horizontal gestures and makes the feature discoverable.

### Fullscreen

When app fullscreen or video fullscreen is active:

- The URL bar and drawer entry remain hidden according to existing fullscreen rules.
- The drawer should not be open.
- Existing fullscreen exit affordances remain available.

If the user enters fullscreen while the drawer is open, the drawer should close first.

### WebView Stability

Opening and closing the drawer must not recreate the WebView. The `WebViewHost` should remain in the browser layer and the drawer should be a Compose overlay above it.

## Panel System

The panels should share a small UI kit rather than each defining its own card structure:

- `PanelHeader`: title, optional subtitle/status, optional icon action.
- `PanelSection`: compact section header plus content, not a heavy nested card by default.
- `PanelRow`: label, optional description, trailing control or action.
- `PanelActionRow`: compact grouped actions.
- `PanelEmptyState`: icon, short text, optional action.
- `PanelListItem`: dense item for cases, history, logs, requests, and downloads.

Panel containers should avoid large rounded cards inside the drawer. Cards remain acceptable for repeated list items only when they improve scanning.

## Config Panel

Config should become denser and easier to scan:

- Runtime toggles stay as switch rows.
- Enum controls should remain chips or become segmented controls when the option count is small.
- User Agent custom text remains visible only when custom mode is selected or disabled with clear affordance.
- Permission policies should use consistent rows and labels.

The goal is to reduce vertical scrolling while keeping each WebView setting understandable.

## Cases Panel

Cases should separate actions from saved items:

- The top area contains save-current-case fields and import/export actions.
- Import/export should use icon buttons or compact action buttons consistently.
- Saved cases use dense list items with title, URL, note preview, and delete action.
- Empty state should be consistent with History and Debug empty states.

## History Panel

History should use the shared panel list item style:

- Header row with clear-history action.
- Dense history items with title, URL, and visit time.
- Empty state using the shared pattern.

## Debug Panel

The current Debug panel has ten tabs:

- Console
- Errors
- Page
- Cookies
- Storage
- Source
- Elements
- JS Exec
- Requests
- Downloads

The drawer version should group these into six modes:

- `Logs`: console logs and page errors, with filtering or sectioned lists.
- `Page`: current page URL, title, status, progress, navigation id, cache action.
- `Storage`: cookies, localStorage, sessionStorage read/clear actions and recent results.
- `Inspect`: source snapshot and elements summary actions/results.
- `Network`: requests and downloads.
- `Execute`: JavaScript input and results.

This keeps the Debug surface powerful while making it fit a narrow drawer. The grouped modes can use tabs, a compact segmented selector, or the same rail/list pattern, but they should not require horizontal scrolling across ten labels.

## Theme And Visual Hierarchy

This pass should not redesign the whole color palette. It should make targeted improvements that support the drawer work:

- Use neutral surfaces for chrome and drawers so WebView content remains visually dominant.
- Use primary color for selected tool state and primary actions.
- Use error/warning accents only for errors, destructive actions, and warning log levels.
- Reduce heavy nested `surfaceVariant` blocks where they create a card-within-card feel.
- Keep typography compact and consistent with the existing tool-oriented type scale.

## Accessibility And Localization

The implementation must preserve:

- Content descriptions for icon-only buttons.
- Touch targets suitable for Android phones.
- Text truncation for long localized labels.
- URL and debug strings that can be long without breaking layout.

The drawer button needs a clear localized content description.

## Testing And Verification

Implementation should update or add tests for:

- Compact Workbench no longer shows the bottom panel navigation bar.
- Drawer opens from the compact URL bar.
- Selecting `Config`, `Debug`, `Cases`, and `History` shows the correct panel in the drawer.
- Back closes the drawer before other Workbench back behavior.
- Wide layout still shows or supports the expected panel access.

Manual verification should include:

- Phone portrait.
- Phone landscape if available.
- A wide emulator/tablet-size window.
- Opening/closing drawer while a page is loaded.
- Fullscreen and video fullscreen paths.
- Debug mode switching with long log/request entries.

## Implementation Notes

The likely implementation path is:

1. Refactor `WorkbenchFrame` compact mode away from bottom bar and `ModalBottomSheet`.
2. Add drawer state and drawer entry to the compact `UrlBar`.
3. Move panel switching into a drawer rail.
4. Introduce shared panel primitives and migrate `Config`, `Cases`, `History`, and `Debug`.
5. Group `DebugPanel` modes and update tests.

The implementation should remain scoped to UI structure and tests. It should not change WebView event handling, database models, import/export payloads, or WebView configuration semantics.
