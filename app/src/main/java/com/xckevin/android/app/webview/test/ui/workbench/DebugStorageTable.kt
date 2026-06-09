package com.xckevin.android.app.webview.test.ui.workbench

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xckevin.android.app.webview.test.R
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

enum class DebugStorageSource(val label: String) {
    LocalStorage("localStorage"),
    SessionStorage("sessionStorage"),
    Cookie("cookie"),
    Unknown("storage"),
}

data class DebugStorageRow(
    val key: String,
    val value: String,
    val source: DebugStorageSource,
    val metadata: String = "",
)

@Composable
fun DebugStorageTableViewer(
    title: String,
    result: String,
    sourceHint: DebugStorageSource,
    onClose: () -> Unit,
    onCopy: (String) -> Unit,
    onSave: (DebugStorageSource, String, String) -> Unit,
    onDelete: (DebugStorageSource, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = remember(result, sourceHint) { parseDebugStorageRows(result, sourceHint) }
    var query by remember { mutableStateOf("") }
    var editingRow by remember { mutableStateOf<DebugStorageRow?>(null) }
    var editorKey by remember { mutableStateOf("") }
    var editorValue by remember { mutableStateOf("") }
    var editorSource by remember(sourceHint) { mutableStateOf(sourceHint.normalizedForEdit()) }
    val filteredRows = remember(rows, query) {
        val needle = query.trim()
        if (needle.isBlank()) {
            rows
        } else {
            rows.filter { row ->
                row.key.contains(needle, ignoreCase = true) ||
                    row.value.contains(needle, ignoreCase = true) ||
                    row.source.label.contains(needle, ignoreCase = true) ||
                    row.metadata.contains(needle, ignoreCase = true)
            }
        }
    }

    fun edit(row: DebugStorageRow?) {
        editingRow = row
        editorKey = row?.key.orEmpty()
        editorValue = row?.value.orEmpty()
        editorSource = row?.source ?: sourceHint.normalizedForEdit()
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            DebugStorageViewerTopBar(
                title = title,
                rowCount = rows.size,
                onClose = onClose,
                onCopy = { onCopy(debugStorageRowsCopyText(rows)) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(stringResource(R.string.action_search)) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Search, contentDescription = null)
                        },
                    )
                }
                item {
                    OutlinedButton(
                        onClick = { edit(null) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null)
                        Text(stringResource(R.string.debug_storage_add_new))
                    }
                }
                item {
                    DebugStorageEditor(
                        editingRow = editingRow,
                        source = editorSource,
                        key = editorKey,
                        value = editorValue,
                        onKeyChange = { editorKey = it },
                        onValueChange = { editorValue = it },
                        onCancel = { edit(null) },
                        onSave = {
                            val key = editorKey.trim()
                            if (key.isNotEmpty()) {
                                onSave(editorSource, key, editorValue)
                                edit(null)
                            }
                        },
                    )
                }
                if (filteredRows.isEmpty()) {
                    item {
                        Text(
                            text = if (rows.isEmpty()) {
                                stringResource(R.string.debug_no_storage_rows)
                            } else {
                                stringResource(R.string.debug_no_matching_rows)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    items(filteredRows) { row ->
                        DebugStorageViewerRow(
                            row = row,
                            onOpen = { edit(row) },
                            onCopy = { onCopy(debugStorageRowsCopyText(listOf(row))) },
                            onDelete = { onDelete(row.source, row.key) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DebugStorageTable(
    result: String,
    sourceHint: DebugStorageSource,
    onCopy: (DebugStorageRow) -> Unit,
    onEdit: (DebugStorageSource, String, String) -> Unit,
    onDelete: (DebugStorageSource, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    DebugStorageTable(
        rows = remember(result, sourceHint) { parseDebugStorageRows(result, sourceHint) },
        onCopy = onCopy,
        onEdit = onEdit,
        onDelete = onDelete,
        modifier = modifier,
    )
}

@Composable
fun DebugStorageTable(
    rows: List<DebugStorageRow>,
    onCopy: (DebugStorageRow) -> Unit,
    onEdit: (DebugStorageSource, String, String) -> Unit,
    onDelete: (DebugStorageSource, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (rows.isEmpty()) {
            Text(
                text = stringResource(R.string.debug_no_storage_rows),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        rows.forEachIndexed { index, row ->
            DebugStorageTableRow(
                row = row,
                onCopy = onCopy,
                onEdit = onEdit,
                onDelete = onDelete,
            )
            if (index < rows.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

fun parseDebugStorageRows(
    result: String,
    sourceHint: DebugStorageSource = DebugStorageSource.Unknown,
): List<DebugStorageRow> {
    val trimmed = result.trim()
    if (trimmed.isBlank()) return emptyList()

    val element = parseResultElement(trimmed)
    if (element != null) {
        val unwrapped = unwrapScriptResult(element)
        val rows = rowsFromJson(unwrapped, sourceHint)
        if (rows.isNotEmpty()) return rows
    }

    return parseCookieHeader(trimmed, sourceHint)
}

fun debugStorageRowsCopyText(rows: List<DebugStorageRow>): String =
    rows.joinToString(separator = "\n") { row ->
        listOf(
            "source=${row.source.label}",
            "key=${row.key}",
            "value=${row.value}",
        ).joinToString(separator = "\t")
    }

@Composable
private fun DebugStorageViewerTopBar(
    title: String,
    rowCount: Int,
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
            Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.action_close))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.debug_rows_count, rowCount),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onCopy) {
            Icon(
                Icons.Outlined.ContentCopy,
                contentDescription = stringResource(R.string.debug_copy_all_storage_rows),
            )
        }
    }
}

@Composable
private fun DebugStorageEditor(
    editingRow: DebugStorageRow?,
    source: DebugStorageSource,
    key: String,
    value: String,
    onKeyChange: (String) -> Unit,
    onValueChange: (String) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = if (editingRow == null) {
                stringResource(R.string.debug_storage_new_row_title, source.label)
            } else {
                stringResource(R.string.debug_storage_edit_row_title, source.label)
            },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        OutlinedTextField(
            value = key,
            onValueChange = onKeyChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(R.string.debug_storage_key)) },
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            label = { Text(stringResource(R.string.debug_storage_value)) },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.action_clear))
            }
            Button(
                onClick = onSave,
                enabled = key.trim().isNotEmpty(),
            ) {
                Icon(Icons.Outlined.Save, contentDescription = null)
                Text(stringResource(R.string.action_save))
            }
        }
    }
}

@Composable
private fun DebugStorageViewerRow(
    row: DebugStorageRow,
    onOpen: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = row.key.ifBlank { stringResource(R.string.debug_blank_value) },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = row.source.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StorageRowIconButton(
                contentDescription = stringResource(R.string.debug_copy_storage_row),
                onClick = onCopy,
            ) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = null)
            }
            StorageRowIconButton(
                contentDescription = stringResource(R.string.debug_delete_storage_row),
                onClick = onDelete,
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = null)
            }
        }
        Text(
            text = row.value,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
        if (row.metadata.isNotBlank()) {
            Text(
                text = row.metadata,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun DebugStorageTableRow(
    row: DebugStorageRow,
    onCopy: (DebugStorageRow) -> Unit,
    onEdit: (DebugStorageSource, String, String) -> Unit,
    onDelete: (DebugStorageSource, String) -> Unit,
) {
    var expanded by remember(row) { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = row.key.ifBlank { stringResource(R.string.debug_blank_value) },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = row.value,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            StorageRowIconButton(
                contentDescription = stringResource(R.string.debug_copy_storage_row),
                onClick = { onCopy(row) },
            ) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = null)
            }
            StorageRowIconButton(
                contentDescription = stringResource(R.string.debug_edit_storage_row),
                onClick = { onEdit(row.source, row.key, row.value) },
            ) {
                Icon(Icons.Outlined.Edit, contentDescription = null)
            }
            StorageRowIconButton(
                contentDescription = stringResource(R.string.debug_delete_storage_row),
                onClick = { onDelete(row.source, row.key) },
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = null)
            }
        }
        if (expanded) {
            Text(
                text = listOf(row.source.label, row.metadata)
                    .filter { it.isNotBlank() }
                    .joinToString(" | "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StorageRowIconButton(
    contentDescription: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(32.dp)
            .semantics { this.contentDescription = contentDescription },
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        content()
    }
}

private val storageJson = Json { ignoreUnknownKeys = true }

private fun DebugStorageSource.normalizedForEdit(): DebugStorageSource =
    if (this == DebugStorageSource.Unknown) DebugStorageSource.LocalStorage else this

private fun parseResultElement(result: String): JsonElement? {
    val first = runCatching { storageJson.parseToJsonElement(result) }.getOrNull() ?: return null
    val nested = (first as? JsonPrimitive)
        ?.contentOrNull
        ?.trim()
        ?.takeIf { it.startsWith("{") || it.startsWith("[") }
        ?.let { runCatching { storageJson.parseToJsonElement(it) }.getOrNull() }
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

private fun rowsFromJson(element: JsonElement, sourceHint: DebugStorageSource): List<DebugStorageRow> =
    when (element) {
        is JsonObject -> rowsFromObject(element, sourceHint)
        is JsonArray -> rowsFromArray(element, sourceHint)
        is JsonPrimitive -> element.contentOrNull
            ?.takeIf { sourceHint == DebugStorageSource.Cookie }
            ?.let { parseCookieHeader(it, sourceHint) }
            .orEmpty()
    }

private fun rowsFromObject(
    objectValue: JsonObject,
    sourceHint: DebugStorageSource,
): List<DebugStorageRow> {
    rowFromObject(objectValue, sourceHint)?.let { return listOf(it) }
    return objectValue.entries.map { (key, value) ->
        DebugStorageRow(
            key = key,
            value = value.compactStorageValue(),
            source = sourceHint,
        )
    }
}

private fun rowsFromArray(
    array: JsonArray,
    sourceHint: DebugStorageSource,
): List<DebugStorageRow> =
    array.mapIndexedNotNull { index, element ->
        when (element) {
            is JsonObject -> rowFromObject(element, sourceHint)
            else -> DebugStorageRow(
                key = (index + 1).toString(),
                value = element.compactStorageValue(),
                source = sourceHint,
            )
        }
    }

private fun rowFromObject(
    objectValue: JsonObject,
    sourceHint: DebugStorageSource,
): DebugStorageRow? {
    val key = objectValue.stringValue("key")
        ?: objectValue.stringValue("name")
        ?: return null
    val value = objectValue["value"]?.compactStorageValue().orEmpty()
    val metadata = objectValue.entries
        .filterNot { (name, _) -> name == "key" || name == "name" || name == "value" }
        .joinToString(", ") { (name, valueElement) -> "$name=${valueElement.compactStorageValue()}" }
    return DebugStorageRow(
        key = key,
        value = value,
        source = sourceHint,
        metadata = metadata,
    )
}

private fun parseCookieHeader(
    cookieHeader: String,
    sourceHint: DebugStorageSource,
): List<DebugStorageRow> {
    if (cookieHeader.isBlank() || "=" !in cookieHeader) return emptyList()
    return cookieHeader
        .split(";")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .map { cookie ->
            val separator = cookie.indexOf("=")
            val key = if (separator >= 0) cookie.substring(0, separator).trim() else cookie.trim()
            val value = if (separator >= 0) cookie.substring(separator + 1).trim() else ""
            DebugStorageRow(
                key = key,
                value = value,
                source = if (sourceHint == DebugStorageSource.Unknown) DebugStorageSource.Cookie else sourceHint,
            )
        }
}

private fun JsonObject.stringValue(name: String): String? =
    (this[name] as? JsonPrimitive)?.contentOrNull

private fun JsonElement.compactStorageValue(): String =
    when (this) {
        is JsonPrimitive -> contentOrNull ?: toString()
        is JsonArray -> joinToString(prefix = "[", postfix = "]") { it.compactStorageValue() }
        is JsonObject -> entries.joinToString(prefix = "{", postfix = "}") { (key, value) ->
            "$key=${value.compactStorageValue()}"
        }
    }
