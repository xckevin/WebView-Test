package com.xckevin.android.app.webview.test.debug

import org.junit.Assert.assertTrue
import org.junit.Test

class PageScriptsTest {
    @Test fun readLocalStorageContainsLocalStorage() {
        assertTrue(PageScripts.readLocalStorage().contains("localStorage"))
    }

    @Test fun clearSessionStorageContainsClearCall() {
        assertTrue(PageScripts.clearSessionStorage().contains("sessionStorage.clear()"))
    }

    @Test fun readSourceContainsOuterHtml() {
        assertTrue(PageScripts.readSource().contains("document.documentElement.outerHTML"))
    }

    @Test fun readElementsSummaryContainsQuerySelectorAll() {
        assertTrue(PageScripts.readElementsSummary().contains("querySelectorAll"))
    }

    @Test fun readElementsSummaryAcceptsSearchAndCssFilter() {
        val script = PageScripts.readElementsSummary(search = "submit", selector = "button.primary")

        assertTrue(script.contains("button.primary"))
        assertTrue(script.contains("submit"))
        assertTrue(script.contains("matchesSearch"))
    }

    @Test fun deleteStorageKeyTargetsRequestedStorageAreaAndKey() {
        val script = PageScripts.deleteStorageKey(storageName = "localStorage", key = "token")

        assertTrue(script.contains("localStorage.removeItem(\"token\")"))
        assertTrue(script.contains("localStorage key removed: token"))
    }

    @Test fun executeUserScriptEscapesClosingScriptTag() {
        val script = PageScripts.executeUserScript("""return "</script>";""")

        assertTrue(script.contains("<\\/script>"))
    }

    @Test fun executeUserScriptPreservesBackslashSemantics() {
        val userScript = """return /\d+/.test("1");"""
        val script = PageScripts.executeUserScript(userScript)

        assertTrue(script.contains(userScript))
    }

    @Test fun executeUserScriptWrapsUserCodeWithErrorCapture() {
        val script = PageScripts.executeUserScript("throw new Error('boom')")

        assertTrue(script.contains("JSON.stringify((function(){"))
        assertTrue(script.contains("JSON.stringify({error:String(e)});"))
    }
}
