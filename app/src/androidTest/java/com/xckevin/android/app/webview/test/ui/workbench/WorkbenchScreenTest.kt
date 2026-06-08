package com.xckevin.android.app.webview.test.ui.workbench

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.xckevin.android.app.webview.test.R
import com.xckevin.android.app.webview.test.debug.DebugState
import com.xckevin.android.app.webview.test.model.HistoryItem
import com.xckevin.android.app.webview.test.model.SourceType
import com.xckevin.android.app.webview.test.model.WebTestCase
import com.xckevin.android.app.webview.test.model.WebTestConfig
import com.xckevin.android.app.webview.test.ui.theme.WebViewTestTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WorkbenchScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun urlBarShowsDrawerLoadAndOverflowActions() {
        var scanClicked = false

        composeRule.setContent {
            WebViewTestTheme(darkTheme = false) {
                UrlBar(
                    urlInput = "https://example.com",
                    urlError = null,
                    isLoading = false,
                    loadProgress = 0,
                    isFullscreen = false,
                    onOpenTools = {},
                    onUrlInputChanged = {},
                    onLoad = {},
                    onScan = { scanClicked = true },
                    onRefresh = {},
                    onOpenSettings = {},
                    onToggleFullscreen = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription(text(R.string.action_open_tools)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(text(R.string.action_load)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(text(R.string.action_more)).assertIsDisplayed()
        composeRule.onAllNodesWithContentDescription(text(R.string.action_scan)).assertCountEquals(0)

        composeRule.onNodeWithContentDescription(text(R.string.action_more)).performClick()
        composeRule.onNodeWithText(text(R.string.action_scan)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.action_scan)).performClick()

        assertTrue(scanClicked)
    }

    @Test
    fun urlBarVisibilityHidesDuringAppFullscreen() {
        assertTrue(WorkbenchState().shouldShowUrlBar())
        assertFalse(WorkbenchState(isFullscreen = true).shouldShowUrlBar())
    }

    @Test
    fun configPanelShowsCoreToggles() {
        composeRule.setContent {
            WebViewTestTheme(darkTheme = false) {
                ConfigPanel(
                    config = WebTestConfig.default(),
                    onConfigChanged = {},
                )
            }
        }

        composeRule.onNodeWithText(text(R.string.config_section_runtime)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.config_javascript)).assertIsDisplayed()
    }

    @Test
    fun debugPanelShowsGroupedModes() {
        composeRule.setContent {
            WebViewTestTheme(darkTheme = false) {
                Box(
                    modifier = Modifier
                        .width(320.dp)
                        .height(640.dp),
                ) {
                    DebugPanel(
                        debugState = DebugState(),
                        onClearDebugLogs = {},
                        onEvaluateJavaScript = { _, callback -> callback("") },
                        onReadCookies = {},
                        onClearCookies = {},
                        onClearWebViewCache = {},
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        composeRule.onAllNodesWithText(text(R.string.debug_group_logs)).assertCountEquals(2)
        composeRule.onAllNodesWithText(text(R.string.debug_group_storage)).assertCountEquals(1)
        composeRule.onAllNodesWithText(text(R.string.debug_group_execute)).assertCountEquals(1)
        composeRule.onAllNodesWithText(text(R.string.debug_tab_console)).assertCountEquals(0)
    }

    @Test
    fun casesPanelShowsSavedCase() {
        composeRule.setContent {
            WebViewTestTheme(darkTheme = false) {
                CasesPanel(
                    cases = listOf(fakeCase),
                    canSaveCurrentCase = true,
                    onOpenCase = {},
                    onDeleteCase = {},
                    onSaveCurrentCase = { _, _ -> },
                    onImport = {},
                    onExport = {},
                )
            }
        }

        composeRule.onNodeWithText(fakeCase.name).assertIsDisplayed()
    }

    @Test
    fun casesPanelImportAndExportActionsInvokeCallbacks() {
        var importClicked = false
        var exportClicked = false

        composeRule.setContent {
            WebViewTestTheme(darkTheme = false) {
                CasesPanel(
                    cases = emptyList(),
                    canSaveCurrentCase = false,
                    onOpenCase = {},
                    onDeleteCase = {},
                    onSaveCurrentCase = { _, _ -> },
                    onImport = { importClicked = true },
                    onExport = { exportClicked = true },
                )
            }
        }

        composeRule.onNodeWithContentDescription(text(R.string.action_import)).performClick()
        composeRule.onNodeWithContentDescription(text(R.string.action_export)).performClick()

        assertTrue(importClicked)
        assertTrue(exportClicked)
    }

    @Test
    fun historyPanelShowsVisitedUrl() {
        composeRule.setContent {
            WebViewTestTheme(darkTheme = false) {
                HistoryPanel(
                    history = listOf(fakeHistoryItem),
                    onOpenHistoryItem = {},
                    onClearHistory = {},
                )
            }
        }

        composeRule.onNodeWithText(fakeHistoryItem.url).assertIsDisplayed()
    }

    @Test
    fun compactPanelContentOpensFromDrawerButton() {
        var openClicks = 0

        composeRule.setContent {
            WebViewTestTheme(darkTheme = false) {
                Box(
                    modifier = Modifier
                        .width(400.dp)
                        .height(640.dp),
                ) {
                    WorkbenchFrame(
                        state = WorkbenchState(),
                        cases = emptyList(),
                        history = emptyList(),
                        browser = { modifier, onOpenTools ->
                            Box(modifier.fillMaxSize()) {
                                Button(
                                    onClick = {
                                        openClicks++
                                        onOpenTools()
                                    },
                                ) {
                                    Text(text(R.string.action_open_tools))
                                }
                            }
                        },
                        onGoBack = {},
                        onGoForward = {},
                        onRefresh = {},
                        onToggleFullscreen = {},
                        onSelectPanel = {},
                        onConfigChanged = {},
                        onOpenCase = {},
                        onDeleteCase = {},
                        onSaveCurrentCase = { _, _ -> },
                        onImportCases = {},
                        onExportCases = {},
                        onOpenHistoryItem = {},
                        onClearHistory = {},
                        onClearDebugLogs = {},
                        onEvaluateJavaScript = { _, callback -> callback("") },
                        onReadCookies = {},
                        onClearCookies = {},
                        onClearWebViewCache = {},
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        composeRule.onAllNodesWithText(text(R.string.panel_config)).assertCountEquals(0)
        composeRule.onAllNodesWithText(text(R.string.config_javascript)).assertCountEquals(0)

        composeRule.onNodeWithText(text(R.string.action_open_tools)).performClick()

        composeRule.onNodeWithText(text(R.string.panel_config)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.config_javascript)).assertIsDisplayed()
        composeRule.onNodeWithTag("workbench_drawer").assertIsDisplayed()

        composeRule.onNodeWithContentDescription(text(R.string.action_close)).performClick()
        composeRule.waitForIdle()

        composeRule.onAllNodesWithText(text(R.string.config_javascript)).assertCountEquals(0)
        composeRule.onNodeWithText(text(R.string.action_open_tools)).performClick()

        assertTrue(openClicks >= 2)
    }

    @Test
    fun floatingBrowserControlsInvokeNavigationActions() {
        var backClicked = false
        var forwardClicked = false
        var refreshClicked = false
        var fullscreenClicked = false

        composeRule.setContent {
            WebViewTestTheme(darkTheme = false) {
                Box(
                    modifier = Modifier
                        .width(400.dp)
                        .height(640.dp),
                ) {
                    WorkbenchFrame(
                        state = WorkbenchState(),
                        cases = emptyList(),
                        history = emptyList(),
                        browser = { modifier, _ -> Box(modifier.fillMaxSize()) },
                        onGoBack = { backClicked = true },
                        onGoForward = { forwardClicked = true },
                        onRefresh = { refreshClicked = true },
                        onToggleFullscreen = { fullscreenClicked = true },
                        onSelectPanel = {},
                        onConfigChanged = {},
                        onOpenCase = {},
                        onDeleteCase = {},
                        onSaveCurrentCase = { _, _ -> },
                        onImportCases = {},
                        onExportCases = {},
                        onOpenHistoryItem = {},
                        onClearHistory = {},
                        onClearDebugLogs = {},
                        onEvaluateJavaScript = { _, callback -> callback("") },
                        onReadCookies = {},
                        onClearCookies = {},
                        onClearWebViewCache = {},
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        composeRule.onNodeWithContentDescription(text(R.string.action_back)).performClick()
        composeRule.onNodeWithContentDescription(text(R.string.action_forward)).performClick()
        composeRule.onNodeWithContentDescription(text(R.string.action_refresh)).performClick()
        composeRule.onNodeWithContentDescription(text(R.string.action_fullscreen)).performClick()

        assertTrue(backClicked)
        assertTrue(forwardClicked)
        assertTrue(refreshClicked)
        assertTrue(fullscreenClicked)
    }

    private fun text(@StringRes resId: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resId)

    private val fakeCase = WebTestCase(
        id = 1L,
        name = "Checkout regression",
        url = "https://example.com/checkout",
        note = "Saved from test",
        config = WebTestConfig.default(),
        createdAt = 1_700_000_000_000L,
        updatedAt = 1_700_000_000_000L,
        lastOpenedAt = null,
    )

    private val fakeHistoryItem = HistoryItem(
        id = 1L,
        url = "https://visited.example/path",
        title = "Visited example",
        sourceType = SourceType.REMOTE_URL,
        visitedAt = 1_700_000_000_000L,
    )
}
