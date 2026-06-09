package com.xckevin.android.app.webview.test.ui.workbench

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Http
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xckevin.android.app.webview.test.debug.DownloadSnapshot
import com.xckevin.android.app.webview.test.debug.DebugNetworkContentFormatter
import com.xckevin.android.app.webview.test.debug.PageError
import com.xckevin.android.app.webview.test.debug.RequestSnapshot
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugNetworkDetailScreen(
    request: RequestSnapshot?,
    error: PageError?,
    download: DownloadSnapshot?,
    onClose: () -> Unit,
    onCopy: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sections = buildNetworkDetailSections(
        request = request,
        error = error,
        download = download,
    )
    val title = when {
        download != null -> "Download detail"
        request != null -> "Request detail"
        error != null -> "Response detail"
        else -> "Network detail"
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        DetailSubtitle(request = request, error = error, download = download)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            onCopy(
                                "network-detail",
                                sections.toCopyText(),
                            )
                        },
                        enabled = sections.isNotEmpty(),
                    ) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy")
                    }
                },
            )
        },
    ) { innerPadding ->
        if (sections.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "No network item selected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                sections.forEach { section ->
                    item {
                        SectionHeader(section)
                    }
                    items(section.rows) { row ->
                        DetailRow(row)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailSubtitle(
    request: RequestSnapshot?,
    error: PageError?,
    download: DownloadSnapshot?,
) {
    val subtitle = when {
        download != null -> listOfNotNull(download.status.name, download.fileName).joinToString(" · ")
        request != null -> "${request.method} ${request.host.ifBlank { request.scheme.ifBlank { "request" } }}"
        error != null -> listOfNotNull(error.statusCode?.let { "HTTP $it" }, error.url).joinToString(" · ")
        else -> ""
    }
    if (subtitle.isNotBlank()) {
        Text(
            text = subtitle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionHeader(section: NetworkDetailSection) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (section.kind == NetworkDetailSectionKind.Download) {
                Icons.Outlined.Download
            } else {
                Icons.Outlined.Http
            },
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = section.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun DetailRow(row: NetworkDetailRow) {
    ListItem(
        headlineContent = {
            Text(
                text = row.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        supportingContent = {
            Text(
                text = row.value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
    )
    HorizontalDivider()
}

data class NetworkDetailSection(
    val title: String,
    val kind: NetworkDetailSectionKind,
    val rows: List<NetworkDetailRow>,
)

enum class NetworkDetailSectionKind {
    Url,
    RequestHeaders,
    RequestBody,
    ResponseHeaders,
    ResponseBody,
    MediaPreview,
    Meta,
    Request,
    Response,
    Download,
}

data class NetworkDetailRow(
    val label: String,
    val value: String,
)

fun buildNetworkDetailSections(
    request: RequestSnapshot?,
    error: PageError?,
    download: DownloadSnapshot?,
): List<NetworkDetailSection> {
    val sections = mutableListOf<NetworkDetailSection>()
    val matchedResponse = error?.takeIf { response ->
        request == null || response.matchesHttpResponse(request)
    }

    if (request != null || matchedResponse != null || download != null) {
        sections += NetworkDetailSection(
            title = "URL",
            kind = NetworkDetailSectionKind.Url,
            rows = urlRows(request = request, error = matchedResponse, download = download),
        )
        sections += NetworkDetailSection(
            title = "Request headers",
            kind = NetworkDetailSectionKind.RequestHeaders,
            rows = request?.requestHeaders.toHeaderRows(emptyLabel = "Headers"),
        )
        sections += NetworkDetailSection(
            title = "Request body",
            kind = NetworkDetailSectionKind.RequestBody,
            rows = bodyRows(
                body = request?.requestBody,
                headers = request?.requestHeaders,
                url = request?.url,
            ),
        )
        sections += NetworkDetailSection(
            title = "Response headers",
            kind = NetworkDetailSectionKind.ResponseHeaders,
            rows = matchedResponse?.responseHeaders.toHeaderRows(emptyLabel = "Headers"),
        )
        sections += NetworkDetailSection(
            title = "Response body",
            kind = NetworkDetailSectionKind.ResponseBody,
            rows = bodyRows(
                body = matchedResponse?.responseBody,
                headers = matchedResponse?.responseHeaders,
                url = matchedResponse?.url ?: request?.url,
            ),
        )
        sections += NetworkDetailSection(
            title = "Meta",
            kind = NetworkDetailSectionKind.Meta,
            rows = metaRows(request = request, error = matchedResponse, download = download),
        )
        mediaPreviewRows(request = request, error = matchedResponse, download = download).takeIf { it.isNotEmpty() }
            ?.let { rows ->
                sections += NetworkDetailSection(
                    title = "Media preview",
                    kind = NetworkDetailSectionKind.MediaPreview,
                    rows = rows,
                )
            }
    }

    if (download != null) {
        sections += NetworkDetailSection(
            title = "Download",
            kind = NetworkDetailSectionKind.Download,
            rows = downloadRows(download),
        )
    }
    return sections
}

fun findMatchingHttpError(
    request: RequestSnapshot,
    errors: List<PageError>,
): PageError? =
    errors
        .asReversed()
        .firstOrNull { error -> error.matchesHttpResponse(request) }

fun List<NetworkDetailSection>.toCopyText(): String =
    joinToString(separator = "\n\n") { section ->
        buildString {
            appendLine(section.title)
            section.rows.forEach { row ->
                append(row.label)
                append(": ")
                appendLine(row.value)
            }
        }.trimEnd()
    }

private fun urlRows(
    request: RequestSnapshot?,
    error: PageError?,
    download: DownloadSnapshot?,
): List<NetworkDetailRow> =
    listOfNotNull(
        request?.url?.let { NetworkDetailRow("Request URL", it) },
        error?.url?.let { NetworkDetailRow("Response URL", it) },
        download?.url?.let { NetworkDetailRow("Download URL", it) },
    ).ifEmpty {
        listOf(NetworkDetailRow("URL", "-"))
    }

private fun metaRows(
    request: RequestSnapshot?,
    error: PageError?,
    download: DownloadSnapshot?,
): List<NetworkDetailRow> {
    val rows = mutableListOf<NetworkDetailRow>()
    if (request != null) {
        rows += listOf(
            NetworkDetailRow("Method", request.method),
            NetworkDetailRow("Scheme", request.scheme),
            NetworkDetailRow("Host", request.host),
            NetworkDetailRow("Path", request.path),
            NetworkDetailRow("Main frame", request.isMainFrame.toString()),
            NetworkDetailRow("Category", request.categoryLabel),
            NetworkDetailRow("Navigation ID", request.navigationId.toString()),
            NetworkDetailRow("Request timestamp", formatNetworkDetailTime(request.timestamp)),
        )
    }
    if (error != null) {
        rows += listOfNotNull(
            error.statusCode?.let { NetworkDetailRow("Status code", it.toString()) },
            NetworkDetailRow("Reason/message", error.message),
            NetworkDetailRow("Response main frame", error.isMainFrame.toString()),
            NetworkDetailRow("Response navigation ID", error.navigationId.toString()),
            NetworkDetailRow("Response timestamp", formatNetworkDetailTime(error.timestamp)),
        )
    }
    if (download != null) {
        rows += downloadRows(download)
    }
    return rows.ifEmpty {
        listOf(NetworkDetailRow("Meta", "-"))
    }
}

private fun requestRows(request: RequestSnapshot): List<NetworkDetailRow> =
    listOf(
        NetworkDetailRow("URL", request.url),
        NetworkDetailRow("Method", request.method),
        NetworkDetailRow("Scheme", request.scheme),
        NetworkDetailRow("Host", request.host),
        NetworkDetailRow("Path", request.path),
        NetworkDetailRow("Main frame", request.isMainFrame.toString()),
        NetworkDetailRow("Category", request.categoryLabel),
        NetworkDetailRow("Navigation ID", request.navigationId.toString()),
        NetworkDetailRow("Timestamp", formatNetworkDetailTime(request.timestamp)),
    ) + headerRows("Request header", request.requestHeaders)

private fun responseRows(error: PageError): List<NetworkDetailRow> =
    listOfNotNull(
        error.url?.let { NetworkDetailRow("URL", it) },
        error.statusCode?.let { NetworkDetailRow("Status code", it.toString()) },
        NetworkDetailRow("Reason/message", error.message),
        NetworkDetailRow("Main frame", error.isMainFrame.toString()),
        NetworkDetailRow("Navigation ID", error.navigationId.toString()),
        NetworkDetailRow("Timestamp", formatNetworkDetailTime(error.timestamp)),
    ) + headerRows("Response header", error.responseHeaders)

private fun downloadRows(download: DownloadSnapshot): List<NetworkDetailRow> =
    listOfNotNull(
        NetworkDetailRow("URL", download.url),
        download.userAgent?.let { NetworkDetailRow("User agent", it) },
        download.contentDisposition?.let { NetworkDetailRow("Content disposition", it) },
        download.mimeType?.let { NetworkDetailRow("MIME type", it) },
        NetworkDetailRow("Content length", download.contentLength.toString()),
        download.downloadId?.let { NetworkDetailRow("Download ID", it.toString()) },
        download.fileName?.let { NetworkDetailRow("File name", it) },
        NetworkDetailRow("Status", download.status.name),
        download.reason?.let { NetworkDetailRow("Reason", it) },
        download.localUri?.let { NetworkDetailRow("Local URI", it) },
        NetworkDetailRow("Navigation ID", download.navigationId.toString()),
        NetworkDetailRow("Timestamp", formatNetworkDetailTime(download.timestamp)),
        NetworkDetailRow("Updated at", formatNetworkDetailTime(download.updatedAt)),
    )

private fun headerRows(prefix: String, headers: Map<String, String>): List<NetworkDetailRow> =
    if (headers.isEmpty()) {
        listOf(NetworkDetailRow("${prefix}s", "(none)"))
    } else {
        headers.toSortedMap(String.CASE_INSENSITIVE_ORDER)
            .map { (key, value) -> NetworkDetailRow("$prefix: $key", value) }
    }

private fun Map<String, String>?.toHeaderRows(emptyLabel: String): List<NetworkDetailRow> =
    if (isNullOrEmpty()) {
        listOf(NetworkDetailRow(emptyLabel, "(none)"))
    } else {
        toSortedMap(String.CASE_INSENSITIVE_ORDER)
            .map { (key, value) -> NetworkDetailRow(key, value) }
    }

private fun bodyRows(
    body: String?,
    headers: Map<String, String>?,
    url: String?,
): List<NetworkDetailRow> {
    val capturedBody = body?.takeIf { it.isNotBlank() }
    if (capturedBody == null) {
        return listOf(
            NetworkDetailRow(
                label = "Body",
                value = WEB_VIEW_BODY_NOT_CAPTURED,
            ),
        )
    }

    val formatted = DebugNetworkContentFormatter.format(
        body = capturedBody,
        contentType = headers.contentTypeHeader(),
        url = url,
    )
    return listOfNotNull(
        NetworkDetailRow("Type", formatted.kind.label),
        NetworkDetailRow(
            label = "Body",
            value = formatted.text,
        ),
        NetworkDetailRow("Truncated", "true").takeIf { formatted.isTruncated },
    )
}

private fun mediaPreviewRows(
    request: RequestSnapshot?,
    error: PageError?,
    download: DownloadSnapshot?,
): List<NetworkDetailRow> {
    val url = download?.url ?: error?.url ?: request?.url
    val localUri = download?.localUri
    val contentType = download?.mimeType ?: error?.responseHeaders.contentTypeHeader()
    val kind = DebugNetworkContentFormatter.classify(contentType = contentType, url = localUri ?: url)
    if (!kind.isPreviewableMedia) return emptyList()

    return listOfNotNull(
        NetworkDetailRow("Kind", kind.label),
        contentType?.let { NetworkDetailRow("MIME type", it) },
        url?.let { NetworkDetailRow("Preview URL", it) },
        localUri?.let { NetworkDetailRow("Preview local URI", it) },
    )
}

private fun Map<String, String>?.contentTypeHeader(): String? =
    orEmpty().entries.firstOrNull { (key, _) ->
        key.equals("Content-Type", ignoreCase = true)
    }?.value

private fun PageError.matchesHttpResponse(request: RequestSnapshot): Boolean =
    type in setOf("HttpError", "ApiResponse") &&
        url == request.url &&
        (navigationId == request.navigationId || navigationId == 0L)

private fun formatNetworkDetailTime(timestamp: Long): String {
    if (timestamp <= 0L) return "-"
    return DateFormat.getDateTimeInstance().format(Date(timestamp))
}

private const val WEB_VIEW_BODY_NOT_CAPTURED = "Not captured by Android WebView callback"
