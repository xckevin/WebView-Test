package com.xckevin.android.app.webview.test.debug

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

object DebugNetworkApiCaptureParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parsePageErrors(result: String): List<PageError> {
        val element = parseResultElement(result) ?: return emptyList()
        val unwrapped = unwrapScriptResult(element)
        return when (unwrapped) {
            is JsonArray -> unwrapped.mapNotNull { it.toPageError() }
            is JsonObject -> listOfNotNull(unwrapped.toPageError())
            else -> emptyList()
        }
    }

    private fun parseResultElement(result: String): JsonElement? {
        val first = runCatching { json.parseToJsonElement(result) }.getOrNull() ?: return null
        val nested = (first as? JsonPrimitive)
            ?.contentOrNull
            ?.trim()
            ?.takeIf { it.startsWith("{") || it.startsWith("[") }
            ?.let { runCatching { json.parseToJsonElement(it) }.getOrNull() }
        return nested ?: first
    }

    private fun unwrapScriptResult(element: JsonElement): JsonElement =
        (element as? JsonObject)?.let { wrapper ->
            if (wrapper["ok"]?.jsonPrimitive?.booleanOrNull != null) {
                wrapper["value"] ?: wrapper["error"] ?: element
            } else {
                element
            }
        } ?: element

    private fun JsonElement.toPageError(): PageError? {
        val capture = this as? JsonObject ?: return null
        val url = capture.stringValue("url")?.takeIf { it.isNotBlank() } ?: return null
        val status = capture.intValue("status")
        val source = capture.stringValue("source").orEmpty().ifBlank { "api" }
        val statusText = capture.stringValue("statusText").orEmpty().ifBlank { "Captured API response" }
        val headers = capture.objectValue("responseHeaders").toStringMap().toMutableMap()
        capture.stringValue("contentType")?.takeIf { it.isNotBlank() }?.let { contentType ->
            if (headers.keys.none { it.equals("Content-Type", ignoreCase = true) }) {
                headers["Content-Type"] = contentType
            }
        }
        headers["X-Debug-Capture-Source"] = source
        capture.stringValue("method")?.takeIf { it.isNotBlank() }?.let { headers["X-Debug-Request-Method"] = it }
        capture.longValue("durationMs")?.let { headers["X-Debug-Duration-Ms"] = it.toString() }
        capture.stringValue("skippedBodyReason")?.takeIf { it.isNotBlank() }?.let {
            headers["X-Debug-Skipped-Body"] = it
        }
        if (capture.booleanValue("bodyTruncated") == true) {
            headers["X-Debug-Body-Truncated"] = "true"
        }

        return PageError(
            type = "ApiResponse",
            message = statusText,
            url = url,
            statusCode = status,
            responseHeaders = headers,
            responseBody = capture.stringValue("responseBody"),
            navigationId = 0L,
            isMainFrame = false,
            timestamp = capture.longValue("timestamp") ?: 0L,
        )
    }

    private fun JsonObject?.toStringMap(): Map<String, String> =
        this?.entries.orEmpty().associate { (key, value) ->
            key to when (value) {
                is JsonPrimitive -> value.contentOrNull ?: value.toString()
                else -> value.toString()
            }
        }

    private fun JsonObject.objectValue(name: String): JsonObject? =
        this[name] as? JsonObject

    private fun JsonObject.stringValue(name: String): String? =
        (this[name] as? JsonPrimitive)?.contentOrNull

    private fun JsonObject.intValue(name: String): Int? =
        (this[name] as? JsonPrimitive)?.intOrNull

    private fun JsonObject.longValue(name: String): Long? =
        stringValue(name)?.toLongOrNull()
            ?: (this[name] as? JsonPrimitive)?.contentOrNull?.toDoubleOrNull()?.toLong()

    private fun JsonObject.booleanValue(name: String): Boolean? =
        (this[name] as? JsonPrimitive)?.booleanOrNull
}
