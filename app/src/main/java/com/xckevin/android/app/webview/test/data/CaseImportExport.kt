package com.xckevin.android.app.webview.test.data

import com.xckevin.android.app.webview.test.model.WebTestCase
import com.xckevin.android.app.webview.test.model.WebTestConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object CaseImportExport {
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true; prettyPrint = true }

    fun exportCases(cases: List<WebTestCase>): String =
        json.encodeToString(CaseExportFile(version = 1, cases = cases.map(CaseExportItem::from)))

    fun importCases(raw: String): List<WebTestCase> =
        json.decodeFromString<CaseExportFile>(raw).cases.map { it.toDomain() }

    fun findConflicts(existing: List<WebTestCase>, incoming: List<WebTestCase>): List<CaseConflict> {
        val existingKeys = existing.map { it.name.trim() to it.url.trim() }.toSet()
        return incoming.filter { it.name.trim() to it.url.trim() in existingKeys }
            .map { CaseConflict(it) }
    }
}

@Serializable
data class CaseExportFile(
    val version: Int,
    val cases: List<CaseExportItem>,
)

@Serializable
data class CaseExportItem(
    val name: String,
    val url: String,
    val note: String,
    val config: WebTestConfig,
    val createdAt: Long,
    val updatedAt: Long,
    val lastOpenedAt: Long?,
) {
    fun toDomain(): WebTestCase =
        WebTestCase(
            id = 0,
            name = name,
            url = url,
            note = note,
            config = config,
            createdAt = createdAt,
            updatedAt = updatedAt,
            lastOpenedAt = lastOpenedAt,
        )

    companion object {
        fun from(testCase: WebTestCase): CaseExportItem =
            CaseExportItem(
                name = testCase.name,
                url = testCase.url,
                note = testCase.note,
                config = testCase.config,
                createdAt = testCase.createdAt,
                updatedAt = testCase.updatedAt,
                lastOpenedAt = testCase.lastOpenedAt,
            )
    }
}

data class CaseConflict(val incoming: WebTestCase)
