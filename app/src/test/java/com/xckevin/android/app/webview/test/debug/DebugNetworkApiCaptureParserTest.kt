package com.xckevin.android.app.webview.test.debug

import org.junit.Assert.assertEquals
import org.junit.Test

class DebugNetworkApiCaptureParserTest {
    @Test fun parsesWrappedFetchCaptureAsApiResponse() {
        val result = """
            {"ok":true,"value":[{"source":"fetch","url":"https://example.com/api","method":"POST","status":201,"statusText":"Created","responseHeaders":{"content-type":"application/json"},"contentType":"application/json","timestamp":1700000000000,"durationMs":42,"responseBody":"{\"ok\":true}","bodyTruncated":true}]}
        """.trimIndent()

        val response = DebugNetworkApiCaptureParser.parsePageErrors(result).single()

        assertEquals("ApiResponse", response.type)
        assertEquals("https://example.com/api", response.url)
        assertEquals(201, response.statusCode)
        assertEquals("Created", response.message)
        assertEquals("""{"ok":true}""", response.responseBody)
        assertEquals("fetch", response.responseHeaders["X-Debug-Capture-Source"])
        assertEquals("POST", response.responseHeaders["X-Debug-Request-Method"])
        assertEquals("42", response.responseHeaders["X-Debug-Duration-Ms"])
        assertEquals("true", response.responseHeaders["X-Debug-Body-Truncated"])
    }

    @Test fun parsesQuotedWebViewScriptResult() {
        val result = "\"{\\\"ok\\\":true,\\\"value\\\":[{\\\"source\\\":\\\"xhr\\\",\\\"url\\\":\\\"https://example.com/user\\\",\\\"method\\\":\\\"GET\\\",\\\"status\\\":200,\\\"statusText\\\":\\\"OK\\\",\\\"responseHeaders\\\":{},\\\"contentType\\\":\\\"application/json\\\",\\\"timestamp\\\":1700000000000,\\\"durationMs\\\":5,\\\"responseBody\\\":\\\"[]\\\",\\\"bodyTruncated\\\":false}]}\""

        val response = DebugNetworkApiCaptureParser.parsePageErrors(result).single()

        assertEquals("ApiResponse", response.type)
        assertEquals("https://example.com/user", response.url)
        assertEquals("xhr", response.responseHeaders["X-Debug-Capture-Source"])
        assertEquals("application/json", response.responseHeaders["Content-Type"])
    }

    @Test fun parsesBridgePushedSingleCaptureObject() {
        val result = """
            {"source":"fetch","url":"https://example.com/products","method":"GET","status":200,"statusText":"OK","responseHeaders":{"content-type":"application/json"},"timestamp":1700000000100,"responseBody":"[{\"id\":1}]"}
        """.trimIndent()

        val response = DebugNetworkApiCaptureParser.parsePageErrors(result).single()

        assertEquals("ApiResponse", response.type)
        assertEquals("https://example.com/products", response.url)
        assertEquals("""[{"id":1}]""", response.responseBody)
    }
}
