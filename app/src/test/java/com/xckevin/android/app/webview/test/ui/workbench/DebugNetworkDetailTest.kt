package com.xckevin.android.app.webview.test.ui.workbench

import com.xckevin.android.app.webview.test.debug.DownloadSnapshot
import com.xckevin.android.app.webview.test.debug.DownloadStatus
import com.xckevin.android.app.webview.test.debug.PageError
import com.xckevin.android.app.webview.test.debug.RequestCategory
import com.xckevin.android.app.webview.test.debug.RequestSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugNetworkDetailTest {
    @Test fun requestDetailContainsStructuredUrlHeadersAndBodySections() {
        val request = RequestSnapshot(
            url = "https://example.com/assets/app.js?cache=1",
            method = "POST",
            isMainFrame = false,
            requestHeaders = mapOf("Accept" to "application/json", "X-Trace" to "abc"),
            category = RequestCategory.RESOURCE,
            navigationId = 42L,
            timestamp = 1_700_000_000_000L,
        )

        val sections = buildNetworkDetailSections(
            request = request,
            error = null,
            download = null,
        )

        assertTrue(sections.map { it.title }.containsAll(networkSectionTitles))
        assertEquals(
            listOf(
                NetworkDetailSectionKind.Url,
                NetworkDetailSectionKind.RequestHeaders,
                NetworkDetailSectionKind.RequestBody,
                NetworkDetailSectionKind.ResponseHeaders,
                NetworkDetailSectionKind.ResponseBody,
                NetworkDetailSectionKind.Meta,
            ),
            sections.map { it.kind },
        )

        assertEquals(request.url, sections.rowValue("URL", "Request URL"))
        assertEquals("application/json", sections.rowValue("Request headers", "Accept"))
        assertEquals("abc", sections.rowValue("Request headers", "X-Trace"))
        assertEquals(
            "Not captured by Android WebView callback",
            sections.rowValue("Request body", "Body"),
        )
        assertEquals("(none)", sections.rowValue("Response headers", "Headers"))
        assertEquals(
            "Not captured by Android WebView callback",
            sections.rowValue("Response body", "Body"),
        )

        val metaRows = sections.sectionRows("Meta")
        assertEquals("POST", metaRows["Method"])
        assertEquals("https", metaRows["Scheme"])
        assertEquals("example.com", metaRows["Host"])
        assertEquals("/assets/app.js", metaRows["Path"])
        assertEquals("false", metaRows["Main frame"])
        assertEquals("resource", metaRows["Category"])
        assertEquals("42", metaRows["Navigation ID"])
        assertTrue(metaRows["Request timestamp"].orEmpty().isNotBlank())
    }

    @Test fun matchingHttpErrorAddsResponseSection() {
        val request = request(navigationId = 7L)
        val error = PageError(
            type = "HttpError",
            message = "Not Found",
            url = request.url,
            statusCode = 404,
            responseHeaders = mapOf("Content-Type" to "text/html"),
            navigationId = 7L,
            isMainFrame = true,
            timestamp = 200L,
        )

        val sections = buildNetworkDetailSections(
            request = request,
            error = error,
            download = null,
        )

        assertTrue(sections.map { it.title }.containsAll(networkSectionTitles))
        val rows = sections.sectionRows("Meta")
        assertEquals("404", rows["Status code"])
        assertEquals("Not Found", rows["Reason/message"])
        assertEquals("text/html", sections.rowValue("Response headers", "Content-Type"))
        assertEquals(
            "Not captured by Android WebView callback",
            sections.rowValue("Response body", "Body"),
        )
    }

    @Test fun nonMatchingHttpErrorIsIgnoredForRequestDetail() {
        val request = request(navigationId = 7L)
        val error = PageError(
            type = "HttpError",
            message = "Server Error",
            url = request.url,
            statusCode = 500,
            navigationId = 8L,
            timestamp = 200L,
        )

        val sections = buildNetworkDetailSections(
            request = request,
            error = error,
            download = null,
        )

        assertEquals(
            listOf(
                NetworkDetailSectionKind.Url,
                NetworkDetailSectionKind.RequestHeaders,
                NetworkDetailSectionKind.RequestBody,
                NetworkDetailSectionKind.ResponseHeaders,
                NetworkDetailSectionKind.ResponseBody,
                NetworkDetailSectionKind.Meta,
            ),
            sections.map { it.kind },
        )
        assertEquals("(none)", sections.rowValue("Response headers", "Headers"))
        assertNull(findMatchingHttpError(request, listOf(error)))
    }

    @Test fun capturedApiResponseMatchesRequestWithoutNavigationId() {
        val request = request(navigationId = 7L).copy(url = "https://example.com/api/user")
        val response = PageError(
            type = "ApiResponse",
            message = "OK",
            url = request.url,
            statusCode = 200,
            responseHeaders = mapOf("Content-Type" to "application/json"),
            responseBody = """{"name":"Ada"}""",
            navigationId = 0L,
            isMainFrame = false,
            timestamp = 200L,
        )

        val matched = findMatchingHttpError(request, listOf(response))
        val sections = buildNetworkDetailSections(
            request = request,
            error = matched,
            download = null,
        )

        assertEquals(response, matched)
        assertEquals("200", sections.rowValue("Meta", "Status code"))
        assertEquals("JSON", sections.rowValue("Response body", "Type"))
        assertTrue(sections.rowValue("Response body", "Body").orEmpty().contains("Ada"))
    }

    @Test fun downloadSectionContainsAllDownloadFields() {
        val download = DownloadSnapshot(
            url = "https://example.com/report.pdf",
            userAgent = "WebViewTest",
            contentDisposition = "attachment; filename=report.pdf",
            mimeType = "application/pdf",
            contentLength = 1234L,
            downloadId = 99L,
            fileName = "report.pdf",
            status = DownloadStatus.SUCCESS,
            reason = "complete",
            localUri = "file:///tmp/report.pdf",
            navigationId = 12L,
            timestamp = 300L,
            updatedAt = 400L,
        )

        val sections = buildNetworkDetailSections(
            request = null,
            error = null,
            download = download,
        )

        assertTrue(sections.map { it.title }.containsAll(networkSectionTitles))
        assertEquals(NetworkDetailSectionKind.Download, sections.last().kind)
        assertEquals("PDF", sections.rowValue("Media preview", "Kind"))
        assertEquals(download.url, sections.rowValue("Media preview", "Preview URL"))
        assertEquals(download.localUri, sections.rowValue("Media preview", "Preview local URI"))
        val rows = sections.last().rows.associate { it.label to it.value }
        assertEquals(download.url, sections.rowValue("URL", "Download URL"))
        assertEquals("WebViewTest", rows["User agent"])
        assertEquals("attachment; filename=report.pdf", rows["Content disposition"])
        assertEquals("application/pdf", rows["MIME type"])
        assertEquals("1234", rows["Content length"])
        assertEquals("99", rows["Download ID"])
        assertEquals("report.pdf", rows["File name"])
        assertEquals("SUCCESS", rows["Status"])
        assertEquals("complete", rows["Reason"])
        assertEquals("file:///tmp/report.pdf", rows["Local URI"])
        assertEquals("12", rows["Navigation ID"])
        assertFalse(rows["Timestamp"].isNullOrBlank())
        assertFalse(rows["Updated at"].isNullOrBlank())
    }

    @Test fun copyTextKeepsSectionHeadingsAndRows() {
        val sections = listOf(
            NetworkDetailSection(
                title = "Request",
                kind = NetworkDetailSectionKind.Request,
                rows = listOf(
                    NetworkDetailRow("URL", "https://example.com"),
                    NetworkDetailRow("Method", "GET"),
                ),
            ),
            NetworkDetailSection(
                title = "Response",
                kind = NetworkDetailSectionKind.Response,
                rows = listOf(NetworkDetailRow("Status code", "404")),
            ),
        )

        val text = sections.toCopyText()

        assertTrue(text.contains("Request\nURL: https://example.com\nMethod: GET"))
        assertTrue(text.contains("Response\nStatus code: 404"))
    }

    @Test fun copyTextUsesStructuredNetworkSectionHeadingsAndRows() {
        val request = request(navigationId = 9L).copy(
            requestHeaders = mapOf("Accept" to "application/json"),
            requestBody = """{"q":"test"}""",
        )
        val error = PageError(
            type = "HttpError",
            message = "OK",
            url = request.url,
            statusCode = 200,
            responseHeaders = mapOf("Content-Type" to "application/json"),
            responseBody = """{"ok":true}""",
            navigationId = 9L,
            timestamp = 200L,
        )

        val text = buildNetworkDetailSections(
            request = request,
            error = error,
            download = null,
        ).toCopyText()

        assertTrue(text.contains("URL\nRequest URL: https://example.com/index.html"))
        assertTrue(text.contains("Request headers\nAccept: application/json"))
        assertTrue(text.contains("Request body\nType: JSON\nBody: {"))
        assertTrue(text.contains("q"))
        assertTrue(text.contains("test"))
        assertTrue(text.contains("Response headers\nContent-Type: application/json"))
        assertTrue(text.contains("Response body\nType: JSON\nBody: {"))
        assertTrue(text.contains("ok"))
        assertTrue(text.contains("true"))
    }

    @Test fun mediaPreviewSectionIsAddedForMediaRequest() {
        val request = request(navigationId = 10L).copy(
            url = "https://example.com/image.webp",
            isMainFrame = false,
            category = RequestCategory.RESOURCE,
        )

        val sections = buildNetworkDetailSections(
            request = request,
            error = null,
            download = null,
        )

        assertEquals("Image", sections.rowValue("Media preview", "Kind"))
        assertEquals(request.url, sections.rowValue("Media preview", "Preview URL"))
    }

    private val networkSectionTitles = listOf(
        "URL",
        "Request headers",
        "Request body",
        "Response headers",
        "Response body",
    )

    private fun List<NetworkDetailSection>.sectionRows(title: String): Map<String, String> =
        single { it.title == title }.rows.associate { it.label to it.value }

    private fun List<NetworkDetailSection>.rowValue(title: String, label: String): String? =
        sectionRows(title)[label]

    private fun request(navigationId: Long): RequestSnapshot =
        RequestSnapshot(
            url = "https://example.com/index.html",
            method = "GET",
            isMainFrame = true,
            category = RequestCategory.MAIN_FRAME,
            navigationId = navigationId,
            timestamp = 100L,
        )
}
