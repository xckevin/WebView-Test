package com.xckevin.android.app.webview.test.ui.workbench

import android.content.ClipData
import android.content.ClipDescription.MIMETYPE_TEXT_PLAIN
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xckevin.android.app.webview.test.AppContainer
import com.xckevin.android.app.webview.test.data.CaseImportExport
import com.xckevin.android.app.webview.test.debug.DebugState
import com.xckevin.android.app.webview.test.model.HistoryItem
import com.xckevin.android.app.webview.test.model.WebTestConfig
import com.xckevin.android.app.webview.test.model.WebTestCase
import com.xckevin.android.app.webview.test.ui.common.AppScaffold
import com.xckevin.android.app.webview.test.web.WebPageEvent
import com.xckevin.android.app.webview.test.web.WebViewHost
import com.xckevin.android.app.webview.test.web.rememberWebViewController
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch

@Composable
fun WorkbenchScreen(
    container: AppContainer,
    onOpenScanner: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val factory = remember(container) {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return WorkbenchViewModel(
                    testCaseRepository = container.testCaseRepository,
                    historyRepository = container.historyRepository,
                ) as T
            }
        }
    }
    val viewModel: WorkbenchViewModel = viewModel(factory = factory)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val cases by viewModel.cases.collectAsStateWithLifecycle(emptyList())
    val history by viewModel.history.collectAsStateWithLifecycle(emptyList())
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()

    val importCasesFromClipboard: () -> Unit = {
        coroutineScope.launch {
            clipboard.getClipEntry()
                ?.plainText()
                ?.let(viewModel::importCasesJson)
        }
    }
    val exportCasesToClipboard: () -> Unit = {
        coroutineScope.launch {
            clipboard.setClipEntry(
                ClipData.newPlainText(
                    "webview-test-cases",
                    CaseImportExport.exportCases(cases),
                ).toClipEntry()
            )
        }
    }

    AppScaffold { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            val sidePanelWidth = 380.dp
            val bottomPanelHeight = 300.dp
            val bottomTabsHeight = 52.dp
            val showPanels = !state.isFullscreen
            val useSidePanel = showPanels && maxWidth > 840.dp
            val browserPadding = when {
                !showPanels -> Modifier
                useSidePanel -> Modifier.padding(end = sidePanelWidth)
                else -> Modifier.padding(bottom = bottomPanelHeight + bottomTabsHeight)
            }

            BrowserColumn(
                state = state,
                showUrlBar = showPanels,
                onUrlInputChanged = viewModel::onUrlInputChanged,
                onLoad = viewModel::loadUrl,
                onScan = onOpenScanner,
                onRefresh = viewModel::refresh,
                onOpenSettings = onOpenSettings,
                onToggleFullscreen = viewModel::toggleFullscreen,
                onWebPageEvent = viewModel::onWebPageEvent,
                modifier = Modifier
                    .fillMaxSize()
                    .then(browserPadding),
            )

            if (showPanels) {
                if (useSidePanel) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(sidePanelWidth)
                            .fillMaxHeight(),
                    ) {
                        VerticalDivider()
                        WorkbenchPanelSurface(
                            state = state,
                            cases = cases,
                            history = history,
                            onSelectPanel = viewModel::selectPanel,
                            onConfigChanged = viewModel::applyConfig,
                            onOpenCase = viewModel::openCase,
                            onDeleteCase = viewModel::deleteCase,
                            onSaveCurrentCase = viewModel::saveCurrentAsCase,
                            onImportCases = importCasesFromClipboard,
                            onExportCases = exportCasesToClipboard,
                            onOpenHistoryItem = viewModel::openHistory,
                            onClearHistory = viewModel::clearHistory,
                            onClearDebugLogs = viewModel::clearDebugLogs,
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(bottomPanelHeight + bottomTabsHeight),
                    ) {
                        HorizontalDivider()
                        WorkbenchPanelSurface(
                            state = state,
                            cases = cases,
                            history = history,
                            onSelectPanel = viewModel::selectPanel,
                            onConfigChanged = viewModel::applyConfig,
                            onOpenCase = viewModel::openCase,
                            onDeleteCase = viewModel::deleteCase,
                            onSaveCurrentCase = viewModel::saveCurrentAsCase,
                            onImportCases = importCasesFromClipboard,
                            onExportCases = exportCasesToClipboard,
                            onOpenHistoryItem = viewModel::openHistory,
                            onClearHistory = viewModel::clearHistory,
                            onClearDebugLogs = viewModel::clearDebugLogs,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            } else {
                Button(
                    onClick = viewModel::toggleFullscreen,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                ) {
                    Text("Exit fullscreen")
                }
            }
        }
    }
}

