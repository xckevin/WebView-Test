package com.xckevin.android.app.webview.test.ui.workbench

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugStorageTableTest {
    @Test
    fun parsesWrappedJsonObjectRows() {
        val rows = parseDebugStorageRows(
            """{"ok":true,"value":{"token":"abc","theme":"dark"}}""",
            DebugStorageSource.LocalStorage,
        )

        assertEquals(2, rows.size)
        assertEquals("token", rows[0].key)
        assertEquals("abc", rows[0].value)
        assertEquals(DebugStorageSource.LocalStorage, rows[0].source)
        assertEquals("theme", rows[1].key)
        assertEquals("dark", rows[1].value)
    }

    @Test
    fun parsesCookieStringRows() {
        val rows = parseDebugStorageRows("sid=abc; theme=dark", DebugStorageSource.Cookie)

        assertEquals(2, rows.size)
        assertEquals("sid", rows[0].key)
        assertEquals("abc", rows[0].value)
        assertEquals(DebugStorageSource.Cookie, rows[0].source)
        assertEquals("theme", rows[1].key)
        assertEquals("dark", rows[1].value)
    }

    @Test
    fun copyTextContainsSourceKeyAndValue() {
        val text = debugStorageRowsCopyText(
            listOf(
                DebugStorageRow(
                    source = DebugStorageSource.SessionStorage,
                    key = "sessionId",
                    value = "xyz",
                ),
            ),
        )

        assertTrue(text.contains("source=sessionStorage"))
        assertTrue(text.contains("key=sessionId"))
        assertTrue(text.contains("value=xyz"))
    }
}
