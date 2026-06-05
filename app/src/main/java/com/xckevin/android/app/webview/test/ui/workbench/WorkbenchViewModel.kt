package com.xckevin.android.app.webview.test.ui.workbench

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xckevin.android.app.webview.test.data.CaseImportExport
import com.xckevin.android.app.webview.test.data.HistoryRepository
import com.xckevin.android.app.webview.test.data.TestCaseRepository
import com.xckevin.android.app.webview.test.debug.DebugAction
import com.xckevin.android.app.webview.test.debug.DebugReducer
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WorkbenchViewModel(
    private val testCaseRepository: TestCaseRepository,
    private val historyRepository: HistoryRepository,
    private val clock: () -> Long = { System.currentTimeMillis() },
) : ViewModel() {
    private val _state = MutableStateFlow(WorkbenchState())
    val state: StateFlow<WorkbenchState> = _state.asStateFlow()
    val cases = testCaseRepository.observeAll()
    val history = historyRepository.observeRecent()
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
                requestedUrl = normalizedUrl,
                requestedNavigationId = navigationId,
                activeNavigationId = navigationId,
                activeNavigationCompleted = false,
                isFullscreen = it.config.startFullscreen,
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

    fun selectPanel(panel: WorkbenchPanel) {
        _state.update { it.copy(selectedPanel = panel) }
    }

    fun refresh() {
        val currentState = state.value
        val url = currentState.currentUrl ?: currentState.requestedUrl ?: return
        val navigationId = nextNavigationId()
        _state.update {
            it.copy(
                urlInput = url,
                currentUrl = url,
                currentTitle = "",
                isLoading = true,
                loadProgress = 0,
                requestedUrl = url,
                requestedNavigationId = navigationId,
                activeNavigationId = navigationId,
                activeNavigationCompleted = false,
                urlError = null,
            )
        }
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

    fun deleteCase(testCase: WebTestCase) {
        viewModelScope.launch {
            testCaseRepository.delete(testCase)
        }
    }

    fun importCasesJson(raw: String) {
        viewModelScope.launch {
            runCatching {
                val incoming = CaseImportExport.importCases(raw)
                val existing = testCaseRepository.observeAll().first()
                val conflictKeys = CaseImportExport.findConflicts(existing, incoming)
                    .map { it.incoming.name.trim() to it.incoming.url.trim() }
                    .toSet()
                val casesToImport = incoming.filterNot { it.name.trim() to it.url.trim() in conflictKeys }
                casesToImport.forEach { testCaseRepository.upsert(it) }
                casesToImport.size to conflictKeys.size
            }.onSuccess { (importedCount, skippedCount) ->
                _state.update {
                    it.copy(
                        selectedPanel = WorkbenchPanel.CASES,
                        debugState = it.debugState.reduceDebug(
                            DebugAction.DebugMessage(
                                message = "Imported $importedCount cases, skipped $skippedCount conflicts",
                            )
                        ),
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        selectedPanel = WorkbenchPanel.CASES,
                        debugState = it.debugState.reduceDebug(
                            DebugAction.DebugMessage(
                                message = "Case import failed: ${error.message.orEmpty()}",
                            )
                        ),
                    )
                }
            }
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
                requestedUrl = testCase.url,
                requestedNavigationId = navigationId,
                activeNavigationId = navigationId,
                activeNavigationCompleted = false,
                isFullscreen = testCase.config.startFullscreen,
                urlError = null,
            )
        }
        viewModelScope.launch {
            testCaseRepository.upsert(testCase.copy(lastOpenedAt = clock()))
        }
    }

    fun openHistory(item: HistoryItem) {
        loadUrl(item.url)
    }

    fun clearHistory() {
        viewModelScope.launch {
            historyRepository.clear()
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
                        debugState = it.debugState.reduceDebug(
                            DebugAction.PageStarted(
                                url = event.url,
                                navigationId = event.navigationId,
                            )
                        ),
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
                        debugState = it.debugState.reduceDebug(
                            DebugAction.PageFinished(
                                url = event.url,
                                title = title,
                                navigationId = event.navigationId,
                            )
                        ),
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
                        debugState = it.debugState.reduceDebug(
                            DebugAction.ProgressChanged(
                                navigationId = event.navigationId,
                                progress = event.progress,
                            )
                        ),
                    )
                }
            }

            is WebPageEvent.Console -> {
                _state.update {
                    it.copy(
                        debugState = it.debugState.reduceDebug(
                            DebugAction.ConsoleEvent(
                                level = event.level,
                                message = event.message,
                                sourceId = event.sourceId,
                                lineNumber = event.lineNumber,
                                navigationId = event.navigationId,
                            )
                        )
                    )
                }
            }

            is WebPageEvent.LoadError -> {
                _state.update {
                    val completesActiveLoad =
                        event.isMainFrame && event.navigationId.isCurrentLoadingNavigation(it)
                    val debugState = it.debugState.reduceDebug(
                        DebugAction.LoadError(
                            url = event.url,
                            code = event.code,
                            description = event.description,
                            navigationId = event.navigationId,
                            isMainFrame = event.isMainFrame,
                            updatesPage = completesActiveLoad,
                        )
                    )
                    if (completesActiveLoad) {
                        it.copy(
                            isLoading = false,
                            loadProgress = 100,
                            activeNavigationCompleted = true,
                            debugState = debugState,
                        )
                    } else {
                        it.copy(debugState = debugState)
                    }
                }
            }

            is WebPageEvent.HttpError -> {
                _state.update {
                    it.copy(
                        debugState = it.debugState.reduceDebug(
                            DebugAction.HttpError(
                                url = event.url,
                                statusCode = event.statusCode,
                                reason = event.reason,
                                navigationId = event.navigationId,
                                isMainFrame = event.isMainFrame,
                            )
                        )
                    )
                }
            }

            is WebPageEvent.SslError -> {
                _state.update {
                    val completesActiveLoad = event.navigationId.isCurrentLoadingNavigation(it)
                    val debugState = it.debugState.reduceDebug(
                        DebugAction.SslError(
                            url = event.url,
                            primaryError = event.primaryError,
                            navigationId = event.navigationId,
                            updatesPage = completesActiveLoad,
                        )
                    )
                    if (completesActiveLoad) {
                        it.copy(
                            isLoading = false,
                            loadProgress = 100,
                            activeNavigationCompleted = true,
                            debugState = debugState,
                        )
                    } else {
                        it.copy(debugState = debugState)
                    }
                }
            }

            is WebPageEvent.ResourceRequest -> {
                _state.update {
                    it.copy(
                        debugState = it.debugState.reduceDebug(
                            DebugAction.ResourceRequest(
                                url = event.url,
                                isMainFrame = event.isMainFrame,
                                navigationId = event.navigationId,
                            )
                        )
                    )
                }
            }

            is WebPageEvent.DownloadRequested -> {
                _state.update {
                    it.copy(
                        debugState = it.debugState.reduceDebug(
                            DebugAction.DownloadRequested(
                                url = event.url,
                                userAgent = event.userAgent,
                                contentDisposition = event.contentDisposition,
                                mimeType = event.mimeType,
                                contentLength = event.contentLength,
                                navigationId = event.navigationId,
                            )
                        )
                    )
                }
            }
        }
    }

    fun clearDebugLogs() {
        _state.update { it.copy(debugState = it.debugState.reduceDebug(DebugAction.ClearLogs(timestamp = clock()))) }
    }

    fun addDebugMessage(message: String) {
        _state.update {
            it.copy(
                debugState = it.debugState.reduceDebug(
                    DebugAction.DebugMessage(message = message)
                )
            )
        }
    }

    fun recordJavaScriptResult(script: String, result: String, isError: Boolean = false) {
        _state.update {
            it.copy(
                debugState = it.debugState.reduceDebug(
                    DebugAction.JavaScriptResult(
                        script = script,
                        result = result,
                        isError = isError,
                    )
                )
            )
        }
    }

    fun toggleFullscreen() {
        _state.update { it.copy(isFullscreen = !it.isFullscreen) }
    }

    private fun DebugState.reduceDebug(action: DebugAction): DebugState {
        val timestampedAction = when (action) {
            is DebugAction.ClearLogs -> action
            is DebugAction.DebugMessage -> action.copy(timestamp = clock())
            is DebugAction.ConsoleEvent -> action.copy(timestamp = clock())
            is DebugAction.PageStarted -> action.copy(timestamp = clock())
            is DebugAction.PageFinished -> action.copy(timestamp = clock())
            is DebugAction.ProgressChanged -> action.copy(timestamp = clock())
            is DebugAction.LoadError -> action.copy(timestamp = clock())
            is DebugAction.HttpError -> action.copy(timestamp = clock())
            is DebugAction.SslError -> action.copy(timestamp = clock())
            is DebugAction.ResourceRequest -> action.copy(timestamp = clock())
            is DebugAction.DownloadRequested -> action.copy(timestamp = clock())
            is DebugAction.JavaScriptResult -> action.copy(timestamp = clock())
        }
        return DebugReducer.reduce(this, timestampedAction)
    }

    private fun nextNavigationId(): Long = nextNavigationId++

    private fun Long.isCurrentLoadingNavigation(currentState: WorkbenchState = state.value): Boolean {
        return this > 0L &&
            this == currentState.activeNavigationId &&
            currentState.isLoading &&
            !currentState.activeNavigationCompleted
    }
}
