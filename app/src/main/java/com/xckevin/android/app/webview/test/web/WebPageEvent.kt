package com.xckevin.android.app.webview.test.web

sealed interface WebPageEvent {
    data class PageStarted(val navigationId: Long, val url: String) : WebPageEvent
    data class PageFinished(val navigationId: Long, val url: String, val title: String? = "") : WebPageEvent
    data class ProgressChanged(val navigationId: Long, val progress: Int) : WebPageEvent
    data class Console(
        val level: String,
        val message: String,
        val sourceId: String,
        val lineNumber: Int,
        val navigationId: Long = 0L,
    ) : WebPageEvent
    data class LoadError(
        val url: String?,
        val code: Int,
        val description: String,
        val navigationId: Long = 0L,
    ) : WebPageEvent
    data class HttpError(
        val url: String?,
        val statusCode: Int,
        val reason: String,
        val navigationId: Long = 0L,
    ) : WebPageEvent
    data class SslError(
        val url: String?,
        val primaryError: Int,
        val navigationId: Long = 0L,
    ) : WebPageEvent
    data class ResourceRequest(
        val url: String,
        val isMainFrame: Boolean,
        val navigationId: Long = 0L,
    ) : WebPageEvent
    data class DownloadRequested(
        val url: String,
        val userAgent: String?,
        val contentDisposition: String?,
        val mimeType: String?,
        val contentLength: Long,
        val navigationId: Long = 0L,
    ) : WebPageEvent
}
