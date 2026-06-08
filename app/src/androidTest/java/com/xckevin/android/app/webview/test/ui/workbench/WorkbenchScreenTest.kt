package com.xckevin.android.app.webview.test.ui.workbench

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.xckevin.android.app.webview.test.R
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
    fun compactDrawerClosesAfterOpeningHistoryItem() {
        var openedItem: HistoryItem? = null

        composeRule.setContent {
            WebViewTestTheme(darkTheme = false) {
                Box(
                    modifier = Modifier
                        .width(400.dp)
                        .height(640.dp),
                ) {
                    WorkbenchFrame(
                        state = WorkbenchState(selectedPanel = WorkbenchPanel.HISTORY),
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

        composeRule.onNodeWithText(text(R.string.action_open_tools)).performClick()
        composeRule.onNodeWithTag("workbench_drawer").assertIsDisplayed()

        composeRule.onNodeWithText(fakeHistoryItem.url).performClick()
        composeRule.waitForIdle()

        assertEquals(fakeHistoryItem, openedItem)
        composeRule.onAllNodesWithText(fakeHistoryItem.url).assertCountEquals(0)
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
