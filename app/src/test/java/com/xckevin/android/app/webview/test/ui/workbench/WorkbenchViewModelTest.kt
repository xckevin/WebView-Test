package com.xckevin.android.app.webview.test.ui.workbench

import com.xckevin.android.app.webview.test.FakeHistoryRepository
import com.xckevin.android.app.webview.test.FakeTestCaseRepository
import com.xckevin.android.app.webview.test.debug.DebugState
import com.xckevin.android.app.webview.test.model.HistoryItem
import com.xckevin.android.app.webview.test.model.SourceType
import com.xckevin.android.app.webview.test.model.WebTestCase
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
    private val testCaseRepository = FakeTestCaseRepository()
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
        assertEquals(1L, state.requestedNavigationId)
        assertEquals(1L, state.activeNavigationId)
        assertNull(state.urlError)
    }

    @Test fun explicitLoadChangesRequestedNavigationId() = runTest {
        val viewModel = viewModel()

        viewModel.loadUrl("https://first.example.com")
        val firstState = viewModel.state.value
        viewModel.loadUrl("https://second.example.com")
        val secondState = viewModel.state.value

        assertEquals(1L, firstState.requestedNavigationId)
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

    @Test fun saveCurrentStateCreatesTestCase() = runTest {
        val viewModel = viewModel(clock = { 1234L })
        val config = WebTestConfig.default().copy(
            javaScriptEnabled = false,
            desktopMode = true,
        )

        viewModel.applyConfig(config)
        viewModel.loadUrl("example.com")
        viewModel.saveCurrentAsCase(name = "Home", note = "Regression target")
        advanceUntilIdle()

        val saved = testCaseRepository.upsertedCases.single()
        assertEquals(0L, saved.id)
        assertEquals("Home", saved.name)
        assertEquals("https://example.com", saved.url)
        assertEquals("Regression target", saved.note)
        assertEquals(config, saved.config)
        assertEquals(1234L, saved.createdAt)
        assertEquals(1234L, saved.updatedAt)
        assertNull(saved.lastOpenedAt)
    }

    @Test fun saveWithoutCurrentUrlSetsErrorAndDoesNotSave() = runTest {
        val viewModel = viewModel()

        viewModel.saveCurrentAsCase(name = "Missing", note = "")
        advanceUntilIdle()

        assertNotNull(viewModel.state.value.urlError)
        assertEquals(emptyList<WebTestCase>(), testCaseRepository.upsertedCases)
    }

    @Test fun openCaseReplacesSingleSessionState() = runTest {
        val viewModel = viewModel(clock = { 2000L })
        val openedConfig = WebTestConfig.default().copy(
            cookiesEnabled = false,
            desktopMode = true,
        )
        val testCase = WebTestCase(
            id = 42L,
            name = "Saved",
            url = "https://saved.example.com/path",
            note = "Saved note",
            config = openedConfig,
            createdAt = 100L,
            updatedAt = 100L,
            lastOpenedAt = null,
        )

        viewModel.loadUrl("https://current.example.com")
        viewModel.applyConfig(WebTestConfig.default().copy(javaScriptEnabled = false))
        viewModel.openCase(testCase)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("https://saved.example.com/path", state.currentUrl)
        assertEquals("https://saved.example.com/path", state.urlInput)
        assertEquals("", state.currentTitle)
        assertEquals(openedConfig, state.config)
        assertEquals(2L, state.requestedNavigationId)
        assertEquals(2L, state.activeNavigationId)
        assertEquals(testCase.copy(lastOpenedAt = 2000L), testCaseRepository.upsertedCases.single())
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
        assertEquals(1L, state.requestedNavigationId)
        assertEquals(2L, state.activeNavigationId)
        assertFalse(state.activeNavigationCompleted)
    }

    @Test fun observedPageStartedDoesNotChangeRequestedNavigationId() = runTest {
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

    @Test fun clearDebugLogsEmptiesDebugState() = runTest {
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
        assertFalse(viewModel.state.value.debugState == DebugState())

        viewModel.clearDebugLogs()

        assertEquals(DebugState(), viewModel.state.value.debugState)
    }

    @Test fun toggleFullscreenFlipsState() = runTest {
        val viewModel = viewModel()

        viewModel.toggleFullscreen()
        assertTrue(viewModel.state.value.isFullscreen)

        viewModel.toggleFullscreen()
        assertFalse(viewModel.state.value.isFullscreen)
    }

    private fun viewModel(clock: () -> Long = { 1000L }) =
        WorkbenchViewModel(
            testCaseRepository = testCaseRepository,
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
