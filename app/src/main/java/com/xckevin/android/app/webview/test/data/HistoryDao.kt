package com.xckevin.android.app.webview.test.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history_entries ORDER BY visitedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 200): Flow<List<HistoryEntryEntity>>

    @Insert
    suspend fun insert(entity: HistoryEntryEntity): Long

    @Query("DELETE FROM history_entries")
    suspend fun clear()
}
