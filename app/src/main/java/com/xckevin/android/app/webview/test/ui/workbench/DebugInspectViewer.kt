package com.xckevin.android.app.webview.test.ui.workbench

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

data class DebugInspectRect(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
)

data class DebugInspectElement(
    val tag: String,
    val id: String = "",
    val className: String = "",
    val text: String = "",
    val visible: Boolean? = null,
    val rect: DebugInspectRect? = null,
    val attributes: Map<String, String> = emptyMap(),
    val href: String = "",
    val src: String = "",
    val depth: Int = 0,
    val childIndex: Int = 0,
    val childCount: Int = 0,
)

sealed interface DebugInspectResult {
    val rawText: String

    data class Elements(
        val elements: List<DebugInspectElement>,
        override val rawText: String,
        val error: String? = null,
    ) : DebugInspectResult

    data class Source(
        val html: String,
        override val rawText: String,
    ) : DebugInspectResult

    data class Tree(
        val path: List<DebugInspectElement>,
        val selectedIndex: Int,
        val truncatedAncestors: Boolean,
        override val rawText: String,
        val error: String? = null,
    ) : DebugInspectResult

    data class PlainText(
        val text: String,
        override val rawText: String,
        val error: String? = null,
    ) : DebugInspectResult
}

object DebugInspectParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(result: String): DebugInspectResult {
        val raw = result.trim()
        val element = parseJsonLenient(raw)
            ?: return classifyText(raw, rawText = result)
        val unwrapped = unwrapEvaluateJavascript(element)

        return when (unwrapped) {
            is JsonArray -> DebugInspectResult.Elements(
                elements = unwrapped.mapNotNull { (it as? JsonObject)?.toInspectElement() },
                rawText = result,
            )

            is JsonPrimitive -> classifyText(unwrapped.contentOrNull ?: unwrapped.toString(), rawText = result)
            is JsonObject -> {
                val error = unwrapped.stringValue("error")
                val treeResult = unwrapped.toInspectTree(rawText = result, error = error)
                if (treeResult != null) return treeResult
                val elementResult = unwrapped.toInspectElement()
                if (elementResult != null) {
                    DebugInspectResult.Elements(listOf(elementResult), rawText = result, error = error)
                } else {
                    DebugInspectResult.PlainText(unwrapped.toString(), rawText = result, error = error)
                }
            }
        }
    }

    private fun parseJsonLenient(raw: String): JsonElement? {
        val direct = runCatching { json.parseToJsonElement(raw) }.getOrNull()
        if (direct != null) return direct

        val quoted = raw.takeIf { it.length >= 2 && it.first() == '"' && it.last() == '"' }
            ?: return null
        val decoded = runCatching { json.decodeFromString<String>(quoted) }.getOrNull()
            ?: return null
        return runCatching { json.parseToJsonElement(decoded) }.getOrNull()
            ?: JsonPrimitive(decoded)
    }

    private fun unwrapEvaluateJavascript(element: JsonElement): JsonElement =
        when (element) {
            is JsonObject -> {
                val ok = element["ok"]?.asJsonPrimitiveOrNull()?.booleanOrNull
                if (ok != null) element["value"] ?: element["error"] ?: element else element
            }

            is JsonPrimitive -> {
                val nested = element.contentOrNull
                    ?.let { runCatching { json.parseToJsonElement(it) }.getOrNull() }
                if (nested != null) unwrapEvaluateJavascript(nested) else element
            }

            else -> element
        }

    private fun classifyText(text: String, rawText: String): DebugInspectResult =
        if (looksLikeHtml(text)) {
            DebugInspectResult.Source(html = text, rawText = rawText)
        } else {
            DebugInspectResult.PlainText(text = text, rawText = rawText)
        }

    private fun looksLikeHtml(text: String): Boolean {
        val trimmed = text.trimStart()
        return trimmed.startsWith("<!doctype", ignoreCase = true) ||
            trimmed.startsWith("<html", ignoreCase = true) ||
            trimmed.startsWith("<body", ignoreCase = true) ||
            trimmed.contains("<head", ignoreCase = true) ||
            trimmed.contains("<script", ignoreCase = true)
    }

    private fun JsonObject.toInspectElement(): DebugInspectElement? {
        val tag = stringValue("tag").ifBlank { return null }
        val rect = (this["rect"] as? JsonObject)?.let {
            DebugInspectRect(
                x = it.doubleValue("x"),
                y = it.doubleValue("y"),
                width = it.doubleValue("width"),
                height = it.doubleValue("height"),
            )
        }
        val attributes = (this["attributes"] as? JsonObject)
            ?.entries
            ?.associate { (key, value) -> key to value.displayValue() }
            .orEmpty()

        return DebugInspectElement(
            tag = tag,
            id = stringValue("id"),
            className = stringValue("className"),
            text = stringValue("text"),
            visible = this["visible"]?.asJsonPrimitiveOrNull()?.booleanOrNull,
            rect = rect,
            attributes = attributes,
            href = stringValue("href"),
            src = stringValue("src"),
            depth = intValue("depth"),
            childIndex = intValue("childIndex"),
            childCount = intValue("childCount"),
        )
    }

    private fun JsonObject.toInspectTree(rawText: String, error: String?): DebugInspectResult.Tree? {
        if (stringValue("type") != "selectedElementTree") return null
        val path = (this["path"] as? JsonArray)
            ?.mapNotNull { (it as? JsonObject)?.toInspectElement() }
            .orEmpty()
        return DebugInspectResult.Tree(
            path = path,
            selectedIndex = intValue("selectedIndex").coerceIn(0, (path.size - 1).coerceAtLeast(0)),
            truncatedAncestors = this["truncatedAncestors"]?.asJsonPrimitiveOrNull()?.booleanOrNull == true,
            rawText = rawText,
            error = error,
        )
    }

    private fun JsonObject.stringValue(key: String): String =
        this[key]?.displayValue().orEmpty()

    private fun JsonObject.intValue(key: String): Int =
        this[key]?.asJsonPrimitiveOrNull()?.intOrNull ?: 0

    private fun JsonObject.doubleValue(key: String): Double =
        this[key]?.asJsonPrimitiveOrNull()?.doubleOrNull ?: 0.0

    private fun JsonElement.displayValue(): String =
        (this as? JsonPrimitive)?.contentOrNull ?: toString()

    private fun JsonElement.asJsonPrimitiveOrNull(): JsonPrimitive? =
        this as? JsonPrimitive
}

