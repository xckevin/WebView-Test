package com.xckevin.android.app.webview.test.web

import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView

class TestWebChromeClient(
    private val navigationId: Long,
    private val onEvent: (WebPageEvent) -> Unit,
) : WebChromeClient() {
    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        onEvent(WebPageEvent.ProgressChanged(navigationId = navigationId, progress = newProgress))
    }

    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        if (consoleMessage == null) return false

        onEvent(
            WebPageEvent.Console(
                level = consoleMessage.messageLevel().name,
                message = consoleMessage.message().orEmpty(),
                sourceId = consoleMessage.sourceId().orEmpty(),
                lineNumber = consoleMessage.lineNumber(),
            )
        )
        return true
    }
}
