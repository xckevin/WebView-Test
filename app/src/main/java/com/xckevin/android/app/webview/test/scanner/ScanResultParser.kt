package com.xckevin.android.app.webview.test.scanner

import com.xckevin.android.app.webview.test.util.UrlNormalizer

sealed interface ParsedScanResult {
    data class Url(val normalizedUrl: String) : ParsedScanResult
    data class Text(val value: String) : ParsedScanResult
    object Empty : ParsedScanResult
}

object ScanResultParser {
    fun parse(rawValue: String?): ParsedScanResult {
        val value = rawValue?.trim().orEmpty()
        if (value.isBlank()) return ParsedScanResult.Empty

        return UrlNormalizer.normalizeRemoteUrl(value)
            ?.let(ParsedScanResult::Url)
            ?: ParsedScanResult.Text(value)
    }
}
