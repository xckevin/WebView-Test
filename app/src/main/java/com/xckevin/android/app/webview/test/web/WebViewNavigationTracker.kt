package com.xckevin.android.app.webview.test.web

class WebViewNavigationTracker {
    private var pendingExplicitNavigation: PendingExplicitNavigation? = null
    private var activeNavigationId: Long = 0L
    private var activeNavigationUrl: String? = null
    private var activeNavigationCompleted: Boolean = false
    private var nextFallbackNavigationId: Long = 1L
    private val startedNavigationIdsByUrl = mutableMapOf<String, Long>()

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
        activeNavigationUrl = url
        activeNavigationCompleted = false
        startedNavigationIdsByUrl[NavigationUrl.normalize(url)] = navigationId
        nextFallbackNavigationId = maxOf(nextFallbackNavigationId, navigationId + 1)
        return navigationId
    }

    fun onPageFinished(url: String): Long? = completeNavigation(url)

    fun onNavigationError(url: String?): Long? = completeNavigation(url.orEmpty())

    fun activeNavigationId(): Long = activeNavigationId

    private fun completeNavigation(url: String): Long? {
        val normalizedUrl = NavigationUrl.normalize(url)
        val startedNavigationId = startedNavigationIdsByUrl.remove(normalizedUrl)
        if (startedNavigationId != null) {
            if (startedNavigationId == activeNavigationId) {
                activeNavigationCompleted = true
            }
            return startedNavigationId
        }

        if (
            activeNavigationId > 0L &&
            !activeNavigationCompleted &&
            activeNavigationUrl?.let { activeUrl ->
                NavigationUrl.matches(activeUrl, url)
            } == true
        ) {
            activeNavigationCompleted = true
            return activeNavigationId
        }

        return null
    }

    private data class PendingExplicitNavigation(
        val navigationId: Long,
        val url: String,
    ) {
        fun matches(startedUrl: String): Boolean =
            NavigationUrl.matches(url, startedUrl)
    }

    private object NavigationUrl {
        fun normalize(url: String): String = url.trimEnd('/')

        fun matches(first: String, second: String): Boolean =
            first == second || normalize(first) == normalize(second)
    }
}
