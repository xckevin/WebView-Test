package com.xckevin.android.app.webview.test.ui.workbench

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Http
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xckevin.android.app.webview.test.R
import com.xckevin.android.app.webview.test.debug.ConsoleLog
import com.xckevin.android.app.webview.test.debug.DebugState
import com.xckevin.android.app.webview.test.debug.DownloadSnapshot
import com.xckevin.android.app.webview.test.debug.JsExecutionResult
import com.xckevin.android.app.webview.test.debug.PageError
import com.xckevin.android.app.webview.test.debug.PageScripts
import com.xckevin.android.app.webview.test.debug.RequestSnapshot
import com.xckevin.android.app.webview.test.ui.theme.Red500
import java.text.DateFormat
import java.util.Date

@Composable
fun DebugPanel(
    debugState: DebugState,
    onClearDebugLogs: () -> Unit,
    onEvaluateJavaScript: (script: String, callback: (String) -> Unit) -> Unit,
    onReadCookies: () -> Unit,
    onClearCookies: () -> Unit,
    onClearWebViewCache: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedMode by remember { mutableStateOf(DebugMode.Logs) }
    val callbackResults = remember { mutableStateListOf<String>() }
    val captureResult: (String) -> Unit = { result ->
        callbackResults.add(0, result)
        while (callbackResults.size > 20) {
            callbackResults.removeAt(callbackResults.lastIndex)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        PrimaryScrollableTabRow(
            selectedTabIndex = DebugMode.entries.indexOf(selectedMode),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            edgePadding = 8.dp,
        ) {
            DebugMode.entries.forEach { mode ->
                Tab(
                    selected = selectedMode == mode,
                    onClick = { selectedMode = mode },
                    text = {
                        Text(
                            text = stringResource(mode.labelRes),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                    icon = {
                        Icon(
                            mode.icon,
                            contentDescription = null,
                        )
                    },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        when (selectedMode) {
            DebugMode.Logs -> LogsTab(
                logs = debugState.consoleLogs,
                errors = debugState.errors,
                onClearDebugLogs = onClearDebugLogs,
                modifier = Modifier.weight(1f),
            )

            DebugMode.Page -> PageTab(
                debugState = debugState,
                onClearWebViewCache = onClearWebViewCache,
                modifier = Modifier.weight(1f),
            )

            DebugMode.Storage -> StorageTab(
                onReadCookies = onReadCookies,
                onClearCookies = onClearCookies,
                onEvaluateJavaScript = onEvaluateJavaScript,
                onResult = captureResult,
                results = callbackResults,
                modifier = Modifier.weight(1f),
            )

            DebugMode.Inspect -> InspectTab(
                onEvaluateJavaScript = onEvaluateJavaScript,
                onResult = captureResult,
                results = callbackResults,
                modifier = Modifier.weight(1f),
            )

            DebugMode.Network -> NetworkTab(
                requests = debugState.requests,
                downloads = debugState.downloads,
                onClearDebugLogs = onClearDebugLogs,
                modifier = Modifier.weight(1f),
            )

            DebugMode.Execute -> JsExecTab(
                jsResults = debugState.jsResults,
                callbackResults = callbackResults,
                onEvaluateJavaScript = onEvaluateJavaScript,
                onResult = captureResult,
                modifier = Modifier.weight(1f),
            )

        }
    }
}

@Composable
private fun LogsTab(
    logs: List<ConsoleLog>,
    errors: List<PageError>,
    onClearDebugLogs: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            HeaderRow(
                title = stringResource(R.string.debug_group_logs),
                canClear = logs.isNotEmpty() || errors.isNotEmpty(),
                onClearDebugLogs = onClearDebugLogs,
            )
        }

        if (logs.isEmpty() && errors.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.debug_no_console_logs),
                    modifier = Modifier.padding(vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            if (logs.isNotEmpty()) {
                item {
                    SectionLabel(text = stringResource(R.string.debug_tab_console))
                }
                items(logs.asReversed()) { log ->
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
                    )
                }
            }

            if (errors.isNotEmpty()) {
                item {
                    SectionLabel(text = stringResource(R.string.debug_tab_errors))
                }
                items(errors.asReversed()) { error ->
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
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun PageTab(
    debugState: DebugState,
    onClearWebViewCache: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val page = debugState.page
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            HeaderRow(title = stringResource(R.string.debug_group_page), canClear = false, onClearDebugLogs = {})
        }
        item {
            ButtonRow {
                OutlinedButton(onClick = onClearWebViewCache) {
                    Text(stringResource(R.string.debug_clear_webview_cache))
                }
            }
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
            )
        }
    }
}

@Composable
private fun StorageTab(
    onReadCookies: () -> Unit,
    onClearCookies: () -> Unit,
    onEvaluateJavaScript: (script: String, callback: (String) -> Unit) -> Unit,
    onResult: (String) -> Unit,
    results: List<String>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            HeaderRow(title = stringResource(R.string.debug_group_storage), canClear = false, onClearDebugLogs = {})
        }
        item {
            SectionLabel(text = stringResource(R.string.debug_tab_cookies))
        }
        item {
            ButtonRow {
                FilledTonalButton(onClick = onReadCookies) {
                    Text(stringResource(R.string.debug_read_cookies))
                }
                OutlinedButton(onClick = onClearCookies) {
                    Text(stringResource(R.string.debug_clear_cookies))
                }
            }
        }
        item {
            SectionLabel(text = stringResource(R.string.debug_tab_storage))
        }
        item {
            ButtonRow {
                FilledTonalButton(onClick = { onEvaluateJavaScript(PageScripts.readLocalStorage(), onResult) }) {
                    Text(stringResource(R.string.debug_read_local))
                }
                OutlinedButton(onClick = { onEvaluateJavaScript(PageScripts.clearLocalStorage(), onResult) }) {
                    Text(stringResource(R.string.debug_clear_local))
                }
            }
        }
        item {
            ButtonRow {
                FilledTonalButton(onClick = { onEvaluateJavaScript(PageScripts.readSessionStorage(), onResult) }) {
                    Text(stringResource(R.string.debug_read_session))
                }
                OutlinedButton(onClick = { onEvaluateJavaScript(PageScripts.clearSessionStorage(), onResult) }) {
                    Text(stringResource(R.string.debug_clear_session))
                }
            }
        }
        resultItems(results)
    }
}

@Composable
private fun InspectTab(
    onEvaluateJavaScript: (script: String, callback: (String) -> Unit) -> Unit,
    onResult: (String) -> Unit,
    results: List<String>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            HeaderRow(title = stringResource(R.string.debug_group_inspect), canClear = false, onClearDebugLogs = {})
        }
        item {
            ButtonRow {
                FilledTonalButton(onClick = { onEvaluateJavaScript(PageScripts.readSource(), onResult) }) {
                    Text(stringResource(R.string.debug_read_source))
                }
                OutlinedButton(onClick = { onEvaluateJavaScript(PageScripts.readElementsSummary(), onResult) }) {
                    Text(stringResource(R.string.debug_read_elements))
                }
            }
        }
        resultItems(results)
    }
}

@Composable
private fun JsExecTab(
    jsResults: List<JsExecutionResult>,
    callbackResults: List<String>,
    onEvaluateJavaScript: (script: String, callback: (String) -> Unit) -> Unit,
    onResult: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var script by remember { mutableStateOf("") }
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            HeaderRow(title = stringResource(R.string.debug_group_execute), canClear = false, onClearDebugLogs = {})
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                Button(
                    enabled = script.isNotBlank(),
                    onClick = {
                        onEvaluateJavaScript(PageScripts.executeUserScript(script), onResult)
                    },
                ) {
                    Text(stringResource(R.string.debug_execute))
                }
            }
        }
        if (jsResults.isNotEmpty()) {
            items(jsResults.asReversed()) { result ->
                DebugItem(
                    title = if (result.isError) stringResource(R.string.debug_error) else stringResource(R.string.debug_result),
                    subtitle = formatTime(result.timestamp),
                    accentColor = if (result.isError) Red500 else null,
                    details = listOf(result.result, stringResource(R.string.debug_script_label, result.script)),
                )
            }
        }
        resultItems(callbackResults)
    }
}