@Composable
fun DebugInspectViewer(
    result: String,
    onClose: () -> Unit,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val parsed = remember(result) { DebugInspectParser.parse(result) }
    var query by remember { mutableStateOf("") }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            DebugInspectTopBar(
                parsed = parsed,
                onClose = onClose,
                onCopy = { onCopy(parsed.rawText) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            when (parsed) {
                is DebugInspectResult.Tree -> TreeInspectContent(
                    result = parsed,
                    modifier = Modifier.weight(1f),
                )

                is DebugInspectResult.Elements -> ElementInspectContent(
                    result = parsed,
                    query = query,
                    onQueryChange = { query = it },
                    modifier = Modifier.weight(1f),
                )

                is DebugInspectResult.Source -> SourceInspectContent(
                    html = parsed.html,
                    query = query,
                    onQueryChange = { query = it },
                    modifier = Modifier.weight(1f),
                )

                is DebugInspectResult.PlainText -> PlainInspectContent(
                    result = parsed,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DebugInspectTopBar(
    parsed: DebugInspectResult,
    onClose: () -> Unit,
    onCopy: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.Outlined.Close, contentDescription = "Close")
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Inspect result",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = parsed.subtitle(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onCopy) {
            Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy all")
        }
    }
}

@Composable
private fun TreeInspectContent(
    result: DebugInspectResult.Tree,
    modifier: Modifier = Modifier,
) {
    var selectedIndex by remember(result.path, result.selectedIndex) {
        mutableStateOf(result.selectedIndex.coerceIn(0, (result.path.size - 1).coerceAtLeast(0)))
    }
    val selectedElement = result.path.getOrNull(selectedIndex)

    Column(modifier = modifier.fillMaxSize()) {
        result.error?.takeIf { it.isNotBlank() }?.let { ErrorBanner(it) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                enabled = selectedIndex > 0,
                onClick = { selectedIndex = (selectedIndex - 1).coerceAtLeast(0) },
            ) {
                Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = "Select parent node")
            }
            IconButton(
                enabled = selectedIndex < result.path.lastIndex,
                onClick = { selectedIndex = (selectedIndex + 1).coerceAtMost(result.path.lastIndex) },
            ) {
                Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Select child node")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = selectedElement?.label() ?: "No element selected",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = buildString {
                        append("Node ${selectedIndex + 1} of ${result.path.size}")
                        if (result.truncatedAncestors) append(" · ancestors truncated")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 12.dp),
        ) {
            if (selectedElement != null) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = "Selected node",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                            ElementDetails(selectedElement)
                        }
                    }
                }
            }
            item {
                Text(
                    text = "Element path",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            items(result.path.mapIndexed { index, element -> index to element }, key = { it.first }) { (index, element) ->
                TreeElementRow(
                    element = element,
                    selected = index == selectedIndex,
                    depth = index,
                    onClick = { selectedIndex = index },
                )
            }
        }
    }
}

