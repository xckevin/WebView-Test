package com.xckevin.android.app.webview.test.web

import org.junit.Assert.assertEquals
import org.junit.Test

class WebViewNavigationTrackerTest {
    @Test fun explicitPageStartUsesMarkedNavigationId() {
        val tracker = WebViewNavigationTracker()

        tracker.markExplicitNavigation(navigationId = 42L, url = "https://example.com")

        assertEquals(42L, tracker.onPageStarted("https://example.com"))
        assertEquals(42L, tracker.activeNavigationId())
    }

    @Test fun webInitiatedNavigationAllocatesFallbackAboveExplicitId() {
        val tracker = WebViewNavigationTracker()

        tracker.markExplicitNavigation(navigationId = 7L, url = "https://example.com")
        tracker.onPageStarted("https://example.com")

        assertEquals(8L, tracker.onPageStarted("https://example.com/next"))
        assertEquals(8L, tracker.activeNavigationId())
    }

    @Test fun markingExplicitNavigationDoesNotChangeActiveIdUntilPageStarted() {
        val tracker = WebViewNavigationTracker()

        tracker.markExplicitNavigation(navigationId = 3L, url = "https://old.example.com")
        tracker.onPageStarted("https://old.example.com")
        tracker.markExplicitNavigation(navigationId = 4L, url = "https://new.example.com")

        assertEquals(3L, tracker.activeNavigationId())
    }

    @Test fun unmatchedPendingExplicitStartUsesFallbackId() {
        val tracker = WebViewNavigationTracker()

        tracker.markExplicitNavigation(navigationId = 10L, url = "https://explicit.example.com")

        assertEquals(11L, tracker.onPageStarted("https://clicked.example.com"))
        assertEquals(11L, tracker.activeNavigationId())
    }

    @Test fun explicitUrlAllowsTrailingSlashDifference() {
        val tracker = WebViewNavigationTracker()

        tracker.markExplicitNavigation(navigationId = 12L, url = "https://example.com")

        assertEquals(12L, tracker.onPageStarted("https://example.com/"))
    }
}
