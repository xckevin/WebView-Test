package com.xckevin.android.app.webview.test.ui.workbench

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ClipData
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xckevin.android.app.webview.test.AppContainer
import com.xckevin.android.app.webview.test.R
import com.xckevin.android.app.webview.test.debug.DebugClearScope
import com.xckevin.android.app.webview.test.debug.DebugState
import com.xckevin.android.app.webview.test.debug.UserFlowKind
import com.xckevin.android.app.webview.test.model.HistoryItem
import com.xckevin.android.app.webview.test.model.WebTestConfig
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
                    historyRepository = container.historyRepository,
                    settingsRepository = container.settingsStore,
                ) as T
            }
        }
    }
    val viewModel: WorkbenchViewModel = viewModel(factory = factory)
    val state by viewModel.state.collectAsStateWithLifecycle()
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
    val readCookies: ((String) -> Unit) -> Unit = { callback ->
        val cookies = webViewController.readCookies()
        viewModel.recordJavaScriptResult(script = "document.cookie", result = cookies)
        callback(cookies)
    }
    val clearCookies: () -> Unit = {
        webViewController.clearCookies { removed ->
            viewModel.addDebugMessage("Cookies cleared: $removed")
            viewModel.addUserFlow(UserFlowKind.COOKIE, "Cookies cleared", "removed=$removed")
        }
    }
    val clearWebViewCache: () -> Unit = {
        webViewController.clearCache()
        viewModel.addDebugMessage("WebView cache cleared")
        viewModel.addUserFlow(UserFlowKind.CACHE, "WebView cache cleared")
    }
    val openLocalHtml: () -> Unit = {
        localHtmlLauncher.launch(arrayOf("text/html", "text/*", "application/xhtml+xml"))
    }
    val openPermissionFixture: () -> Unit = {
        viewModel.loadLocalFile("file:///android_asset/fixtures/permission_fixture.html")
    }
    val openLocalFixture: () -> Unit = {
        viewModel.loadLocalFile("file:///android_asset/fixtures/local_debug_fixture.html")
    }

    BackHandler(enabled = state.isVideoFullscreen || state.isFullscreen || state.canGoBack) {
        if (state.isVideoFullscreen && webViewController.hideCustomView()) {
            return@BackHandler
        }
        if (state.isFullscreen) {
            viewModel.toggleFullscreen()
            return@BackHandler
        }
        webViewController.goBack()
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

    DisposableEffect(context, viewModel) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context, intent: Intent) {
                if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
                val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (downloadId <= 0L) return

                viewModel.onWebPageEvent(receiverContext.downloadStatusChangedEvent(downloadId))
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    LaunchedEffect(scanResult) {
        val result = scanResult ?: return@LaunchedEffect
        viewModel.loadUrl(result)
        onScanResultConsumed()
    }

    AppScaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            WorkbenchFrame(
                state = state,
                history = history,
                browser = { browserModifier, onOpenTools ->
                    BrowserColumn(
                        state = state,
                        showUrlBar = state.shouldShowUrlBar(),
                        onOpenTools = onOpenTools,
                        onUrlInputChanged = viewModel::onUrlInputChanged,
                        onLoad = viewModel::loadUrl,
                        onScan = onOpenScanner,
                        onOpenLocalHtml = openLocalHtml,
                        onOpenPermissionFixture = openPermissionFixture,
                        onOpenLocalFixture = openLocalFixture,
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
                        modifier = browserModifier,
                    )
                },
                onGoBack = { webViewController.goBack() },
                onGoForward = { webViewController.goForward() },
                onRefresh = viewModel::refresh,
                onToggleFullscreen = viewModel::toggleFullscreen,
                onSelectPanel = viewModel::selectPanel,
                onConfigChanged = viewModel::applyConfig,
                onOpenHistoryItem = viewModel::openHistory,
                onClearHistory = viewModel::clearHistory,
                onDeleteHistoryItem = viewModel::deleteHistoryItem,
                onClearDebugLogs = viewModel::clearDebugLogs,
                onClearDebug = viewModel::clearDebug,
                onEvaluateJavaScript = evaluateJavaScript,
                onReadCookies = readCookies,
                onClearCookies = clearCookies,
                onClearWebViewCache = clearWebViewCache,
                onFullscreenExit = {
                    if (!state.isVideoFullscreen || !webViewController.hideCustomView()) {
                        viewModel.toggleFullscreen()
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )

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
                    viewModel.addUserFlow(UserFlowKind.CONTEXT_MENU, "Context menu download requested", target.url)
                },
                onViewResourceUrl = { target ->
                    viewModel.addDebugMessage("${target.label} URL: ${target.url}")
                    viewModel.addUserFlow(UserFlowKind.CONTEXT_MENU, "Context menu resource inspected", target.url)
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
                            Text(stringResource(R.string.action_allow))
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                permissionPrompt = null
                                prompt.onDeny()
                            },
                        ) {
                            Text(stringResource(R.string.action_deny))
                        }
                    },
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun WorkbenchFrame(
    state: WorkbenchState,
    history: List<HistoryItem>,
    browser: @Composable (Modifier, onOpenTools: () -> Unit) -> Unit,
    onGoBack: () -> Unit,
    onGoForward: () -> Unit,
    onRefresh: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onSelectPanel: (WorkbenchPanel) -> Unit,
    onConfigChanged: (WebTestConfig) -> Unit,
    onOpenHistoryItem: (HistoryItem) -> Unit,
    onClearHistory: () -> Unit,
    onDeleteHistoryItem: (HistoryItem) -> Unit,
    onClearDebugLogs: () -> Unit,
    onClearDebug: (DebugClearScope) -> Unit = {},
    onEvaluateJavaScript: (script: String, callback: (String) -> Unit) -> Unit,
    onReadCookies: ((String) -> Unit) -> Unit,
    onClearCookies: () -> Unit,
    onClearWebViewCache: () -> Unit,
    modifier: Modifier = Modifier,
    onFullscreenExit: () -> Unit = {},
) {
    BoxWithConstraints(modifier = modifier) {
        val sidePanelWidth = 380.dp
        val showPanels = state.shouldShowPanels()
        val useSidePanel = showPanels && maxWidth > 840.dp
        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current
        var isDrawerOpen by remember { mutableStateOf(false) }
        var isHistoryPageOpen by remember { mutableStateOf(false) }
        val browserPadding = when {
            !showPanels -> Modifier
            useSidePanel -> Modifier.padding(end = sidePanelWidth)
            else -> Modifier
        }
        val openTools: () -> Unit = {
            if (showPanels && !useSidePanel) {
                focusManager.clearFocus(force = true)
                keyboardController?.hide()
                isDrawerOpen = true
            }
        }
        val closeTools: () -> Unit = {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
            isDrawerOpen = false
        }

        BackHandler(enabled = showPanels && !useSidePanel && isDrawerOpen) {
            closeTools()
        }

        LaunchedEffect(showPanels, useSidePanel) {
            if (!showPanels || useSidePanel) {
                isDrawerOpen = false
            }
        }

        val browserContent: @Composable () -> Unit = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(browserPadding),
            ) {
                browser(
                    Modifier.fillMaxSize(),
                    openTools,
                )
                if (!state.isVideoFullscreen) {
                    FloatingBrowserControls(
                        isFullscreen = state.isFullscreen,
                        onGoBack = onGoBack,
                        onGoForward = onGoForward,
                        onRefresh = onRefresh,
                        onOpenHistory = { isHistoryPageOpen = true },
                        onToggleFullscreen = onToggleFullscreen,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp)
                            .zIndex(2f),
                    )
                }
            }
        }

        if (showPanels) {
            if (useSidePanel) {
                browserContent()
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(sidePanelWidth)
                        .fillMaxHeight(),
                ) {
                    VerticalDivider()
                    WorkbenchPanelSurface(
                        state = state,
                        history = history,
                        onSelectPanel = onSelectPanel,
                        onConfigChanged = onConfigChanged,
                        onOpenHistoryItem = onOpenHistoryItem,
                        onClearHistory = onClearHistory,
                        onDeleteHistoryItem = onDeleteHistoryItem,
                        onClearDebugLogs = onClearDebugLogs,
                        onClearDebug = onClearDebug,
                        onEvaluateJavaScript = onEvaluateJavaScript,
                        onReadCookies = onReadCookies,
                        onClearCookies = onClearCookies,
                        onClearWebViewCache = onClearWebViewCache,
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                browserContent()
                if (isDrawerOpen) {
                    WorkbenchDrawer(
                        state = state,
                        history = history,
                        onClose = closeTools,
                        onSelectPanel = onSelectPanel,
                        onConfigChanged = onConfigChanged,
                        onOpenHistoryItem = { item ->
                            onOpenHistoryItem(item)
                            closeTools()
                        },
                        onClearHistory = onClearHistory,
                        onDeleteHistoryItem = onDeleteHistoryItem,
                        onClearDebugLogs = onClearDebugLogs,
                        onClearDebug = onClearDebug,
                        onEvaluateJavaScript = onEvaluateJavaScript,
                        onReadCookies = onReadCookies,
                        onClearCookies = onClearCookies,
                        onClearWebViewCache = onClearWebViewCache,
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(4f),
                    )
                }
            }
        } else if (state.shouldShowFullscreenExitOverlay()) {
            browserContent()
//            FilledTonalIconButton(
//                onClick = onFullscreenExit,
//                modifier = Modifier
//                    .align(Alignment.TopEnd)
//                    .padding(12.dp),
//            ) {
//                Icon(
//                    Icons.Outlined.FullscreenExit,
//                    contentDescription = stringResource(R.string.action_exit_fullscreen),
//                )
//            }
        } else {
            browserContent()
        }

        if (isHistoryPageOpen && !state.isVideoFullscreen) {
            HistoryPage(
                history = history,
                onClose = { isHistoryPageOpen = false },
                onOpenHistoryItem = { item ->
                    onOpenHistoryItem(item)
                    isHistoryPageOpen = false
                },
                onClearHistory = onClearHistory,
                onDeleteHistoryItem = onDeleteHistoryItem,
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(5f),
            )
        }
    }
}