@Composable
private fun BrowserColumn(
    state: WorkbenchState,
    showUrlBar: Boolean,
    onUrlInputChanged: (String) -> Unit,
    onLoad: () -> Unit,
    onScan: () -> Unit,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onWebPageEvent: (WebPageEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val webViewController = rememberWebViewController()

    Column(modifier = modifier.fillMaxSize()) {
        if (showUrlBar) {
            UrlBar(
                urlInput = state.urlInput,
                urlError = state.urlError,
                isLoading = state.isLoading,
                loadProgress = state.loadProgress,
                isFullscreen = state.isFullscreen,
                onUrlInputChanged = onUrlInputChanged,
                onLoad = onLoad,
                onScan = onScan,
                onRefresh = onRefresh,
                onOpenSettings = onOpenSettings,
                onToggleFullscreen = onToggleFullscreen,
            )
        }
        WebViewHost(
            requestedUrl = state.requestedUrl,
            config = state.config,
            isFullscreen = state.isFullscreen,
            requestedNavigationId = state.requestedNavigationId,
            onEvent = onWebPageEvent,
            controller = webViewController,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
    }
}

@Composable
private fun WorkbenchPanelSurface(
    state: WorkbenchState,
    cases: List<WebTestCase>,
    history: List<HistoryItem>,
    onSelectPanel: (WorkbenchPanel) -> Unit,
    onConfigChanged: (WebTestConfig) -> Unit,
    onOpenCase: (WebTestCase) -> Unit,
    onDeleteCase: (WebTestCase) -> Unit,
    onSaveCurrentCase: (String, String) -> Unit,
    onImportCases: () -> Unit,
    onExportCases: () -> Unit,
    onOpenHistoryItem: (HistoryItem) -> Unit,
    onClearHistory: () -> Unit,
    onClearDebugLogs: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        PanelTabs(
            selectedPanel = state.selectedPanel,
            onSelectPanel = onSelectPanel,
        )
        PanelContent(
            selectedPanel = state.selectedPanel,
            state = state,
            cases = cases,
            history = history,
            onConfigChanged = onConfigChanged,
            onOpenCase = onOpenCase,
            onDeleteCase = onDeleteCase,
            onSaveCurrentCase = onSaveCurrentCase,
            onImportCases = onImportCases,
            onExportCases = onExportCases,
            onOpenHistoryItem = onOpenHistoryItem,
            onClearHistory = onClearHistory,
            onClearDebugLogs = onClearDebugLogs,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PanelTabs(
    selectedPanel: WorkbenchPanel,
    onSelectPanel: (WorkbenchPanel) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        WorkbenchPanel.entries.forEach { panel ->
            val selected = panel == selectedPanel
            TextButton(
                onClick = { onSelectPanel(panel) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColors(
                    containerColor = if (selected) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    contentColor = if (selected) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                ),
            ) {
                Text(
                    text = panel.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun PanelContent(
    selectedPanel: WorkbenchPanel,
    state: WorkbenchState,
    cases: List<WebTestCase>,
    history: List<HistoryItem>,
    onConfigChanged: (WebTestConfig) -> Unit,
    onOpenCase: (WebTestCase) -> Unit,
    onDeleteCase: (WebTestCase) -> Unit,
    onSaveCurrentCase: (String, String) -> Unit,
    onImportCases: () -> Unit,
    onExportCases: () -> Unit,
    onOpenHistoryItem: (HistoryItem) -> Unit,
    onClearHistory: () -> Unit,
    onClearDebugLogs: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (selectedPanel) {
        WorkbenchPanel.CONFIG -> ConfigPanel(
            config = state.config,
            onConfigChanged = onConfigChanged,
            modifier = modifier,
        )

        WorkbenchPanel.CASES -> CasesPanel(
            cases = cases,
            canSaveCurrentCase = state.currentUrl != null,
            onOpenCase = onOpenCase,
            onDeleteCase = onDeleteCase,
            onSaveCurrentCase = onSaveCurrentCase,
            onImport = onImportCases,
            onExport = onExportCases,
            modifier = modifier,
        )

        WorkbenchPanel.HISTORY -> HistoryPanel(
            history = history,
            onOpenHistoryItem = onOpenHistoryItem,
            onClearHistory = onClearHistory,
            modifier = modifier,
        )

        WorkbenchPanel.DEBUG -> DebugPanel(
            debugState = state.debugState,
            onClearDebugLogs = onClearDebugLogs,
            modifier = modifier,
        )
    }
}

@Composable
private fun DebugPanel(
    debugState: DebugState,
    onClearDebugLogs: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Debug logs",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                OutlinedButton(
                    enabled = debugState.logs.isNotEmpty(),
                    onClick = onClearDebugLogs,
                ) {
                    Text("Clear")
                }
            }
        }

        if (debugState.logs.isEmpty()) {
            item {
                Text(
                    text = "No debug logs",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            items(debugState.logs.asReversed()) { log ->
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = DateFormat.getDateTimeInstance().format(Date(log.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = log.message,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

private val WorkbenchPanel.label: String
    get() = when (this) {
        WorkbenchPanel.CONFIG -> "Config"
        WorkbenchPanel.DEBUG -> "Debug"
        WorkbenchPanel.CASES -> "Cases"
        WorkbenchPanel.HISTORY -> "History"
    }

private fun androidx.compose.ui.platform.ClipEntry.plainText(): String? {
    val clipData = clipData.takeIf {
        it.description.hasMimeType(MIMETYPE_TEXT_PLAIN) ||
            it.description.hasMimeType("text/*")
    } ?: return null
    return clipData
        .takeIf { it.itemCount > 0 }
        ?.getItemAt(0)
        ?.text
        ?.toString()
        ?.takeIf { it.isNotBlank() }
}
