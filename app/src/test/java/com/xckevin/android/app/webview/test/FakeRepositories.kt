package com.xckevin.android.app.webview.test

import com.xckevin.android.app.webview.test.data.HistoryRepository
import com.xckevin.android.app.webview.test.data.TestCaseRepository
import com.xckevin.android.app.webview.test.model.HistoryItem
import com.xckevin.android.app.webview.test.model.WebTestCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeTestCaseRepository(
    initialCases: List<WebTestCase> = emptyList(),
) : TestCaseRepository {
    private val cases = MutableStateFlow(initialCases)
    val upsertedCases = mutableListOf<WebTestCase>()
    val deletedCases = mutableListOf<WebTestCase>()

    override fun observeAll(): Flow<List<WebTestCase>> = cases

    override suspend fun getById(id: Long): WebTestCase? =
        cases.value.firstOrNull { it.id == id }

    override suspend fun upsert(testCase: WebTestCase): Long {
        upsertedCases += testCase
        val id = if (testCase.id == 0L) {
            ((cases.value.maxOfOrNull { it.id } ?: 0L) + 1L)
        } else {
            testCase.id
        }
        val saved = testCase.copy(id = id)
        cases.value = cases.value.filterNot { it.id == id } + saved
        return id
    }

    override suspend fun delete(testCase: WebTestCase) {
        deletedCases += testCase
        cases.value = cases.value.filterNot { it.id == testCase.id }
    }
}

class FakeHistoryRepository(
    initialItems: List<HistoryItem> = emptyList(),
) : HistoryRepository {
    private val items = MutableStateFlow(initialItems)
    val insertedItems = mutableListOf<HistoryItem>()
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
}
