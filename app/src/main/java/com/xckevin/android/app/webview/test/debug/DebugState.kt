package com.xckevin.android.app.webview.test.debug

data class DebugState(
    val consoleLogs: List<ConsoleLog> = emptyList(),
    val errors: List<PageError> = emptyList(),
    val page: PageSnapshot = PageSnapshot(),
    val requests: List<RequestSnapshot> = emptyList(),
    val downloads: List<DownloadSnapshot> = emptyList(),
    val jsResults: List<JsExecutionResult> = emptyList(),
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
    val isMainFrame: Boolean,
    val navigationId: Long = 0L,
    val timestamp: Long,
)

data class DownloadSnapshot(
    val url: String,
    val userAgent: String? = null,
    val contentDisposition: String? = null,
    val mimeType: String? = null,
    val contentLength: Long = 0L,
    val navigationId: Long = 0L,
    val timestamp: Long,
)

data class JsExecutionResult(
    val script: String,
    val result: String,
    val isError: Boolean = false,
    val timestamp: Long,
)
