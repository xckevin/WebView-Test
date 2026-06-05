package com.xckevin.android.app.webview.test.web

import android.view.View
import android.webkit.WebChromeClient

class FullscreenVideoHandler(
    private val onFullscreenViewChanged: (View?) -> Unit,
) {
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    fun onShowCustomView(view: View?, callback: WebChromeClient.CustomViewCallback?) {
        if (view == null) {
            callback?.onCustomViewHidden()
            return
        }

        if (customView != null) {
            onHideCustomView()
        }

        customView = view
        customViewCallback = callback
        onFullscreenViewChanged(view)
    }

    fun onHideCustomView() {
        val callback = customViewCallback
        customView = null
        customViewCallback = null
        onFullscreenViewChanged(null)
        callback?.onCustomViewHidden()
    }

    fun isFullscreenActive(): Boolean = customView != null
}
