package com.xckevin.android.app.webview.test.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class WebTestConfigSerializationTest {
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    @Test fun roundTripsDefaultConfig() {
        val config = WebTestConfig.default()
        val decoded = json.decodeFromString<WebTestConfig>(json.encodeToString(config))
        assertEquals(config, decoded)
    }

    @Test fun roundTripsCustomConfig() {
        val config = WebTestConfig.default().copy(
            userAgentMode = UserAgentMode.CUSTOM,
            customUserAgent = "DesktopFixture",
            cacheMode = WebCacheMode.NO_CACHE,
            geolocationPolicy = WebPermissionPolicy.ASK_EVERY_TIME,
            startFullscreen = true,
        )
        val decoded = json.decodeFromString<WebTestConfig>(json.encodeToString(config))
        assertEquals(config, decoded)
    }
}
