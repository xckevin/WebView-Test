package com.xckevin.android.app.webview.test.debug

object DebugReducer {
    const val MaxEntries = 500

    fun reduce(state: DebugState, action: DebugAction): DebugState {
        return when (action) {
            is DebugAction.ClearLogs -> state.copy(
                consoleLogs = emptyList(),
                errors = emptyList(),
                requests = emptyList(),
                downloads = emptyList(),
                jsResults = emptyList(),
            )

            is DebugAction.DebugMessage -> state.copy(
                consoleLogs = state.consoleLogs.appendCapped(
                    ConsoleLog(
                        level = "INFO",
                        message = action.message,
                        sourceId = "workbench",
                        timestamp = action.timestamp,
                    )
                )
            )

            is DebugAction.ConsoleEvent -> state.copy(
                consoleLogs = state.consoleLogs.appendCapped(
                    ConsoleLog(
                        level = action.level,
                        message = action.message,
                        sourceId = action.sourceId,
                        lineNumber = action.lineNumber,
                        navigationId = action.navigationId,
                        timestamp = action.timestamp,
                    )
                )
            )

            is DebugAction.PageStarted -> state.copy(
                page = PageSnapshot(
                    url = action.url,
                    title = "",
                    navigationId = action.navigationId,
                    progress = 0,
                    status = PageStatus.Loading,
                    timestamp = action.timestamp,
                )
            )

            is DebugAction.PageFinished -> state.copy(
                page = state.page.copy(
                    url = action.url,
                    title = action.title,
                    navigationId = action.navigationId,
                    progress = 100,
                    status = PageStatus.Finished,
                    timestamp = action.timestamp,
                )
            )

            is DebugAction.ProgressChanged -> state.copy(
                page = state.page.copy(
                    navigationId = action.navigationId,
                    progress = action.progress.coerceIn(0, 100),
                    status = PageStatus.Loading,
                    timestamp = action.timestamp,
                )
            )

            is DebugAction.LoadError -> state.copy(
                errors = state.errors.appendCapped(
                    PageError(
                        type = "LoadError",
                        message = action.description,
                        url = action.url,
                        code = action.code,
                        navigationId = action.navigationId,
                        isMainFrame = action.isMainFrame,
                        timestamp = action.timestamp,
                    )
                ),
                page = if (action.isMainFrame && action.updatesPage) {
                    state.page.copy(
                        url = action.url ?: state.page.url,
                        navigationId = action.navigationId,
                        progress = 100,
                        status = PageStatus.Error,
                        timestamp = action.timestamp,
                    )
                } else {
                    state.page
                },
            )

            is DebugAction.HttpError -> state.copy(
                errors = state.errors.appendCapped(
                    PageError(
                        type = "HttpError",
                        message = action.reason,
                        url = action.url,
                        statusCode = action.statusCode,
                        navigationId = action.navigationId,
                        isMainFrame = action.isMainFrame,
                        timestamp = action.timestamp,
                    )
                )
            )

            is DebugAction.SslError -> state.copy(
                errors = state.errors.appendCapped(
                    PageError(
                        type = "SslError",
                        message = "SSL error ${action.primaryError}",
                        url = action.url,
                        code = action.primaryError,
                        navigationId = action.navigationId,
                        timestamp = action.timestamp,
                    )
                ),
                page = if (action.updatesPage) {
                    state.page.copy(
                        url = action.url ?: state.page.url,
                        navigationId = action.navigationId,
                        progress = 100,
                        status = PageStatus.Error,
                        timestamp = action.timestamp,
                    )
                } else {
                    state.page
                },
            )

            is DebugAction.ResourceRequest -> state.copy(
                requests = state.requests.appendCapped(
                    RequestSnapshot(
                        url = action.url,
                        isMainFrame = action.isMainFrame,
                        category = action.toRequestCategory(state.page),
                        navigationId = action.navigationId,
                        timestamp = action.timestamp,
                    )
                )
            )

            is DebugAction.DownloadRequested -> state.copy(
                downloads = state.downloads.appendCapped(
                    DownloadSnapshot(
                        url = action.url,
                        userAgent = action.userAgent,
                        contentDisposition = action.contentDisposition,
                        mimeType = action.mimeType,
                        contentLength = action.contentLength,
                        downloadId = action.downloadId,
                        fileName = action.fileName,
                        status = action.status,
                        reason = action.reason,
                        navigationId = action.navigationId,
                        timestamp = action.timestamp,
                    )
                )
            )

            is DebugAction.DownloadStatusChanged -> {
                val downloadIndex = state.downloads.indexOfLast { it.downloadId == action.downloadId }
                if (downloadIndex < 0) {
                    state.copy(
                        consoleLogs = state.consoleLogs.appendCapped(
                            ConsoleLog(
                                level = "WARN",
                                message = "Download ${action.downloadId} finished without a matching request: ${action.status}",
                                sourceId = "download-manager",
                                timestamp = action.timestamp,
                            )
                        )
                    )
                } else {
                    state.copy(
                        downloads = state.downloads.mapIndexed { index, download ->
                            if (index != downloadIndex) {
                                download
                            } else {
                                download.copy(
                                    status = action.status,
                                    reason = action.reason,
                                    localUri = action.localUri,
                                    updatedAt = action.timestamp,
                                )
                            }
                        }
                    )
                }
            }

            is DebugAction.JavaScriptResult -> state.copy(
                jsResults = state.jsResults.appendCapped(
                    JsExecutionResult(
                        script = action.script,
                        result = action.result,
                        isError = action.isError,
                        timestamp = action.timestamp,
                    )
                )
            )
        }
    }

