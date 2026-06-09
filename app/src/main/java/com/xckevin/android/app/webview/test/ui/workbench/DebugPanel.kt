package com.xckevin.android.app.webview.test.ui.workbench

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Http
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.xckevin.android.app.webview.test.R
import com.xckevin.android.app.webview.test.debug.ConsoleLog
import com.xckevin.android.app.webview.test.debug.DebugClearScope
import com.xckevin.android.app.webview.test.debug.DebugEvent
import com.xckevin.android.app.webview.test.debug.DebugEventType
import com.xckevin.android.app.webview.test.debug.DebugSeverity
import com.xckevin.android.app.webview.test.debug.DebugState
import com.xckevin.android.app.webview.test.debug.DownloadSnapshot
import com.xckevin.android.app.webview.test.debug.DebugResultFormatter
import com.xckevin.android.app.webview.test.debug.JsExecutionResult
import com.xckevin.android.app.webview.test.debug.PageError
import com.xckevin.android.app.webview.test.debug.PageScripts
import com.xckevin.android.app.webview.test.debug.RequestSnapshot
import com.xckevin.android.app.webview.test.debug.UserFlowSnapshot
import com.xckevin.android.app.webview.test.model.SourceType
import com.xckevin.android.app.webview.test.model.WebTestConfig
import com.xckevin.android.app.webview.test.ui.theme.Red500
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

private data class DebugStorageResult(
    val result: String,
    val source: DebugStorageSource,
)

