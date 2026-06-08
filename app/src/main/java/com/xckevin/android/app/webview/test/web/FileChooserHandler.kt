package com.xckevin.android.app.webview.test.web

import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import com.xckevin.android.app.webview.test.model.FeaturePolicy
import com.xckevin.android.app.webview.test.model.WebTestConfig

class FileChooserHandler(
    private val configProvider: () -> WebTestConfig,
    private val openDocument: (Array<String>, (Uri?) -> Unit) -> Unit,
    private val onMessage: (String) -> Unit = {},
    private val onUserFlow: (String, String) -> Unit = { _, _ -> },
) {
    private var pendingCallback: ValueCallback<Array<Uri>>? = null

    fun onShowFileChooser(
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: WebChromeClient.FileChooserParams?,
    ): Boolean {
        if (filePathCallback == null) return false

        if (configProvider().fileChooserPolicy == FeaturePolicy.DENY) {
            filePathCallback.onReceiveValue(null)
            onMessage("File chooser denied by feature policy")
            onUserFlow("File chooser denied", "policy=DENY")
            return true
        }

        pendingCallback?.onReceiveValue(null)
        pendingCallback = filePathCallback
        val mimeTypes = acceptedMimeTypes(fileChooserParams)
        onUserFlow("File chooser opened", "accept=${mimeTypes.joinToString()}")
        openDocument(mimeTypes) { uri ->
            onDocumentSelected(uri)
        }
        return true
    }

    fun onDocumentSelected(uri: Uri?) {
        val callback = pendingCallback ?: return
        pendingCallback = null
        callback.onReceiveValue(uri?.let { arrayOf(it) })
        onUserFlow(
            if (uri == null) "File chooser canceled" else "File chooser selected",
            uri?.toString().orEmpty(),
        )
    }

    fun cancelPending() {
        pendingCallback?.onReceiveValue(null)
        pendingCallback = null
        onUserFlow("File chooser canceled", "host released")
    }

    private fun acceptedMimeTypes(fileChooserParams: WebChromeClient.FileChooserParams?): Array<String> {
        val acceptedTypes = fileChooserParams
            ?.acceptTypes
            .orEmpty()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

        return acceptedTypes.ifEmpty { listOf("*/*") }.toTypedArray()
    }
}
