package com.xckevin.android.app.webview.test.model

import kotlinx.serialization.Serializable

@Serializable
data class WebTestConfig(
    val userAgentMode: UserAgentMode = UserAgentMode.DEFAULT,
    val customUserAgent: String = "",
    val cookiesEnabled: Boolean = true,
    val thirdPartyCookiesEnabled: Boolean = true,
    val cacheMode: WebCacheMode = WebCacheMode.DEFAULT,
    val mixedContentMode: MixedContentMode = MixedContentMode.BLOCK,
    val colorMode: WebColorMode = WebColorMode.FOLLOW_SYSTEM,
    val javaScriptEnabled: Boolean = true,
    val domStorageEnabled: Boolean = true,
    val geolocationPolicy: WebPermissionPolicy = WebPermissionPolicy.ASK_EVERY_TIME,
    val fileChooserPolicy: FeaturePolicy = FeaturePolicy.ALLOW,
    val cameraPolicy: WebPermissionPolicy = WebPermissionPolicy.ASK_EVERY_TIME,
    val microphonePolicy: WebPermissionPolicy = WebPermissionPolicy.ASK_EVERY_TIME,
    val webViewBackFirst: Boolean = true,
    val desktopMode: Boolean = false,
    val startFullscreen: Boolean = false,
) {
    companion object {
        fun default() = WebTestConfig()
    }
}

@Serializable enum class UserAgentMode { DEFAULT, CUSTOM, DESKTOP }
@Serializable enum class WebCacheMode { DEFAULT, NO_CACHE, CLEAR_BEFORE_LOAD }
@Serializable enum class MixedContentMode { ALLOW, BLOCK }
@Serializable enum class WebColorMode { FOLLOW_SYSTEM, FORCE_LIGHT, FORCE_DARK }
@Serializable enum class WebPermissionPolicy { ALLOW, DENY, ASK_EVERY_TIME }
@Serializable enum class FeaturePolicy { ALLOW, DENY }
