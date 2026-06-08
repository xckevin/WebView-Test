package com.xckevin.android.app.webview.test.debug

import java.net.URI

data class DebugState(
    val consoleLogs: List<ConsoleLog> = emptyList(),
    val errors: List<PageError> = emptyList(),
    val page: PageSnapshot = PageSnapshot(),
    val requests: List<RequestSnapshot> = emptyList(),
    val downloads: List<DownloadSnapshot> = emptyList(),
    val jsResults: List<JsExecutionResult> = emptyList(),
    val userFlows: List<UserFlowSnapshot> = emptyList(),
    val timeline: List<DebugEvent> = emptyList(),
)

data class ConsoleLog(
    val level: String,
    val message: String,
    val sourceId: String = "",
    val lineNumber: Int = 0,
    val navigationId: Long = 0L,
    val timestamp: Long,
)

data class PageError(
    val type: String,
    val message: String,
    val url: String? = null,
    val code: Int? = null,
    val statusCode: Int? = null,
    val responseHeaders: Map<String, String> = emptyMap(),
    val responseBody: String? = null,
    val navigationId: Long = 0L,
    val isMainFrame: Boolean = true,
    val timestamp: Long,
)

data class PageSnapshot(
    val url: String? = null,
    val title: String = "",
    val navigationId: Long = 0L,
    val progress: Int = 0,
    val status: PageStatus = PageStatus.Idle,
    val timestamp: Long = 0L,
)

enum class PageStatus {
    Idle,
    Loading,
    Finished,
    Error,
}

data class RequestSnapshot(
    val url: String,
    val method: String = "GET",
    val isMainFrame: Boolean,
    val requestHeaders: Map<String, String> = emptyMap(),
    val requestBody: String? = null,
    val category: RequestCategory = if (isMainFrame) RequestCategory.MAIN_FRAME else RequestCategory.RESOURCE,
    val navigationId: Long = 0L,
    val timestamp: Long,
) {
    val scheme: String
        get() = parsedUri()?.scheme.orEmpty()

    val host: String
        get() = parsedUri()?.host.orEmpty()

    val path: String
        get() = parsedUri()?.rawPath.orEmpty().ifBlank { "/" }

    val categoryLabel: String
        get() = when (category) {
            RequestCategory.MAIN_FRAME -> "main-frame"
            RequestCategory.RESOURCE -> "resource"
            RequestCategory.REDIRECT -> "redirect"
        }

    private fun parsedUri(): URI? =
        runCatching { URI(url) }.getOrNull()
}

enum class RequestCategory {
    MAIN_FRAME,
    RESOURCE,
    REDIRECT,
}

data class DownloadSnapshot(
    val url: String,
    val userAgent: String? = null,
    val contentDisposition: String? = null,
    val mimeType: String? = null,
    val contentLength: Long = 0L,
    val downloadId: Long? = null,
    val fileName: String? = null,
    val status: DownloadStatus = DownloadStatus.REQUESTED,
    val reason: String? = null,
    val localUri: String? = null,
    val navigationId: Long = 0L,
    val timestamp: Long,
    val updatedAt: Long = timestamp,
)

enum class DownloadStatus {
    REQUESTED,
    QUEUED,
    LOGGED,
    SKIPPED,
    SUCCESS,
    FAILED,
    UNKNOWN,
}

data class JsExecutionResult(
    val script: String,
    val result: String,
    val isError: Boolean = false,
    val timestamp: Long,
)

data class UserFlowSnapshot(
    val kind: UserFlowKind,
    val summary: String,
    val detail: String = "",
    val navigationId: Long = 0L,
    val timestamp: Long,
)

enum class UserFlowKind {
    PERMISSION,
    FILE_CHOOSER,
    DOWNLOAD,
    CACHE,
    COOKIE,
    CONTEXT_MENU,
    GENERAL,
}

data class DebugEvent(
    val id: Long,
    val type: DebugEventType,
    val severity: DebugSeverity,
    val summary: String,
    val details: List<String> = emptyList(),
    val navigationId: Long = 0L,
    val timestamp: Long,
)

enum class DebugEventType {
    PAGE,
    CONSOLE,
    ERROR,
    NETWORK,
    DOWNLOAD,
    JS,
    USER_FLOW,
}

enum class DebugSeverity {
    INFO,
    WARNING,
    ERROR,
}
