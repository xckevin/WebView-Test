package com.xckevin.android.app.webview.test.debug

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugReducerTest {
    @Test fun consoleEventIsAppended() {
        val state = DebugReducer.reduce(
            DebugState(),
            DebugAction.ConsoleEvent(
                level = "LOG",
                message = "hello",
                sourceId = "index.html",
                lineNumber = 12,
                navigationId = 7L,
                timestamp = 100L,
            )
        )

        assertEquals(1, state.consoleLogs.size)
        assertEquals(
            ConsoleLog(
                level = "LOG",
                message = "hello",
                sourceId = "index.html",
                lineNumber = 12,
                navigationId = 7L,
                timestamp = 100L,
            ),
            state.consoleLogs.single(),
        )
    }

    @Test fun loadErrorIsAppended() {
        val state = DebugReducer.reduce(
            DebugState(),
            DebugAction.LoadError(
                url = "https://example.com",
                code = -2,
                description = "Host lookup failed",
                navigationId = 3L,
                isMainFrame = true,
                timestamp = 200L,
            )
        )

        assertEquals(1, state.errors.size)
        assertEquals("LoadError", state.errors.single().type)
        assertEquals("Host lookup failed", state.errors.single().message)
        assertEquals(PageStatus.Error, state.page.status)
        assertEquals(100, state.page.progress)
    }

    @Test fun progressUpdatesPageState() {
        val state = DebugReducer.reduce(
            DebugState(page = PageSnapshot(url = "https://example.com", navigationId = 4L)),
            DebugAction.ProgressChanged(
                navigationId = 4L,
                progress = 250,
                timestamp = 300L,
            )
        )

        assertEquals(4L, state.page.navigationId)
        assertEquals(100, state.page.progress)
        assertEquals(PageStatus.Loading, state.page.status)
        assertEquals(300L, state.page.timestamp)
    }

    @Test fun httpErrorPreservesFrameScope() {
        val state = DebugReducer.reduce(
            DebugState(),
            DebugAction.HttpError(
                url = "https://example.com/app.js",
                statusCode = 404,
                reason = "Not Found",
                isMainFrame = false,
                timestamp = 350L,
            )
        )

        assertEquals(1, state.errors.size)
        assertEquals("HttpError", state.errors.single().type)
        assertFalse(state.errors.single().isMainFrame)
    }


    @Test fun clearLogsKeepsCurrentPageState() {
        val page = PageSnapshot(
            url = "https://example.com",
            title = "Example",
            navigationId = 5L,
            progress = 80,
            status = PageStatus.Loading,
            timestamp = 400L,
        )
        val state = DebugState(
            consoleLogs = listOf(
                ConsoleLog(level = "LOG", message = "one", timestamp = 1L)
            ),
            errors = listOf(
                PageError(type = "LoadError", message = "failed", timestamp = 2L)
            ),
            page = page,
            requests = listOf(
                RequestSnapshot(url = "https://example.com/app.js", isMainFrame = false, timestamp = 3L)
            ),
            downloads = listOf(
                DownloadSnapshot(url = "https://example.com/file.zip", timestamp = 4L)
            ),
            jsResults = listOf(
                JsExecutionResult(script = "return 1", result = "1", timestamp = 5L)
            ),
        )

        val cleared = DebugReducer.reduce(state, DebugAction.ClearLogs(timestamp = 500L))

        assertEquals(page, cleared.page)
        assertTrue(cleared.consoleLogs.isEmpty())
        assertTrue(cleared.errors.isEmpty())
        assertTrue(cleared.requests.isEmpty())
        assertTrue(cleared.downloads.isEmpty())
        assertTrue(cleared.jsResults.isEmpty())
    }
}
