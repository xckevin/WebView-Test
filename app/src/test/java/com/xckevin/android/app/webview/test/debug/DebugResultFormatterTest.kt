package com.xckevin.android.app.webview.test.debug

import com.xckevin.android.app.webview.test.ui.workbench.DebugStorageSource
import com.xckevin.android.app.webview.test.ui.workbench.parseDebugStorageRows
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugResultFormatterTest {
    @Test fun formatsCookiePairs() {
        val lines = DebugResultFormatter.formatCookieHeader("sid=abc; theme=dark")

        assertTrue(lines.contains("sid = abc"))
        assertTrue(lines.contains("theme = dark"))
    }

    @Test fun formatsWrappedSourceWithLineNumbers() {
        val lines = DebugResultFormatter.formatScriptResult(
            """{"ok":true,"value":"<html>\n<body></body>\n</html>"}"""
        )

        assertTrue(lines.contains("1: <html>"))
        assertTrue(lines.contains("2: <body></body>"))
    }

    @Test fun formatsElementSummariesAsReadableLines() {
        val lines = DebugResultFormatter.formatScriptResult(
            """{"ok":true,"value":[{"tag":"button","id":"submit","className":"primary","text":"Send","visible":true}]}"""
        )

        assertTrue(lines.any { it.contains("button#submit.primary") })
        assertTrue(lines.any { it.contains("text=Send") })
    }

    @Test fun parsesWrappedStorageObjectAsKeyValueRows() {
        val rows = parseDebugStorageRows(
            """{"ok":true,"value":{"sid":"abc","theme":"dark"}}""",
            DebugStorageSource.LocalStorage,
        )

        assertEquals(2, rows.size)
        assertEquals("sid", rows[0].key)
        assertEquals("abc", rows[0].value)
        assertEquals(DebugStorageSource.LocalStorage, rows[0].source)
    }

    @Test fun parsesWrappedCookieArrayWithMetadata() {
        val rows = parseDebugStorageRows(
            """{"ok":true,"value":[{"name":"sid","value":"abc","length":3}]}""",
            DebugStorageSource.Cookie,
        )

        assertEquals(1, rows.size)
        assertEquals("sid", rows[0].key)
        assertEquals("abc", rows[0].value)
        assertEquals(DebugStorageSource.Cookie, rows[0].source)
        assertEquals("length=3", rows[0].metadata)
    }

    @Test fun parsesDocumentCookieHeaderAsCookieRows() {
        val rows = parseDebugStorageRows("sid=abc; theme=dark", DebugStorageSource.Cookie)

        assertEquals(2, rows.size)
        assertEquals("sid", rows[0].key)
        assertEquals("abc", rows[0].value)
        assertEquals("theme", rows[1].key)
        assertEquals("dark", rows[1].value)
    }
}
