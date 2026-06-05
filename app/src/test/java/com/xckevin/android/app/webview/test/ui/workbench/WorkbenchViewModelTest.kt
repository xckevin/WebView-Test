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
        assertNull(state.urlError)
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
        assertEquals(testCase.copy(lastOpenedAt = 2000L), testCaseRepository.upsertedCases.single())
    }

    @Test fun stalePageFinishedIsIgnoredAndDoesNotInsertHistory() = runTest {
        val viewModel = viewModel(clock = { 3000L })

        viewModel.loadUrl("https://active.example.com")
        viewModel.onWebPageEvent(
            WebPageEvent.PageFinished(
                url = "https://stale.example.com",
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
        viewModel.onWebPageEvent(
            WebPageEvent.PageFinished(
                url = "https://active.example.com",
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

    @Test fun progressChangedUpdatesOnlyActiveLoadingNavigation() = runTest {
        val viewModel = viewModel()

        viewModel.onWebPageEvent(WebPageEvent.ProgressChanged(progress = 40))
        assertEquals(0, viewModel.state.value.loadProgress)

        viewModel.loadUrl("https://active.example.com")
        viewModel.onWebPageEvent(WebPageEvent.ProgressChanged(progress = 250))
        assertEquals(100, viewModel.state.value.loadProgress)
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
        viewModel.onWebPageEvent(WebPageEvent.PageStarted("https://example.com"))
        viewModel.onWebPageEvent(WebPageEvent.ProgressChanged(50))
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