@Composable
private fun WorkbenchDrawer(
    state: WorkbenchState,
    history: List<HistoryItem>,
    onClose: () -> Unit,
    onSelectPanel: (WorkbenchPanel) -> Unit,
    onConfigChanged: (WebTestConfig) -> Unit,
    onOpenHistoryItem: (HistoryItem) -> Unit,
    onClearHistory: () -> Unit,
    onDeleteHistoryItem: (HistoryItem) -> Unit,
    onClearDebugLogs: () -> Unit,
    onClearDebug: (DebugClearScope) -> Unit,
    onEvaluateJavaScript: (script: String, callback: (String) -> Unit) -> Unit,
    onReadCookies: ((String) -> Unit) -> Unit,
    onClearCookies: () -> Unit,
    onClearWebViewCache: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("workbench_drawer"),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRail(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                header = {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(R.string.action_close),
                        )
                    }
                },
            ) {
                WorkbenchPanel.entries.forEach { panel ->
                    NavigationRailItem(
                        selected = panel == state.selectedPanel,
                        onClick = { onSelectPanel(panel) },
                        icon = {
                            Icon(
                                panel.debugMode.icon,
                                contentDescription = null,
                            )
                        },
                        label = {
                            Text(
                                text = panel.debugMode.label(state.debugState),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                    )
                }
            }
            VerticalDivider()
            PanelContent(
                selectedPanel = state.selectedPanel,
                state = state,
                history = history,
                onConfigChanged = onConfigChanged,
                onOpenHistoryItem = onOpenHistoryItem,
                onClearHistory = onClearHistory,
                onDeleteHistoryItem = onDeleteHistoryItem,
                onClearDebugLogs = onClearDebugLogs,
                onClearDebug = onClearDebug,
                onEvaluateJavaScript = onEvaluateJavaScript,
                onReadCookies = onReadCookies,
                onClearCookies = onClearCookies,
                onClearWebViewCache = onClearWebViewCache,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun HistoryPage(
    history: List<HistoryItem>,
    onClose: () -> Unit,
    onOpenHistoryItem: (HistoryItem) -> Unit,
    onClearHistory: () -> Unit,
    onDeleteHistoryItem: (HistoryItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onClose)
    Surface(
        modifier = modifier.testTag("workbench_history_page"),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PanelHeader(
                title = stringResource(R.string.panel_history),
                actions = {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(R.string.action_close),
                        )
                    }
                },
            )
            HistoryPanel(
                history = history,
                onOpenHistoryItem = onOpenHistoryItem,
                onClearHistory = onClearHistory,
                onDeleteHistoryItem = onDeleteHistoryItem,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun FloatingBrowserControls(
    isFullscreen: Boolean,
    onGoBack: () -> Unit,
    onGoForward: () -> Unit,
    onRefresh: () -> Unit,
    onOpenHistory: () -> Unit,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(percent = 50),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onGoBack,
                modifier = Modifier.size(44.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                )
            }
            IconButton(
                onClick = onGoForward,
                modifier = Modifier.size(44.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = stringResource(R.string.action_forward),
                )
            }
            IconButton(
                onClick = onRefresh,
                modifier = Modifier.size(44.dp),
            ) {
                Icon(
                    Icons.Outlined.Refresh,
                    contentDescription = stringResource(R.string.action_refresh),
                )
            }
            IconButton(
                onClick = onOpenHistory,
                modifier = Modifier.size(44.dp),
            ) {
                Icon(
                    Icons.Outlined.History,
                    contentDescription = stringResource(R.string.panel_history),
                )
            }
            IconButton(
                onClick = onToggleFullscreen,
                modifier = Modifier.size(44.dp),
            ) {
                Icon(
                    if (isFullscreen) Icons.Outlined.FullscreenExit else Icons.Outlined.Fullscreen,
                    contentDescription = stringResource(
                        if (isFullscreen) R.string.action_exit_fullscreen else R.string.action_fullscreen
                    ),
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
                text = { Text(stringResource(R.string.action_copy_url)) },
                onClick = {
                    onCopyUrl(currentTarget)
                    onDismiss()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_open_current_session)) },
                onClick = {
                    onOpenCurrentSession(currentTarget)
                    onDismiss()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_download_url)) },
                onClick = {
                    onDownloadUrl(currentTarget)
                    onDismiss()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_view_resource_url)) },
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
    onOpenTools: () -> Unit,
    onUrlInputChanged: (String) -> Unit,
    onLoad: () -> Unit,
    onScan: () -> Unit,
    onOpenLocalHtml: () -> Unit,
    onOpenPermissionFixture: () -> Unit,
    onOpenLocalFixture: () -> Unit,
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
    val density = LocalDensity.current
    var urlBarHeightPx by remember { mutableStateOf(0) }
    val urlBarHeight = with(density) { urlBarHeightPx.toDp() }

    Box(modifier = modifier.fillMaxSize()) {
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
                .fillMaxSize()
                .padding(top = if (showUrlBar) urlBarHeight else 0.dp),
        )

        if (showUrlBar) {
            Surface(
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .zIndex(1f)
                    .onSizeChanged { urlBarHeightPx = it.height },
            ) {
                UrlBar(
                    urlInput = state.urlInput,
                    urlError = state.urlError,
                    isLoading = state.isLoading,
                    loadProgress = state.loadProgress,
                    isFullscreen = state.isFullscreen,
                    onOpenTools = onOpenTools,
                    onUrlInputChanged = onUrlInputChanged,
                    onLoad = onLoad,
                    onScan = onScan,
                    onOpenLocalHtml = onOpenLocalHtml,
                    onOpenPermissionFixture = onOpenPermissionFixture,
                    onOpenLocalFixture = onOpenLocalFixture,
                    onRefresh = onRefresh,
                    onOpenSettings = onOpenSettings,
                    onToggleFullscreen = onToggleFullscreen,
                )
            }
        }
    }
}

