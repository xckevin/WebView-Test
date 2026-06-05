package com.xckevin.android.app.webview.test.data

import com.xckevin.android.app.webview.test.model.WebTestCase
import com.xckevin.android.app.webview.test.model.WebTestConfig
import org.junit.Assert.assertEquals
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
        assertEquals(WebTestConfig.default(), imported.single().config)
    }

    @Test fun detectsConflictByNameAndUrl() {
        val existing = listOf(WebTestCase(1, "Home", "https://example.com", "", WebTestConfig.default(), 1, 1, null))
        val incoming = WebTestCase(0, "Home", "https://example.com", "", WebTestConfig.default(), 2, 2, null)
        val conflicts = CaseImportExport.findConflicts(existing, listOf(incoming))
        assertEquals(1, conflicts.size)
        assertEquals("Home", conflicts.single().incoming.name)
    }
}
