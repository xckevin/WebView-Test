package com.xckevin.android.app.webview.test.web

import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.xckevin.android.app.webview.test.R
import com.xckevin.android.app.webview.test.model.WebCacheMode
import com.xckevin.android.app.webview.test.model.WebTestConfig

class WebViewController {
    private var webView: WebView? = null
    private var fileChooserHandler: FileChooserHandler? = null
    private var downloadHandler: DownloadHandler? = null
    private var fullscreenVideoHandler: FullscreenVideoHandler? = null
    private var webPermissionHandler: WebPermissionHandler? = null

    fun canGoBack(): Boolean = webView?.canGoBack() == true

    fun canGoForward(): Boolean = webView?.canGoForward() == true

    fun goBack(): Boolean {
        val webView = webView ?: return false
        if (!webView.canGoBack()) return false

        webView.goBack()
        return true
    }

    fun goForward(): Boolean {
        val webView = webView ?: return false
        if (!webView.canGoForward()) return false

        webView.goForward()
        return true
    }

    fun evaluateJavaScript(script: String, callback: (String) -> Unit) {
        val webView = webView
        if (webView == null) {
            callback("No WebView attached")
            return
        }

        webView.evaluateJavascript(script, callback)
    }

    fun readCookies(): String {
        val url = webView?.url ?: return ""
        return CookieManager.getInstance().getCookie(url).orEmpty()
    }

    fun clearCookies(callback: (Boolean) -> Unit) {
        CookieManager.getInstance().removeAllCookies(callback)
        CookieManager.getInstance().flush()
    }

    fun clearCache(includeDiskFiles: Boolean = true) {
        webView?.clearCache(includeDiskFiles)
    }

    fun downloadUrl(url: String): Boolean =
        downloadHandler?.requestDownload(url = url) == true

    fun cancelFileChooser() {
        fileChooserHandler?.cancelPending()
    }

    fun cancelWebPermissionPrompts() {
        webPermissionHandler?.cancelPendingPrompts()
    }

    fun hideCustomView(): Boolean {
        val handler = fullscreenVideoHandler ?: return false
        if (!handler.isFullscreenActive()) return false

        handler.onHideCustomView()
        return true
    }

    internal fun attach(webView: WebView) {
        this.webView = webView
    }

    internal fun attachFileChooserHandler(fileChooserHandler: FileChooserHandler) {
        this.fileChooserHandler = fileChooserHandler
    }

    internal fun attachDownloadHandler(downloadHandler: DownloadHandler) {
        this.downloadHandler = downloadHandler
    }

    internal fun attachFullscreenVideoHandler(fullscreenVideoHandler: FullscreenVideoHandler) {
        this.fullscreenVideoHandler = fullscreenVideoHandler
    }

    internal fun attachWebPermissionHandler(webPermissionHandler: WebPermissionHandler) {
        this.webPermissionHandler = webPermissionHandler
    }

    internal fun detach() {
        webView = null
        fileChooserHandler = null
        downloadHandler = null
        fullscreenVideoHandler = null
        webPermissionHandler = null
    }
}

internal data class LoadedNavigationKey(
    val url: String,
    val navigationId: Long,
)

internal fun loadedNavigationKey(requestedUrl: String?, navigationId: Long): LoadedNavigationKey? =
    requestedUrl?.takeUnless { it.isBlank() }?.let {
        LoadedNavigationKey(url = it, navigationId = navigationId)
    }

@Composable
fun rememberWebViewController(): WebViewController = remember { WebViewController() }

@Composable
fun WebViewHost(
    requestedUrl: String?,
    config: WebTestConfig,
    isFullscreen: Boolean,
    requestedNavigationId: Long,
    onEvent: (WebPageEvent) -> Unit,
    onOpenDocument: (Array<String>, (Uri?) -> Unit) -> Unit = { _, onResult -> onResult(null) },
    onRequestRuntimePermissions: (Array<String>, (Map<String, Boolean>) -> Unit) -> Unit = { _, onResult -> onResult(emptyMap()) },
    onWebPermissionPrompt: (WebPermissionPrompt?) -> Unit = { it?.onDeny() },
    onFullscreenVideoChanged: (Boolean) -> Unit = {},
    onContextMenuTarget: (WebContextMenuTarget) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val controller = rememberWebViewController()
    WebViewHost(
        requestedUrl = requestedUrl,
        config = config,
        isFullscreen = isFullscreen,
        requestedNavigationId = requestedNavigationId,
        onEvent = onEvent,
        onOpenDocument = onOpenDocument,
        onRequestRuntimePermissions = onRequestRuntimePermissions,
        onWebPermissionPrompt = onWebPermissionPrompt,
        onFullscreenVideoChanged = onFullscreenVideoChanged,
        onContextMenuTarget = onContextMenuTarget,
        controller = controller,
        modifier = modifier,
    )
}

