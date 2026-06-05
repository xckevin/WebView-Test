package com.xckevin.android.app.webview.test.web

import android.net.Uri
import android.webkit.ValueCallback
import com.xckevin.android.app.webview.test.model.FeaturePolicy
import com.xckevin.android.app.webview.test.model.WebTestConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FileChooserHandlerTest {
    @Test fun denyPolicyCompletesCallbackWithNull() {
        val callback = RecordingValueCallback()
        val messages = mutableListOf<String>()
        val handler = FileChooserHandler(
            configProvider = { WebTestConfig.default().copy(fileChooserPolicy = FeaturePolicy.DENY) },
            openDocument = { _, _ -> error("OpenDocument should not launch when denied") },
            onMessage = messages::add,
        )

        val handled = handler.onShowFileChooser(callback, null)

        assertEquals(true, handled)
        assertEquals(listOf(null), callback.values)
        assertEquals(listOf("File chooser denied by feature policy"), messages)
    }

    @Test fun secondChooserCancelsPreviousPendingCallback() {
        val firstCallback = RecordingValueCallback()
        val secondCallback = RecordingValueCallback()
        val documentResults = mutableListOf<(Uri?) -> Unit>()
        val handler = FileChooserHandler(
            configProvider = { WebTestConfig.default() },
            openDocument = { _, onResult -> documentResults.add(onResult) },
        )

        handler.onShowFileChooser(firstCallback, null)
        handler.onShowFileChooser(secondCallback, null)
        documentResults.last().invoke(null)

        assertEquals(listOf(null), firstCallback.values)
        assertEquals(listOf(null), secondCallback.values)
    }

    @Test fun documentCancelCompletesLatestCallbackOnce() {
        val callback = RecordingValueCallback()
        lateinit var documentResult: (Uri?) -> Unit
        val handler = FileChooserHandler(
            configProvider = { WebTestConfig.default() },
            openDocument = { _, onResult -> documentResult = onResult },
        )

        handler.onShowFileChooser(callback, null)
        documentResult(null)
        documentResult(null)

        assertEquals(1, callback.values.size)
        assertNull(callback.values.single())
    }
}

private class RecordingValueCallback : ValueCallback<Array<Uri>> {
    val values = mutableListOf<Array<Uri>?>()

    override fun onReceiveValue(value: Array<Uri>?) {
        values.add(value)
    }
}
