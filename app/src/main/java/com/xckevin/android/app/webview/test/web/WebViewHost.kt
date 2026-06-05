package com.xckevin.android.app.webview.test.web

import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.xckevin.android.app.webview.test.model.WebCacheMode
import com.xckevin.android.app.webview.test.model.WebTestConfig

class WebViewController {
    private var webView: WebView? = null

    fun canGoBack(): Boolean = webView?.canGoBack() == true

    fun goBack(): Boolean {
        val webView = webView ?: return false
        if (!webView.canGoBack()) return false

        webView.goBack()
        return true
    }

    internal fun attach(webView: WebView) {
        this.webView = webView
    }

    internal fun detach() {
        webView = null
    }
}

@Composable
fun rememberWebViewController(): WebViewController = remember { WebViewController() }

@Composable
fun WebViewHost(
    url: String?,
    config: WebTestConfig,
    isFullscreen: Boolean,
    navigationId: Long,
    onEvent: (WebPageEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val controller = rememberWebViewController()
    WebViewHost(
        url = url,
        config = config,
        isFullscreen = isFullscreen,
        navigationId = navigationId,
        onEvent = onEvent,
        controller = controller,
        modifier = modifier,
    )
}

@Composable
fun WebViewHost(
    url: String?,
    config: WebTestConfig,
    isFullscreen: Boolean,
    navigationId: Long,
    onEvent: (WebPageEvent) -> Unit,
    controller: WebViewController,
    modifier: Modifier = Modifier,
) {
    val currentOnEvent by rememberUpdatedState(onEvent)
    var lastLoadedUrl by remember { mutableStateOf<String?>(null) }
    val eventSink: (WebPageEvent) -> Unit = { event -> currentOnEvent(event) }

    DisposableEffect(controller) {
        onDispose { controller.detach() }
    }

    AndroidView(
        modifier = if (isFullscreen) modifier.fillMaxSize() else modifier,
        factory = { context ->
            WebView(context).also { webView ->
                controller.attach(webView)
            }
        },
        update = { webView ->
            controller.attach(webView)
            WebViewSettingsApplier.apply(webView = webView, config = config)
            webView.webViewClient = TestWebViewClient(
                navigationId = navigationId,
                onEvent = eventSink,
            )
            webView.webChromeClient = TestWebChromeClient(
                navigationId = navigationId,
                onEvent = eventSink,
            )
            webView.setDownloadListener { downloadUrl, userAgent, contentDisposition, mimeType, contentLength ->
                currentOnEvent(
                    WebPageEvent.DownloadRequested(
                        url = downloadUrl,
                        userAgent = userAgent,
                        contentDisposition = contentDisposition,
                        mimeType = mimeType,
                        contentLength = contentLength,
                    )
                )
            }

            if (url != lastLoadedUrl) {
                lastLoadedUrl = url
                if (!url.isNullOrBlank()) {
                    if (config.cacheMode == WebCacheMode.CLEAR_BEFORE_LOAD) {
                        webView.clearCache(true)
                    }
                    webView.loadUrl(url)
                }
            }
        },
    )
}
