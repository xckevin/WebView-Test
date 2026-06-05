package com.xckevin.android.app.webview.test.ui.workbench

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xckevin.android.app.webview.test.data.HistoryRepository
import com.xckevin.android.app.webview.test.data.TestCaseRepository
import com.xckevin.android.app.webview.test.debug.DebugLogEntry
import com.xckevin.android.app.webview.test.debug.DebugState
import com.xckevin.android.app.webview.test.model.HistoryItem
import com.xckevin.android.app.webview.test.model.SourceType
import com.xckevin.android.app.webview.test.model.WebTestCase
import com.xckevin.android.app.webview.test.model.WebTestConfig
import com.xckevin.android.app.webview.test.util.UrlNormalizer
import com.xckevin.android.app.webview.test.web.WebPageEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WorkbenchViewModel(
    private val testCaseRepository: TestCaseRepository,
    private val historyRepository: HistoryRepository,
    private val clock: () -> Long = { System.currentTimeMillis() },
) : ViewModel() {
    private val _state = MutableStateFlow(WorkbenchState())
    val state: StateFlow<WorkbenchState> = _state.asStateFlow()
    private var nextNavigationId = 1L

    fun onUrlInputChanged(value: String) {
        _state.update { it.copy(urlInput = value, urlError = null) }
    }

    fun loadUrl() {
        val rawUrl = state.value.urlInput
        val normalizedUrl = UrlNormalizer.normalizeRemoteUrl(rawUrl)
        if (normalizedUrl == null) {
            _state.update { it.copy(urlError = "Enter a valid http or https URL") }
            return
        }

        val navigationId = nextNavigationId()
        _state.update {
            it.copy(
                urlInput = normalizedUrl,
                currentUrl = normalizedUrl,
                currentTitle = "",
                isLoading = true,
                loadProgress = 0,
                activeNavigationId = navigationId,
                activeNavigationCompleted = false,
                urlError = null,
            )
        }
    }

    fun loadUrl(rawUrl: String) {
        onUrlInputChanged(rawUrl)
        loadUrl()
    }

    fun applyConfig(config: WebTestConfig) {
        _state.update { it.copy(config = config) }
    }

    fun saveCurrentAsCase(name: String, note: String) {
        val currentState = state.value
        val currentUrl = currentState.currentUrl
        if (currentUrl == null) {
            _state.update { it.copy(urlError = "Load a valid URL before saving a case") }
            return
        }

        val now = clock()
        val testCase = WebTestCase(
            id = 0L,
            name = name,
            url = currentUrl,
            note = note,
            config = currentState.config,
            createdAt = now,
            updatedAt = now,
            lastOpenedAt = null,
        )
        viewModelScope.launch {
            testCaseRepository.upsert(testCase)
        }
    }

    fun openCase(testCase: WebTestCase) {
        val navigationId = nextNavigationId()
        _state.update {
            it.copy(
                urlInput = testCase.url,
                currentUrl = testCase.url,
                currentTitle = "",
                config = testCase.config,
                isLoading = true,
                loadProgress = 0,
                activeNavigationId = navigationId,
                activeNavigationCompleted = false,
                urlError = null,
            )
        }
        viewModelScope.launch {
            testCaseRepository.upsert(testCase.copy(lastOpenedAt = clock()))
        }
    }

    fun onWebPageEvent(event: WebPageEvent) {
        when (event) {
            is WebPageEvent.PageStarted -> {
                val currentState = state.value
                if (event.navigationId <= 0L) return
                if (event.navigationId < currentState.activeNavigationId) return
                if (
                    event.navigationId == currentState.activeNavigationId &&
                    currentState.activeNavigationCompleted
                ) {
                    return
                }

                nextNavigationId = maxOf(nextNavigationId, event.navigationId + 1)
                _state.update {
                    it.copy(
                        currentUrl = event.url,
                        urlInput = event.url,
                        currentTitle = "",
                        isLoading = true,
                        loadProgress = 0,
                        activeNavigationId = event.navigationId,
                        activeNavigationCompleted = false,
                        debugState = it.debugState.withLog("Page started: ${event.url}"),
                    )
                }
            }

            is WebPageEvent.PageFinished -> {
                if (!event.navigationId.isCurrentLoadingNavigation()) return
                val title = event.title.orEmpty()
                _state.update {
                    it.copy(
                        currentUrl = event.url,
                        urlInput = event.url,
                        currentTitle = title,
                        isLoading = false,
                        loadProgress = 100,
                        activeNavigationCompleted = true,
                        debugState = it.debugState.withLog("Page finished: ${event.url}"),
                    )
                }
                viewModelScope.launch {
                    historyRepository.insert(
                        HistoryItem(
                            id = 0L,
                            url = event.url,
                            title = title,
                            sourceType = SourceType.REMOTE_URL,
                            visitedAt = clock(),
                        )
                    )
                }
            }

            is WebPageEvent.ProgressChanged -> {
                if (!event.navigationId.isCurrentLoadingNavigation()) return
                _state.update {
                    it.copy(
                        loadProgress = event.progress.coerceIn(0, 100),
                        debugState = it.debugState.withLog("Progress changed: ${event.progress}"),
                    )
                }
            }

            is WebPageEvent.Console -> {
                _state.update {
                    it.copy(debugState = it.debugState.withLog("Console ${event.level}: ${event.message}"))
                }
            }

            is WebPageEvent.LoadError -> {
                _state.update {
                    it.copy(debugState = it.debugState.withLog("Load error ${event.code}: ${event.description}"))
                }
            }

            is WebPageEvent.HttpError -> {
                _state.update {
                    it.copy(debugState = it.debugState.withLog("HTTP error ${event.statusCode}: ${event.reason}"))
                }
            }

            is WebPageEvent.SslError -> {
                _state.update {
                    it.copy(debugState = it.debugState.withLog("SSL error: ${event.primaryError}"))
                }
            }

            is WebPageEvent.ResourceRequest -> Unit

            is WebPageEvent.DownloadRequested -> {
                _state.update {
                    it.copy(debugState = it.debugState.withLog("Download requested: ${event.url}"))
                }
            }
        }
    }

    fun clearDebugLogs() {
        _state.update { it.copy(debugState = DebugState()) }
    }

    fun toggleFullscreen() {
        _state.update { it.copy(isFullscreen = !it.isFullscreen) }
    }

    private fun DebugState.withLog(message: String): DebugState =
        copy(logs = logs + DebugLogEntry(message = message, timestamp = clock()))

    private fun nextNavigationId(): Long = nextNavigationId++

    private fun Long.isCurrentLoadingNavigation(): Boolean {
        val currentState = state.value
        return this > 0L &&
            this == currentState.activeNavigationId &&
            currentState.isLoading &&
            !currentState.activeNavigationCompleted
    }
}
