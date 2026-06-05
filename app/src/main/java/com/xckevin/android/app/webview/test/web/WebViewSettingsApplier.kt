package com.xckevin.android.app.webview.test.web

import android.webkit.WebSettings
import android.webkit.WebView
import com.xckevin.android.app.webview.test.model.MixedContentMode
import com.xckevin.android.app.webview.test.model.UserAgentMode
import com.xckevin.android.app.webview.test.model.WebCacheMode
import com.xckevin.android.app.webview.test.model.WebTestConfig

data class WebViewSettingsSnapshot(
    val javaScriptEnabled: Boolean,
    val domStorageEnabled: Boolean,
    val cacheMode: Int,
    val mixedContentMode: Int,
    val userAgentString: String?,
)

object WebViewSettingsApplier {
    fun snapshot(config: WebTestConfig, defaultUserAgent: String): WebViewSettingsSnapshot =
        WebViewSettingsSnapshot(
            javaScriptEnabled = config.javaScriptEnabled,
            domStorageEnabled = config.domStorageEnabled,
            cacheMode = config.cacheMode.toWebSettingsCacheMode(),
            mixedContentMode = config.mixedContentMode.toWebSettingsMixedContentMode(),
            userAgentString = config.toUserAgentString(defaultUserAgent),
        )

    fun apply(webView: WebView, config: WebTestConfig) {
        val settings = webView.settings
        val snapshot = snapshot(config = config, defaultUserAgent = settings.userAgentString.orEmpty())

        settings.javaScriptEnabled = snapshot.javaScriptEnabled
        settings.domStorageEnabled = snapshot.domStorageEnabled
        settings.cacheMode = snapshot.cacheMode
        settings.mixedContentMode = snapshot.mixedContentMode
        snapshot.userAgentString?.let { settings.userAgentString = it }
    }

    private fun WebCacheMode.toWebSettingsCacheMode(): Int =
        when (this) {
            WebCacheMode.DEFAULT -> WebSettings.LOAD_DEFAULT
            WebCacheMode.NO_CACHE,
            WebCacheMode.CLEAR_BEFORE_LOAD -> WebSettings.LOAD_NO_CACHE
        }

    private fun MixedContentMode.toWebSettingsMixedContentMode(): Int =
        when (this) {
            MixedContentMode.ALLOW -> WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            MixedContentMode.BLOCK -> WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }

    private fun WebTestConfig.toUserAgentString(defaultUserAgent: String): String? {
        val customUserAgent = customUserAgent.trim()
        if (userAgentMode == UserAgentMode.CUSTOM && customUserAgent.isNotEmpty()) {
            return customUserAgent
        }

        return if (desktopMode || userAgentMode == UserAgentMode.DESKTOP) {
            defaultUserAgent.toDesktopUserAgent()
        } else {
            null
        }
    }

    private fun String.toDesktopUserAgent(): String {
        val chromeVersion = Regex("""(?:Chrome|CriOS)/[^\s]+""")
            .find(this)
            ?.value
            ?.replace("CriOS/", "Chrome/")
            ?: "Chrome/125.0.0.0"

        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) $chromeVersion Safari/537.36"
    }
}
