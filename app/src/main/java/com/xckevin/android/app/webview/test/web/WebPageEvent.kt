package com.xckevin.android.app.webview.test.web

sealed interface WebPageEvent {
    data class PageStarted(val url: String, val navigationId: Long) : WebPageEvent
    data class PageFinished(val url: String, val navigationId: Long, val title: String = "") : WebPageEvent
    data class ProgressChanged(val progress: Int, val navigationId: Long) : WebPageEvent
}
