package com.xckevin.android.app.webview.test.debug

object DebugReducer {
    const val MaxEntries = 500

    fun reduce(state: DebugState, action: DebugAction): DebugState {
        return when (action) {
            is DebugAction.ClearLogs -> clear(state, DebugClearScope.ALL, navigationId = 0L)

            is DebugAction.Clear -> clear(state, action.scope, action.navigationId)

            is DebugAction.DebugMessage -> state.copy(
                consoleLogs = state.consoleLogs.appendCapped(
                    ConsoleLog(
                        level = "INFO",
                        message = action.message,
                        sourceId = "workbench",
                        timestamp = action.timestamp,
                    )
                ),
                timeline = state.timeline.appendEvent(
                    type = DebugEventType.CONSOLE,
                    severity = DebugSeverity.INFO,
                    summary = action.message,
                    details = listOf("Source: workbench"),
                    navigationId = 0L,
                    timestamp = action.timestamp,
                ),
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
                ),
                timeline = state.timeline.appendEvent(
                    type = DebugEventType.CONSOLE,
                    severity = action.level.consoleSeverity(),
                    summary = action.message,
                    details = listOfNotNull(
                        "Level: ${action.level}",
                        action.sourceId.takeIf { it.isNotBlank() }?.let { "Source: $it" },
                        action.lineNumber.takeIf { it > 0 }?.let { "Line: $it" },
                    ),
                    navigationId = action.navigationId,
                    timestamp = action.timestamp,
                ),
            )

            is DebugAction.PageStarted -> state.copy(
                page = PageSnapshot(
                    url = action.url,
                    title = "",
                    navigationId = action.navigationId,
                    progress = 0,
                    status = PageStatus.Loading,
                    timestamp = action.timestamp,
                ),
                timeline = state.timeline.appendEvent(
                    type = DebugEventType.PAGE,
                    severity = DebugSeverity.INFO,
                    summary = "Page started",
                    details = listOf("URL: ${action.url}"),
                    navigationId = action.navigationId,
                    timestamp = action.timestamp,
                ),
            )

            is DebugAction.PageFinished -> state.copy(
                page = state.page.copy(
                    url = action.url,
                    title = action.title,
                    navigationId = action.navigationId,
                    progress = 100,
                    status = PageStatus.Finished,
                    timestamp = action.timestamp,
                ),
                timeline = state.timeline.appendEvent(
                    type = DebugEventType.PAGE,
                    severity = DebugSeverity.INFO,
                    summary = "Page finished",
                    details = listOf("URL: ${action.url}", "Title: ${action.title}"),
                    navigationId = action.navigationId,
                    timestamp = action.timestamp,
                ),
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
                timeline = state.timeline.appendEvent(
                    type = DebugEventType.ERROR,
                    severity = DebugSeverity.ERROR,
                    summary = "Load error: ${action.description}",
                    details = listOfNotNull(
                        action.url?.let { "URL: $it" },
                        "Code: ${action.code}",
                        "Main frame: ${action.isMainFrame}",
                    ),
                    navigationId = action.navigationId,
                    timestamp = action.timestamp,
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
                        responseHeaders = action.responseHeaders,
                        responseBody = action.responseBody,
                        navigationId = action.navigationId,
                        isMainFrame = action.isMainFrame,
                        timestamp = action.timestamp,
                    )
                ),
                timeline = state.timeline.appendEvent(
                    type = DebugEventType.ERROR,
                    severity = if (action.statusCode >= 500) DebugSeverity.ERROR else DebugSeverity.WARNING,
                    summary = "HTTP ${action.statusCode}: ${action.reason}",
                    details = listOfNotNull(
                        action.url?.let { "URL: $it" },
                        "Main frame: ${action.isMainFrame}",
                    ) + action.responseHeaders.map { (key, value) -> "$key: $value" },
                    navigationId = action.navigationId,
                    timestamp = action.timestamp,
                ),
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
                timeline = state.timeline.appendEvent(
                    type = DebugEventType.ERROR,
                    severity = DebugSeverity.ERROR,
                    summary = "SSL error ${action.primaryError}",
                    details = listOfNotNull(action.url?.let { "URL: $it" }),
                    navigationId = action.navigationId,
                    timestamp = action.timestamp,
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
                        method = action.method,
                        isMainFrame = action.isMainFrame,
                        requestHeaders = action.requestHeaders,
                        requestBody = action.requestBody,
                        category = action.toRequestCategory(state.page),
                        navigationId = action.navigationId,
                        timestamp = action.timestamp,
                    )
                ),
                timeline = state.timeline.appendEvent(
                    type = DebugEventType.NETWORK,
                    severity = DebugSeverity.INFO,
                    summary = "${action.method} ${action.url}",
                    details = listOf(
                        "Main frame: ${action.isMainFrame}",
                        "Category: ${action.toRequestCategory(state.page).name}",
                    ) + action.requestHeaders.map { (key, value) -> "$key: $value" },
                    navigationId = action.navigationId,
                    timestamp = action.timestamp,
                ),
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
                ),
                timeline = state.timeline.appendEvent(
                    type = DebugEventType.DOWNLOAD,
                    severity = if (action.status == DownloadStatus.FAILED || action.status == DownloadStatus.SKIPPED) {
                        DebugSeverity.WARNING
                    } else {
                        DebugSeverity.INFO
                    },
                    summary = "Download ${action.status}: ${action.fileName ?: action.url}",
                    details = listOfNotNull(
                        "URL: ${action.url}",
                        action.reason?.let { "Reason: $it" },
                        action.mimeType?.let { "MIME: $it" },
                    ),
                    navigationId = action.navigationId,
                    timestamp = action.timestamp,
                ),
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
                        ),
                        timeline = state.timeline.appendEvent(
                            type = DebugEventType.DOWNLOAD,
                            severity = DebugSeverity.WARNING,
                            summary = "Download ${action.downloadId} finished without a matching request: ${action.status}",
                            details = listOfNotNull(action.reason?.let { "Reason: $it" }),
                            navigationId = 0L,
                            timestamp = action.timestamp,
                        ),
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
                        },
                        timeline = state.timeline.appendEvent(
                            type = DebugEventType.DOWNLOAD,
                            severity = if (action.status == DownloadStatus.SUCCESS) DebugSeverity.INFO else DebugSeverity.WARNING,
                            summary = "Download ${action.status}: ${state.downloads[downloadIndex].fileName ?: action.downloadId}",
                            details = listOfNotNull(
                                action.reason?.let { "Reason: $it" },
                                action.localUri?.let { "Local URI: $it" },
                            ),
                            navigationId = state.downloads[downloadIndex].navigationId,
                            timestamp = action.timestamp,
                        ),
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
                ),
                timeline = state.timeline.appendEvent(
                    type = DebugEventType.JS,
                    severity = if (action.isError || action.result.contains("\"error\"")) DebugSeverity.WARNING else DebugSeverity.INFO,
                    summary = if (action.isError) "JavaScript error" else "JavaScript result",
                    details = listOf("Script: ${action.script}", "Result: ${action.result}"),
                    navigationId = 0L,
                    timestamp = action.timestamp,
                ),
            )

            is DebugAction.UserFlowEvent -> state.copy(
                userFlows = state.userFlows.appendCapped(
                    UserFlowSnapshot(
                        kind = action.kind,
                        summary = action.summary,
                        detail = action.detail,
                        navigationId = action.navigationId,
                        timestamp = action.timestamp,
                    )
                ),
                timeline = state.timeline.appendEvent(
                    type = DebugEventType.USER_FLOW,
                    severity = action.summary.flowSeverity(),
                    summary = action.summary,
                    details = listOfNotNull(action.detail.takeIf { it.isNotBlank() }),
                    navigationId = action.navigationId,
                    timestamp = action.timestamp,
                ),
            )
        }
    }

    private fun <T> List<T>.appendCapped(value: T): List<T> =
        (this + value).takeLast(MaxEntries)

    private fun List<DebugEvent>.appendEvent(
        type: DebugEventType,
        severity: DebugSeverity,
        summary: String,
        details: List<String>,
        navigationId: Long,
        timestamp: Long,
    ): List<DebugEvent> =
        appendCapped(
            DebugEvent(
                id = ((maxOfOrNull { it.id } ?: 0L) + 1L),
                type = type,
                severity = severity,
                summary = summary,
                details = details,
                navigationId = navigationId,
                timestamp = timestamp,
            )
        )

    private fun clear(state: DebugState, scope: DebugClearScope, navigationId: Long): DebugState =
        when (scope) {
            DebugClearScope.ALL -> state.copy(
                consoleLogs = emptyList(),
                errors = emptyList(),
                requests = emptyList(),
                downloads = emptyList(),
                jsResults = emptyList(),
                userFlows = emptyList(),
                timeline = emptyList(),
            )

            DebugClearScope.CURRENT_NAVIGATION -> state.copy(
                consoleLogs = state.consoleLogs.filterNot { it.navigationId == navigationId },
                errors = state.errors.filterNot { it.navigationId == navigationId },
                requests = state.requests.filterNot { it.navigationId == navigationId },
                downloads = state.downloads.filterNot { it.navigationId == navigationId },
                userFlows = state.userFlows.filterNot { it.navigationId == navigationId },
                timeline = state.timeline.filterNot { it.navigationId == navigationId },
            )

            DebugClearScope.CONSOLE -> state.copy(
                consoleLogs = emptyList(),
                errors = emptyList(),
                timeline = state.timeline.filterNot { it.type == DebugEventType.CONSOLE || it.type == DebugEventType.ERROR },
            )

            DebugClearScope.NETWORK -> state.copy(
                requests = emptyList(),
                downloads = emptyList(),
                timeline = state.timeline.filterNot { it.type == DebugEventType.NETWORK || it.type == DebugEventType.DOWNLOAD },
            )

            DebugClearScope.JS -> state.copy(
                jsResults = emptyList(),
                timeline = state.timeline.filterNot { it.type == DebugEventType.JS },
            )

            DebugClearScope.USER_FLOW -> state.copy(
                userFlows = emptyList(),
                timeline = state.timeline.filterNot { it.type == DebugEventType.USER_FLOW },
            )
        }

    private fun DebugAction.ResourceRequest.toRequestCategory(page: PageSnapshot): RequestCategory {
        if (!isMainFrame) return RequestCategory.RESOURCE
        if (navigationId == page.navigationId && page.url != null && url != page.url) {
            return RequestCategory.REDIRECT
        }
        return RequestCategory.MAIN_FRAME
    }

    private fun String.consoleSeverity(): DebugSeverity =
        when (uppercase()) {
            "ERROR" -> DebugSeverity.ERROR
            "WARN", "WARNING" -> DebugSeverity.WARNING
            else -> DebugSeverity.INFO
        }

    private fun String.flowSeverity(): DebugSeverity =
        when {
            contains("denied", ignoreCase = true) ||
                contains("failed", ignoreCase = true) ||
                contains("skipped", ignoreCase = true) -> DebugSeverity.WARNING
            else -> DebugSeverity.INFO
        }
}

enum class DebugClearScope {
    ALL,
    CURRENT_NAVIGATION,
    CONSOLE,
    NETWORK,
    JS,
    USER_FLOW,
}

sealed interface DebugAction {
    data class ClearLogs(val timestamp: Long = 0L) : DebugAction

    data class Clear(
        val scope: DebugClearScope,
        val navigationId: Long = 0L,
        val timestamp: Long = 0L,
    ) : DebugAction

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
        val responseHeaders: Map<String, String> = emptyMap(),
        val responseBody: String? = null,
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
        val method: String = "GET",
        val requestHeaders: Map<String, String> = emptyMap(),
        val requestBody: String? = null,
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

    data class UserFlowEvent(
        val kind: UserFlowKind,
        val summary: String,
        val detail: String = "",
        val navigationId: Long = 0L,
        val timestamp: Long = 0L,
    ) : DebugAction
}
