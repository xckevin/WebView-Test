package com.xckevin.android.app.webview.test.data

import com.xckevin.android.app.webview.test.model.HistoryItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface HistoryRepository {
    fun observeRecent(limit: Int = 200): Flow<List<HistoryItem>>
    suspend fun insert(item: HistoryItem): Long
    suspend fun clear()
    suspend fun delete(item: HistoryItem)
}

class RoomHistoryRepository(
    private val dao: HistoryDao,
) : HistoryRepository {
    override fun observeRecent(limit: Int): Flow<List<HistoryItem>> =
        dao.observeRecent(limit).map { entities -> entities.map { it.toDomain() } }

    override suspend fun insert(item: HistoryItem): Long =
        dao.insert(item.toEntity())

    override suspend fun clear() {
        dao.clear()
    }

    override suspend fun delete(item: HistoryItem) {
        dao.deleteById(item.id)
    }
}

private fun HistoryEntryEntity.toDomain(): HistoryItem =
    HistoryItem(
        id = id,
        url = url,
        title = title,
        sourceType = sourceType,
        visitedAt = visitedAt,
    )

private fun HistoryItem.toEntity(): HistoryEntryEntity =
    HistoryEntryEntity(
        id = id,
        url = url,
        title = title,
        sourceType = sourceType,
        visitedAt = visitedAt,
    )
