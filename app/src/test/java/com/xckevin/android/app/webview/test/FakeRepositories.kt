package com.xckevin.android.app.webview.test

import com.xckevin.android.app.webview.test.data.HistoryRepository
import com.xckevin.android.app.webview.test.model.HistoryItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeHistoryRepository(
    initialItems: List<HistoryItem> = emptyList(),
) : HistoryRepository {
    private val items = MutableStateFlow(initialItems)
    val insertedItems = mutableListOf<HistoryItem>()
    val deletedIds = mutableListOf<Long>()
    var clearCount = 0

    override fun observeRecent(limit: Int): Flow<List<HistoryItem>> =
        items.map { it.take(limit) }

    override suspend fun insert(item: HistoryItem): Long {
        insertedItems += item
        val id = if (item.id == 0L) {
            ((items.value.maxOfOrNull { it.id } ?: 0L) + 1L)
        } else {
            item.id
        }
        val saved = item.copy(id = id)
        items.value = listOf(saved) + items.value.filterNot { it.id == id }
        return id
    }

    override suspend fun clear() {
        clearCount += 1
        items.value = emptyList()
    }

    override suspend fun delete(item: HistoryItem) {
        deletedIds += item.id
        items.value = items.value.filterNot { it.id == item.id }
    }
}
