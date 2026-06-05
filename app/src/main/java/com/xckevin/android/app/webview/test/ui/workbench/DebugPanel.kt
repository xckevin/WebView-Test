package com.xckevin.android.app.webview.test.ui.workbench

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xckevin.android.app.webview.test.debug.ConsoleLog
import com.xckevin.android.app.webview.test.debug.DebugState
import com.xckevin.android.app.webview.test.debug.DownloadSnapshot
import com.xckevin.android.app.webview.test.debug.JsExecutionResult
import com.xckevin.android.app.webview.test.debug.PageError
import com.xckevin.android.app.webview.test.debug.PageScripts
import com.xckevin.android.app.webview.test.debug.RequestSnapshot
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
    var selectedTab by remember { mutableStateOf(DebugTab.Console) }
    val callbackResults = remember { mutableStateListOf<String>() }
    val captureResult: (String) -> Unit = { result ->
        callbackResults.add(0, result)
        while (callbackResults.size > 20) {
            callbackResults.removeAt(callbackResults.lastIndex)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        PrimaryScrollableTabRow(selectedTabIndex = DebugTab.entries.indexOf(selectedTab)) {
            DebugTab.entries.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = {
                        Text(
                            text = tab.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
            }
        }

        when (selectedTab) {
            DebugTab.Console -> ConsoleTab(
                logs = debugState.consoleLogs,
                onClearDebugLogs = onClearDebugLogs,
                modifier = Modifier.weight(1f),
            )

            DebugTab.Errors -> ErrorsTab(
                errors = debugState.errors,
                onClearDebugLogs = onClearDebugLogs,
                modifier = Modifier.weight(1f),
            )

            DebugTab.Page -> PageTab(
                debugState = debugState,
                onClearWebViewCache = onClearWebViewCache,
                modifier = Modifier.weight(1f),
            )

            DebugTab.Cookies -> CookiesTab(
                onReadCookies = onReadCookies,
                onClearCookies = onClearCookies,
                modifier = Modifier.weight(1f),
            )

            DebugTab.Storage -> StorageTab(
                onEvaluateJavaScript = onEvaluateJavaScript,
                onResult = captureResult,
                results = callbackResults,
                modifier = Modifier.weight(1f),
            )

            DebugTab.Source -> ScriptResultTab(
                title = "Source",
                actionLabel = "Read source",
                script = PageScripts.readSource(),
                onEvaluateJavaScript = onEvaluateJavaScript,
                onResult = captureResult,
                results = callbackResults,
                modifier = Modifier.weight(1f),
            )

            DebugTab.Elements -> ScriptResultTab(
                title = "Elements",
                actionLabel = "Read elements",
                script = PageScripts.readElementsSummary(),
                onEvaluateJavaScript = onEvaluateJavaScript,
                onResult = captureResult,
                results = callbackResults,
                modifier = Modifier.weight(1f),
            )

            DebugTab.JsExec -> JsExecTab(
                jsResults = debugState.jsResults,
                callbackResults = callbackResults,
                onEvaluateJavaScript = onEvaluateJavaScript,
                onResult = captureResult,
                modifier = Modifier.weight(1f),
            )

            DebugTab.Requests -> RequestsTab(
                requests = debugState.requests,
                onClearDebugLogs = onClearDebugLogs,
                modifier = Modifier.weight(1f),
            )

            DebugTab.Downloads -> DownloadsTab(
                downloads = debugState.downloads,
                onClearDebugLogs = onClearDebugLogs,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ConsoleTab(
    logs: List<ConsoleLog>,
    onClearDebugLogs: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DebugList(
        title = "Console",
        isEmpty = logs.isEmpty(),
        emptyText = "No console logs",
        canClear = logs.isNotEmpty(),
        onClearDebugLogs = onClearDebugLogs,
        modifier = modifier,
    ) {
        items(logs.asReversed()) { log ->
            DebugItem(
                title = "${log.level}: ${log.message}",
                subtitle = formatTime(log.timestamp),
                details = listOfNotNull(
                    log.sourceId.takeIf { it.isNotBlank() }?.let { "Source: $it" },
                    log.lineNumber.takeIf { it > 0 }?.let { "Line: $it" },
                    log.navigationId.takeIf { it > 0L }?.let { "Navigation: $it" },
                ),
            )
        }
    }
}

@Composable
private fun ErrorsTab(
    errors: List<PageError>,
    onClearDebugLogs: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DebugList(
        title = "Errors",
        isEmpty = errors.isEmpty(),
        emptyText = "No page errors",
        canClear = errors.isNotEmpty(),
        onClearDebugLogs = onClearDebugLogs,
        modifier = modifier,
    ) {
        items(errors.asReversed()) { error ->
            DebugItem(
                title = "${error.type}: ${error.message}",
                subtitle = formatTime(error.timestamp),
                details = listOfNotNull(
                    error.url?.let { "URL: $it" },
                    error.code?.let { "Code: $it" },
                    error.statusCode?.let { "Status: $it" },
                    "Main frame: ${error.isMainFrame}",
                    error.navigationId.takeIf { it > 0L }?.let { "Navigation: $it" },
                ),
            )
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
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            HeaderRow(title = "Page", canClear = false, onClearDebugLogs = {})
        }
        item {
            ButtonRow {
                OutlinedButton(onClick = onClearWebViewCache) {
                    Text("Clear WebView cache")
                }
            }
        }
        item {
            DebugItem(
                title = page.url ?: "No page loaded",
                subtitle = "Status: ${page.status}",
                details = listOf(
                    "Title: ${page.title}",
                    "Progress: ${page.progress}",
                    "Navigation: ${page.navigationId}",
                    "Updated: ${formatTime(page.timestamp)}",
                ),
            )
        }
    }
}

@Composable
private fun CookiesTab(
    onReadCookies: () -> Unit,
    onClearCookies: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            HeaderRow(title = "Cookies", canClear = false, onClearDebugLogs = {})
        }
        item {
            ButtonRow {
                Button(onClick = onReadCookies) {
                    Text("Read cookies")
                }
                OutlinedButton(onClick = onClearCookies) {
                    Text("Clear cookies")
                }
            }
        }
    }
}

@Composable
private fun StorageTab(
    onEvaluateJavaScript: (script: String, callback: (String) -> Unit) -> Unit,
    onResult: (String) -> Unit,
    results: List<String>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            HeaderRow(title = "Storage", canClear = false, onClearDebugLogs = {})
        }
        item {
            ButtonRow {
                Button(onClick = { onEvaluateJavaScript(PageScripts.readLocalStorage(), onResult) }) {
                    Text("Read local")
                }
                OutlinedButton(onClick = { onEvaluateJavaScript(PageScripts.clearLocalStorage(), onResult) }) {
                    Text("Clear local")
                }
            }
        }
        item {
            ButtonRow {
                Button(onClick = { onEvaluateJavaScript(PageScripts.readSessionStorage(), onResult) }) {
                    Text("Read session")
                }
                OutlinedButton(onClick = { onEvaluateJavaScript(PageScripts.clearSessionStorage(), onResult) }) {
                    Text("Clear session")
                }
            }
        }
        resultItems(results)
    }
}

@Composable
private fun ScriptResultTab(
    title: String,
    actionLabel: String,
    script: String,
    onEvaluateJavaScript: (script: String, callback: (String) -> Unit) -> Unit,
    onResult: (String) -> Unit,
    results: List<String>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            HeaderRow(title = title, canClear = false, onClearDebugLogs = {})
        }
        item {
            ButtonRow {
                Button(onClick = { onEvaluateJavaScript(script, onResult) }) {
                    Text(actionLabel)
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
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            HeaderRow(title = "JS Exec", canClear = false, onClearDebugLogs = {})
        }
        item {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = script,
                    onValueChange = { script = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("JavaScript") },
                    minLines = 4,
                )
                Button(
                    enabled = script.isNotBlank(),
                    onClick = {
                        onEvaluateJavaScript(PageScripts.executeUserScript(script), onResult)
                    },
                ) {
                    Text("Execute")
                }
            }
        }
        if (jsResults.isNotEmpty()) {
            items(jsResults.asReversed()) { result ->
                DebugItem(
                    title = if (result.isError) "Error" else "Result",
                    subtitle = formatTime(result.timestamp),
                    details = listOf(result.result, "Script: ${result.script}"),
                )
            }
        }
        resultItems(callbackResults)
    }
}

