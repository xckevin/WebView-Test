package com.xckevin.android.app.webview.test.web

import android.webkit.WebSettings
import com.xckevin.android.app.webview.test.model.MixedContentMode
import com.xckevin.android.app.webview.test.model.UserAgentMode
import com.xckevin.android.app.webview.test.model.WebCacheMode
import com.xckevin.android.app.webview.test.model.WebTestConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WebViewSettingsApplierTest {
    private val defaultUserAgent = "Mozilla/5.0 (Linux; Android 15; Pixel 9) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"

    @Test fun desktopModeBuildsDesktopUserAgent() {
        val snapshot = WebViewSettingsApplier.snapshot(
            config = WebTestConfig.default().copy(desktopMode = true),
            defaultUserAgent = defaultUserAgent,
        )

        assertNotNull(snapshot.userAgentString)
        assertTrue(
            snapshot.userAgentString!!.contains("Windows NT") ||
                snapshot.userAgentString.contains("Macintosh")
        )
        assertFalse(snapshot.userAgentString.contains("Mobile"))
    }

    @Test fun customUserAgentWinsOverDesktopMode() {
        val snapshot = WebViewSettingsApplier.snapshot(
            config = WebTestConfig.default().copy(
                userAgentMode = UserAgentMode.CUSTOM,
                customUserAgent = "CustomTestAgent/1.0",
                desktopMode = true,
            ),
            defaultUserAgent = defaultUserAgent,
        )

        assertEquals("CustomTestAgent/1.0", snapshot.userAgentString)
    }

    @Test fun noCacheMapsToWebSettingsNoCache() {
        val snapshot = WebViewSettingsApplier.snapshot(
            config = WebTestConfig.default().copy(cacheMode = WebCacheMode.NO_CACHE),
            defaultUserAgent = defaultUserAgent,
        )

        assertEquals(WebSettings.LOAD_NO_CACHE, snapshot.cacheMode)
    }

    @Test fun mixedContentBlockMapsToNeverAllow() {
        val snapshot = WebViewSettingsApplier.snapshot(
            config = WebTestConfig.default().copy(mixedContentMode = MixedContentMode.BLOCK),
            defaultUserAgent = defaultUserAgent,
        )

        assertEquals(WebSettings.MIXED_CONTENT_NEVER_ALLOW, snapshot.mixedContentMode)
    }
}
