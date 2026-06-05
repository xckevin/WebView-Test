package com.xckevin.android.app.webview.test.ui.workbench

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.xckevin.android.app.webview.test.model.HistoryItem
import com.xckevin.android.app.webview.test.model.SourceType
import com.xckevin.android.app.webview.test.model.WebTestCase
import com.xckevin.android.app.webview.test.model.WebTestConfig
import com.xckevin.android.app.webview.test.ui.theme.WebViewTestTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WorkbenchScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun urlBarShowsLoadAndScanActions() {
        composeRule.setContent {
            WebViewTestTheme(dynamicColor = false) {
                UrlBar(
                    urlInput = "https://example.com",
                    urlError = null,
                    isLoading = false,
                    loadProgress = 0,
                    isFullscreen = false,
                    onUrlInputChanged = {},
                    onLoad = {},
                    onScan = {},
                    onRefresh = {},
                    onOpenSettings = {},
                    onToggleFullscreen = {},
                )
            }
        }

        composeRule.onNodeWithText("Load").assertIsDisplayed()
        composeRule.onNodeWithText("Paste").assertIsDisplayed()
        composeRule.onNodeWithText("Scan").assertIsDisplayed()
    }

    @Test
    fun configPanelShowsCoreToggles() {
        composeRule.setContent {
            WebViewTestTheme(dynamicColor = false) {
                ConfigPanel(
                    config = WebTestConfig.default(),
                    onConfigChanged = {},
                )
            }
        }

        composeRule.onNodeWithText("JavaScript").assertIsDisplayed()
        composeRule.onNodeWithText("DOM Storage").assertIsDisplayed()
        composeRule.onNodeWithText("Desktop mode").assertIsDisplayed()
        composeRule.onNodeWithText("Cookies").assertIsDisplayed()
    }

    @Test
    fun casesPanelShowsSavedCase() {
        composeRule.setContent {
            WebViewTestTheme(dynamicColor = false) {
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
            WebViewTestTheme(dynamicColor = false) {
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

        composeRule.onNodeWithText("Import").performClick()
        composeRule.onNodeWithText("Export").performClick()

        assertTrue(importClicked)
        assertTrue(exportClicked)
    }

    @Test
    fun historyPanelShowsVisitedUrl() {
        composeRule.setContent {
            WebViewTestTheme(dynamicColor = false) {
                HistoryPanel(
                    history = listOf(fakeHistoryItem),
                    onOpenHistoryItem = {},
                    onClearHistory = {},
                )
            }
        }

        composeRule.onNodeWithText(fakeHistoryItem.url).assertIsDisplayed()
    }

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
