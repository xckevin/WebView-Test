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
    private val navigationId: Long,
    private val onEvent: (WebPageEvent) -> Unit,
) : WebViewClient() {
    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        emit(view, WebPageEvent.PageStarted(navigationId = navigationId, url = url.orEmpty()))
    }

    override fun onPageFinished(view: WebView?, url: String?) {
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
        emit(
            view,
            WebPageEvent.LoadError(
                url = request?.url?.toString(),
                code = error?.errorCode ?: 0,
                description = error?.description?.toString().orEmpty(),
            )
        )
    }

    override fun onReceivedHttpError(
        view: WebView?,
        request: WebResourceRequest?,
        errorResponse: WebResourceResponse?,
    ) {
        emit(
            view,
            WebPageEvent.HttpError(
                url = request?.url?.toString(),
                statusCode = errorResponse?.statusCode ?: 0,
                reason = errorResponse?.reasonPhrase.orEmpty(),
            )
        )
    }

    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
        handler?.cancel()
        emit(
            view,
            WebPageEvent.SslError(
                url = error?.url,
                primaryError = error?.primaryError ?: 0,
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
