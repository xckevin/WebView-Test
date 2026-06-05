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
            return true
        }

        pendingCallback?.onReceiveValue(null)
        pendingCallback = filePathCallback
        openDocument(acceptedMimeTypes(fileChooserParams)) { uri ->
            onDocumentSelected(uri)
        }
        return true
    }

    fun onDocumentSelected(uri: Uri?) {
        val callback = pendingCallback ?: return
        pendingCallback = null
        callback.onReceiveValue(uri?.let { arrayOf(it) })
    }

    fun cancelPending() {
        pendingCallback?.onReceiveValue(null)
        pendingCallback = null
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