@Composable
private fun RequestsTab(
    requests: List<RequestSnapshot>,
    onClearDebugLogs: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DebugList(
        title = "Requests",
        isEmpty = requests.isEmpty(),
        emptyText = "No requests captured",
        canClear = requests.isNotEmpty(),
        onClearDebugLogs = onClearDebugLogs,
        modifier = modifier,
    ) {
        items(requests.asReversed()) { request ->
            DebugItem(
                title = request.url,
                subtitle = formatTime(request.timestamp),
                details = listOf(
                    "Main frame: ${request.isMainFrame}",
                    "Navigation: ${request.navigationId}",
                ),
            )
        }
    }
}

@Composable
private fun DownloadsTab(
    downloads: List<DownloadSnapshot>,
    onClearDebugLogs: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DebugList(
        title = "Downloads",
        isEmpty = downloads.isEmpty(),
        emptyText = "No downloads requested",
        canClear = downloads.isNotEmpty(),
        onClearDebugLogs = onClearDebugLogs,
        modifier = modifier,
    ) {
        items(downloads.asReversed()) { download ->
            DebugItem(
                title = download.url,
                subtitle = formatTime(download.timestamp),
                details = listOfNotNull(
                    download.mimeType?.let { "MIME: $it" },
                    download.contentDisposition?.let { "Disposition: $it" },
                    download.userAgent?.let { "User agent: $it" },
                    "Length: ${download.contentLength}",
                    download.navigationId.takeIf { it > 0L }?.let { "Navigation: $it" },
                ),
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.resultItems(results: List<String>) {
    if (results.isEmpty()) {
        item {
            Text(
                text = "No callback results",
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    } else {
        items(results) { result ->
            DebugItem(
                title = "Callback result",
                subtitle = "",
                details = listOf(result),
            )
        }
    }
}

@Composable
private fun DebugList(
    title: String,
    isEmpty: Boolean,
    emptyText: String,
    canClear: Boolean,
    onClearDebugLogs: () -> Unit,
    modifier: Modifier = Modifier,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            HeaderRow(
                title = title,
                canClear = canClear,
                onClearDebugLogs = onClearDebugLogs,
            )
        }

        if (isEmpty) {
            item {
                Text(
                    text = emptyText,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            content()
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        OutlinedButton(
            enabled = canClear,
            onClick = onClearDebugLogs,
        ) {
            Text("Clear")
        }
    }
}

@Composable
private fun ButtonRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
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
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        details.forEach { detail ->
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
    }
}

private enum class DebugTab(val label: String) {
    Console("Console"),
    Errors("Errors"),
    Page("Page"),
    Cookies("Cookies"),
    Storage("Storage"),
    Source("Source"),
    Elements("Elements"),
    JsExec("JS Exec"),
    Requests("Requests"),
    Downloads("Downloads"),
}

private fun formatTime(timestamp: Long): String {
    if (timestamp <= 0L) return "-"
    return DateFormat.getDateTimeInstance().format(Date(timestamp))
}