    private fun <T> List<T>.appendCapped(value: T): List<T> =
        (this + value).takeLast(MaxEntries)

    private fun DebugAction.ResourceRequest.toRequestCategory(page: PageSnapshot): RequestCategory {
        if (!isMainFrame) return RequestCategory.RESOURCE
        if (navigationId == page.navigationId && page.url != null && url != page.url) {
            return RequestCategory.REDIRECT
        }
        return RequestCategory.MAIN_FRAME
    }
}

sealed interface DebugAction {
    data class ClearLogs(val timestamp: Long = 0L) : DebugAction

    data class DebugMessage(
        val message: String,
        val timestamp: Long = 0L,
    ) : DebugAction

    data class ConsoleEvent(
        val level: String,
        val message: String,
        val sourceId: String = "",
        val lineNumber: Int = 0,
        val navigationId: Long = 0L,
        val timestamp: Long = 0L,
    ) : DebugAction

    data class PageStarted(
        val url: String,
        val navigationId: Long,
        val timestamp: Long = 0L,
    ) : DebugAction

    data class PageFinished(
        val url: String,
        val title: String = "",
        val navigationId: Long,
        val timestamp: Long = 0L,
    ) : DebugAction

    data class ProgressChanged(
        val navigationId: Long,
        val progress: Int,
        val timestamp: Long = 0L,
    ) : DebugAction

    data class LoadError(
        val url: String?,
        val code: Int,
        val description: String,
        val navigationId: Long = 0L,
        val isMainFrame: Boolean = true,
        val updatesPage: Boolean = true,
        val timestamp: Long = 0L,
    ) : DebugAction

    data class HttpError(
        val url: String?,
        val statusCode: Int,
        val reason: String,
        val navigationId: Long = 0L,
        val isMainFrame: Boolean = true,
        val timestamp: Long = 0L,
    ) : DebugAction

    data class SslError(
        val url: String?,
        val primaryError: Int,
        val navigationId: Long = 0L,
        val updatesPage: Boolean = true,
        val timestamp: Long = 0L,
    ) : DebugAction

    data class ResourceRequest(
        val url: String,
        val isMainFrame: Boolean,
        val navigationId: Long = 0L,
        val timestamp: Long = 0L,
    ) : DebugAction

    data class DownloadRequested(
        val url: String,
        val userAgent: String?,
        val contentDisposition: String?,
        val mimeType: String?,
        val contentLength: Long,
        val navigationId: Long = 0L,
        val downloadId: Long? = null,
        val fileName: String? = null,
        val status: DownloadStatus = DownloadStatus.REQUESTED,
        val reason: String? = null,
        val timestamp: Long = 0L,
    ) : DebugAction

    data class DownloadStatusChanged(
        val downloadId: Long,
        val status: DownloadStatus,
        val reason: String?,
        val localUri: String?,
        val timestamp: Long = 0L,
    ) : DebugAction

    data class JavaScriptResult(
        val script: String,
        val result: String,
        val isError: Boolean = false,
        val timestamp: Long = 0L,
    ) : DebugAction
}
