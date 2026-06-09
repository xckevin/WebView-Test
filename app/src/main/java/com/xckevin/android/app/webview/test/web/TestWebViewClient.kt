package com.xckevin.android.app.webview.test.web

import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Looper
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.xckevin.android.app.webview.test.debug.DebugNetworkContentFormatter
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

class TestWebViewClient(
    private val navigationTracker: WebViewNavigationTracker,
    private val onEvent: (WebPageEvent) -> Unit,
    private val onDebugPageReady: (WebView) -> Unit = {},
) : WebViewClient() {
    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        val navigationId = navigationTracker.onPageStarted(url.orEmpty()) ?: return
        view?.let(onDebugPageReady)
        emit(view, WebPageEvent.PageStarted(navigationId = navigationId, url = url.orEmpty()))
        emitNavigationState(view = view, navigationId = navigationId, url = url)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        val navigationId = navigationTracker.onPageFinished(url.orEmpty()) ?: return
        view?.let(onDebugPageReady)
        emit(
            view,
            WebPageEvent.PageFinished(
                navigationId = navigationId,
                url = url.orEmpty(),
                title = view?.title.orEmpty(),
            )
        )
        emitNavigationState(view = view, navigationId = navigationId, url = url)
    }

    override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
        super.doUpdateVisitedHistory(view, url, isReload)
        emitNavigationState(
            view = view,
            navigationId = navigationTracker.activeNavigationId(),
            url = url,
        )
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?,
    ) {
        val navigationId = if (request?.isForMainFrame == true) {
            navigationTracker.onNavigationError(request.url?.toString()) ?: 0L
        } else {
            navigationTracker.activeNavigationId()
        }
        emit(
            view,
            WebPageEvent.LoadError(
                url = request?.url?.toString(),
                code = error?.errorCode ?: 0,
                description = error?.description?.toString().orEmpty(),
                navigationId = navigationId,
                isMainFrame = request?.isForMainFrame == true,
            )
        )
    }

    override fun onReceivedHttpError(
        view: WebView?,
        request: WebResourceRequest?,
        errorResponse: WebResourceResponse?,
    ) {
        val navigationId = if (request?.isForMainFrame == true) {
            navigationTracker.navigationIdForHttpError(request.url?.toString()) ?: 0L
        } else {
            navigationTracker.activeNavigationId()
        }
        emit(
            view,
            WebPageEvent.HttpError(
                url = request?.url?.toString(),
                statusCode = errorResponse?.statusCode ?: 0,
                reason = errorResponse?.reasonPhrase.orEmpty(),
                responseHeaders = errorResponse?.responseHeaders.orEmpty(),
                responseBody = errorResponse.capturedTextBody(request?.url?.toString()),
                navigationId = navigationId,
                isMainFrame = request?.isForMainFrame == true,
            )
        )
    }

    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
        handler?.cancel()
        val navigationId = navigationTracker.onNavigationError(error?.url) ?: 0L
        emit(
            view,
            WebPageEvent.SslError(
                url = error?.url,
                primaryError = error?.primaryError ?: 0,
                navigationId = navigationId,
            )
        )
    }

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?,
    ): WebResourceResponse? {
        val url = request?.url?.toString()
        if (url != null) {
            emit(
                view,
                WebPageEvent.ResourceRequest(
                    url = url,
                    method = request?.method ?: "GET",
                    requestHeaders = request?.requestHeaders.orEmpty(),
                    isMainFrame = request.isForMainFrame,
                    navigationId = navigationTracker.activeNavigationId(),
                )
            )
        }
        return null
    }

    private fun emit(view: WebView?, event: WebPageEvent) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            onEvent(event)
        } else {
            view?.post { onEvent(event) }
        }
    }

    private fun emitNavigationState(view: WebView?, navigationId: Long, url: String?) {
        if (navigationId <= 0L) return
        emit(
            view,
            WebPageEvent.NavigationStateChanged(
                navigationId = navigationId,
                url = url ?: view?.url,
                canGoBack = view?.canGoBack() == true,
                canGoForward = view?.canGoForward() == true,
            )
        )
    }

    private fun WebResourceResponse?.capturedTextBody(url: String?): String? {
        this ?: return null
        val contentType = responseHeaders.contentTypeHeader() ?: mimeType
        if (!DebugNetworkContentFormatter.isTextLike(contentType, url)) return null
        val stream = data ?: return null
        return runCatching {
            stream.readBoundedText(
                charset = charsetFrom(encoding, contentType),
                maxBytes = DebugNetworkContentFormatter.MaxCapturedBodyBytes,
            )
        }.getOrNull()
    }

    private fun InputStream.readBoundedText(charset: Charset, maxBytes: Int): String {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var totalBytes = 0
        var isTruncated = false
        while (true) {
            val maxRead = minOf(buffer.size, maxBytes + 1 - totalBytes)
            if (maxRead <= 0) {
                isTruncated = true
                break
            }
            val read = read(buffer, 0, maxRead)
            if (read <= 0) break
            output.write(buffer, 0, read)
            totalBytes += read
            if (totalBytes > maxBytes) {
                isTruncated = true
                break
            }
        }

        val bytes = output.toByteArray()
        val visibleBytes = if (bytes.size > maxBytes) bytes.copyOf(maxBytes) else bytes
        val text = String(visibleBytes, charset)
        return if (isTruncated) {
            "$text\n\n[truncated to $maxBytes bytes]"
        } else {
            text
        }
    }

    private fun Map<String, String>?.contentTypeHeader(): String? =
        orEmpty().entries.firstOrNull { (key, _) ->
            key.equals("Content-Type", ignoreCase = true)
        }?.value

    private fun charsetFrom(encoding: String?, contentType: String?): Charset {
        val charsetName = encoding?.takeIf { it.isNotBlank() }
            ?: contentType
                ?.split(";")
                ?.map { it.trim() }
                ?.firstOrNull { it.startsWith("charset=", ignoreCase = true) }
                ?.substringAfter("=")
                ?.trim('"')
        return charsetName
            ?.let { runCatching { Charset.forName(it) }.getOrNull() }
            ?: StandardCharsets.UTF_8
    }
}