@Composable
internal fun DebugPanel(
    debugState: DebugState,
    config: WebTestConfig,
    sourceType: SourceType,
    onClearDebugLogs: () -> Unit,
    onEvaluateJavaScript: (script: String, callback: (String) -> Unit) -> Unit,
    onReadCookies: ((String) -> Unit) -> Unit,
    onClearCookies: () -> Unit,
    onClearWebViewCache: () -> Unit,
    onClearDebug: (DebugClearScope) -> Unit = {},
    apiResponses: List<PageError> = emptyList(),
    onClearApiResponses: () -> Unit = {},
    onInspectPointerStarted: () -> Unit = {},
    selectedMode: DebugMode? = null,
    showModeTabs: Boolean = true,
    modifier: Modifier = Modifier,
) {
    var localSelectedMode by remember { mutableStateOf(DebugMode.Overview) }
    val activeMode = selectedMode ?: localSelectedMode
    val callbackResults = remember { mutableStateListOf<String>() }
    val storageResults = remember { mutableStateListOf<DebugStorageResult>() }
    var selectedStorageResult by remember { mutableStateOf<DebugStorageResult?>(null) }
    var inspectResult by remember { mutableStateOf<String?>(null) }
    var selectedNetworkRequest by remember { mutableStateOf<RequestSnapshot?>(null) }
    var selectedNetworkDownload by remember { mutableStateOf<DownloadSnapshot?>(null) }
    var selectedNetworkResponse by remember { mutableStateOf<PageError?>(null) }
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val copyText: (String, String) -> Unit = { label, value ->
        coroutineScope.launch {
            clipboard.setClipEntry(ClipData.newPlainText(label, value).toClipEntry())
        }
    }
    val captureResult: (String) -> Unit = { result ->
        callbackResults.add(0, result)
        while (callbackResults.size > 20) {
            callbackResults.removeAt(callbackResults.lastIndex)
        }
    }
    val captureStorageResult: (DebugStorageSource, String) -> Unit = { source, result ->
        val storageResult = DebugStorageResult(result = result, source = source)
        storageResults.add(0, storageResult)
        while (storageResults.size > 10) {
            storageResults.removeAt(storageResults.lastIndex)
        }
        selectedStorageResult = storageResult
    }
    val shareText: (String, String) -> Unit = { title, value ->
        shareDebugText(
            context = context,
            title = title,
            value = value,
            onFailure = { copyText(title, value) },
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (showModeTabs) {
            PrimaryScrollableTabRow(
                selectedTabIndex = DebugMode.entries.indexOf(activeMode),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                edgePadding = 8.dp,
            ) {
                DebugMode.entries.forEach { mode ->
                    Tab(
                        selected = activeMode == mode,
                        onClick = { localSelectedMode = mode },
                        icon = {
                            Icon(
                                mode.icon,
                                contentDescription = null,
                            )
                        },
                        text = {
                            Text(
                                text = mode.label(debugState),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        when (activeMode) {
            DebugMode.Overview -> OverviewTab(
                debugState = debugState,
                config = config,
                sourceType = sourceType,
                onClearDebug = onClearDebug,
                onCopy = copyText,
                onShare = shareText,
                showHeader = !showModeTabs,
                modifier = Modifier.weight(1f),
            )

            DebugMode.Timeline -> TimelineTab(
                events = debugState.timeline,
                onClearDebug = onClearDebug,
                onCopy = copyText,
                showHeader = !showModeTabs,
                modifier = Modifier.weight(1f),
            )

            DebugMode.Logs -> LogsTab(
                logs = debugState.consoleLogs,
                errors = debugState.errors,
                onClearDebugLogs = onClearDebugLogs,
                onClearDebug = onClearDebug,
                onCopy = copyText,
                showHeader = !showModeTabs,
                modifier = Modifier.weight(1f),
            )

            DebugMode.Page -> PageTab(
                debugState = debugState,
                config = config,
                sourceType = sourceType,
                onClearWebViewCache = onClearWebViewCache,
                onCopy = copyText,
                showHeader = !showModeTabs,
                modifier = Modifier.weight(1f),
            )

            DebugMode.Storage -> StorageTab(
                onReadCookies = onReadCookies,
                onEvaluateJavaScript = onEvaluateJavaScript,
                onResult = captureStorageResult,
                results = storageResults,
                onCopy = copyText,
                onOpenResult = { selectedStorageResult = it },
                showHeader = !showModeTabs,
                modifier = Modifier.weight(1f),
            )

            DebugMode.Inspect -> InspectTab(
                onEvaluateJavaScript = onEvaluateJavaScript,
                onResult = { result -> inspectResult = result },
                hasResult = inspectResult != null,
                onCopy = copyText,
                onInspectPointerStarted = onInspectPointerStarted,
                showHeader = !showModeTabs,
                modifier = Modifier.weight(1f),
            )

            DebugMode.Network -> NetworkTab(
                requests = debugState.requests,
                errors = debugState.errors + apiResponses,
                downloads = debugState.downloads,
                apiResponses = apiResponses,
                onClearDebugLogs = {
                    onClearApiResponses()
                    onClearDebugLogs()
                },
                onClearDebug = onClearDebug,
                onCopy = copyText,
                onOpenRequest = { selectedNetworkRequest = it },
                onOpenApiResponse = { selectedNetworkResponse = it },
                onOpenDownload = { selectedNetworkDownload = it },
                showHeader = !showModeTabs,
                modifier = Modifier.weight(1f),
            )

            DebugMode.Execute -> JsExecTab(
                jsResults = debugState.jsResults,
                callbackResults = callbackResults,
                onEvaluateJavaScript = onEvaluateJavaScript,
                onResult = captureResult,
                onCopy = copyText,
                onClearDebug = onClearDebug,
                showHeader = !showModeTabs,
                modifier = Modifier.weight(1f),
            )

        }
    }

    inspectResult?.let { result ->
        FullscreenDebugDialog(onClose = { inspectResult = null }) {
            DebugInspectViewer(
                result = result,
                onClose = { inspectResult = null },
                onCopy = { value -> copyText("inspect-result", value) },
            )
        }
    }

    selectedStorageResult?.let { storageResult ->
        FullscreenDebugDialog(onClose = { selectedStorageResult = null }) {
            DebugStorageTableViewer(
                title = storageResult.source.label,
                result = storageResult.result,
                sourceHint = storageResult.source,
                onClose = { selectedStorageResult = null },
                onCopy = { value -> copyText("storage-table", value) },
                onSave = { source, key, value ->
                    when (source) {
                        DebugStorageSource.LocalStorage -> onEvaluateJavaScript(PageScripts.writeStorageKey("localStorage", key, value)) { result ->
                            captureStorageResult(source, result)
                        }
                        DebugStorageSource.SessionStorage -> onEvaluateJavaScript(PageScripts.writeStorageKey("sessionStorage", key, value)) { result ->
                            captureStorageResult(source, result)
                        }
                        DebugStorageSource.Cookie -> onEvaluateJavaScript(PageScripts.writeCookie(key, value)) { result ->
                            captureStorageResult(source, result)
                        }
                        DebugStorageSource.Unknown -> Unit
                    }
                },
                onDelete = { source, key ->
                    when (source) {
                        DebugStorageSource.LocalStorage -> onEvaluateJavaScript(PageScripts.deleteStorageKey("localStorage", key)) { result ->
                            captureStorageResult(source, result)
                        }
                        DebugStorageSource.SessionStorage -> onEvaluateJavaScript(PageScripts.deleteStorageKey("sessionStorage", key)) { result ->
                            captureStorageResult(source, result)
                        }
                        DebugStorageSource.Cookie -> onEvaluateJavaScript(PageScripts.deleteCookie(key)) { result ->
                            captureStorageResult(source, result)
                        }
                        DebugStorageSource.Unknown -> Unit
                    }
                },
            )
        }
    }

    selectedNetworkRequest?.let { request ->
        FullscreenDebugDialog(onClose = { selectedNetworkRequest = null }) {
            DebugNetworkDetailScreen(
                request = request,
                error = findMatchingHttpError(request, debugState.errors + apiResponses),
                download = null,
                onClose = { selectedNetworkRequest = null },
                onCopy = copyText,
            )
        }
    }

    selectedNetworkResponse?.let { response ->
        FullscreenDebugDialog(onClose = { selectedNetworkResponse = null }) {
            DebugNetworkDetailScreen(
                request = null,
                error = response,
                download = null,
                onClose = { selectedNetworkResponse = null },
                onCopy = copyText,
            )
        }
    }

    selectedNetworkDownload?.let { download ->
        FullscreenDebugDialog(onClose = { selectedNetworkDownload = null }) {
            DebugNetworkDetailScreen(
                request = null,
                error = null,
                download = download,
                onClose = { selectedNetworkDownload = null },
                onCopy = copyText,
            )
        }
    }
}

internal fun shareDebugText(
    context: Context,
    title: String,
    value: String,
    onFailure: (Throwable) -> Unit = {},
): Boolean {
    val sendIntent = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_SUBJECT, title)
        .putExtra(Intent.EXTRA_TEXT, value)
    val chooserIntent = Intent.createChooser(sendIntent, title).apply {
        if (context !is Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    return try {
        context.startActivity(chooserIntent)
        true
    } catch (error: RuntimeException) {
        onFailure(error)
        false
    }
}

@Composable
private fun OverviewTab(
    debugState: DebugState,
    config: WebTestConfig,
    sourceType: SourceType,
    onClearDebug: (DebugClearScope) -> Unit,
    onCopy: (String, String) -> Unit,
    onShare: (String, String) -> Unit,
    showHeader: Boolean,
    modifier: Modifier = Modifier,
) {
    val page = debugState.page
    val debugBundleTitle = stringResource(R.string.debug_share_bundle_title)
    val errorCount = debugState.errors.size
    val warningCount = debugState.timeline.count { it.severity == DebugSeverity.WARNING }
    val requestCount = debugState.requests.size
    val failedDownloads = debugState.downloads.count {
        it.status == com.xckevin.android.app.webview.test.debug.DownloadStatus.FAILED ||
            it.status == com.xckevin.android.app.webview.test.debug.DownloadStatus.SKIPPED
    }
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (showHeader) {
            item {
            DebugPanelHeader(
                mode = DebugMode.Overview,
                canClear = debugState.hasDebugEntries(),
                onClear = { onClearDebug(DebugClearScope.ALL) },
            )
            }
        }
        item {
            val bundle = debugState.toDebugBundle(config, sourceType)
            DebugControlSection(title = stringResource(R.string.debug_section_actions)) {
                ButtonRow {
                    FilledTonalButton(onClick = { onCopy("debug-bundle", bundle) }) {
                        Text(stringResource(R.string.debug_copy_bundle))
                    }
                    OutlinedButton(onClick = { onShare(debugBundleTitle, bundle) }) {
                        Text(stringResource(R.string.action_share))
                    }
                }
            }
        }
        item {
            DebugItem(
                title = page.url ?: stringResource(R.string.debug_no_page_loaded),
                subtitle = stringResource(R.string.debug_status_label, page.status),
                details = listOf(
                    stringResource(R.string.debug_title_label, page.title.ifBlank { "-" }),
                    stringResource(R.string.debug_progress_percent_label, page.progress),
                    stringResource(R.string.debug_navigation_label, page.navigationId),
                    stringResource(R.string.debug_updated_label, formatTime(page.timestamp)),
                ),
                onCopy = onCopy,
            )
        }
        item {
            SectionLabel(text = stringResource(R.string.debug_section_signals))
        }
        item {
            DebugItem(
                title = if (errorCount == 0 && warningCount == 0) {
                    stringResource(R.string.debug_no_blocking_signals)
                } else {
                    stringResource(R.string.debug_signal_problem_counts, errorCount, warningCount)
                },
                subtitle = stringResource(
                    R.string.debug_signal_counts,
                    debugState.consoleLogs.size,
                    requestCount,
                    debugState.downloads.size,
                ),
                accentColor = when {
                    errorCount > 0 -> Red500
                    warningCount > 0 || failedDownloads > 0 -> Color(0xFFF59E0B)
                    else -> null
                },
                details = listOf(
                    stringResource(R.string.debug_js_results_count, debugState.jsResults.size),
                    stringResource(R.string.debug_user_flows_count, debugState.userFlows.size),
                    stringResource(R.string.debug_timeline_events_count, debugState.timeline.size),
                    stringResource(R.string.debug_failed_downloads_count, failedDownloads),
                ),
                onCopy = onCopy,
            )
        }
        item {
            SectionLabel(text = stringResource(R.string.debug_section_environment))
        }
        item {
            DebugItem(
                title = stringResource(R.string.debug_current_webview_environment),
                subtitle = stringResource(R.string.debug_source_type_label, sourceType),
                details = listOf(
                    stringResource(R.string.debug_javascript_enabled_label, config.javaScriptEnabled),
                    stringResource(R.string.debug_dom_storage_enabled_label, config.domStorageEnabled),
                    stringResource(R.string.debug_desktop_mode_enabled_label, config.desktopMode),
                    stringResource(R.string.debug_user_agent_mode_value_label, config.userAgentMode),
                    stringResource(R.string.debug_cache_mode_value_label, config.cacheMode),
                    stringResource(R.string.debug_mixed_content_value_label, config.mixedContentMode),
                    stringResource(R.string.debug_cookies_enabled_label, config.cookiesEnabled),
                    stringResource(R.string.debug_third_party_cookies_enabled_label, config.thirdPartyCookiesEnabled),
                    stringResource(
                        R.string.debug_permissions_config_label,
                        config.cameraPolicy,
                        config.microphonePolicy,
                        config.geolocationPolicy,
                    ),
                    stringResource(R.string.debug_chrome_remote_inspect_note),
                ),
                onCopy = onCopy,
            )
        }
        val latestProblems = debugState.timeline
            .filter { it.severity != DebugSeverity.INFO }
            .takeLast(5)
            .asReversed()
        if (latestProblems.isNotEmpty()) {
            item {
                SectionLabel(text = stringResource(R.string.debug_latest_problems))
            }
            items(latestProblems) { event ->
                DebugItem(
                    title = event.summary,
                    subtitle = "${event.type} · ${formatTime(event.timestamp)}",
                    accentColor = event.severity.color(),
                    details = event.details + event.diagnosticHint(),
                    onCopy = onCopy,
                )
            }
        }
    }
}

@Composable
private fun TimelineTab(
    events: List<DebugEvent>,
    onClearDebug: (DebugClearScope) -> Unit,
    onCopy: (String, String) -> Unit,
    showHeader: Boolean,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    var severity by remember { mutableStateOf<DebugSeverity?>(null) }
    var type by remember { mutableStateOf<DebugEventType?>(null) }
    val filtered = events
        .asReversed()
        .filter { event ->
            (severity == null || event.severity == severity) &&
                (type == null || event.type == type) &&
                (query.isBlank() || event.summary.contains(query, ignoreCase = true) ||
                    event.details.any { it.contains(query, ignoreCase = true) })
        }
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (showHeader) {
            item {
            DebugPanelHeader(
                mode = DebugMode.Timeline,
                canClear = events.isNotEmpty(),
                onClear = { onClearDebug(DebugClearScope.ALL) },
            )
            }
        }
        item {
            DebugControlSection(title = stringResource(R.string.debug_section_filters)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.debug_search_timeline)) },
                    leadingIcon = {
                        Icon(Icons.Outlined.FilterList, contentDescription = null)
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall,
                )
                DropdownFilter(
                    label = stringResource(R.string.debug_severity),
                    selectedLabel = severity?.name ?: stringResource(R.string.debug_all),
                    options = listOf<Pair<String, DebugSeverity?>>(stringResource(R.string.debug_all) to null) +
                        DebugSeverity.entries.map { it.name to it },
                    onSelected = { severity = it },
                )
                DropdownFilter(
                    label = stringResource(R.string.debug_event_type),
                    selectedLabel = type?.name?.lowercase() ?: stringResource(R.string.debug_any_type),
                    options = listOf<Pair<String, DebugEventType?>>(stringResource(R.string.debug_any_type) to null) +
                        DebugEventType.entries.map { it.name.lowercase() to it },
                    onSelected = { type = it },
                )
            }
        }
        item {
            DebugControlSection(title = stringResource(R.string.debug_section_actions)) {
                ButtonRow {
                    OutlinedButton(onClick = { onCopy("debug-timeline", filtered.joinToString("\n") { it.copyText() }) }) {
                        Text(stringResource(R.string.action_copy_filtered))
                    }
                }
            }
        }
        if (filtered.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.debug_no_timeline_events),
                    modifier = Modifier.padding(vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(filtered) { event ->
                DebugItem(
                    title = event.summary,
                    subtitle = "${event.type} · ${event.severity} · ${formatTime(event.timestamp)}",
                    accentColor = event.severity.color(),
                    details = listOf(stringResource(R.string.debug_navigation_label, event.navigationId)) +
                        event.details +
                        event.diagnosticHint(),
                    onCopy = onCopy,
                )
            }
        }
    }
}

@Composable
private fun LogsTab(
    logs: List<ConsoleLog>,
    errors: List<PageError>,
    onClearDebugLogs: () -> Unit,
    onClearDebug: (DebugClearScope) -> Unit,
    onCopy: (String, String) -> Unit,
    showHeader: Boolean,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    var level by remember { mutableStateOf<String?>(null) }
    val filteredLogs = logs.asReversed().filter { log ->
        (level == null || log.level.equals(level, ignoreCase = true)) &&
            (query.isBlank() || log.message.contains(query, ignoreCase = true) ||
                log.sourceId.contains(query, ignoreCase = true))
    }
    val filteredErrors = errors.asReversed().filter { error ->
        query.isBlank() || error.message.contains(query, ignoreCase = true) ||
            error.url.orEmpty().contains(query, ignoreCase = true) ||
            error.type.contains(query, ignoreCase = true)
    }
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (showHeader) {
            item {
            DebugPanelHeader(
                mode = DebugMode.Logs,
                canClear = logs.isNotEmpty() || errors.isNotEmpty(),
                onClear = onClearDebugLogs,
            )
            }
        }
        item {
            DebugControlSection(title = stringResource(R.string.debug_section_filters)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.debug_search_logs)) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall,
                )
                DropdownFilter(
                    label = stringResource(R.string.debug_level),
                    selectedLabel = level ?: stringResource(R.string.debug_all),
                    options = listOf<Pair<String, String?>>(stringResource(R.string.debug_all) to null) +
                        listOf("ERROR", "WARNING", "WARN", "LOG", "INFO").map { it to it },
                    onSelected = { level = it },
                )
            }
        }
        if (filteredLogs.isEmpty() && filteredErrors.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.debug_no_console_logs),
                    modifier = Modifier.padding(vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            if (filteredLogs.isNotEmpty()) {
                item {
                    SectionLabel(text = stringResource(R.string.debug_tab_console))
                }
                items(filteredLogs) { log ->
                    DebugItem(
                        title = "${log.level}: ${log.message}",
                        subtitle = formatTime(log.timestamp),
                        accentColor = logLevelColor(log.level),
                        details = listOfNotNull(
                            log.sourceId.takeIf { it.isNotBlank() }?.let {
                                stringResource(R.string.debug_source_label, it)
                            },
                            log.lineNumber.takeIf { it > 0 }?.let { stringResource(R.string.debug_line_label, it) },
                            log.navigationId.takeIf { it > 0L }?.let {
                                stringResource(R.string.debug_navigation_label, it)
                            },
                        ),
                        onCopy = onCopy,
                    )
                }
            }

            if (filteredErrors.isNotEmpty()) {
                item {
                    SectionLabel(text = stringResource(R.string.debug_tab_errors))
                }
                items(filteredErrors) { error ->
                    DebugItem(
                        title = "${error.type}: ${error.message}",
                        subtitle = formatTime(error.timestamp),
                        accentColor = Red500,
                        details = listOfNotNull(
                            error.url?.let { stringResource(R.string.debug_url_label, it) },
                            error.code?.let { stringResource(R.string.debug_code_label, it) },
                            error.statusCode?.let { stringResource(R.string.debug_status_code_label, it) },
                            stringResource(R.string.debug_main_frame_label, error.isMainFrame),
                            error.navigationId.takeIf { it > 0L }?.let {
                                stringResource(R.string.debug_navigation_label, it)
                            },
                        ) + error.responseHeaders.map { (key, value) -> "$key: $value" } + error.diagnosticHint(),
                        onCopy = onCopy,
                    )
                }
            }
        }
    }
}