@Composable
fun WebViewHost(
    requestedUrl: String?,
    config: WebTestConfig,
    isFullscreen: Boolean,
    requestedNavigationId: Long,
    onEvent: (WebPageEvent) -> Unit,
    controller: WebViewController,
    modifier: Modifier = Modifier,
) {
    WebViewHost(
        requestedUrl = requestedUrl,
        config = config,
        isFullscreen = isFullscreen,
        requestedNavigationId = requestedNavigationId,
        onEvent = onEvent,
        onOpenDocument = { _, onResult -> onResult(null) },
        onRequestRuntimePermissions = { _, onResult -> onResult(emptyMap()) },
        onWebPermissionPrompt = { it?.onDeny() },
        onFullscreenVideoChanged = {},
        onContextMenuTarget = {},
        controller = controller,
        modifier = modifier,
    )
}

@Composable
fun WebViewHost(
    requestedUrl: String?,
    config: WebTestConfig,
    isFullscreen: Boolean,
    requestedNavigationId: Long,
    onEvent: (WebPageEvent) -> Unit,
    onOpenDocument: (Array<String>, (Uri?) -> Unit) -> Unit = { _, onResult -> onResult(null) },
    onRequestRuntimePermissions: (Array<String>, (Map<String, Boolean>) -> Unit) -> Unit = { _, onResult -> onResult(emptyMap()) },
    onWebPermissionPrompt: (WebPermissionPrompt?) -> Unit = { it?.onDeny() },
    onFullscreenVideoChanged: (Boolean) -> Unit = {},
    onContextMenuTarget: (WebContextMenuTarget) -> Unit = {},
    controller: WebViewController,
    modifier: Modifier = Modifier,
) {
    val currentOnEvent = rememberUpdatedState(onEvent)
    val currentConfig = rememberUpdatedState(config)
    val currentOnOpenDocument = rememberUpdatedState(onOpenDocument)
    val currentOnRequestRuntimePermissions = rememberUpdatedState(onRequestRuntimePermissions)
    val currentOnWebPermissionPrompt = rememberUpdatedState(onWebPermissionPrompt)
    val currentOnFullscreenVideoChanged = rememberUpdatedState(onFullscreenVideoChanged)
    val currentOnContextMenuTarget = rememberUpdatedState(onContextMenuTarget)
    val context = LocalContext.current
    val webCaptureMessageTemplate = context.getString(R.string.permission_allow_web_capture_message)
    val permissionAndTemplate = context.getString(R.string.permission_list_and)
    val permissionTextProvider = WebPermissionTextProvider(
        webCaptureTitle = context.getString(R.string.permission_allow_web_capture_title),
        webCaptureMessage = { label -> webCaptureMessageTemplate.format(label) },
        geolocationTitle = context.getString(R.string.permission_allow_geolocation_title),
        geolocationMessage = context.getString(R.string.permission_allow_geolocation_message),
        camera = context.getString(R.string.permission_camera),
        microphone = context.getString(R.string.permission_microphone),
        protectedDevicePermissions = context.getString(R.string.permission_protected_device_permissions),
        joinAnd = { first, second -> permissionAndTemplate.format(first, second) },
    )
    val currentPermissionTextProvider = rememberUpdatedState(permissionTextProvider)
    val navigationTracker = remember { WebViewNavigationTracker() }
    var lastLoadedNavigation by remember { mutableStateOf<LoadedNavigationKey?>(null) }
    var fullscreenVideoView by remember { mutableStateOf<View?>(null) }
    val eventSink: (WebPageEvent) -> Unit = remember {
        { event -> currentOnEvent.value(event) }
    }
    val messageSink: (String) -> Unit = remember {
        { message ->
            eventSink(
                WebPageEvent.Console(
                    level = "INFO",
                    message = message,
                    sourceId = "webview-host",
                    lineNumber = 0,
                    navigationId = navigationTracker.activeNavigationId(),
                )
            )
        }
    }
    val userFlowSink: (String, String, String) -> Unit = remember {
        { kind, summary, detail ->
            eventSink(
                WebPageEvent.UserFlow(
                    kind = kind,
                    summary = summary,
                    detail = detail,
                    navigationId = navigationTracker.activeNavigationId(),
                )
            )
        }
    }

    Box(modifier = if (isFullscreen) modifier.fillMaxSize() else modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).also { webView ->
                    val fileChooserHandler = FileChooserHandler(
                        configProvider = { currentConfig.value },
                        openDocument = { mimeTypes, onResult ->
                            currentOnOpenDocument.value(mimeTypes, onResult)
                        },
                        onMessage = messageSink,
                        onUserFlow = { summary, detail -> userFlowSink("FILE_CHOOSER", summary, detail) },
                    )
                    val fullscreenVideoHandler = FullscreenVideoHandler { view ->
                        fullscreenVideoView = view
                        currentOnFullscreenVideoChanged.value(view != null)
                    }
                    val webPermissionHandler = WebPermissionHandler(
                        configProvider = { currentConfig.value },
                        requestRuntimePermissions = { permissions, onResult ->
                            currentOnRequestRuntimePermissions.value(permissions, onResult)
                        },
                        showPrompt = { prompt -> currentOnWebPermissionPrompt.value(prompt) },
                        onMessage = messageSink,
                        onUserFlow = { summary, detail -> userFlowSink("PERMISSION", summary, detail) },
                        textProvider = currentPermissionTextProvider.value,
                    )
                    val downloadHandler = DownloadHandler(
                        context = context.applicationContext,
                        navigationTracker = navigationTracker,
                        onEvent = eventSink,
                        onMessage = messageSink,
                        onUserFlow = { summary, detail -> userFlowSink("DOWNLOAD", summary, detail) },
                    )

                    controller.attach(webView)
                    controller.attachFileChooserHandler(fileChooserHandler)
                    controller.attachDownloadHandler(downloadHandler)
                    controller.attachFullscreenVideoHandler(fullscreenVideoHandler)
                    controller.attachWebPermissionHandler(webPermissionHandler)

                    webView.webViewClient = TestWebViewClient(
                        navigationTracker = navigationTracker,
                        onEvent = eventSink,
                    )
                    webView.webChromeClient = TestWebChromeClient(
                        navigationTracker = navigationTracker,
                        onEvent = eventSink,
                        fileChooserHandler = fileChooserHandler,
                        fullscreenVideoHandler = fullscreenVideoHandler,
                        webPermissionHandler = webPermissionHandler,
                    )
                    webView.setDownloadListener(downloadHandler)
                    WebContextMenu(webView = webView) { target ->
                        currentOnContextMenuTarget.value(target)
                    }.attach()
                }
            },
            update = { webView ->
                controller.attach(webView)
                WebViewSettingsApplier.apply(webView = webView, config = config)

                val requestedNavigation = loadedNavigationKey(requestedUrl, requestedNavigationId)
                if (requestedNavigation != null && requestedNavigation != lastLoadedNavigation) {
                    lastLoadedNavigation = requestedNavigation
                    if (config.cacheMode == WebCacheMode.CLEAR_BEFORE_LOAD) {
                        webView.clearCache(true)
                    }
                    navigationTracker.markExplicitNavigation(
                        navigationId = requestedNavigation.navigationId,
                        url = requestedNavigation.url,
                    )
                    webView.loadUrl(requestedNavigation.url)
                }
            },
            onRelease = { webView ->
                controller.cancelFileChooser()
                controller.cancelWebPermissionPrompts()
                controller.hideCustomView()
                webView.stopLoading()
                webView.setDownloadListener(null)
                webView.webChromeClient = null
                webView.webViewClient = WebViewClient()
                webView.setOnLongClickListener(null)
                webView.clearHistory()
                webView.clearFormData()
                webView.removeAllViews()
                controller.detach()
                webView.destroy()
            },
        )

        fullscreenVideoView?.let { customView ->
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                factory = {
                    (customView.parent as? ViewGroup)?.removeView(customView)
                    customView
                },
            )
        }
    }
}
