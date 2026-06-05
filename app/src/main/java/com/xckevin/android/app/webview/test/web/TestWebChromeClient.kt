package com.xckevin.android.app.webview.test.web

import android.webkit.ConsoleMessage
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.view.View

class TestWebChromeClient(
    private val navigationTracker: WebViewNavigationTracker,
    private val onEvent: (WebPageEvent) -> Unit,
    private val fileChooserHandler: FileChooserHandler,
    private val fullscreenVideoHandler: FullscreenVideoHandler,
    private val webPermissionHandler: WebPermissionHandler,
) : WebChromeClient() {
    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        onEvent(
            WebPageEvent.ProgressChanged(
                navigationId = navigationTracker.activeNavigationId(),
                progress = newProgress,
            )
        )
    }

    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        if (consoleMessage == null) return false

        onEvent(
            WebPageEvent.Console(
                level = consoleMessage.messageLevel().name,
                message = consoleMessage.message().orEmpty(),
                sourceId = consoleMessage.sourceId().orEmpty(),
                lineNumber = consoleMessage.lineNumber(),
                navigationId = navigationTracker.activeNavigationId(),
            )
        )
        return true
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<android.net.Uri>>?,
        fileChooserParams: FileChooserParams?,
    ): Boolean = fileChooserHandler.onShowFileChooser(
        filePathCallback = filePathCallback,
        fileChooserParams = fileChooserParams,
    )

    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
        fullscreenVideoHandler.onShowCustomView(view, callback)
    }

    override fun onHideCustomView() {
        fullscreenVideoHandler.onHideCustomView()
    }

    override fun onPermissionRequest(request: PermissionRequest?) {
        webPermissionHandler.onPermissionRequest(request)
    }

    override fun onPermissionRequestCanceled(request: PermissionRequest?) {
        webPermissionHandler.onPermissionRequestCanceled(request)
    }

    override fun onGeolocationPermissionsShowPrompt(
        origin: String?,
        callback: GeolocationPermissions.Callback?,
    ) {
        webPermissionHandler.onGeolocationPermissionsShowPrompt(origin, callback)
    }

    override fun onGeolocationPermissionsHidePrompt() {
        webPermissionHandler.onGeolocationPermissionsHidePrompt()
    }
}
