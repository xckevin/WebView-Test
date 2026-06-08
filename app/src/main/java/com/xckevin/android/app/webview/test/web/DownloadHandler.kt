package com.xckevin.android.app.webview.test.web

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.DownloadListener
import android.webkit.URLUtil

class DownloadHandler(
    private val context: Context,
    private val navigationTracker: WebViewNavigationTracker,
    private val onEvent: (WebPageEvent) -> Unit,
    private val onMessage: (String) -> Unit = {},
) : DownloadListener {
    override fun onDownloadStart(
        url: String?,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?,
        contentLength: Long,
    ) {
        requestDownload(
            url = url.orEmpty(),
            userAgent = userAgent,
            contentDisposition = contentDisposition,
            mimeType = mimeType,
            contentLength = contentLength,
        )
    }

    fun requestDownload(
        url: String,
        userAgent: String? = null,
        contentDisposition: String? = null,
        mimeType: String? = null,
        contentLength: Long = -1L,
    ): Boolean {
        if (url.isBlank()) return false

        val uri = Uri.parse(url)
        return when (uri.scheme?.lowercase()) {
            "http", "https" -> {
                val result = enqueueSystemDownload(
                    uri = uri,
                    userAgent = userAgent,
                    contentDisposition = contentDisposition,
                    mimeType = mimeType,
                )
                emitDownloadRequested(
                    url = url,
                    userAgent = userAgent,
                    contentDisposition = contentDisposition,
                    mimeType = mimeType,
                    contentLength = contentLength,
                    downloadId = result.downloadId,
                    fileName = result.fileName,
                    status = if (result.success) "QUEUED" else "FAILED",
                    reason = result.message,
                )
                result.success
            }

            "data" -> {
                onMessage("Inline data download logged but not saved in v1")
                emitDownloadRequested(
                    url = url,
                    userAgent = userAgent,
                    contentDisposition = contentDisposition,
                    mimeType = mimeType,
                    contentLength = contentLength,
                    status = "LOGGED",
                    reason = "Inline data download logged but not saved",
                )
                true
            }

            else -> {
                onMessage("Download skipped: unsupported URL scheme")
                emitDownloadRequested(
                    url = url,
                    userAgent = userAgent,
                    contentDisposition = contentDisposition,
                    mimeType = mimeType,
                    contentLength = contentLength,
                    status = "SKIPPED",
                    reason = "Unsupported URL scheme",
                )
                false
            }
        }
    }

    private fun emitDownloadRequested(
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?,
        contentLength: Long,
        downloadId: Long? = null,
        fileName: String? = null,
        status: String,
        reason: String?,
    ) {
        onEvent(
            WebPageEvent.DownloadRequested(
                url = url,
                userAgent = userAgent,
                contentDisposition = contentDisposition,
                mimeType = mimeType,
                contentLength = contentLength,
                navigationId = navigationTracker.activeNavigationId(),
                downloadId = downloadId,
                fileName = fileName,
                status = status,
                reason = reason,
            )
        )
    }

    private fun enqueueSystemDownload(
        uri: Uri,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?,
    ): EnqueueResult {
        return runCatching {
            val fileName = URLUtil.guessFileName(uri.toString(), contentDisposition, mimeType)
            val request = DownloadManager.Request(uri)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                .setTitle(fileName)
                .setMimeType(mimeType)

            if (!userAgent.isNullOrBlank()) {
                request.addRequestHeader("User-Agent", userAgent)
            }

            val manager = context.getSystemService(DownloadManager::class.java)
            val downloadId = manager.enqueue(request)
            val message = "Download started: $fileName"
            onMessage(message)
            EnqueueResult(success = true, downloadId = downloadId, fileName = fileName, message = message)
        }.getOrElse { error ->
            val message = "Download failed: ${error.message.orEmpty()}"
            onMessage(message)
            EnqueueResult(success = false, downloadId = null, fileName = null, message = message)
        }
    }

    private data class EnqueueResult(
        val success: Boolean,
        val downloadId: Long?,
        val fileName: String?,
        val message: String,
    )
}
