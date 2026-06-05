package com.xckevin.android.app.webview.test.data

import com.xckevin.android.app.webview.test.model.WebTestCase
import com.xckevin.android.app.webview.test.model.WebTestConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CaseImportExportTest {
    @Test fun exportsAndImportsCases() {
        val cases = listOf(
            WebTestCase(1, "Home", "https://example.com", "note", WebTestConfig.default(), 10, 11, 12)
        )
        val json = CaseImportExport.exportCases(cases)
        val imported = CaseImportExport.importCases(json)
        assertEquals("Home", imported.single().name)
        assertEquals("https://example.com", imported.single().url)
        assertEquals(0L, imported.single().id)
        assertEquals("note", imported.single().note)
        assertEquals(WebTestConfig.default(), imported.single().config)
        assertEquals(10L, imported.single().createdAt)
        assertEquals(11L, imported.single().updatedAt)
        assertEquals(12L, imported.single().lastOpenedAt)
        assertEquals("1", Json.parseToJsonElement(json).jsonObject.getValue("version").jsonPrimitive.content)
    }

    @Test fun rejectsUnsupportedVersion() {
        val error = assertThrows(UnsupportedCaseExportVersionException::class.java) {
            CaseImportExport.importCases("""{"version":2,"cases":[]}""")
        }
        assertEquals(2, error.version)
    }

    @Test fun detectsConflictByNameAndUrl() {
        val existing = listOf(WebTestCase(1, " Home ", "https://example.com ", "", WebTestConfig.default(), 1, 1, null))
        val incoming = WebTestCase(0, "Home", " https://example.com", "", WebTestConfig.default(), 2, 2, null)
        val conflicts = CaseImportExport.findConflicts(existing, listOf(incoming))
        assertEquals(1, conflicts.size)
        assertEquals("Home", conflicts.single().incoming.name)
    }

    @Test fun ignoresPartialConflictMatches() {
        val existing = listOf(WebTestCase(1, "Home", "https://example.com", "", WebTestConfig.default(), 1, 1, null))
        val incoming = listOf(
            WebTestCase(0, "Home", "https://different.example.com", "", WebTestConfig.default(), 2, 2, null),
            WebTestCase(0, "Docs", "https://example.com", "", WebTestConfig.default(), 3, 3, null),
        )
        val conflicts = CaseImportExport.findConflicts(existing, incoming)
        assertEquals(0, conflicts.size)
    }
}
