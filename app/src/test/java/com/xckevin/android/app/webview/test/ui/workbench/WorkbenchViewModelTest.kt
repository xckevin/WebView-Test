package com.xckevin.android.app.webview.test.ui.workbench

import com.xckevin.android.app.webview.test.FakeHistoryRepository
import com.xckevin.android.app.webview.test.FakeTestCaseRepository
import com.xckevin.android.app.webview.test.debug.DebugState
import com.xckevin.android.app.webview.test.model.WebTestCase
import com.xckevin.android.app.webview.test.model.WebTestConfig
import com.xckevin.android.app.webview.test.web.WebPageEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
        assertEquals(openedConfig, state.config)
        assertEquals(testCase.copy(lastOpenedAt = 2000L), testCaseRepository.upsertedCases.single())
    }

    @Test fun clearDebugLogsEmptiesDebugState() = runTest {
        val viewModel = viewModel()

        viewModel.onWebPageEvent(WebPageEvent.PageStarted("https://example.com"))
        viewModel.onWebPageEvent(WebPageEvent.ProgressChanged(50))
        assertFalse(viewModel.state.value.debugState == DebugState())

        viewModel.clearDebugLogs()

        assertEquals(DebugState(), viewModel.state.value.debugState)
    }

    private fun viewModel(clock: () -> Long = { 1000L }) =
        WorkbenchViewModel(
            testCaseRepository = testCaseRepository,
            historyRepository = historyRepository,
            clock = clock,
        )
}
