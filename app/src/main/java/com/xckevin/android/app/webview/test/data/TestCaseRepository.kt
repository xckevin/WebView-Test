package com.xckevin.android.app.webview.test.data

import com.xckevin.android.app.webview.test.model.WebTestCase
import com.xckevin.android.app.webview.test.model.WebTestConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

interface TestCaseRepository {
    fun observeAll(): Flow<List<WebTestCase>>
    suspend fun getById(id: Long): WebTestCase?
    suspend fun upsert(testCase: WebTestCase): Long
    suspend fun delete(testCase: WebTestCase)
}

class RoomTestCaseRepository(
    private val dao: TestCaseDao,
) : TestCaseRepository {
    override fun observeAll(): Flow<List<WebTestCase>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getById(id: Long): WebTestCase? =
        dao.getById(id)?.toDomain()

    override suspend fun upsert(testCase: WebTestCase): Long =
        dao.upsert(testCase.toEntity())

    override suspend fun delete(testCase: WebTestCase) {
        dao.delete(testCase.toEntity())
    }
}

private val testCaseJson = Json { encodeDefaults = true; ignoreUnknownKeys = true }

private fun TestCaseEntity.toDomain(): WebTestCase =
    WebTestCase(
        id = id,
        name = name,
        url = url,
        note = note,
        config = testCaseJson.decodeFromString<WebTestConfig>(configJson),
        createdAt = createdAt,
        updatedAt = updatedAt,
        lastOpenedAt = lastOpenedAt,
    )

private fun WebTestCase.toEntity(): TestCaseEntity =
    TestCaseEntity(
        id = id,
        name = name,
        url = url,
        note = note,
        configJson = testCaseJson.encodeToString(config),
        createdAt = createdAt,
        updatedAt = updatedAt,
        lastOpenedAt = lastOpenedAt,
    )
