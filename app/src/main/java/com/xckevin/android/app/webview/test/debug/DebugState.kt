package com.xckevin.android.app.webview.test.debug

data class DebugState(
    val logs: List<DebugLogEntry> = emptyList(),
)

data class DebugLogEntry(
    val message: String,
    val timestamp: Long,
)
