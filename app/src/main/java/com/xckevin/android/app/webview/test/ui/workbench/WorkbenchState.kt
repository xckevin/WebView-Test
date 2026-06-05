package com.xckevin.android.app.webview.test.ui.workbench

import com.xckevin.android.app.webview.test.debug.DebugState
import com.xckevin.android.app.webview.test.model.WebTestConfig

data class WorkbenchState(
    val urlInput: String = "",
    val currentUrl: String? = null,
    val currentTitle: String = "",
    val config: WebTestConfig = WebTestConfig.default(),
    val selectedPanel: WorkbenchPanel = WorkbenchPanel.CONFIG,
    val isLoading: Boolean = false,
    val loadProgress: Int = 0,
    val activeNavigationId: Long = 0,
    val activeNavigationCompleted: Boolean = false,
    val isFullscreen: Boolean = false,
    val urlError: String? = null,
    val debugState: DebugState = DebugState(),
)

enum class WorkbenchPanel { CONFIG, DEBUG, CASES, HISTORY }
