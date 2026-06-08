package com.xckevin.android.app.webview.test.ui.workbench

import com.xckevin.android.app.webview.test.debug.DebugState
import com.xckevin.android.app.webview.test.model.SourceType
import com.xckevin.android.app.webview.test.model.WebTestConfig

data class WorkbenchState(
    val urlInput: String = "",
    val currentUrl: String? = null,
    val currentTitle: String = "",
    val config: WebTestConfig = WebTestConfig.default(),
    val selectedPanel: WorkbenchPanel = WorkbenchPanel.CONFIG,
    val isLoading: Boolean = false,
    val loadProgress: Int = 0,
    val requestedUrl: String? = null,
    val requestedNavigationId: Long = 0,
    val requestedSourceType: SourceType = SourceType.REMOTE_URL,
    val activeNavigationId: Long = 0,
    val activeSourceType: SourceType = SourceType.REMOTE_URL,
    val activeNavigationCompleted: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isFullscreen: Boolean = false,
    val isVideoFullscreen: Boolean = false,
    val urlError: String? = null,
    val debugState: DebugState = DebugState(),
)

enum class WorkbenchPanel { CONFIG, DEBUG, HISTORY }