@Composable
private fun TreeElementRow(
    element: DebugInspectElement,
    selected: Boolean,
    depth: Int,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clickable(onClick = onClick),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = "${"  ".repeat(depth)}${element.label().ifBlank { "(unknown element)" }}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
            )
            Text(
                text = buildString {
                    append("child ${element.childIndex + 1}")
                    append(" · ${element.childCount} children")
                    element.text.ifBlank { element.href.ifBlank { element.src } }.takeIf { it.isNotBlank() }?.let {
                        append(" · ")
                        append(it)
                    }
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ElementInspectContent(
    result: DebugInspectResult.Elements,
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val expanded = remember(result.elements) { mutableStateMapOf<Int, Boolean>() }
    val filtered = remember(result.elements, query) {
        result.elements
            .mapIndexed { index, element -> index to element }
            .filter { (_, element) -> element.matches(query) }
    }

    Column(modifier = modifier.fillMaxSize()) {
        SearchField(query = query, onQueryChange = onQueryChange)
        result.error?.takeIf { it.isNotBlank() }?.let { ErrorBanner(it) }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 6.dp),
        ) {
            items(filtered, key = { it.first }) { (index, element) ->
                ElementRow(
                    element = element,
                    expanded = expanded[index] == true,
                    onToggle = { expanded[index] = expanded[index] != true },
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
    }
}

@Composable
private fun ElementRow(
    element: DebugInspectElement,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = element.label(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = element.text.ifBlank { element.href.ifBlank { element.src } },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            AssistChip(
                onClick = onToggle,
                label = { Text(if (element.visible == false) "hidden" else "visible") },
            )
        }
        if (expanded) {
            ElementDetails(element)
        }
    }
}

@Composable
private fun ElementDetails(element: DebugInspectElement) {
    Column(
        modifier = Modifier.padding(start = 36.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        DetailLine("tag", element.tag)
        DetailLine("id", element.id)
        DetailLine("class", element.className)
        DetailLine("text", element.text)
        DetailLine("href", element.href)
        DetailLine("src", element.src)
        element.rect?.let {
            DetailLine("rect", "x=${it.x.clean()} y=${it.y.clean()} w=${it.width.clean()} h=${it.height.clean()}")
        }
        element.attributes.forEach { (key, value) ->
            DetailLine("@$key", value)
        }
    }
}

@Composable
private fun SourceInspectContent(
    html: String,
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lines = remember(html, query) {
        html.lines()
            .mapIndexed { index, line -> index + 1 to line }
            .filter { (_, line) -> query.isBlank() || line.contains(query, ignoreCase = true) }
    }
    Column(modifier = modifier.fillMaxSize()) {
        SearchField(query = query, onQueryChange = onQueryChange)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(lines, key = { it.first }) { (lineNumber, line) ->
                Text(
                    text = "$lineNumber: $line",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun PlainInspectContent(
    result: DebugInspectResult.PlainText,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
    ) {
        result.error?.takeIf { it.isNotBlank() }?.let {
            item { ErrorBanner(it) }
        }
        item {
            Text(
                text = result.text,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        singleLine = true,
        leadingIcon = {
            Icon(Icons.Outlined.Search, contentDescription = null)
        },
        placeholder = { Text("Search") },
    )
}

@Composable
private fun ErrorBanner(error: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Text(
            text = error,
            modifier = Modifier.padding(10.dp),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    if (value.isBlank()) return
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun DebugInspectResult.subtitle(): String =
    when (this) {
        is DebugInspectResult.Tree -> "${path.size} selected path nodes"
        is DebugInspectResult.Elements -> "${elements.size} elements"
        is DebugInspectResult.Source -> "${html.lines().size} source lines"
        is DebugInspectResult.PlainText -> "Text result"
    }

private fun DebugInspectElement.label(): String =
    buildString {
        append(tag)
        if (id.isNotBlank()) append("#").append(id)
        className
            .split(" ")
            .filter { it.isNotBlank() }
            .forEach { append(".").append(it) }
    }

private fun DebugInspectElement.matches(query: String): Boolean {
    if (query.isBlank()) return true
    val haystack = buildList {
        add(tag)
        add(id)
        add(className)
        add(text)
        add(href)
        add(src)
        attributes.forEach { (key, value) ->
            add(key)
            add(value)
        }
    }
    return haystack.any { it.contains(query, ignoreCase = true) }
}

private fun Double.clean(): String =
    if (this % 1.0 == 0.0) toInt().toString() else toString()
