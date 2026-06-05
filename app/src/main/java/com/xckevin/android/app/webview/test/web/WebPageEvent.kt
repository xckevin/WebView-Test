package com.xckevin.android.app.webview.test.web

sealed interface WebPageEvent {
    data class PageStarted(val url: String) : WebPageEvent
    data class PageFinished(val url: String, val title: String = "") : WebPageEvent
    data class ProgressChanged(val progress: Int) : WebPageEvent
}
