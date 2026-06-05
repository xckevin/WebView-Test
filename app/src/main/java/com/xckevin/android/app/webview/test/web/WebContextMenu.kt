package com.xckevin.android.app.webview.test.web

import android.webkit.WebView

data class WebContextMenuTarget(
    val url: String,
    val label: String,
)

class WebContextMenu(
    private val webView: WebView,
    private val onTarget: (WebContextMenuTarget) -> Unit,
) {
    fun attach() {
        webView.setOnLongClickListener {
            val target = webView.hitTestResult.toContextMenuTarget() ?: return@setOnLongClickListener false
            onTarget(target)
            true
        }
    }

    fun detach() {
        webView.setOnLongClickListener(null)
    }

    private fun WebView.HitTestResult?.toContextMenuTarget(): WebContextMenuTarget? {
        val result = this ?: return null
        val url = result.extra?.takeIf { it.isNotBlank() } ?: return null
        val label = when (result.type) {
            WebView.HitTestResult.SRC_ANCHOR_TYPE -> "Link"
            WebView.HitTestResult.IMAGE_TYPE -> "Image"
            WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> "Image link"
            WebView.HitTestResult.EMAIL_TYPE,
            WebView.HitTestResult.PHONE_TYPE,
            WebView.HitTestResult.GEO_TYPE -> return null
            else -> "Resource"
        }
        return WebContextMenuTarget(url = url, label = label)
    }
}
