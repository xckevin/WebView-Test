package com.xckevin.android.app.webview.test.web

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
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
    val currentOnEvent = rememberUpdatedState(onEvent)
    val navigationTracker = remember { WebViewNavigationTracker() }
    var lastLoadedUrl by remember { mutableStateOf<String?>(null) }
    val eventSink: (WebPageEvent) -> Unit = remember {
        { event -> currentOnEvent.value(event) }
    }

    AndroidView(
        modifier = if (isFullscreen) modifier.fillMaxSize() else modifier,
        factory = { context ->
            WebView(context).also { webView ->
                controller.attach(webView)
                webView.webViewClient = TestWebViewClient(
                    navigationTracker = navigationTracker,
                    onEvent = eventSink,
                )
                webView.webChromeClient = TestWebChromeClient(
                    navigationTracker = navigationTracker,
                    onEvent = eventSink,
                )
                webView.setDownloadListener { downloadUrl, userAgent, contentDisposition, mimeType, contentLength ->
                    eventSink(
                        WebPageEvent.DownloadRequested(
                            url = downloadUrl,
                            userAgent = userAgent,
                            contentDisposition = contentDisposition,
                            mimeType = mimeType,
                            contentLength = contentLength,
                            navigationId = navigationTracker.activeNavigationId(),
                        )
                    )
                }
            }
        },
        update = { webView ->
            controller.attach(webView)
            WebViewSettingsApplier.apply(webView = webView, config = config)

            if (url != lastLoadedUrl) {
                lastLoadedUrl = url
                if (!url.isNullOrBlank()) {
                    if (config.cacheMode == WebCacheMode.CLEAR_BEFORE_LOAD) {
                        webView.clearCache(true)
                    }
                    navigationTracker.markExplicitNavigation(
                        navigationId = navigationId,
                        url = url,
                    )
                    webView.loadUrl(url)
                }
            }
        },
        onRelease = { webView ->
            webView.stopLoading()
            webView.setDownloadListener(null)
            webView.webChromeClient = null
            webView.webViewClient = WebViewClient()
            webView.clearHistory()
            webView.clearFormData()
            webView.removeAllViews()
            controller.detach()
            webView.destroy()
        },
    )
}
