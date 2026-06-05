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
