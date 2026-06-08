package com.xckevin.android.app.webview.test.ui.workbench

import com.xckevin.android.app.webview.test.FakeHistoryRepository
import com.xckevin.android.app.webview.test.debug.PageStatus
import com.xckevin.android.app.webview.test.debug.DownloadStatus
import com.xckevin.android.app.webview.test.model.HistoryItem
import com.xckevin.android.app.webview.test.model.SourceType
import com.xckevin.android.app.webview.test.model.WebTestConfig
import com.xckevin.android.app.webview.test.web.WebPageEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkbenchViewModelTest {
    private val historyRepository = FakeHistoryRepository()
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUpMainDispatcher() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test fun loadUrlNormalizesInputAndUpdatesState() = runTest {
        val viewModel = viewModel()

        viewModel.onUrlInputChanged(" example.com ")
        viewModel.loadUrl()

        val state = viewModel.state.value
        assertEquals("https://example.com", state.currentUrl)
        assertEquals("https://example.com", state.urlInput)
        assertEquals("", state.currentTitle)
        assertTrue(state.isLoading)
        assertEquals("https://example.com", state.requestedUrl)
        assertEquals(1L, state.requestedNavigationId)
        assertEquals(1L, state.activeNavigationId)
        assertNull(state.urlError)
    }

    @Test fun explicitLoadChangesRequestedUrlAndNavigationId() = runTest {
        val viewModel = viewModel()

        viewModel.loadUrl("https://first.example.com")
        val firstState = viewModel.state.value
        viewModel.loadUrl("https://second.example.com")
        val secondState = viewModel.state.value

        assertEquals("https://first.example.com", firstState.requestedUrl)
        assertEquals(1L, firstState.requestedNavigationId)
        assertEquals("https://second.example.com", secondState.requestedUrl)
        assertEquals(2L, secondState.requestedNavigationId)
        assertEquals(2L, secondState.activeNavigationId)
    }

    @Test fun invalidUrlSetsEditableError() = runTest {
        val viewModel = viewModel()

        viewModel.onUrlInputChanged("ftp://example.com/file")
        viewModel.loadUrl()

        val state = viewModel.state.value
        assertEquals("ftp://example.com/file", state.urlInput)
        assertNull(state.currentUrl)
        assertNotNull(state.urlError)
    }

    @Test fun stalePageFinishedIsIgnoredAndDoesNotInsertHistory() = runTest {
        val viewModel = viewModel(clock = { 3000L })

        viewModel.loadUrl("https://stale.example.com")
        val staleNavigationId = viewModel.state.value.activeNavigationId
        viewModel.loadUrl("https://active.example.com")
        viewModel.onWebPageEvent(
            WebPageEvent.PageFinished(
                url = "https://stale.example.com",
                navigationId = staleNavigationId,
                title = "Stale",
            )
        )
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("https://active.example.com", state.currentUrl)
        assertEquals("", state.currentTitle)
        assertTrue(state.isLoading)
        assertEquals(emptyList<HistoryItem>(), historyRepository.insertedItems)
    }

    @Test fun activePageFinishedSetsTitleAndInsertsHistory() = runTest {
        val viewModel = viewModel(clock = { 4000L })

        viewModel.loadUrl("active.example.com")
        val navigationId = viewModel.state.value.activeNavigationId
        viewModel.onWebPageEvent(
            WebPageEvent.PageFinished(
                url = "https://active.example.com",
                navigationId = navigationId,
                title = "Active title",
            )
        )
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("https://active.example.com", state.currentUrl)
        assertEquals("Active title", state.currentTitle)
        assertFalse(state.isLoading)
        assertEquals(100, state.loadProgress)
        assertEquals(
            HistoryItem(
                id = 0L,
                url = "https://active.example.com",
                title = "Active title",
                sourceType = SourceType.REMOTE_URL,
                visitedAt = 4000L,
            ),
            historyRepository.insertedItems.single(),
        )
    }

    @Test fun newerPageStartedBecomesActiveNavigation() = runTest {
        val viewModel = viewModel()

        viewModel.loadUrl("https://initial.example.com")
        viewModel.onWebPageEvent(WebPageEvent.ProgressChanged(progress = 70, navigationId = 1L))
        viewModel.onWebPageEvent(
            WebPageEvent.PageStarted(
                url = "https://clicked.example.com",
                navigationId = 2L,
            )
        )

        val state = viewModel.state.value
        assertEquals("https://clicked.example.com", state.currentUrl)
        assertEquals("https://clicked.example.com", state.urlInput)
        assertEquals("", state.currentTitle)
        assertTrue(state.isLoading)
        assertEquals(0, state.loadProgress)
        assertEquals("https://initial.example.com", state.requestedUrl)
        assertEquals(1L, state.requestedNavigationId)
        assertEquals(2L, state.activeNavigationId)
        assertFalse(state.activeNavigationCompleted)
    }

    @Test fun observedPageStartedDoesNotChangeRequestedUrlOrNavigationId() = runTest {
        val viewModel = viewModel()

        viewModel.loadUrl("https://initial.example.com")
        viewModel.onWebPageEvent(
            WebPageEvent.PageStarted(
                url = "https://clicked.example.com",
                navigationId = 2L,
            )
        )

        val state = viewModel.state.value
        assertEquals("https://clicked.example.com", state.currentUrl)
        assertEquals("https://initial.example.com", state.requestedUrl)
        assertEquals(1L, state.requestedNavigationId)
        assertEquals(2L, state.activeNavigationId)
    }

    @Test fun olderPageStartedIsIgnored() = runTest {
        val viewModel = viewModel()

        viewModel.loadUrl("https://initial.example.com")
        viewModel.onWebPageEvent(
            WebPageEvent.PageStarted(
                url = "https://clicked.example.com",
                navigationId = 2L,
            )
        )
        viewModel.onWebPageEvent(
            WebPageEvent.PageStarted(
                url = "https://old.example.com",
                navigationId = 1L,
            )
        )

        val state = viewModel.state.value
        assertEquals("https://clicked.example.com", state.currentUrl)
        assertEquals("https://clicked.example.com", state.urlInput)
        assertEquals(2L, state.activeNavigationId)
    }

    @Test fun lateSameNavigationProgressAfterFinishIsIgnored() = runTest {
        val viewModel = viewModel()

        viewModel.loadUrl("https://active.example.com")
        val navigationId = viewModel.state.value.activeNavigationId
        viewModel.onWebPageEvent(WebPageEvent.ProgressChanged(progress = 70, navigationId = navigationId))
        viewModel.onWebPageEvent(
            WebPageEvent.PageFinished(
                url = "https://active.example.com",
                navigationId = navigationId,
                title = "Active title",
            )
        )
        viewModel.onWebPageEvent(WebPageEvent.ProgressChanged(progress = 20, navigationId = navigationId))

        val state = viewModel.state.value
        assertEquals(100, state.loadProgress)
        assertFalse(state.isLoading)
        assertTrue(state.activeNavigationCompleted)
    }

    @Test fun duplicateSameNavigationFinishDoesNotInsertDuplicateHistory() = runTest {
        val viewModel = viewModel(clock = { 4100L })

        viewModel.loadUrl("https://active.example.com")
        val navigationId = viewModel.state.value.activeNavigationId
        val finishEvent = WebPageEvent.PageFinished(
            url = "https://active.example.com",
            navigationId = navigationId,
            title = "Active title",
        )
        viewModel.onWebPageEvent(finishEvent)
        viewModel.onWebPageEvent(finishEvent)
        advanceUntilIdle()

        assertEquals(1, historyRepository.insertedItems.size)
        assertEquals("https://active.example.com", historyRepository.insertedItems.single().url)
    }

    @Test fun zeroNavigationIdEventsAreIgnored() = runTest {
        val viewModel = viewModel()

        viewModel.onWebPageEvent(
            WebPageEvent.PageStarted(
                url = "https://zero.example.com",
                navigationId = 0L,
            )
        )
        viewModel.onWebPageEvent(WebPageEvent.ProgressChanged(progress = 50, navigationId = 0L))
        viewModel.onWebPageEvent(
            WebPageEvent.PageFinished(
                url = "https://zero.example.com",
                navigationId = 0L,
                title = "Zero",
            )
        )
        advanceUntilIdle()

        val state = viewModel.state.value
        assertNull(state.currentUrl)
        assertEquals("", state.urlInput)
        assertEquals(0, state.loadProgress)
        assertEquals(0L, state.activeNavigationId)
        assertFalse(state.isLoading)
        assertEquals(emptyList<HistoryItem>(), historyRepository.insertedItems)
    }

    @Test fun currentPageFinishedWithDifferentUrlUpdatesStateAndHistory() = runTest {
        val viewModel = viewModel(clock = { 5000L })

        viewModel.loadUrl("https://active.example.com")
        val navigationId = viewModel.state.value.activeNavigationId
        viewModel.onWebPageEvent(
            WebPageEvent.PageFinished(
                url = "https://redirected.example.com/path",
                navigationId = navigationId,
                title = "Redirected title",
            )
        )
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("https://redirected.example.com/path", state.currentUrl)
        assertEquals("https://redirected.example.com/path", state.urlInput)
        assertEquals("https://active.example.com", state.requestedUrl)
        assertEquals("Redirected title", state.currentTitle)
        assertFalse(state.isLoading)
        assertEquals(
            HistoryItem(
                id = 0L,
                url = "https://redirected.example.com/path",
                title = "Redirected title",
                sourceType = SourceType.REMOTE_URL,
                visitedAt = 5000L,
            ),
            historyRepository.insertedItems.single(),
        )
    }

    @Test fun staleProgressWithOldNavigationIdIsIgnored() = runTest {
        val viewModel = viewModel()

        viewModel.loadUrl("https://stale.example.com")
        val staleNavigationId = viewModel.state.value.activeNavigationId
        viewModel.loadUrl("https://active.example.com")
        viewModel.onWebPageEvent(WebPageEvent.ProgressChanged(progress = 80, navigationId = staleNavigationId))

        assertEquals(0, viewModel.state.value.loadProgress)
    }

    @Test fun currentProgressWithCurrentNavigationIdUpdatesProgress() = runTest {
        val viewModel = viewModel()

        viewModel.loadUrl("https://active.example.com")
        val navigationId = viewModel.state.value.activeNavigationId
        viewModel.onWebPageEvent(WebPageEvent.ProgressChanged(progress = 250, navigationId = navigationId))

        assertEquals(100, viewModel.state.value.loadProgress)
    }

    @Test fun progressForCurrentNavigationDoesNotMoveBackward() = runTest {
        val viewModel = viewModel()

        viewModel.loadUrl("https://active.example.com")
        val navigationId = viewModel.state.value.activeNavigationId
        viewModel.onWebPageEvent(WebPageEvent.ProgressChanged(progress = 70, navigationId = navigationId))
        viewModel.onWebPageEvent(WebPageEvent.ProgressChanged(progress = 30, navigationId = navigationId))

        assertEquals(70, viewModel.state.value.loadProgress)
    }

    @Test fun progressAt100HidesLoadingButStillAcceptsPageFinished() = runTest {
        val viewModel = viewModel(clock = { 5100L })

        viewModel.loadUrl("https://active.example.com")
        val navigationId = viewModel.state.value.activeNavigationId
        viewModel.onWebPageEvent(WebPageEvent.ProgressChanged(progress = 100, navigationId = navigationId))

        val progressedState = viewModel.state.value
        assertEquals(100, progressedState.loadProgress)
        assertFalse(progressedState.isLoading)
        assertFalse(progressedState.activeNavigationCompleted)

        viewModel.onWebPageEvent(
            WebPageEvent.PageFinished(
                url = "https://active.example.com",
                navigationId = navigationId,
                title = "Active title",
            )
        )
        advanceUntilIdle()

        val finishedState = viewModel.state.value
        assertEquals("Active title", finishedState.currentTitle)
        assertTrue(finishedState.activeNavigationCompleted)
        assertEquals(
            HistoryItem(
                id = 0L,
                url = "https://active.example.com",
                title = "Active title",
                sourceType = SourceType.REMOTE_URL,
                visitedAt = 5100L,
            ),
            historyRepository.insertedItems.single(),
        )
    }

    @Test fun navigationStateChangedUpdatesUrlInputAndBackForwardAvailability() = runTest {
        val viewModel = viewModel()

        viewModel.loadUrl("https://active.example.com")
        val navigationId = viewModel.state.value.activeNavigationId
        viewModel.onWebPageEvent(
            WebPageEvent.PageFinished(
                url = "https://active.example.com",
                navigationId = navigationId,
                title = "Active title",
            )
        )

        viewModel.onWebPageEvent(
            WebPageEvent.NavigationStateChanged(
                navigationId = navigationId,
                url = "https://active.example.com/pushed",
                canGoBack = true,
                canGoForward = false,
            )
        )

        val state = viewModel.state.value
        assertEquals("https://active.example.com/pushed", state.currentUrl)
        assertEquals("https://active.example.com/pushed", state.urlInput)
        assertTrue(state.canGoBack)
        assertFalse(state.canGoForward)
        assertTrue(state.activeNavigationCompleted)
    }

    @Test fun staleNavigationStateChangedIsIgnored() = runTest {
        val viewModel = viewModel()

        viewModel.loadUrl("https://stale.example.com")
        val staleNavigationId = viewModel.state.value.activeNavigationId
        viewModel.loadUrl("https://active.example.com")
        viewModel.onWebPageEvent(
            WebPageEvent.NavigationStateChanged(
                navigationId = staleNavigationId,
                url = "https://stale.example.com/back",
                canGoBack = true,
                canGoForward = true,
            )
        )

        val state = viewModel.state.value
        assertEquals("https://active.example.com", state.currentUrl)
        assertEquals("https://active.example.com", state.urlInput)
        assertFalse(state.canGoBack)
        assertFalse(state.canGoForward)
    }

    @Test fun mainFrameLoadErrorCompletesLoadingWithoutHistoryInsert() = runTest {
        val viewModel = viewModel()

        viewModel.loadUrl("https://active.example.com")
        val navigationId = viewModel.state.value.activeNavigationId
        viewModel.onWebPageEvent(
            WebPageEvent.LoadError(
                url = "https://active.example.com",
                code = -2,
                description = "Host lookup failed",
                navigationId = navigationId,
                isMainFrame = true,
            )
        )
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals(100, state.loadProgress)
        assertTrue(state.activeNavigationCompleted)
        assertEquals(emptyList<HistoryItem>(), historyRepository.insertedItems)
    }

    @Test fun staleMainFrameLoadErrorDoesNotReplaceDebugPageSnapshot() = runTest {
        val viewModel = viewModel()

        viewModel.loadUrl("https://stale.example.com")
        val staleNavigationId = viewModel.state.value.activeNavigationId
        viewModel.loadUrl("https://active.example.com")
        val activeNavigationId = viewModel.state.value.activeNavigationId
        viewModel.onWebPageEvent(
            WebPageEvent.PageStarted(
                url = "https://active.example.com",
                navigationId = activeNavigationId,
            )
        )
        val pageBeforeError = viewModel.state.value.debugState.page

        viewModel.onWebPageEvent(
            WebPageEvent.LoadError(
                url = "https://stale.example.com",
                code = -2,
                description = "Host lookup failed",
                navigationId = staleNavigationId,
                isMainFrame = true,
            )
        )

        val state = viewModel.state.value
        assertTrue(state.isLoading)
        assertEquals(pageBeforeError, state.debugState.page)
        assertEquals("https://stale.example.com", state.debugState.errors.single().url)
        assertTrue(state.debugState.errors.single().isMainFrame)
    }

    @Test fun sslErrorCompletesLoadingWithoutHistoryInsert() = runTest {
        val viewModel = viewModel()

        viewModel.loadUrl("https://active.example.com")
        val navigationId = viewModel.state.value.activeNavigationId
        viewModel.onWebPageEvent(
            WebPageEvent.SslError(
                url = "https://active.example.com",
                primaryError = 3,
                navigationId = navigationId,
            )
        )
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals(100, state.loadProgress)
        assertTrue(state.activeNavigationCompleted)
        assertEquals(emptyList<HistoryItem>(), historyRepository.insertedItems)
    }

    @Test fun staleSslErrorDoesNotReplaceDebugPageSnapshot() = runTest {
        val viewModel = viewModel()

        viewModel.loadUrl("https://stale.example.com")
        val staleNavigationId = viewModel.state.value.activeNavigationId
        viewModel.loadUrl("https://active.example.com")
        val activeNavigationId = viewModel.state.value.activeNavigationId
        viewModel.onWebPageEvent(
            WebPageEvent.PageStarted(
                url = "https://active.example.com",
                navigationId = activeNavigationId,
            )
        )
        val pageBeforeError = viewModel.state.value.debugState.page

        viewModel.onWebPageEvent(
            WebPageEvent.SslError(
                url = "https://stale.example.com",
                primaryError = 3,
                navigationId = staleNavigationId,
            )
        )

        val state = viewModel.state.value
        assertTrue(state.isLoading)
        assertEquals(pageBeforeError, state.debugState.page)
        assertEquals("https://stale.example.com", state.debugState.errors.single().url)
    }

    @Test fun fakeHistoryObserveRecentEmitsAfterInsertAndClear() = runTest {
        val repository = FakeHistoryRepository()
        val recentHistory = repository.observeRecent(limit = 1)
        val first = historyItem(id = 0L, url = "https://first.example.com")
        val second = historyItem(id = 0L, url = "https://second.example.com")

        assertEquals(emptyList<HistoryItem>(), recentHistory.first())

        repository.insert(first)
        assertEquals(listOf(first.copy(id = 1L)), recentHistory.first())

        repository.insert(second)
        assertEquals(listOf(second.copy(id = 2L)), recentHistory.first())

        repository.clear()
        assertEquals(emptyList<HistoryItem>(), recentHistory.first())
    }

    @Test fun clearDebugLogsKeepsPageSnapshotAndClearsLists() = runTest {
        val viewModel = viewModel()

        viewModel.loadUrl("https://example.com")
        val navigationId = viewModel.state.value.activeNavigationId
        viewModel.onWebPageEvent(
            WebPageEvent.PageStarted(
                url = "https://example.com",
                navigationId = navigationId,
            )
        )
        viewModel.onWebPageEvent(WebPageEvent.ProgressChanged(progress = 50, navigationId = navigationId))
        val pageBeforeClear = viewModel.state.value.debugState.page
        assertEquals(PageStatus.Loading, pageBeforeClear.status)
        assertEquals(50, pageBeforeClear.progress)

        viewModel.clearDebugLogs()

        val debugState = viewModel.state.value.debugState
        assertEquals(pageBeforeClear, debugState.page)
        assertTrue(debugState.consoleLogs.isEmpty())
        assertTrue(debugState.errors.isEmpty())
        assertTrue(debugState.requests.isEmpty())
        assertTrue(debugState.downloads.isEmpty())
        assertTrue(debugState.jsResults.isEmpty())
    }

    @Test fun deleteHistoryItemRemovesOneHistoryEntry() = runTest {
        val repository = FakeHistoryRepository(
            initialItems = listOf(
                historyItem(id = 1L, url = "https://first.example.com"),
                historyItem(id = 2L, url = "https://second.example.com"),
            )
        )
        val viewModel = WorkbenchViewModel(
            historyRepository = repository,
            clock = { 1000L },
        )

        viewModel.deleteHistoryItem(historyItem(id = 1L, url = "https://first.example.com"))
        advanceUntilIdle()

        assertEquals(listOf(1L), repository.deletedIds)
        assertEquals(
            listOf(historyItem(id = 2L, url = "https://second.example.com")),
            repository.observeRecent().first(),
        )
    }

    @Test fun recordJavaScriptResultAddsDebugResult() = runTest {
        val viewModel = viewModel(clock = { 5000L })

        viewModel.recordJavaScriptResult(script = "return 1", result = "1")

        val result = viewModel.state.value.debugState.jsResults.single()
        assertEquals("return 1", result.script)
        assertEquals("1", result.result)
        assertFalse(result.isError)
        assertEquals(5000L, result.timestamp)
    }

    @Test fun downloadStatusEventUpdatesDebugDownload() = runTest {
        val viewModel = viewModel(clock = { 5500L })

        viewModel.onWebPageEvent(
            WebPageEvent.DownloadRequested(
                url = "https://example.com/file.zip",
                userAgent = null,
                contentDisposition = null,
                mimeType = "application/zip",
                contentLength = 10L,
                navigationId = 1L,
                downloadId = 77L,
                fileName = "file.zip",
                status = "QUEUED",
            )
        )
        viewModel.onWebPageEvent(
            WebPageEvent.DownloadStatusChanged(
                downloadId = 77L,
                status = "SUCCESS",
                reason = "Complete",
                localUri = "file:///downloads/file.zip",
            )
        )

        val download = viewModel.state.value.debugState.downloads.single()
        assertEquals(DownloadStatus.SUCCESS, download.status)
        assertEquals("Complete", download.reason)
        assertEquals("file:///downloads/file.zip", download.localUri)
        assertEquals(5500L, download.updatedAt)
    }

    @Test fun addDebugMessageAddsInfoConsoleEntry() = runTest {
        val viewModel = viewModel(clock = { 6000L })

        viewModel.addDebugMessage("Cookies cleared")

        val log = viewModel.state.value.debugState.consoleLogs.single()
        assertEquals("INFO", log.level)
        assertEquals("Cookies cleared", log.message)
        assertEquals(6000L, log.timestamp)
    }

    @Test fun localFileLoadTracksLocalSourceTypeAndInsertsLocalHistory() = runTest {
        val viewModel = viewModel(clock = { 7000L })

        viewModel.loadLocalFile("content://provider/page.html")
        val navigationId = viewModel.state.value.activeNavigationId
        viewModel.onWebPageEvent(
            WebPageEvent.PageFinished(
                url = "content://provider/page.html",
                navigationId = navigationId,
                title = "Local page",
            )
        )
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("content://provider/page.html", state.currentUrl)
        assertEquals(SourceType.LOCAL_FILE, state.requestedSourceType)
        assertEquals(SourceType.LOCAL_FILE, state.activeSourceType)
        assertEquals(
            HistoryItem(
                id = 0L,
                url = "content://provider/page.html",
                title = "Local page",
                sourceType = SourceType.LOCAL_FILE,
                visitedAt = 7000L,
            ),
            historyRepository.insertedItems.single(),
        )
    }

    @Test fun refreshKeepsLocalFileSourceType() = runTest {
        val viewModel = viewModel()

        viewModel.loadLocalFile("content://provider/page.html")
        viewModel.refresh()

        val state = viewModel.state.value
        assertEquals("content://provider/page.html", state.requestedUrl)
        assertEquals(SourceType.LOCAL_FILE, state.requestedSourceType)
        assertEquals(SourceType.LOCAL_FILE, state.activeSourceType)
        assertEquals(2L, state.activeNavigationId)
    }

    @Test fun openLocalHistoryBypassesRemoteUrlNormalization() = runTest {
        val viewModel = viewModel()

        viewModel.openHistory(
            HistoryItem(
                id = 5L,
                url = "content://provider/page.html",
                title = "Local page",
                sourceType = SourceType.LOCAL_FILE,
                visitedAt = 123L,
            )
        )

        val state = viewModel.state.value
        assertEquals("content://provider/page.html", state.currentUrl)
        assertEquals("content://provider/page.html", state.requestedUrl)
        assertEquals(SourceType.LOCAL_FILE, state.activeSourceType)
        assertNull(state.urlError)
    }

    @Test fun toggleFullscreenFlipsState() = runTest {
        val viewModel = viewModel()

        viewModel.toggleFullscreen()
        assertTrue(viewModel.state.value.isFullscreen)

        viewModel.toggleFullscreen()
        assertFalse(viewModel.state.value.isFullscreen)
    }

    @Test fun setVideoFullscreenUpdatesState() = runTest {
        val viewModel = viewModel()

        viewModel.setVideoFullscreen(true)
        assertTrue(viewModel.state.value.isVideoFullscreen)

        viewModel.setVideoFullscreen(false)
        assertFalse(viewModel.state.value.isVideoFullscreen)
    }

    @Test fun appFullscreenHidesUrlBarPanelsAndShowsExitOverlay() = runTest {
        assertTrue(WorkbenchState().shouldShowUrlBar())
        assertFalse(WorkbenchState().shouldShowFullscreenExitOverlay())

        val state = WorkbenchState(isFullscreen = true)

        assertFalse(state.shouldShowUrlBar())
        assertFalse(state.shouldShowPanels())
        assertTrue(state.shouldShowFullscreenExitOverlay())
    }

    @Test fun availableWorkbenchPanelsExcludeCases() {
        assertFalse(WorkbenchPanel.entries.any { it.name == "CASES" })
    }

    @Test fun videoFullscreenHidesUrlBarAndShowsExitOverlay() = runTest {
        val state = WorkbenchState(isVideoFullscreen = true)

        assertFalse(state.shouldShowUrlBar())
        assertFalse(state.shouldShowPanels())
        assertTrue(state.shouldShowFullscreenExitOverlay())
    }

    private fun viewModel(clock: () -> Long = { 1000L }) =
        WorkbenchViewModel(
            historyRepository = historyRepository,
            clock = clock,
        )

    private fun historyItem(id: Long, url: String) =
        HistoryItem(
            id = id,
            url = url,
            title = "",
            sourceType = SourceType.REMOTE_URL,
            visitedAt = 10L,
        )
}
