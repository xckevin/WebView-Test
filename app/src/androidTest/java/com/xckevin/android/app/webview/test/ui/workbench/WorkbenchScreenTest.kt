package com.xckevin.android.app.webview.test.ui.workbench

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.xckevin.android.app.webview.test.R
import com.xckevin.android.app.webview.test.debug.DebugEvent
import com.xckevin.android.app.webview.test.debug.DebugEventType
import com.xckevin.android.app.webview.test.debug.DebugSeverity
import com.xckevin.android.app.webview.test.debug.DebugState
import com.xckevin.android.app.webview.test.model.HistoryItem
import com.xckevin.android.app.webview.test.model.SourceType
import com.xckevin.android.app.webview.test.model.WebTestConfig
import com.xckevin.android.app.webview.test.ui.theme.WebViewTestTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

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
    fun urlBarHeightStaysStableWhenProgressAppears() {
        var isLoading by mutableStateOf(false)

        composeRule.setContent {
            WebViewTestTheme(darkTheme = false) {
                UrlBar(
                    urlInput = "https://example.com",
                    urlError = null,
                    isLoading = isLoading,
                    loadProgress = 50,
                    isFullscreen = false,
                    onOpenTools = {},
                    onUrlInputChanged = {},
                    onLoad = {},
                    onScan = {},
                    onRefresh = {},
                    onOpenSettings = {},
                    onToggleFullscreen = {},
                    modifier = Modifier.testTag("url_bar"),
                )
            }
        }

        val idleBounds = composeRule.onNodeWithTag("url_bar").getUnclippedBoundsInRoot()
        val idleHeight = idleBounds.bottom - idleBounds.top

        composeRule.runOnIdle {
            isLoading = true
        }

        val loadingBounds = composeRule.onNodeWithTag("url_bar").getUnclippedBoundsInRoot()
        val loadingHeight = loadingBounds.bottom - loadingBounds.top
        assertEquals(idleHeight, loadingHeight)
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
                        config = WebTestConfig.default(),
                        sourceType = SourceType.REMOTE_URL,
                        onClearDebugLogs = {},
                        onEvaluateJavaScript = { _, callback -> callback("") },
                        onReadCookies = { callback -> callback("") },
                        onClearCookies = {},
                        onClearWebViewCache = {},
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        composeRule.onAllNodesWithText("Overview").assertCountEquals(1)
        composeRule.onAllNodesWithText("Timeline").assertCountEquals(1)
        composeRule.onAllNodesWithText(text(R.string.debug_group_logs)).assertCountEquals(1)
        composeRule.onAllNodesWithText(text(R.string.debug_group_storage)).assertCountEquals(1)
        composeRule.onAllNodesWithText(text(R.string.debug_group_execute)).assertCountEquals(1)
        composeRule.onAllNodesWithText(text(R.string.debug_tab_console)).assertCountEquals(0)
    }

    @Test
    fun debugPanelHeaderUsesModeTitleConsistently() {
        composeRule.setContent {
            WebViewTestTheme(darkTheme = false) {
                Box(
                    modifier = Modifier
                        .width(320.dp)
                        .height(640.dp),
                ) {
                    DebugPanel(
                        debugState = DebugState(),
                        config = WebTestConfig.default(),
                        sourceType = SourceType.REMOTE_URL,
                        onClearDebugLogs = {},
                        onEvaluateJavaScript = { _, callback -> callback("") },
                        onReadCookies = { callback -> callback("") },
                        onClearCookies = {},
                        onClearWebViewCache = {},
                        selectedMode = DebugMode.Overview,
                        showModeTabs = false,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        composeRule.onNodeWithText("Overview").assertIsDisplayed()
        composeRule.onAllNodesWithText("Debug overview").assertCountEquals(0)
    }

    @Test
    fun debugPanelHeaderKeepsLayoutWhenClearActionAppears() {
        var debugState by mutableStateOf(DebugState())

        composeRule.setContent {
            WebViewTestTheme(darkTheme = false) {
                Box(
                    modifier = Modifier
                        .width(320.dp)
                        .height(640.dp),
                ) {
                    DebugPanel(
                        debugState = debugState,
                        config = WebTestConfig.default(),
                        sourceType = SourceType.REMOTE_URL,
                        onClearDebugLogs = {},
                        onEvaluateJavaScript = { _, callback -> callback("") },
                        onReadCookies = { callback -> callback("") },
                        onClearCookies = {},
                        onClearWebViewCache = {},
                        selectedMode = DebugMode.Overview,
                        showModeTabs = false,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        val emptyHeaderTitle = composeRule.onNodeWithText("Overview").getUnclippedBoundsInRoot()

        composeRule.runOnIdle {
            debugState = DebugState(
                timeline = listOf(
                    DebugEvent(
                        id = 1L,
                        type = DebugEventType.PAGE,
                        severity = DebugSeverity.INFO,
                        summary = "Page loaded",
                        timestamp = 1L,
                    ),
                ),
            )
        }
        composeRule.waitForIdle()

        val populatedHeaderTitle = composeRule.onNodeWithText("Overview").getUnclippedBoundsInRoot()
        assertEquals(emptyHeaderTitle.top, populatedHeaderTitle.top)
        assertEquals(emptyHeaderTitle.bottom - emptyHeaderTitle.top, populatedHeaderTitle.bottom - populatedHeaderTitle.top)
        composeRule.onNodeWithContentDescription(text(R.string.action_clear)).assertIsDisplayed()
    }

    @Test
    fun debugUtilityPanelsUseUnifiedSections() {
        var selectedMode by mutableStateOf(DebugMode.Page)

        composeRule.setContent {
            WebViewTestTheme(darkTheme = false) {
                Box(
                    modifier = Modifier
                        .width(320.dp)
                        .height(640.dp),
                ) {
                    DebugPanel(
                        debugState = DebugState(),
                        config = WebTestConfig.default(),
                        sourceType = SourceType.REMOTE_URL,
                        onClearDebugLogs = {},
                        onEvaluateJavaScript = { _, callback -> callback("") },
                        onReadCookies = { callback -> callback("") },
                        onClearCookies = {},
                        onClearWebViewCache = {},
                        selectedMode = selectedMode,
                        showModeTabs = false,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        composeRule.onNodeWithText("Page details").assertIsDisplayed()
        composeRule.onNodeWithText("Actions").assertIsDisplayed()
        composeRule.onNodeWithText("Environment").assertIsDisplayed()

        composeRule.runOnIdle { selectedMode = DebugMode.Storage }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Actions").assertIsDisplayed()
        composeRule.onNodeWithText("Results").assertIsDisplayed()
        composeRule.onAllNodesWithText(text(R.string.debug_tab_cookies)).assertCountEquals(0)

        composeRule.runOnIdle { selectedMode = DebugMode.Inspect }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Inputs").assertIsDisplayed()
        composeRule.onNodeWithText("Actions").assertIsDisplayed()
        composeRule.onAllNodesWithText("Element query").assertCountEquals(0)

        composeRule.runOnIdle { selectedMode = DebugMode.Execute }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Inputs").assertIsDisplayed()
        composeRule.onNodeWithText("Actions").assertIsDisplayed()
        composeRule.onAllNodesWithText("Script").assertCountEquals(0)
    }

    @Test
    fun debugFiltersUseDropdownSelectors() {
        composeRule.setContent {
            WebViewTestTheme(darkTheme = false) {
                Box(
                    modifier = Modifier
                        .width(320.dp)
                        .height(640.dp),
                ) {
                    DebugPanel(
                        debugState = DebugState(),
                        config = WebTestConfig.default(),
                        sourceType = SourceType.REMOTE_URL,
                        onClearDebugLogs = {},
                        onEvaluateJavaScript = { _, callback -> callback("") },
                        onReadCookies = { callback -> callback("") },
                        onClearCookies = {},
                        onClearWebViewCache = {},
                        selectedMode = DebugMode.Timeline,
                        showModeTabs = false,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        composeRule.onNodeWithText("Severity: All").assertIsDisplayed()
        composeRule.onNodeWithText("Event type: Any type").assertIsDisplayed()
        composeRule.onNodeWithText("Severity: All").performClick()
        composeRule.onNodeWithText("WARNING").assertIsDisplayed()
    }

    @Test
    fun debugPanelDoesNotShowDuplicateClearActions() {
        var selectedMode by mutableStateOf(DebugMode.Overview)

        composeRule.setContent {
            WebViewTestTheme(darkTheme = false) {
                Box(
                    modifier = Modifier
                        .width(320.dp)
                        .height(640.dp),
                ) {
                    DebugPanel(
                        debugState = DebugState(),
                        config = WebTestConfig.default(),
                        sourceType = SourceType.REMOTE_URL,
                        onClearDebugLogs = {},
                        onEvaluateJavaScript = { _, callback -> callback("") },
                        onReadCookies = { callback -> callback("") },
                        onClearCookies = {},
                        onClearWebViewCache = {},
                        selectedMode = selectedMode,
                        showModeTabs = false,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        DebugMode.entries.forEach { mode ->
            composeRule.runOnIdle {
                selectedMode = mode
            }
            composeRule.onAllNodesWithText("Clear current nav").assertCountEquals(0)
            composeRule.onAllNodesWithText("Clear logs tab").assertCountEquals(0)
            composeRule.onAllNodesWithText("Clear network").assertCountEquals(0)
        }
    }

    @Test
    fun historyPanelShowsVisitedUrl() {
        composeRule.setContent {
            WebViewTestTheme(darkTheme = false) {
                HistoryPanel(
                    history = listOf(fakeHistoryItem),
                    onOpenHistoryItem = {},
                    onClearHistory = {},
                    onDeleteHistoryItem = {},
                )
            }
        }

        composeRule.onNodeWithText(fakeHistoryItem.url).assertIsDisplayed()
    }

    @Test
    fun historyPanelGroupsItemsByVisitDate() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.JUNE, 8, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val yesterday = Calendar.getInstance().apply {
            timeInMillis = now.timeInMillis
            add(Calendar.DATE, -1)
        }
        val older = Calendar.getInstance().apply {
            set(2026, Calendar.JUNE, 1, 9, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        composeRule.setContent {
            WebViewTestTheme(darkTheme = false) {
                HistoryPanel(
                    history = listOf(
                        fakeHistoryItem.copy(id = 1L, url = "https://today.example", visitedAt = now.timeInMillis),
                        fakeHistoryItem.copy(id = 2L, url = "https://yesterday.example", visitedAt = yesterday.timeInMillis),
                        fakeHistoryItem.copy(id = 3L, url = "https://older.example", visitedAt = older.timeInMillis),
                    ),
                    onOpenHistoryItem = {},
                    onClearHistory = {},
                    onDeleteHistoryItem = {},
                    nowMillis = now.timeInMillis,
                )
            }
        }

        composeRule.onNodeWithText(text(R.string.history_group_today)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.history_group_yesterday)).assertIsDisplayed()
        composeRule.onNodeWithText(DateFormat.getDateInstance().format(Date(older.timeInMillis))).assertIsDisplayed()
    }

    @Test
    fun historyPanelLongPressShowsDeleteAction() {
        var deletedItem: HistoryItem? = null

        composeRule.setContent {
            WebViewTestTheme(darkTheme = false) {
                HistoryPanel(
                    history = listOf(fakeHistoryItem),
                    onOpenHistoryItem = {},
                    onClearHistory = {},
                    onDeleteHistoryItem = { deletedItem = it },
                )
            }
        }

        composeRule.onNodeWithText(fakeHistoryItem.title).performTouchInput { longClick() }
        composeRule.onNodeWithText(text(R.string.action_delete_history_item)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.action_delete_history_item)).performClick()

        assertEquals(fakeHistoryItem, deletedItem)
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
                        onOpenHistoryItem = {},
                        onClearHistory = {},
                        onDeleteHistoryItem = {},
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

        composeRule.onAllNodesWithText("Overview").assertCountEquals(2)
        composeRule.onAllNodesWithText("Timeline").assertCountEquals(1)
        composeRule.onAllNodesWithText(text(R.string.panel_config)).assertCountEquals(0)
        composeRule.onAllNodesWithText(text(R.string.config_javascript)).assertCountEquals(0)
        composeRule.onNodeWithTag("workbench_drawer").assertIsDisplayed()

        composeRule.onNodeWithContentDescription(text(R.string.action_close)).performClick()
        composeRule.waitForIdle()

        composeRule.onAllNodesWithText("Overview").assertCountEquals(0)
        composeRule.onNodeWithText(text(R.string.action_open_tools)).performClick()

        assertTrue(openClicks >= 2)
    }

    @Test
    fun floatingBrowserControlsOpenHistoryPage() {
        var openedItem: HistoryItem? = null

        composeRule.setContent {
            WebViewTestTheme(darkTheme = false) {
                Box(
                    modifier = Modifier
                        .width(400.dp)
                        .height(640.dp),
                ) {
                    WorkbenchFrame(
                        state = WorkbenchState(),
                        history = listOf(fakeHistoryItem),
                        browser = { modifier, onOpenTools ->
                            Box(modifier.fillMaxSize()) {
                                Button(onClick = onOpenTools) {
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
                        onOpenHistoryItem = { openedItem = it },
                        onClearHistory = {},
                        onDeleteHistoryItem = {},
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

        composeRule.onNodeWithContentDescription(text(R.string.panel_history)).performClick()
        composeRule.onNodeWithTag("workbench_history_page").assertIsDisplayed()

        composeRule.onNodeWithText(fakeHistoryItem.url).performClick()
        composeRule.waitForIdle()

        assertEquals(fakeHistoryItem, openedItem)
        composeRule.onAllNodesWithText(fakeHistoryItem.url).assertCountEquals(0)
    }

    @Test
    fun wideWorkbenchPanelPromotesDebugModes() {
        composeRule.setContent {
            WebViewTestTheme(darkTheme = false) {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    Box(
                        modifier = Modifier
                            .width(1000.dp)
                            .height(640.dp),
                    ) {
                        WorkbenchFrame(
                            state = WorkbenchState(),
                            history = emptyList(),
                            browser = { modifier, _ -> Box(modifier.fillMaxSize()) },
                            onGoBack = {},
                            onGoForward = {},
                            onRefresh = {},
                            onToggleFullscreen = {},
                            onSelectPanel = {},
                            onConfigChanged = {},
                            onOpenHistoryItem = {},
                            onClearHistory = {},
                            onDeleteHistoryItem = {},
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
        }

        composeRule.onAllNodesWithText("Overview").assertCountEquals(2)
        composeRule.onAllNodesWithText("Timeline").assertCountEquals(1)
        composeRule.onAllNodesWithText("Network").assertCountEquals(1)
        composeRule.onAllNodesWithText(text(R.string.panel_config)).assertCountEquals(0)
        composeRule.onAllNodesWithText(text(R.string.panel_history)).assertCountEquals(0)
        composeRule.onAllNodesWithText(text(R.string.config_javascript)).assertCountEquals(0)
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
                        history = emptyList(),
                        browser = { modifier, _ -> Box(modifier.fillMaxSize()) },
                        onGoBack = { backClicked = true },
                        onGoForward = { forwardClicked = true },
                        onRefresh = { refreshClicked = true },
                        onToggleFullscreen = { fullscreenClicked = true },
                        onSelectPanel = {},
                        onConfigChanged = {},
                        onOpenHistoryItem = {},
                        onClearHistory = {},
                        onDeleteHistoryItem = {},
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

    @Test
    fun shareDebugTextReportsSystemFailureWithoutThrowing() {
        var failure: Throwable? = null
        val throwingContext = object : ContextWrapper(
            InstrumentationRegistry.getInstrumentation().targetContext,
        ) {
            override fun startActivity(intent: Intent?) {
                throw RuntimeException("Failure from system")
            }
        }

        val started = shareDebugText(
            context = throwingContext,
            title = "Debug bundle",
            value = "payload",
            onFailure = { failure = it },
        )

        assertFalse(started)
        assertEquals("Failure from system", failure?.message)
    }

    private fun text(@StringRes resId: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resId)

    private val fakeHistoryItem = HistoryItem(
        id = 1L,
        url = "https://visited.example/path",
        title = "Visited example",
        sourceType = SourceType.REMOTE_URL,
        visitedAt = 1_700_000_000_000L,
    )
}
