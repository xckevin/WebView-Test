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

class TestWebViewClient(
    private val navigationTracker: WebViewNavigationTracker,
    private val onEvent: (WebPageEvent) -> Unit,
) : WebViewClient() {
    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        val navigationId = navigationTracker.onPageStarted(url.orEmpty()) ?: return
        emit(view, WebPageEvent.PageStarted(navigationId = navigationId, url = url.orEmpty()))
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        val navigationId = navigationTracker.onPageFinished(url.orEmpty()) ?: return
        emit(
            view,
            WebPageEvent.PageFinished(
                navigationId = navigationId,
                url = url.orEmpty(),
                title = view?.title.orEmpty(),
            )
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
}
