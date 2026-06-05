package com.xckevin.android.app.webview.test.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.xckevin.android.app.webview.test.model.SourceType

@Entity(tableName = "history_entries", indices = [Index("visitedAt")])
data class HistoryEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String,
    val sourceType: SourceType,
    val visitedAt: Long,
)
