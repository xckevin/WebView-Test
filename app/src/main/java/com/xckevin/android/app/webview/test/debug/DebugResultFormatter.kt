package com.xckevin.android.app.webview.test.debug

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object DebugResultFormatter {
    private val json = Json { ignoreUnknownKeys = true }

    fun formatCookieHeader(cookieHeader: String): List<String> {
        if (cookieHeader.isBlank()) return listOf("(empty)")
        return cookieHeader
            .split(";")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { cookie ->
                val name = cookie.substringBefore("=", cookie)
                val value = cookie.substringAfter("=", "")
                "$name = $value"
            }
    }

    fun formatScriptResult(result: String): List<String> {
        val element = runCatching { json.parseToJsonElement(result) }.getOrNull()
            ?: return result.lines().ifEmpty { listOf(result) }
        val unwrapped = (element as? JsonObject)?.let { wrapper ->
            if (wrapper["ok"]?.jsonPrimitive?.booleanOrNull != null) {
                wrapper["value"] ?: wrapper["error"] ?: element
            } else {
                element
            }
        } ?: element
        return formatJsonElement(unwrapped)
    }

    private fun formatJsonElement(element: JsonElement): List<String> =
        when (element) {
            is JsonArray -> formatJsonArray(element)
            is JsonObject -> element.entries.map { (key, value) -> "$key = ${value.compactValue()}" }
            is JsonPrimitive -> formatPrimitive(element)
        }

    private fun formatJsonArray(array: JsonArray): List<String> {
        if (array.isEmpty()) return listOf("[]")
        val objectLines = array.mapNotNull { it as? JsonObject }
        if (objectLines.size == array.size) {
            return objectLines.mapIndexed { index, item ->
                val label = item.elementLabel().ifBlank { "item ${index + 1}" }
                val details = item.entries
                    .filterNot { (key, _) -> key in setOf("tag", "id", "className") }
                    .joinToString(", ") { (key, value) -> "$key=${value.compactValue()}" }
                if (details.isBlank()) label else "$label | $details"
            }
        }
        return array.mapIndexed { index, item -> "${index + 1}: ${item.compactValue()}" }
    }

    private fun formatPrimitive(primitive: JsonPrimitive): List<String> {
        val content = primitive.contentOrNull ?: primitive.toString()
        val lines = content.lines()
        return if (lines.size > 1) {
            lines.mapIndexed { index, line -> "${index + 1}: $line" }
        } else {
            listOf(content)
        }
    }

    private fun JsonObject.elementLabel(): String {
        val tag = this["tag"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val id = this["id"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val className = this["className"]?.jsonPrimitive?.contentOrNull.orEmpty()
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(separator = ".", prefix = ".")
            .takeIf { it != "." }
            .orEmpty()
        return buildString {
            append(tag)
            if (id.isNotBlank()) append("#").append(id)
            append(className)
        }
    }

    private fun JsonElement.compactValue(): String =
        when (this) {
            is JsonPrimitive -> contentOrNull ?: toString()
            is JsonArray -> jsonArray.joinToString(prefix = "[", postfix = "]") { it.compactValue() }
            is JsonObject -> jsonObject.entries.joinToString(prefix = "{", postfix = "}") { (key, value) ->
                "$key=${value.compactValue()}"
            }
        }
}