@Composable
private fun NetworkTab(
    requests: List<RequestSnapshot>,
    downloads: List<DownloadSnapshot>,
    onClearDebugLogs: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            HeaderRow(
                title = stringResource(R.string.debug_group_network),
                canClear = requests.isNotEmpty() || downloads.isNotEmpty(),
                onClearDebugLogs = onClearDebugLogs,
            )
        }

        if (requests.isEmpty() && downloads.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.debug_no_requests),
                    modifier = Modifier.padding(vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            if (requests.isNotEmpty()) {
                item {
                    SectionLabel(text = stringResource(R.string.debug_tab_requests))
                }
                items(requests.asReversed()) { request ->
                    DebugItem(
                        title = request.url,
                        subtitle = formatTime(request.timestamp),
                        details = listOf(
                            stringResource(R.string.debug_main_frame_label, request.isMainFrame),
                            stringResource(R.string.debug_navigation_label, request.navigationId),
                        ),
                    )
                }
            }

            if (downloads.isNotEmpty()) {
                item {
                    SectionLabel(text = stringResource(R.string.debug_tab_downloads))
                }
                items(downloads.asReversed()) { download ->
                    DebugItem(
                        title = download.url,
                        subtitle = formatTime(download.timestamp),
                        details = listOfNotNull(
                            download.mimeType?.let { stringResource(R.string.debug_mime_label, it) },
                            download.contentDisposition?.let { stringResource(R.string.debug_disposition_label, it) },
                            download.userAgent?.let { stringResource(R.string.debug_user_agent_label, it) },
                            stringResource(R.string.debug_length_label, download.contentLength),
                            download.navigationId.takeIf { it > 0L }?.let {
                                stringResource(R.string.debug_navigation_label, it)
                            },
                        ),
                    )
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.resultItems(results: List<String>) {
    if (results.isEmpty()) {
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
                details = listOf(result),
            )
        }
    }
}

@Composable
private fun HeaderRow(
    title: String,
    canClear: Boolean,
    onClearDebugLogs: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        if (canClear) {
            IconButton(onClick = onClearDebugLogs) {
                Icon(
                    Icons.Outlined.DeleteSweep,
                    contentDescription = stringResource(R.string.action_clear),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
private fun ButtonRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun DebugItem(
    title: String,
    subtitle: String,
    details: List<String>,
    accentColor: Color? = null,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
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
                maxLines = 3,
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
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private enum class DebugMode(@get:StringRes val labelRes: Int, val icon: ImageVector) {
    Logs(R.string.debug_group_logs, Icons.Outlined.Terminal),
    Page(R.string.debug_group_page, Icons.Outlined.Description),
    Storage(R.string.debug_group_storage, Icons.Outlined.Storage),
    Inspect(R.string.debug_group_inspect, Icons.Outlined.AccountTree),
    Network(R.string.debug_group_network, Icons.Outlined.Http),
    Execute(R.string.debug_group_execute, Icons.Outlined.Code),
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
