package com.xckevin.android.app.webview.test.web

class WebViewNavigationTracker {
    private var pendingExplicitNavigation: PendingExplicitNavigation? = null
    private var activeNavigationId: Long = 0L
    private var nextFallbackNavigationId: Long = 1L

    fun markExplicitNavigation(navigationId: Long, url: String) {
        pendingExplicitNavigation = PendingExplicitNavigation(
            navigationId = navigationId,
            url = url,
        )
        nextFallbackNavigationId = maxOf(nextFallbackNavigationId, navigationId + 1)
    }

    fun onPageStarted(url: String): Long {
        val pendingNavigation = pendingExplicitNavigation
        pendingExplicitNavigation = null

        val navigationId = if (pendingNavigation?.matches(url) == true) {
            pendingNavigation.navigationId
        } else {
            nextFallbackNavigationId++
        }

        activeNavigationId = navigationId
        nextFallbackNavigationId = maxOf(nextFallbackNavigationId, navigationId + 1)
        return navigationId
    }

    fun activeNavigationId(): Long = activeNavigationId

    private data class PendingExplicitNavigation(
        val navigationId: Long,
        val url: String,
    ) {
        fun matches(startedUrl: String): Boolean =
            url == startedUrl || url.trimEnd('/') == startedUrl.trimEnd('/')
    }
}
