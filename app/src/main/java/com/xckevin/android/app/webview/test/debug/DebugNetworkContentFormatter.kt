package com.xckevin.android.app.webview.test.debug

import kotlinx.serialization.json.Json

object DebugNetworkContentFormatter {
    const val MaxCapturedBodyBytes = 64 * 1024
    const val MaxDisplayedBodyChars = 64 * 1024

    private val prettyJson = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun classify(contentType: String?, url: String?): NetworkContentKind {
        val normalizedType = contentType
            ?.substringBefore(";")
            ?.trim()
            ?.lowercase()
            .orEmpty()
        val normalizedUrl = url
            ?.substringBefore("?")
            ?.substringBefore("#")
            ?.lowercase()
            .orEmpty()

        return when {
            normalizedType == "application/json" ||
                normalizedType == "text/json" ||
                normalizedType.endsWith("+json") ||
                normalizedUrl.endsWith(".json") -> NetworkContentKind.JSON

            normalizedType == "text/html" ||
                normalizedUrl.endsWith(".html") ||
                normalizedUrl.endsWith(".htm") -> NetworkContentKind.HTML

            normalizedType == "text/css" ||
                normalizedUrl.endsWith(".css") -> NetworkContentKind.CSS

            normalizedType in javascriptMimeTypes ||
                normalizedUrl.endsWith(".js") ||
                normalizedUrl.endsWith(".mjs") -> NetworkContentKind.JAVASCRIPT

            normalizedType == "image/svg+xml" ||
                normalizedUrl.endsWith(".svg") -> NetworkContentKind.TEXT

            normalizedType.startsWith("text/") ||
                normalizedType.contains("xml") ||
                normalizedUrl.endsWith(".txt") ||
                normalizedUrl.endsWith(".text") ||
                normalizedUrl.endsWith(".xml") -> NetworkContentKind.TEXT

            normalizedType.startsWith("image/") ||
                imageExtensions.any { normalizedUrl.endsWith(it) } -> NetworkContentKind.IMAGE

            normalizedType.startsWith("audio/") ||
                audioExtensions.any { normalizedUrl.endsWith(it) } -> NetworkContentKind.AUDIO

            normalizedType.startsWith("video/") ||
                videoExtensions.any { normalizedUrl.endsWith(it) } -> NetworkContentKind.VIDEO

            normalizedType == "application/pdf" ||
                normalizedUrl.endsWith(".pdf") -> NetworkContentKind.PDF

            normalizedType.isBlank() -> NetworkContentKind.UNKNOWN

            else -> NetworkContentKind.BINARY
        }
    }

    fun isTextLike(contentType: String?, url: String?): Boolean =
        classify(contentType, url).isTextLike

    fun format(
        body: String,
        contentType: String?,
        url: String?,
        maxChars: Int = MaxDisplayedBodyChars,
    ): NetworkContentFormat {
        val initialKind = classify(contentType, url)
        val trimmed = body.trimStart()
        val jsonText = if (
            initialKind == NetworkContentKind.JSON ||
            ((initialKind.isTextLike || initialKind == NetworkContentKind.UNKNOWN) &&
                (trimmed.startsWith("{") || trimmed.startsWith("[")))
        ) {
            runCatching {
                prettyJson.encodeToString(prettyJson.parseToJsonElement(body))
            }.getOrNull()
        } else {
            null
        }
        val kind = if (jsonText != null) NetworkContentKind.JSON else initialKind.asDisplayTextKind()
        val displayText = jsonText ?: body
        val truncated = displayText.length > maxChars
        val cappedText = if (truncated) {
            displayText.take(maxChars) + "\n\n[truncated to $maxChars chars]"
        } else {
            displayText
        }
        return NetworkContentFormat(
            kind = kind,
            text = cappedText,
            isTruncated = truncated,
        )
    }

    private fun NetworkContentKind.asDisplayTextKind(): NetworkContentKind =
        if (isTextLike) this else NetworkContentKind.TEXT

    private val javascriptMimeTypes = setOf(
        "application/javascript",
        "application/ecmascript",
        "application/x-javascript",
        "text/javascript",
        "text/ecmascript",
    )

    private val imageExtensions = setOf(".png", ".jpg", ".jpeg", ".gif", ".webp", ".avif", ".bmp", ".ico")
    private val audioExtensions = setOf(".mp3", ".m4a", ".aac", ".wav", ".ogg", ".oga", ".flac")
    private val videoExtensions = setOf(".mp4", ".m4v", ".webm", ".mov", ".avi", ".mkv")
}

data class NetworkContentFormat(
    val kind: NetworkContentKind,
    val text: String,
    val isTruncated: Boolean,
)

enum class NetworkContentKind(
    val label: String,
    val isTextLike: Boolean,
    val isPreviewableMedia: Boolean,
) {
    JSON("JSON", isTextLike = true, isPreviewableMedia = false),
    HTML("HTML", isTextLike = true, isPreviewableMedia = false),
    CSS("CSS", isTextLike = true, isPreviewableMedia = false),
    JAVASCRIPT("JavaScript", isTextLike = true, isPreviewableMedia = false),
    TEXT("Text", isTextLike = true, isPreviewableMedia = false),
    IMAGE("Image", isTextLike = false, isPreviewableMedia = true),
    AUDIO("Audio", isTextLike = false, isPreviewableMedia = true),
    VIDEO("Video", isTextLike = false, isPreviewableMedia = true),
    PDF("PDF", isTextLike = false, isPreviewableMedia = true),
    BINARY("Binary", isTextLike = false, isPreviewableMedia = false),
    UNKNOWN("Unknown", isTextLike = false, isPreviewableMedia = false),
}