@Composable
private fun PageTab(
    debugState: DebugState,
    config: WebTestConfig,
    sourceType: SourceType,
    onClearWebViewCache: () -> Unit,
    onCopy: (String, String) -> Unit,
    showHeader: Boolean,
    modifier: Modifier = Modifier,
) {
    val page = debugState.page
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (showHeader) {
            item {
                DebugPanelHeader(mode = DebugMode.Page, canClear = false, onClear = {})
            }
        }
        item {
            SectionLabel(text = stringResource(R.string.debug_page_details))
        }
        item {
            DebugItem(
                title = page.url ?: stringResource(R.string.debug_no_page_loaded),
                subtitle = stringResource(R.string.debug_status_label, page.status),
                details = listOf(
                    stringResource(R.string.debug_title_label, page.title),
                    stringResource(R.string.debug_progress_label, page.progress),
                    stringResource(R.string.debug_navigation_label, page.navigationId),
                    stringResource(R.string.debug_updated_label, formatTime(page.timestamp)),
                ),
                onCopy = onCopy,
            )
        }
        item {
            DebugControlSection(title = stringResource(R.string.debug_section_actions)) {
                DebugActionButton(
                    text = stringResource(R.string.debug_clear_webview_cache),
                    onClick = onClearWebViewCache,
                )
            }
        }
        item {
            SectionLabel(text = stringResource(R.string.debug_section_environment))
        }
        item {
            DebugItem(
                title = stringResource(R.string.debug_current_webview_environment),
                subtitle = "",
                details = listOf(
                    stringResource(R.string.debug_source_type_label, sourceType),
                    stringResource(R.string.debug_javascript_enabled_label, config.javaScriptEnabled),
                    stringResource(R.string.debug_dom_storage_enabled_label, config.domStorageEnabled),
                    stringResource(R.string.debug_desktop_mode_enabled_label, config.desktopMode),
                    stringResource(R.string.debug_user_agent_mode_value_label, config.userAgentMode),
                    stringResource(R.string.debug_cache_mode_value_label, config.cacheMode),
                    stringResource(R.string.debug_mixed_content_value_label, config.mixedContentMode),
                    stringResource(R.string.debug_cookies_enabled_label, config.cookiesEnabled),
                    stringResource(R.string.debug_third_party_cookies_enabled_label, config.thirdPartyCookiesEnabled),
                    stringResource(
                        R.string.debug_permissions_config_label,
                        config.cameraPolicy,
                        config.microphonePolicy,
                        config.geolocationPolicy,
                    ),
                ),
                onCopy = onCopy,
            )
        }
    }
}

