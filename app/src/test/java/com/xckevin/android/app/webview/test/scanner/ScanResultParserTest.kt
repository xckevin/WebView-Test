package com.xckevin.android.app.webview.test.scanner

import org.junit.Assert.assertEquals
import org.junit.Test

class ScanResultParserTest {
    @Test fun urlResultNormalizesUrl() {
        assertEquals(
            ParsedScanResult.Url("https://example.com/path"),
            ScanResultParser.parse(" example.com/path "),
        )
    }

    @Test fun plainTextResultIsNotUrl() {
        assertEquals(
            ParsedScanResult.Text("not a url"),
            ScanResultParser.parse(" not a url "),
        )
    }

    @Test fun blankResultIsRejected() {
        assertEquals(ParsedScanResult.Empty, ScanResultParser.parse("  "))
    }
}
