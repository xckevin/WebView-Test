package com.xckevin.android.app.webview.test.ui.workbench

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugInspectParserTest {
    @Test fun parsesElementsSummaryWrapper() {
        val result = """
            {"ok":true,"value":[{"tag":"a","id":"hero","className":"primary link","text":"Open docs","visible":true,"href":"https://example.com/docs","src":""}]}
        """.trimIndent()

        val parsed = DebugInspectParser.parse(result) as DebugInspectResult.Elements

        assertEquals(1, parsed.elements.size)
        assertEquals("a", parsed.elements.first().tag)
        assertEquals("hero", parsed.elements.first().id)
        assertEquals("primary link", parsed.elements.first().className)
        assertEquals("Open docs", parsed.elements.first().text)
        assertEquals(true, parsed.elements.first().visible)
        assertEquals("https://example.com/docs", parsed.elements.first().href)
    }

    @Test fun parsesElementDetailsWrapperWithRectAndAttributes() {
        val result = """
            {
              "ok": true,
              "value": [{
                "tag": "button",
                "id": "submit",
                "className": "primary",
                "text": "Submit",
                "visible": false,
                "rect": {"x": 10, "y": 20, "width": 140, "height": 48},
                "attributes": {"aria-label": "Submit form", "data-testid": "submit-button"},
                "href": "",
                "src": ""
              }]
            }
        """.trimIndent()

        val element = (DebugInspectParser.parse(result) as DebugInspectResult.Elements).elements.first()

        assertEquals("button", element.tag)
        assertEquals(false, element.visible)
        assertEquals(10.0, element.rect?.x ?: -1.0, 0.0)
        assertEquals(48.0, element.rect?.height ?: -1.0, 0.0)
        assertEquals("Submit form", element.attributes["aria-label"])
        assertEquals("submit-button", element.attributes["data-testid"])
    }

    @Test fun parsesReadSourceWrapperAsSource() {
        val result = """{"ok":true,"value":"<!doctype html><html><body><main>Content</main></body></html>"}"""

        val parsed = DebugInspectParser.parse(result) as DebugInspectResult.Source

        assertTrue(parsed.html.contains("<main>Content</main>"))
    }

    @Test fun parsesQuotedWebViewJsonString() {
        val result = """"{\"ok\":true,\"value\":[{\"tag\":\"img\",\"id\":\"logo\",\"className\":\"\",\"text\":\"\",\"visible\":true,\"src\":\"https://example.com/logo.png\"}]}""""

        val parsed = DebugInspectParser.parse(result) as DebugInspectResult.Elements

        assertEquals("img", parsed.elements.first().tag)
        assertEquals("https://example.com/logo.png", parsed.elements.first().src)
    }

    @Test fun preservesWrapperErrorAsPlainText() {
        val result = """{"ok":false,"error":"Error: selector failed"}"""

        val parsed = DebugInspectParser.parse(result) as DebugInspectResult.PlainText

        assertEquals("Error: selector failed", parsed.text)
    }

    @Test fun parsesElementWhenOptionalFieldsAreObjects() {
        val result = """
            {
              "ok": true,
              "value": [{
                "tag": "custom-card",
                "id": {"nested": "hero"},
                "className": ["featured", "wide"],
                "text": {"label": "Open"},
                "visible": true,
                "href": {"url": "https://example.com"},
                "src": ""
              }]
            }
        """.trimIndent()

        val element = (DebugInspectParser.parse(result) as DebugInspectResult.Elements).elements.first()

        assertEquals("custom-card", element.tag)
        assertTrue(element.id.contains("nested"))
        assertTrue(element.className.contains("featured"))
        assertTrue(element.text.contains("Open"))
        assertTrue(element.href.contains("https://example.com"))
    }

    @Test fun parsesObjectErrorWithoutCrashing() {
        val result = """{"error":{"message":"selector failed"},"tag":"div"}"""

        val parsed = DebugInspectParser.parse(result) as DebugInspectResult.Elements

        assertTrue(parsed.error.orEmpty().contains("selector failed"))
        assertEquals("div", parsed.elements.first().tag)
    }
}
