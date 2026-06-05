package com.xckevin.android.app.webview.test.ui.workbench

import android.content.Context
import android.content.ClipData
import android.content.ClipDescription.MIMETYPE_TEXT_PLAIN
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xckevin.android.app.webview.test.AppContainer
import com.xckevin.android.app.webview.test.data.CaseImportExport
import com.xckevin.android.app.webview.test.model.HistoryItem
import com.xckevin.android.app.webview.test.model.WebTestConfig
import com.xckevin.android.app.webview.test.model.WebTestCase
import com.xckevin.android.app.webview.test.ui.common.AppScaffold
import com.xckevin.android.app.webview.test.web.WebPageEvent
import com.xckevin.android.app.webview.test.web.WebContextMenuTarget
import com.xckevin.android.app.webview.test.web.WebPermissionPrompt
import com.xckevin.android.app.webview.test.web.WebViewController
import com.xckevin.android.app.webview.test.web.WebViewHost
import com.xckevin.android.app.webview.test.web.rememberWebViewController
import kotlinx.coroutines.launch

@Composable
fun WorkbenchScreen(
    container: AppContainer,
    scanResult: String? = null,
    onScanResultConsumed: () -> Unit = {},
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
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    val webViewController = rememberWebViewController()
    var pendingOpenDocumentResult by remember { mutableStateOf<((Uri?) -> Unit)?>(null) }
    var pendingRuntimePermissionResult by remember { mutableStateOf<((Map<String, Boolean>) -> Unit)?>(null) }
    var pendingRuntimePermissionAlreadyGranted by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var permissionPrompt by remember { mutableStateOf<WebPermissionPrompt?>(null) }
    var contextMenuTarget by remember { mutableStateOf<WebContextMenuTarget?>(null) }
    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        pendingOpenDocumentResult?.invoke(uri)
        pendingOpenDocumentResult = null
    }
    val localHtmlLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            context.takePersistableReadPermissionIfAvailable(uri, viewModel::addDebugMessage)
            viewModel.loadLocalFile(uri.toString())
        }
    }
    val runtimePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        pendingRuntimePermissionResult?.invoke(pendingRuntimePermissionAlreadyGranted + results)
        pendingRuntimePermissionResult = null
        pendingRuntimePermissionAlreadyGranted = emptyMap()
    }
    val openDocument: (Array<String>, (Uri?) -> Unit) -> Unit = { mimeTypes, onResult ->
        pendingOpenDocumentResult?.invoke(null)
        pendingOpenDocumentResult = onResult
        fileChooserLauncher.launch(mimeTypes.ifEmpty { arrayOf("*/*") })
    }
    val requestRuntimePermissions: (Array<String>, (Map<String, Boolean>) -> Unit) -> Unit = { permissions, onResult ->
        val distinctPermissions = permissions.distinct()
        val missingPermissions = permissions
            .distinct()
            .filter { permission ->
                ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
            }
        val alreadyGranted = distinctPermissions
            .filterNot { it in missingPermissions }
            .associateWith { true }
        if (missingPermissions.isEmpty()) {
            onResult(alreadyGranted)
        } else if (pendingRuntimePermissionResult != null) {
            onResult(distinctPermissions.associateWith { false })
        } else {
            pendingRuntimePermissionResult = onResult
            pendingRuntimePermissionAlreadyGranted = alreadyGranted
            runtimePermissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }
    val evaluateJavaScript: (String, (String) -> Unit) -> Unit = { script, callback ->
        webViewController.evaluateJavaScript(script) { result ->
            viewModel.recordJavaScriptResult(script = script, result = result)
            callback(result)
        }
    }
    val readCookies: () -> Unit = {
        val cookies = webViewController.readCookies()
        viewModel.recordJavaScriptResult(script = "document.cookie", result = cookies)
    }
    val clearCookies: () -> Unit = {
        webViewController.clearCookies { removed ->
            viewModel.addDebugMessage("Cookies cleared: $removed")
        }
    }
    val clearWebViewCache: () -> Unit = {
        webViewController.clearCache()
        viewModel.addDebugMessage("WebView cache cleared")
    }
    val openLocalHtml: () -> Unit = {
        localHtmlLauncher.launch(arrayOf("text/html", "text/*", "application/xhtml+xml"))
    }

    BackHandler(enabled = state.isVideoFullscreen || state.isFullscreen) {
        if (state.isVideoFullscreen && webViewController.hideCustomView()) {
            return@BackHandler
        }
        if (state.isFullscreen) {
            viewModel.toggleFullscreen()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            pendingOpenDocumentResult?.invoke(null)
            pendingOpenDocumentResult = null
            pendingRuntimePermissionResult?.invoke(emptyMap())
            pendingRuntimePermissionResult = null
            pendingRuntimePermissionAlreadyGranted = emptyMap()
            permissionPrompt?.onDeny()
            permissionPrompt = null
        }
    }

    LaunchedEffect(scanResult) {
        val result = scanResult ?: return@LaunchedEffect
        viewModel.loadUrl(result)
        onScanResultConsumed()
    }

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
            val showPanels = !state.isFullscreen && !state.isVideoFullscreen
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
                onOpenLocalHtml = openLocalHtml,
                onRefresh = viewModel::refresh,
                onOpenSettings = onOpenSettings,
                onToggleFullscreen = viewModel::toggleFullscreen,
                onWebPageEvent = viewModel::onWebPageEvent,
                onOpenDocument = openDocument,
                onRequestRuntimePermissions = requestRuntimePermissions,
                onWebPermissionPrompt = { permissionPrompt = it },
                onFullscreenVideoChanged = viewModel::setVideoFullscreen,
                onContextMenuTarget = { contextMenuTarget = it },
                controller = webViewController,
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
                            onEvaluateJavaScript = evaluateJavaScript,
                            onReadCookies = readCookies,
                            onClearCookies = clearCookies,
                            onClearWebViewCache = clearWebViewCache,
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
                            onEvaluateJavaScript = evaluateJavaScript,
                            onReadCookies = readCookies,
                            onClearCookies = clearCookies,
                            onClearWebViewCache = clearWebViewCache,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            } else {
                Button(
                    onClick = {
                        if (!state.isVideoFullscreen || !webViewController.hideCustomView()) {
                            viewModel.toggleFullscreen()
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                ) {
                    Text("Exit fullscreen")
                }
            }

            WebContextMenuDropdown(
                target = contextMenuTarget,
                onDismiss = { contextMenuTarget = null },
                onCopyUrl = { target ->
                    coroutineScope.launch {
                        clipboard.setClipEntry(
                            ClipData.newPlainText("web-resource-url", target.url).toClipEntry()
                        )
                    }
                },
                onOpenCurrentSession = { target -> viewModel.openContextMenuTarget(target.url) },
                onDownloadUrl = { target ->
                    if (!webViewController.downloadUrl(target.url)) {
                        viewModel.addDebugMessage("Download skipped: unsupported URL scheme")
                    }
                },
                onViewResourceUrl = { target ->
                    viewModel.addDebugMessage("${target.label} URL: ${target.url}")
                },
            )

            permissionPrompt?.let { prompt ->
                AlertDialog(
                    onDismissRequest = {
                        permissionPrompt = null
                        prompt.onDeny()
                    },
                    title = { Text(prompt.title) },
                    text = { Text(prompt.message) },
                    confirmButton = {
                        Button(
                            onClick = {
                                permissionPrompt = null
                                prompt.onAllow()
                            },
                        ) {
                            Text("Allow")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                permissionPrompt = null
                                prompt.onDeny()
                            },
                        ) {
                            Text("Deny")
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun WebContextMenuDropdown(
    target: WebContextMenuTarget?,
    onDismiss: () -> Unit,
    onCopyUrl: (WebContextMenuTarget) -> Unit,
    onOpenCurrentSession: (WebContextMenuTarget) -> Unit,
    onDownloadUrl: (WebContextMenuTarget) -> Unit,
    onViewResourceUrl: (WebContextMenuTarget) -> Unit,
) {
    Box {
        DropdownMenu(
            expanded = target != null,
            onDismissRequest = onDismiss,
        ) {
            val currentTarget = target ?: return@DropdownMenu
            DropdownMenuItem(
                text = { Text("Copy URL") },
                onClick = {
                    onCopyUrl(currentTarget)
                    onDismiss()
                },
            )
            DropdownMenuItem(
                text = { Text("Open in current session") },
                onClick = {
                    onOpenCurrentSession(currentTarget)
                    onDismiss()
                },
            )
            DropdownMenuItem(
                text = { Text("Download URL") },
                onClick = {
                    onDownloadUrl(currentTarget)
                    onDismiss()
                },
            )
            DropdownMenuItem(
                text = { Text("View resource URL") },
                onClick = {
                    onViewResourceUrl(currentTarget)
                    onDismiss()
                },
            )
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
    onOpenLocalHtml: () -> Unit,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onWebPageEvent: (WebPageEvent) -> Unit,
    onOpenDocument: (Array<String>, (Uri?) -> Unit) -> Unit,
    onRequestRuntimePermissions: (Array<String>, (Map<String, Boolean>) -> Unit) -> Unit,
    onWebPermissionPrompt: (WebPermissionPrompt?) -> Unit,
    onFullscreenVideoChanged: (Boolean) -> Unit,
    onContextMenuTarget: (WebContextMenuTarget) -> Unit,
    controller: WebViewController,
    modifier: Modifier = Modifier,
) {
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
                onOpenLocalHtml = onOpenLocalHtml,
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
            onOpenDocument = onOpenDocument,
            onRequestRuntimePermissions = onRequestRuntimePermissions,
            onWebPermissionPrompt = onWebPermissionPrompt,
            onFullscreenVideoChanged = onFullscreenVideoChanged,
            onContextMenuTarget = onContextMenuTarget,
            controller = controller,
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
    onEvaluateJavaScript: (script: String, callback: (String) -> Unit) -> Unit,
    onReadCookies: () -> Unit,
    onClearCookies: () -> Unit,
    onClearWebViewCache: () -> Unit,
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
            onEvaluateJavaScript = onEvaluateJavaScript,
            onReadCookies = onReadCookies,
            onClearCookies = onClearCookies,
            onClearWebViewCache = onClearWebViewCache,
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
    onEvaluateJavaScript: (script: String, callback: (String) -> Unit) -> Unit,
    onReadCookies: () -> Unit,
    onClearCookies: () -> Unit,
    onClearWebViewCache: () -> Unit,
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
            onEvaluateJavaScript = onEvaluateJavaScript,
            onReadCookies = onReadCookies,
            onClearCookies = onClearCookies,
            onClearWebViewCache = onClearWebViewCache,
            modifier = modifier,
        )
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

private fun Context.takePersistableReadPermissionIfAvailable(
    uri: Uri,
    onMessage: (String) -> Unit,
) {
    runCatching {
        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }.onFailure { error ->
        if (error is SecurityException || error is IllegalArgumentException) {
            onMessage("Persistable local file permission unavailable: ${error.message.orEmpty()}")
        }
    }
}

private fun WorkbenchViewModel.openContextMenuTarget(url: String) {
    when (Uri.parse(url).scheme?.lowercase()) {
        "http", "https" -> loadUrl(url)
        "content", "file" -> loadLocalFile(url)
        else -> addDebugMessage("Open skipped: unsupported URL scheme")
    }
}