@Composable
private fun WorkbenchPanelSurface(
    state: WorkbenchState,
    history: List<HistoryItem>,
    onSelectPanel: (WorkbenchPanel) -> Unit,
    onConfigChanged: (WebTestConfig) -> Unit,
    onOpenHistoryItem: (HistoryItem) -> Unit,
    onClearHistory: () -> Unit,
    onDeleteHistoryItem: (HistoryItem) -> Unit,
    onClearDebugLogs: () -> Unit,
    onClearDebug: (DebugClearScope) -> Unit,
    onEvaluateJavaScript: (script: String, callback: (String) -> Unit) -> Unit,
    onReadCookies: ((String) -> Unit) -> Unit,
    onClearCookies: () -> Unit,
    onClearWebViewCache: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxSize(),
    ) {
        Column {
            PanelTabs(
                selectedPanel = state.selectedPanel,
                debugState = state.debugState,
                onSelectPanel = onSelectPanel,
            )
            PanelContent(
                selectedPanel = state.selectedPanel,
                state = state,
                history = history,
                onConfigChanged = onConfigChanged,
                onOpenHistoryItem = onOpenHistoryItem,
                onClearHistory = onClearHistory,
                onDeleteHistoryItem = onDeleteHistoryItem,
                onClearDebugLogs = onClearDebugLogs,
                onClearDebug = onClearDebug,
                onEvaluateJavaScript = onEvaluateJavaScript,
                onReadCookies = onReadCookies,
                onClearCookies = onClearCookies,
                onClearWebViewCache = onClearWebViewCache,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PanelTabs(
    selectedPanel: WorkbenchPanel,
    debugState: DebugState,
    onSelectPanel: (WorkbenchPanel) -> Unit,
    modifier: Modifier = Modifier,
) {
    PrimaryScrollableTabRow(
        selectedTabIndex = WorkbenchPanel.entries.indexOf(selectedPanel),
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        edgePadding = 8.dp,
    ) {
        WorkbenchPanel.entries.forEach { panel ->
            Tab(
                selected = panel == selectedPanel,
                onClick = { onSelectPanel(panel) },
                text = {
                    Text(
                        text = panel.debugMode.label(debugState),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                icon = {
                    Icon(
                        panel.debugMode.icon,
                        contentDescription = null,
                    )
                },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PanelContent(
    selectedPanel: WorkbenchPanel,
    state: WorkbenchState,
    history: List<HistoryItem>,
    onConfigChanged: (WebTestConfig) -> Unit,
    onOpenHistoryItem: (HistoryItem) -> Unit,
    onClearHistory: () -> Unit,
    onDeleteHistoryItem: (HistoryItem) -> Unit,
    onClearDebugLogs: () -> Unit,
    onClearDebug: (DebugClearScope) -> Unit,
    onEvaluateJavaScript: (script: String, callback: (String) -> Unit) -> Unit,
    onReadCookies: ((String) -> Unit) -> Unit,
    onClearCookies: () -> Unit,
    onClearWebViewCache: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DebugPanel(
        debugState = state.debugState,
        config = state.config,
        sourceType = state.activeSourceType,
        onClearDebugLogs = onClearDebugLogs,
        onClearDebug = onClearDebug,
        onEvaluateJavaScript = onEvaluateJavaScript,
        onReadCookies = onReadCookies,
        onClearCookies = onClearCookies,
        onClearWebViewCache = onClearWebViewCache,
        selectedMode = selectedPanel.debugMode,
        showModeTabs = false,
        modifier = modifier,
    )
}

private val WorkbenchPanel.debugMode: DebugMode
    get() = when (this) {
        WorkbenchPanel.OVERVIEW -> DebugMode.Overview
        WorkbenchPanel.TIMELINE -> DebugMode.Timeline
        WorkbenchPanel.LOGS -> DebugMode.Logs
        WorkbenchPanel.PAGE -> DebugMode.Page
        WorkbenchPanel.STORAGE -> DebugMode.Storage
        WorkbenchPanel.INSPECT -> DebugMode.Inspect
        WorkbenchPanel.NETWORK -> DebugMode.Network
        WorkbenchPanel.EXECUTE -> DebugMode.Execute
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

private fun Context.downloadStatusChangedEvent(downloadId: Long): WebPageEvent.DownloadStatusChanged {
    val manager = getSystemService(DownloadManager::class.java)
    return runCatching {
        manager.query(DownloadManager.Query().setFilterById(downloadId)).use { cursor ->
            if (!cursor.moveToFirst()) {
                return@use WebPageEvent.DownloadStatusChanged(
                    downloadId = downloadId,
                    status = "UNKNOWN",
                    reason = "DownloadManager row not found",
                    localUri = null,
                )
            }
            val status = cursor.getIntColumn(DownloadManager.COLUMN_STATUS).toDownloadStatusName()
            val reason = cursor.getIntColumn(DownloadManager.COLUMN_REASON).takeIf { it > 0 }?.let {
                "DownloadManager reason: $it"
            }
            WebPageEvent.DownloadStatusChanged(
                downloadId = downloadId,
                status = status,
                reason = reason,
                localUri = cursor.getStringColumn(DownloadManager.COLUMN_LOCAL_URI),
            )
        }
    }.getOrElse { error ->
        WebPageEvent.DownloadStatusChanged(
            downloadId = downloadId,
            status = "UNKNOWN",
            reason = "DownloadManager query failed: ${error.message.orEmpty()}",
            localUri = null,
        )
    }
}

private fun android.database.Cursor.getIntColumn(name: String): Int {
    val index = getColumnIndex(name)
    return if (index >= 0) getInt(index) else 0
}

private fun android.database.Cursor.getStringColumn(name: String): String? {
    val index = getColumnIndex(name)
    return if (index >= 0) getString(index) else null
}

private fun Int.toDownloadStatusName(): String =
    when (this) {
        DownloadManager.STATUS_SUCCESSFUL -> "SUCCESS"
        DownloadManager.STATUS_FAILED -> "FAILED"
        DownloadManager.STATUS_PENDING,
        DownloadManager.STATUS_PAUSED,
        DownloadManager.STATUS_RUNNING -> "QUEUED"
        else -> "UNKNOWN"
    }

private fun WorkbenchViewModel.openContextMenuTarget(url: String) {
    when (Uri.parse(url).scheme?.lowercase()) {
        "http", "https" -> loadUrl(url)
        "content", "file" -> loadLocalFile(url)
        else -> addDebugMessage("Open skipped: unsupported URL scheme")
    }
}

internal fun WorkbenchState.shouldShowUrlBar(): Boolean = !isFullscreen && !isVideoFullscreen

internal fun WorkbenchState.shouldShowPanels(): Boolean = !isFullscreen && !isVideoFullscreen

internal fun WorkbenchState.shouldShowFullscreenExitOverlay(): Boolean = isFullscreen || isVideoFullscreen
