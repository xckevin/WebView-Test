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

        onEvent(
            WebPageEvent.DownloadRequested(
                url = url,
                userAgent = userAgent,
                contentDisposition = contentDisposition,
                mimeType = mimeType,
                contentLength = contentLength,
                navigationId = navigationTracker.activeNavigationId(),
            )
        )

        val uri = Uri.parse(url)
        return when (uri.scheme?.lowercase()) {
            "http", "https" -> enqueueSystemDownload(
                uri = uri,
                userAgent = userAgent,
                contentDisposition = contentDisposition,
                mimeType = mimeType,
            )

            "data" -> {
                onMessage("Inline data download logged but not saved in v1")
                true
            }

            else -> {
                onMessage("Download skipped: unsupported URL scheme")
                false
            }
        }
    }

    private fun enqueueSystemDownload(
        uri: Uri,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?,
    ): Boolean {
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
            manager.enqueue(request)
            onMessage("Download started: $fileName")
            true
        }.getOrElse { error ->
            onMessage("Download failed: ${error.message.orEmpty()}")
            false
        }
    }
}