@Composable
private fun StorageTab(
    onReadCookies: ((String) -> Unit) -> Unit,
    onEvaluateJavaScript: (script: String, callback: (String) -> Unit) -> Unit,
    onResult: (DebugStorageSource, String) -> Unit,
    results: List<DebugStorageResult>,
    onCopy: (String, String) -> Unit,
    onOpenResult: (DebugStorageResult) -> Unit,
    showHeader: Boolean,
    modifier: Modifier = Modifier,
) {
    val runStorageScript: (DebugStorageSource, String) -> Unit = { source, script ->
        onEvaluateJavaScript(script) { result -> onResult(source, result) }
    }
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (showHeader) {
            item {
                DebugPanelHeader(mode = DebugMode.Storage, canClear = false, onClear = {})
            }
        }
        item {
            DebugControlSection(title = stringResource(R.string.debug_section_actions)) {
                DebugActionButton(
                    text = stringResource(R.string.debug_read_cookies),
                    onClick = { onReadCookies { result -> onResult(DebugStorageSource.Cookie, result) } },
                )
                DebugActionButton(
                    text = stringResource(R.string.debug_read_local),
                    onClick = { runStorageScript(DebugStorageSource.LocalStorage, PageScripts.readLocalStorage()) },
                )
                DebugActionButton(
                    text = stringResource(R.string.debug_read_session),
                    onClick = { runStorageScript(DebugStorageSource.SessionStorage, PageScripts.readSessionStorage()) },
                )
            }
        }
        if (results.isEmpty()) {
            item {
                SectionLabel(text = stringResource(R.string.debug_section_results))
            }
            item {
                Text(
                    text = stringResource(R.string.debug_no_callback_results),
                    modifier = Modifier.padding(vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            item {
                SectionLabel(text = stringResource(R.string.debug_section_results))
            }
            items(results) { result ->
                val rows = remember(result.result, result.source) {
                    parseDebugStorageRows(result.result, result.source)
                }
                DebugItem(
                    title = stringResource(R.string.debug_storage_table_title, result.source.label),
                    subtitle = stringResource(R.string.debug_storage_table_summary, rows.size),
                    details = rows.take(5).map { "${it.key} = ${it.value}" } +
                        if (rows.size > 5) {
                            listOf(stringResource(R.string.debug_more_rows, rows.size - 5))
                        } else {
                            emptyList()
                        },
                    onCopy = onCopy,
                    onClick = { onOpenResult(result) },
                )
            }
        }
    }
}

@Composable
private fun InspectTab(
    onEvaluateJavaScript: (script: String, callback: (String) -> Unit) -> Unit,
    onResult: (String) -> Unit,
    hasResult: Boolean,
    onCopy: (String, String) -> Unit,
    onInspectPointerStarted: () -> Unit,
    showHeader: Boolean,
    modifier: Modifier = Modifier,
) {
    var elementSearch by remember { mutableStateOf("") }
    var elementSelector by remember { mutableStateOf("") }
    var pointerMessage by remember { mutableStateOf<String?>(null) }
    val pointerStartedMessage = stringResource(R.string.debug_pointer_started)
    val inspectPointerAriaLabel = stringResource(R.string.debug_inspect_pointer)
    val confirmLabel = stringResource(R.string.debug_confirm)
    val cancelLabel = stringResource(R.string.action_cancel)
    val noElementLabel = stringResource(R.string.debug_no_element)
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (showHeader) {
            item {
                DebugPanelHeader(mode = DebugMode.Inspect, canClear = false, onClear = {})
            }
        }
        item {
            DebugControlSection(title = stringResource(R.string.debug_section_inputs)) {
                OutlinedTextField(
                    value = elementSelector,
                    onValueChange = { elementSelector = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.debug_css_selector)) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = elementSearch,
                    onValueChange = { elementSearch = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.debug_search_element_hint)) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall,
                )
            }
        }
        item {
            DebugControlSection(title = stringResource(R.string.debug_floating_pointer)) {
                DebugActionButton(
                    text = stringResource(R.string.debug_start_pointer_overlay),
                    onClick = {
                        onEvaluateJavaScript(
                            PageScripts.startFloatingInspectPointer(
                                ariaLabel = inspectPointerAriaLabel,
                                confirmLabel = confirmLabel,
                                cancelLabel = cancelLabel,
                                noElementLabel = noElementLabel,
                            ),
                        ) {
                            pointerMessage = pointerStartedMessage
                            onInspectPointerStarted()
                        }
                    },
                )
                Text(
                    text = stringResource(R.string.debug_pointer_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                pointerMessage?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        item {
            DebugControlSection(title = stringResource(R.string.debug_section_actions)) {
                DebugActionButton(
                    text = stringResource(R.string.debug_read_source),
                    onClick = { onEvaluateJavaScript(PageScripts.readSource(), onResult) },
                )
                DebugActionButton(
                    text = stringResource(R.string.debug_read_elements),
                    onClick = {
                        onEvaluateJavaScript(
                            PageScripts.readElementsSummary(search = elementSearch, selector = elementSelector),
                            onResult,
                        )
                    },
                )
                DebugActionButton(
                    text = stringResource(R.string.debug_element_details),
                    enabled = elementSelector.isNotBlank(),
                    onClick = { onEvaluateJavaScript(PageScripts.readElementDetails(elementSelector), onResult) },
                )
            }
        }
        if (hasResult) {
            item {
                SectionLabel(text = stringResource(R.string.debug_section_results))
            }
            item {
                Text(
                    text = stringResource(R.string.debug_latest_inspect_result_opens_full_screen),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun JsExecTab(
    jsResults: List<JsExecutionResult>,
    callbackResults: List<String>,
    onEvaluateJavaScript: (script: String, callback: (String) -> Unit) -> Unit,
    onResult: (String) -> Unit,
    onCopy: (String, String) -> Unit,
    onClearDebug: (DebugClearScope) -> Unit,
    showHeader: Boolean,
    modifier: Modifier = Modifier,
) {
    var script by remember { mutableStateOf("") }
    var templatesExpanded by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (showHeader) {
            item {
                DebugPanelHeader(mode = DebugMode.Execute, canClear = false, onClear = {})
            }
        }
        item {
            DebugControlSection(title = stringResource(R.string.debug_section_inputs)) {
                Box {
                    DebugActionButton(
                        text = stringResource(R.string.debug_templates),
                        onClick = { templatesExpanded = true },
                    )
                    DropdownMenu(
                        expanded = templatesExpanded,
                        onDismissRequest = { templatesExpanded = false },
                    ) {
                        PageScripts.templates().forEach { template ->
                            DropdownMenuItem(
                                text = { Text(template.name) },
                                onClick = {
                                    script = template.script
                                    templatesExpanded = false
                                },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = script,
                    onValueChange = { script = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.debug_javascript)) },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Code,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    minLines = 3,
                    textStyle = MaterialTheme.typography.bodySmall,
                )
            }
        }
        item {
            DebugControlSection(title = stringResource(R.string.debug_section_actions)) {
                DebugActionButton(
                    text = stringResource(R.string.debug_execute),
                    enabled = script.isNotBlank(),
                    onClick = {
                        onEvaluateJavaScript(PageScripts.executeUserScript(script), onResult)
                    },
                )
                DebugActionButton(
                    text = stringResource(R.string.debug_clear_js_results),
                    onClick = { onClearDebug(DebugClearScope.JS) },
                )
            }
        }
        if (jsResults.isNotEmpty() || callbackResults.isNotEmpty()) {
            item {
                SectionLabel(text = stringResource(R.string.debug_section_results))
            }
        }
        if (jsResults.isNotEmpty()) {
            items(jsResults.asReversed()) { result ->
                DebugItem(
                    title = if (result.isError) stringResource(R.string.debug_error) else stringResource(R.string.debug_result),
                    subtitle = formatTime(result.timestamp),
                    accentColor = if (result.isError) Red500 else null,
                    details = DebugResultFormatter.formatScriptResult(result.result) +
                        stringResource(R.string.debug_script_label, result.script),
                    onCopy = onCopy,
                )
            }
        }
        resultItems(callbackResults, onCopy, showEmpty = jsResults.isEmpty())
    }
}

@Composable
private fun NetworkTab(
    requests: List<RequestSnapshot>,
    errors: List<PageError>,
    downloads: List<DownloadSnapshot>,
    apiResponses: List<PageError>,
    onClearDebugLogs: () -> Unit,
    onClearDebug: (DebugClearScope) -> Unit,
    onCopy: (String, String) -> Unit,
    onOpenRequest: (RequestSnapshot) -> Unit,
    onOpenApiResponse: (PageError) -> Unit,
    onOpenDownload: (DownloadSnapshot) -> Unit,
    showHeader: Boolean,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    var mainFrameOnly by remember { mutableStateOf(false) }
    val filteredRequests = requests.asReversed().filter { request ->
        (!mainFrameOnly || request.isMainFrame) &&
            (query.isBlank() || request.url.contains(query, ignoreCase = true) ||
                request.host.contains(query, ignoreCase = true) ||
                request.method.contains(query, ignoreCase = true))
    }
    val filteredDownloads = downloads.asReversed().filter { download ->
        query.isBlank() || download.url.contains(query, ignoreCase = true) ||
            download.fileName.orEmpty().contains(query, ignoreCase = true) ||
            download.status.name.contains(query, ignoreCase = true)
    }
    val filteredApiResponses = apiResponses.asReversed().filter { response ->
        query.isBlank() ||
            response.url.orEmpty().contains(query, ignoreCase = true) ||
            response.message.contains(query, ignoreCase = true) ||
            response.statusCode?.toString().orEmpty().contains(query, ignoreCase = true)
    }
    val topHosts = requests
        .groupingBy { it.host.ifBlank { "(no host)" } }
        .eachCount()
        .entries
        .sortedByDescending { it.value }
        .take(6)
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (showHeader) {
            item {
            DebugPanelHeader(
                mode = DebugMode.Network,
                canClear = requests.isNotEmpty() || downloads.isNotEmpty(),
                onClear = onClearDebugLogs,
            )
            }
        }
        item {
            DebugControlSection(title = stringResource(R.string.debug_section_filters)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.debug_search_network)) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall,
                )
                DropdownFilter(
                    label = stringResource(R.string.debug_frame),
                    selectedLabel = if (mainFrameOnly) {
                        stringResource(R.string.debug_main_frame)
                    } else {
                        stringResource(R.string.debug_all)
                    },
                    options = listOf(
                        stringResource(R.string.debug_all) to false,
                        stringResource(R.string.debug_main_frame) to true,
                    ),
                    onSelected = { mainFrameOnly = it },
                )
            }
        }
        if (requests.isNotEmpty()) {
            item {
                DebugItem(
                    title = stringResource(R.string.debug_host_summary),
                    subtitle = stringResource(
                        R.string.debug_host_summary_count,
                        requests.size,
                        requests.count { it.isMainFrame },
                    ),
                    details = topHosts.map { "${it.key}: ${it.value}" },
                    onCopy = onCopy,
                )
            }
        }

        if (filteredRequests.isEmpty() && filteredDownloads.isEmpty() && filteredApiResponses.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.debug_no_requests),
                    modifier = Modifier.padding(vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            if (filteredRequests.isNotEmpty()) {
                item {
                    SectionLabel(text = stringResource(R.string.debug_tab_requests))
                }
                items(filteredRequests) { request ->
                    DebugItem(
                        title = "${request.method} ${request.url}",
                        subtitle = formatTime(request.timestamp),
                        details = listOf(
                            stringResource(R.string.debug_category_label, request.categoryLabel),
                            stringResource(R.string.debug_scheme_label, request.scheme),
                            stringResource(R.string.debug_host_label, request.host),
                            stringResource(R.string.debug_path_label, request.path),
                            stringResource(R.string.debug_main_frame_label, request.isMainFrame),
                            stringResource(R.string.debug_navigation_label, request.navigationId),
                            findMatchingHttpError(request, errors)?.let {
                                stringResource(R.string.debug_response_http_label, it.statusCode ?: 0, it.message)
                            } ?: stringResource(R.string.debug_response_none_captured),
                        ) + request.requestHeaders.map { (key, value) -> "$key: $value" },
                        onCopy = onCopy,
                        onClick = { onOpenRequest(request) },
                    )
                }
            }

            if (filteredApiResponses.isNotEmpty()) {
                item {
                    SectionLabel(text = stringResource(R.string.debug_captured_api_responses))
                }
                items(filteredApiResponses) { response ->
                    DebugItem(
                        title = "${response.responseHeaders["X-Debug-Request-Method"] ?: stringResource(R.string.debug_api)} ${response.url.orEmpty()}",
                        subtitle = formatTime(response.timestamp),
                        details = listOfNotNull(
                            response.statusCode?.let {
                                stringResource(R.string.debug_status_with_message_label, it, response.message)
                            },
                            response.responseHeaders["X-Debug-Capture-Source"]?.let {
                                stringResource(R.string.debug_source_label, it)
                            },
                            response.responseHeaders["Content-Type"]?.let {
                                stringResource(R.string.debug_content_type_label, it)
                            },
                            response.responseHeaders["content-type"]?.let {
                                stringResource(R.string.debug_content_type_label, it)
                            },
                            response.responseHeaders["X-Debug-Duration-Ms"]?.let {
                                stringResource(R.string.debug_duration_ms_label, it)
                            },
                            response.responseHeaders["X-Debug-Body-Truncated"]?.let {
                                stringResource(R.string.debug_body_truncated_label, it)
                            },
                            response.responseHeaders["X-Debug-Skipped-Body"]?.let {
                                stringResource(R.string.debug_body_skipped_label, it)
                            },
                            if (response.responseBody.isNullOrBlank()) {
                                stringResource(R.string.debug_response_body_none_captured)
                            } else {
                                stringResource(R.string.debug_response_body_captured)
                            },
                        ),
                        onCopy = onCopy,
                        onClick = { onOpenApiResponse(response) },
                    )
                }
            }

            if (filteredDownloads.isNotEmpty()) {
                item {
                    SectionLabel(text = stringResource(R.string.debug_tab_downloads))
                }
                items(filteredDownloads) { download ->
                    DebugItem(
                        title = download.url,
                        subtitle = formatTime(download.timestamp),
                        details = listOfNotNull(
                            download.downloadId?.let { stringResource(R.string.debug_download_id_label, it) },
                            download.fileName?.let { stringResource(R.string.debug_file_label, it) },
                            stringResource(R.string.debug_status_label, download.status),
                            download.reason?.let { stringResource(R.string.debug_reason_label, it) },
                            download.localUri?.let { stringResource(R.string.debug_local_uri_label, it) },
                            download.mimeType?.let { stringResource(R.string.debug_mime_label, it) },
                            download.contentDisposition?.let { stringResource(R.string.debug_disposition_label, it) },
                            download.userAgent?.let { stringResource(R.string.debug_user_agent_label, it) },
                            stringResource(R.string.debug_length_label, download.contentLength),
                            download.navigationId.takeIf { it > 0L }?.let {
                                stringResource(R.string.debug_navigation_label, it)
                            },
                        ),
                        onCopy = onCopy,
                        onClick = { onOpenDownload(download) },
                    )
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.resultItems(
    results: List<String>,
    onCopy: (String, String) -> Unit,
    showEmpty: Boolean = true,
) {
    if (results.isEmpty() && showEmpty) {
        item {
            Text(
                text = stringResource(R.string.debug_no_callback_results),
                modifier = Modifier.padding(vertical = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        items(results) { result ->
            DebugItem(
                title = stringResource(R.string.debug_callback_result),
                subtitle = "",
                details = formatCallbackResult(result),
                onCopy = onCopy,
            )
        }
    }
}

@Composable
internal fun FullscreenDebugDialog(
    onClose: () -> Unit,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}

private fun formatCallbackResult(result: String): List<String> =
    if (!result.trimStart().startsWith("{") && result.contains("=")) {
        DebugResultFormatter.formatCookieHeader(result)
    } else {
        DebugResultFormatter.formatScriptResult(result)
    }

@Composable
private fun DebugPanelHeader(
    mode: DebugMode,
    canClear: Boolean,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(mode.labelRes),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        if (canClear) {
            IconButton(onClick = onClear) {
                Icon(
                    Icons.Outlined.DeleteSweep,
                    contentDescription = stringResource(R.string.action_clear),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .heightIn(min = 48.dp),
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun DebugControlSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionLabel(text = title)
        content()
    }
}

@Composable
private fun DebugActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp),
    ) {
        Text(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ButtonRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun <T> DropdownFilter(
    label: String,
    selectedLabel: String,
    options: List<Pair<String, T>>,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.debug_filter_selected_label, label, selectedLabel),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { (optionLabel, optionValue) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = optionLabel,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    onClick = {
                        onSelected(optionValue)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun DebugItem(
    title: String,
    subtitle: String,
    details: List<String>,
    accentColor: Color? = null,
    onCopy: ((String, String) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (onClick != null) {
                    onClick()
                } else {
                    expanded = !expanded
                }
            },
    ) {
        if (accentColor != null) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .background(accentColor)
                    .align(Alignment.Top)
                    .padding(vertical = 2.dp),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = if (accentColor != null) 8.dp else 0.dp, bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = accentColor ?: MaterialTheme.colorScheme.onSurface,
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            details.forEach { detail ->
                Text(
                    text = detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (expanded) Int.MAX_VALUE else 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (onCopy != null) {
                TextButton(
                    onClick = { onCopy("debug-item", listOf(title, subtitle).plus(details).joinToString("\n")) },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Icon(
                        Icons.Outlined.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                    Text(stringResource(R.string.action_copy))
                }
            }
        }
    }
}

internal enum class DebugMode(@get:StringRes val labelRes: Int, val icon: ImageVector) {
    Overview(R.string.debug_group_overview, Icons.Outlined.Info),
    Timeline(R.string.debug_group_timeline, Icons.Outlined.Timeline),
    Logs(R.string.debug_group_logs, Icons.Outlined.Terminal),
    Page(R.string.debug_group_page, Icons.Outlined.Description),
    Storage(R.string.debug_group_storage, Icons.Outlined.Storage),
    Inspect(R.string.debug_group_inspect, Icons.Outlined.AccountTree),
    Network(R.string.debug_group_network, Icons.Outlined.Http),
    Execute(R.string.debug_group_execute, Icons.Outlined.Code),
}

@Composable
internal fun DebugMode.label(state: DebugState): String {
    val base = stringResource(labelRes)
    val count = when (this) {
        DebugMode.Overview -> state.timeline.count { it.severity != DebugSeverity.INFO }
        DebugMode.Timeline -> state.timeline.size
        DebugMode.Logs -> state.consoleLogs.size + state.errors.size
        DebugMode.Page -> if (state.page.url == null) 0 else 1
        DebugMode.Storage -> 0
        DebugMode.Inspect -> 0
        DebugMode.Network -> state.requests.size + state.downloads.size
        DebugMode.Execute -> state.jsResults.size
    }
    return if (count > 0) "$base $count" else base
}

private fun formatTime(timestamp: Long): String {
    if (timestamp <= 0L) return "-"
    return DateFormat.getDateTimeInstance().format(Date(timestamp))
}

@Composable
private fun logLevelColor(level: String): Color? {
    return when (level.uppercase()) {
        "ERROR" -> Red500
        "WARN", "WARNING" -> Color(0xFFF59E0B)
        else -> null
    }
}

@Composable
private fun DebugSeverity.color(): Color? =
    when (this) {
        DebugSeverity.ERROR -> Red500
        DebugSeverity.WARNING -> Color(0xFFF59E0B)
        DebugSeverity.INFO -> null
    }

private fun DebugState.hasDebugEntries(): Boolean =
    consoleLogs.isNotEmpty() ||
        errors.isNotEmpty() ||
        requests.isNotEmpty() ||
        downloads.isNotEmpty() ||
        jsResults.isNotEmpty() ||
        userFlows.isNotEmpty() ||
        timeline.isNotEmpty()

private fun DebugEvent.copyText(): String =
    buildString {
        append("[")
        append(formatTime(timestamp))
        append("] ")
        append(type)
        append(" ")
        append(severity)
        append(": ")
        append(summary)
        details.forEach { detail ->
            append("\n  ")
            append(detail)
        }
    }

private fun DebugEvent.diagnosticHint(): List<String> =
    diagnosticHint(summary, details)

private fun PageError.diagnosticHint(): List<String> =
    diagnosticHint("$type $message", listOfNotNull(url))

private fun diagnosticHint(summary: String, details: List<String>): List<String> {
    val text = (summary + " " + details.joinToString(" ")).lowercase()
    val hint = when {
        "ssl" in text -> "Next check: verify certificate chain, Android trust store, and mixed-content policy."
        "mixed content" in text -> "Next check: current config blocks mixed content; try HTTPS resources or Mixed content = ALLOW."
        "404" in text -> "Likely cause: missing resource or route mismatch."
        "500" in text || "server error" in text -> "Likely cause: upstream server/API failure."
        "host lookup" in text || "err_name" in text -> "Next check: DNS/network connectivity and URL spelling."
        "javascript: false" in text -> "Next check: JavaScript is disabled for this test config."
        "dom storage: false" in text -> "Next check: DOM storage is disabled, localStorage/sessionStorage may fail."
        "cookie" in text && "false" in text -> "Next check: cookie policy may block auth/session state."
        "permission denied" in text || "denied by policy" in text -> "Next check: WebTestConfig permission policy and Android runtime permission."
        else -> null
    }
    return listOfNotNull(hint)
}

private fun DebugState.toDebugBundle(config: WebTestConfig, sourceType: SourceType): String =
    buildString {
        appendLine("WebViewTest Debug Bundle")
        appendLine("Generated: ${formatTime(System.currentTimeMillis())}")
        appendLine("Source type: $sourceType")
        appendLine("URL: ${page.url.orEmpty()}")
        appendLine("Title: ${page.title}")
        appendLine("Status: ${page.status}")
        appendLine("Navigation: ${page.navigationId}")
        appendLine("Config: $config")
        appendLine()
        appendLine("Timeline:")
        timeline.forEach { event ->
            appendLine(event.copyText())
        }
        appendLine()
        appendLine("Console:")
        consoleLogs.forEach { log ->
            appendLine("[${formatTime(log.timestamp)}] ${log.level} ${log.sourceId}:${log.lineNumber} ${log.message}")
        }
        appendLine()
        appendLine("Errors:")
        errors.forEach { error ->
            appendLine("[${formatTime(error.timestamp)}] ${error.type} ${error.statusCode ?: error.code ?: ""} ${error.url.orEmpty()} ${error.message}")
        }
        appendLine()
        appendLine("Requests:")
        requests.forEach { request ->
            appendLine("[${formatTime(request.timestamp)}] ${request.method} ${request.url} ${request.categoryLabel}")
        }
        appendLine()
        appendLine("Downloads:")
        downloads.forEach { download ->
            appendLine("[${formatTime(download.timestamp)}] ${download.status} ${download.fileName.orEmpty()} ${download.url}")
        }
    }
