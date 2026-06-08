package com.xckevin.android.app.webview.test.debug

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
}
