package com.xckevin.android.app.webview.test.model

data class HistoryItem(
    val id: Long,
    val url: String,
    val title: String,
    val sourceType: SourceType,
    val visitedAt: Long,
)

enum class SourceType { REMOTE_URL, LOCAL_FILE }
