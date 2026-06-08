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
                responseHeaders = mapOf("Content-Type" to "application/javascript"),
                responseBody = "not found",
                isMainFrame = false,
                timestamp = 350L,
            )
        )

        assertEquals(1, state.errors.size)
        assertEquals("HttpError", state.errors.single().type)
        assertFalse(state.errors.single().isMainFrame)
        assertEquals("application/javascript", state.errors.single().responseHeaders["Content-Type"])
        assertEquals("not found", state.errors.single().responseBody)
    }

    @Test fun resourceRequestStoresMainFrameCategory() {
        val state = DebugReducer.reduce(
            DebugState(),
            DebugAction.ResourceRequest(
                url = "https://example.com",
                method = "GET",
                requestHeaders = mapOf("User-Agent" to "Test UA"),
                requestBody = "payload",
                isMainFrame = true,
                navigationId = 8L,
                timestamp = 450L,
            )
        )

        val request = state.requests.single()
        assertEquals(RequestCategory.MAIN_FRAME, request.category)
        assertEquals("main-frame", request.categoryLabel)
        assertEquals("GET", request.method)
        assertEquals("example.com", request.host)
        assertEquals("https", request.scheme)
        assertEquals("Test UA", request.requestHeaders["User-Agent"])
        assertEquals("payload", request.requestBody)
    }

    @Test fun resourceRequestDetectsRedirectForSameMainFrameNavigation() {
        val state = DebugReducer.reduce(
            DebugState(page = PageSnapshot(url = "https://first.example.com", navigationId = 8L)),
            DebugAction.ResourceRequest(
                url = "https://second.example.com",
                method = "GET",
                requestHeaders = emptyMap(),
                isMainFrame = true,
                navigationId = 8L,
                timestamp = 451L,
            )
        )

        val request = state.requests.single()
        assertEquals(RequestCategory.REDIRECT, request.category)
        assertEquals("redirect", request.categoryLabel)
    }

    @Test fun timelineStoresEventsInOrderWithSeverity() {
        val state = listOf<DebugAction>(
            DebugAction.PageStarted(url = "https://example.com", navigationId = 2L, timestamp = 100L),
            DebugAction.ConsoleEvent(
                level = "ERROR",
                message = "boom",
                sourceId = "index.js",
                lineNumber = 9,
                navigationId = 2L,
                timestamp = 110L,
            ),
            DebugAction.HttpError(
                url = "https://example.com/api",
                statusCode = 500,
                reason = "Server Error",
                isMainFrame = false,
                navigationId = 2L,
                timestamp = 120L,
            ),
        ).fold(DebugState()) { current, action -> DebugReducer.reduce(current, action) }

        assertEquals(3, state.timeline.size)
        assertEquals(DebugEventType.PAGE, state.timeline[0].type)
        assertEquals(DebugSeverity.INFO, state.timeline[0].severity)
        assertEquals(DebugEventType.CONSOLE, state.timeline[1].type)
        assertEquals(DebugSeverity.ERROR, state.timeline[1].severity)
        assertEquals(DebugEventType.ERROR, state.timeline[2].type)
        assertEquals("HTTP 500: Server Error", state.timeline[2].summary)
    }

    @Test fun clearCurrentNavigationOnlyKeepsOtherNavigationEvents() {
        val state = listOf<DebugAction>(
            DebugAction.ConsoleEvent(level = "LOG", message = "old", navigationId = 1L, timestamp = 1L),
            DebugAction.ConsoleEvent(level = "LOG", message = "current", navigationId = 2L, timestamp = 2L),
            DebugAction.ResourceRequest(
                url = "https://example.com/app.js",
                method = "GET",
                requestHeaders = emptyMap(),
                isMainFrame = false,
                navigationId = 2L,
                timestamp = 3L,
            ),
        ).fold(DebugState()) { current, action -> DebugReducer.reduce(current, action) }

        val cleared = DebugReducer.reduce(
            state,
            DebugAction.Clear(scope = DebugClearScope.CURRENT_NAVIGATION, navigationId = 2L, timestamp = 4L)
        )

        assertEquals(listOf("old"), cleared.consoleLogs.map { it.message })
        assertTrue(cleared.requests.isEmpty())
        assertEquals(listOf(1L), cleared.timeline.map { it.navigationId })
    }

    @Test fun permissionAndFileChooserActionsAreTracked() {
        val state = listOf<DebugAction>(
            DebugAction.UserFlowEvent(
                kind = UserFlowKind.PERMISSION,
                summary = "Web permission granted: camera",
                detail = "policy=ASK_EVERY_TIME",
                navigationId = 4L,
                timestamp = 10L,
            ),
            DebugAction.UserFlowEvent(
                kind = UserFlowKind.FILE_CHOOSER,
                summary = "File chooser opened",
                detail = "accept=image/png",
                navigationId = 4L,
                timestamp = 11L,
            ),
        ).fold(DebugState()) { current, action -> DebugReducer.reduce(current, action) }

        assertEquals(2, state.userFlows.size)
        assertEquals(UserFlowKind.PERMISSION, state.userFlows.first().kind)
        assertEquals(DebugEventType.USER_FLOW, state.timeline.first().type)
        assertEquals("File chooser opened", state.timeline.last().summary)
    }

    @Test fun downloadStatusUpdatesExistingDownloadById() {
        val requested = DebugReducer.reduce(
            DebugState(),
            DebugAction.DownloadRequested(
                url = "https://example.com/file.zip",
                userAgent = "ua",
                contentDisposition = null,
                mimeType = "application/zip",
                contentLength = 10L,
                navigationId = 9L,
                downloadId = 42L,
                fileName = "file.zip",
                status = DownloadStatus.QUEUED,
                timestamp = 500L,
            )
        )

        val updated = DebugReducer.reduce(
            requested,
            DebugAction.DownloadStatusChanged(
                downloadId = 42L,
                status = DownloadStatus.SUCCESS,
                reason = "Saved",
                localUri = "file:///sdcard/Download/file.zip",
                timestamp = 600L,
            )
        )

        val download = updated.downloads.single()
        assertEquals(DownloadStatus.SUCCESS, download.status)
        assertEquals("Saved", download.reason)
        assertEquals("file:///sdcard/Download/file.zip", download.localUri)
        assertEquals(600L, download.updatedAt)
    }

    @Test fun unmatchedDownloadStatusAddsDiagnosticLog() {
        val state = DebugReducer.reduce(
            DebugState(),
            DebugAction.DownloadStatusChanged(
                downloadId = 99L,
                status = DownloadStatus.FAILED,
                reason = "Unknown id",
                localUri = null,
                timestamp = 700L,
            )
        )

        val log = state.consoleLogs.single()
        assertEquals("WARN", log.level)
        assertEquals("Download 99 finished without a matching request: FAILED", log.message)
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
                RequestSnapshot(url = "https://example.com/app.js", method = "GET", isMainFrame = false, timestamp = 3L)
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
        assertTrue(cleared.timeline.isEmpty())
    }
}
