package com.xckevin.android.app.webview.test.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScannerViewModelTest {
    @Test fun firstScanResultWinsWithinSession() {
        val viewModel = ScannerViewModel()

        viewModel.onRawScanValue("https://first.example.com")
        viewModel.onRawScanValue("https://second.example.com")

        assertEquals(
            ParsedScanResult.Url("https://first.example.com"),
            viewModel.state.value.parsedResult,
        )
    }

    @Test fun editedTextCanBecomeUrl() {
        val viewModel = ScannerViewModel()

        viewModel.onRawScanValue("example.com/path with spaces")
        viewModel.onEditableUrlChanged("example.com/path")
        viewModel.useEditedTextAsUrl()

        assertEquals(
            ParsedScanResult.Url("https://example.com/path"),
            viewModel.state.value.parsedResult,
        )
        assertEquals("https://example.com/path", viewModel.state.value.editableUrl)
        assertNull(viewModel.state.value.editError)
    }

    @Test fun invalidEditedTextShowsError() {
        val viewModel = ScannerViewModel()

        viewModel.onRawScanValue("plain text")
        viewModel.onEditableUrlChanged("ftp://example.com")
        viewModel.useEditedTextAsUrl()

        assertTrue(viewModel.state.value.parsedResult is ParsedScanResult.Text)
        assertEquals("Enter a valid http or https URL", viewModel.state.value.editError)
    }
}
