package com.xckevin.android.app.webview.test.web

class WebViewNavigationTracker {
    private val pendingExplicitNavigations = mutableListOf<PendingExplicitNavigation>()
    private var activeNavigationId: Long = 0L
    private var activeNavigationUrl: String? = null
    private var activeNavigationCompleted: Boolean = false
    private var nextFallbackNavigationId: Long = 1L
    private val startedNavigationIdsByUrl = mutableMapOf<String, MutableList<Long>>()

    @Synchronized
    fun markExplicitNavigation(navigationId: Long, url: String) {
        pendingExplicitNavigations.add(
            PendingExplicitNavigation(
                navigationId = navigationId,
                url = url,
            )
        )
        nextFallbackNavigationId = maxOf(nextFallbackNavigationId, navigationId + 1)
    }

    @Synchronized
    fun onPageStarted(url: String): Long {
        val pendingNavigationIndex = pendingExplicitNavigations.indexOfFirst { it.matches(url) }
        val pendingNavigation = pendingNavigationIndex.takeIf { it >= 0 }?.let { index ->
            val navigation = pendingExplicitNavigations[index]
            pendingExplicitNavigations.subList(0, index + 1).clear()
            navigation
        }

        val navigationId = if (pendingNavigation != null) {
            pendingNavigation.navigationId
        } else {
            nextFallbackNavigationId++
        }

        activeNavigationId = navigationId
        activeNavigationUrl = url
        activeNavigationCompleted = false
        startedNavigationIdsByUrl.getOrPut(NavigationUrl.normalize(url)) { mutableListOf() }
            .add(navigationId)
        nextFallbackNavigationId = maxOf(nextFallbackNavigationId, navigationId + 1)
        return navigationId
    }

    @Synchronized
    fun onPageFinished(url: String): Long? = completeNavigation(url)

    @Synchronized
    fun onNavigationError(url: String?): Long? = completeNavigation(url.orEmpty())

    @Synchronized
    fun navigationIdForHttpError(url: String?): Long? = findNavigation(url.orEmpty())

    @Synchronized
    fun activeNavigationId(): Long = activeNavigationId

    private fun completeNavigation(url: String): Long? {
        val normalizedUrl = NavigationUrl.normalize(url)
        val startedNavigationIds = startedNavigationIdsByUrl[normalizedUrl]
        val startedNavigationId = startedNavigationIds?.removeFirstOrNull()
        if (startedNavigationId != null) {
            if (startedNavigationIds.isEmpty()) {
                startedNavigationIdsByUrl.remove(normalizedUrl)
            }
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

    private fun findNavigation(url: String): Long? {
        val normalizedUrl = NavigationUrl.normalize(url)
        val startedNavigationId = startedNavigationIdsByUrl[normalizedUrl]?.firstOrNull()
        if (startedNavigationId != null) {
            return startedNavigationId
        }

        return activeNavigationId.takeIf {
            it > 0L &&
                !activeNavigationCompleted &&
                activeNavigationUrl?.let { activeUrl ->
                    NavigationUrl.matches(activeUrl, url)
                } == true
        }
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
