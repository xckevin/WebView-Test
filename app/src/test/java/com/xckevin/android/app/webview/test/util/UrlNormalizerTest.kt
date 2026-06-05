package com.xckevin.android.app.webview.test.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UrlNormalizerTest {
    @Test fun addsHttpsWhenSchemeIsMissing() {
        assertEquals("https://example.com", UrlNormalizer.normalizeRemoteUrl("example.com"))
    }

    @Test fun keepsHttpScheme() {
        assertEquals("http://10.0.2.2:8080/path", UrlNormalizer.normalizeRemoteUrl("http://10.0.2.2:8080/path"))
    }

    @Test fun keepsHttpsScheme() {
        assertEquals("https://xckevin.com/en/", UrlNormalizer.normalizeRemoteUrl(" https://xckevin.com/en/ "))
    }

    @Test fun rejectsUnsupportedScheme() {
        assertNull(UrlNormalizer.normalizeRemoteUrl("ftp://example.com/file"))
    }

    @Test fun rejectsBlankInput() {
        assertNull(UrlNormalizer.normalizeRemoteUrl("  "))
    }
}
