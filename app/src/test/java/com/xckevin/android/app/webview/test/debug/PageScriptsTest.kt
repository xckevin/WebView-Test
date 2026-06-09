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

    @Test fun writeStorageKeyTargetsRequestedStorageAreaKeyAndValue() {
        val script = PageScripts.writeStorageKey(
            storageName = "sessionStorage",
            key = "token",
            value = """{"id":1}""",
        )

        assertTrue(script.contains("sessionStorage.setItem(\"token\", \"{\\\"id\\\":1}\")"))
        assertTrue(script.contains("sessionStorage key saved: token"))
    }

    @Test fun readCookiesStructuredSplitsDocumentCookie() {
        val script = PageScripts.readCookiesStructured()

        assertTrue(script.contains("document.cookie"))
        assertTrue(script.contains("name"))
        assertTrue(script.contains("value"))
    }

    @Test fun writeCookieSetsDocumentCookie() {
        val script = PageScripts.writeCookie(name = "sid", value = "abc")

        assertTrue(script.contains("document.cookie"))
        assertTrue(script.contains("\"sid\" + \"=\" + encodeURIComponent(\"abc\")"))
        assertTrue(script.contains("cookie saved: sid"))
    }

    @Test fun deleteCookieExpiresDocumentCookie() {
        val script = PageScripts.deleteCookie(name = "sid")

        assertTrue(script.contains("document.cookie"))
        assertTrue(script.contains("expires=Thu, 01 Jan 1970 00:00:00 GMT"))
        assertTrue(script.contains("cookie deleted: sid"))
    }

    @Test fun readElementDetailsIncludesGeometryAndAttributes() {
        val script = PageScripts.readElementDetails(selector = "button.primary")

        assertTrue(script.contains("querySelectorAll(\"button.primary\")"))
        assertTrue(script.contains("getBoundingClientRect"))
        assertTrue(script.contains("attributes"))
    }

    @Test fun startFloatingInspectPointerUsesElementFromPointAndBoundsSerialization() {
        val script = PageScripts.startFloatingInspectPointer()

        assertTrue(script.contains("__wvDebugInspect"))
        assertTrue(script.contains("document.elementFromPoint"))
        assertTrue(script.contains("Confirm"))
        assertTrue(script.contains("Cancel"))
        assertTrue(script.contains("right:calc(env(safe-area-inset-right, 0px) + 12px)"))
        assertTrue(script.contains("top:calc(env(safe-area-inset-top, 0px) + 12px)"))
        assertTrue(script.contains("window.__wvDebugInspectLastResult = payload"))
        assertTrue(script.contains("__wvAndroidDebug.onInspectResult"))
        assertTrue(script.contains("document.addEventListener(\"pointerdown\""))
        assertTrue(script.contains("MAX_PATH_DEPTH = 24"))
        assertTrue(script.contains("MAX_ATTRIBUTES = 16"))
        assertTrue(script.contains("MAX_TEXT_LENGTH = 500"))
    }

    @Test fun inspectPointerResultFallbackCanBeReadAndCleared() {
        val read = PageScripts.readLastFloatingInspectPointerResult()
        val clear = PageScripts.clearLastFloatingInspectPointerResult()

        assertTrue(read.contains("__wvDebugInspectLastResult || null"))
        assertTrue(read.contains("delete window.__wvDebugInspectLastResult"))
        assertTrue(clear.contains("Inspect pointer result cleared."))
    }

    @Test fun confirmFloatingInspectPointerReadsExistingOverlaySelection() {
        val script = PageScripts.confirmFloatingInspectPointer()

        assertTrue(script.contains("__wvDebugInspect.confirm"))
        assertTrue(script.contains("Inspect pointer is not active."))
    }

    @Test fun cancelFloatingInspectPointerCleansExistingOverlay() {
        val script = PageScripts.cancelFloatingInspectPointer()

        assertTrue(script.contains("__wvDebugInspect.cleanup"))
        assertTrue(script.contains("Inspect pointer cancelled."))
    }

    @Test fun networkApiCaptureScriptsWrapFetchAndXhr() {
        val install = PageScripts.installNetworkApiCapture()

        assertTrue(install.contains("__wvDebugNetworkApi"))
        assertTrue(install.contains("window.fetch = function"))
        assertTrue(install.contains("XMLHttpRequest.prototype.open"))
        assertTrue(install.contains("__wvAndroidDebug.onNetworkApiCapture"))
        assertTrue(install.contains("MAX_BODY_CHARS = 65536"))
    }

    @Test fun readAndClearNetworkApiCaptureScriptsUseSharedState() {
        val read = PageScripts.readNetworkApiCaptures()
        val clear = PageScripts.clearNetworkApiCaptures()

        assertTrue(read.contains("__wvDebugNetworkApi.captures.slice()"))
        assertTrue(clear.contains("__wvDebugNetworkApi.clear"))
    }

    @Test fun scriptTemplatesExposeCommonDiagnostics() {
        val templates = PageScripts.templates()

        assertTrue(templates.any { it.name == "Page info" && it.script.contains("location.href") })
        assertTrue(templates.any { it.name == "List links" && it.script.contains("document.links") })
        assertTrue(templates.any { it.name == "Viewport" && it.script.contains("innerWidth") })
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
