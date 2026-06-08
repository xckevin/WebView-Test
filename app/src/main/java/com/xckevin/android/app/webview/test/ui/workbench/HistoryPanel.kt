package com.xckevin.android.app.webview.test.ui.workbench

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.HistoryToggleOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xckevin.android.app.webview.test.R
import com.xckevin.android.app.webview.test.model.HistoryItem
import java.util.Calendar
import java.text.DateFormat
import java.util.Date

@Composable
fun HistoryPanel(
    history: List<HistoryItem>,
    onOpenHistoryItem: (HistoryItem) -> Unit,
    onClearHistory: () -> Unit,
    onDeleteHistoryItem: (HistoryItem) -> Unit,
    modifier: Modifier = Modifier,
    nowMillis: Long = System.currentTimeMillis(),
) {
    val groups = remember(history, nowMillis) {
        history.groupByVisitDate(nowMillis)
    }
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            PanelHeader(
                title = stringResource(R.string.history_recent),
                actions = {
                    if (history.isNotEmpty()) {
                        IconButton(onClick = onClearHistory) {
                            Icon(
                                Icons.Outlined.DeleteSweep,
                                contentDescription = stringResource(R.string.action_clear_history),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
            )
        }

        if (history.isEmpty()) {
            item {
                PanelEmptyState(
                    icon = Icons.Outlined.HistoryToggleOff,
                    text = stringResource(R.string.history_empty),
                )
            }
        } else {
            groups.forEach { group ->
                item(key = "history-date-${group.dayKey}") {
                    Text(
                        text = group.localizedDateLabel(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    )
                }
                items(
                    items = group.items,
                    key = { "history-${it.id}-${it.visitedAt}-${it.url}" },
                ) { item ->
                    HistoryRow(
                        item = item,
                        onOpenHistoryItem = onOpenHistoryItem,
                        onDeleteHistoryItem = onDeleteHistoryItem,
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun HistoryRow(
    item: HistoryItem,
    onOpenHistoryItem: (HistoryItem) -> Unit,
    onDeleteHistoryItem: (HistoryItem) -> Unit,
) {
    var isMenuOpen by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClickLabel = stringResource(R.string.action_open_history_item),
                    role = Role.Button,
                    onClick = { onOpenHistoryItem(item) },
                    onLongClick = { isMenuOpen = true },
                ),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = item.title.ifBlank { item.url },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = DateFormat.getDateTimeInstance().format(Date(item.visitedAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        DropdownMenu(
            expanded = isMenuOpen,
            onDismissRequest = { isMenuOpen = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_delete_history_item)) },
                onClick = {
                    isMenuOpen = false
                    onDeleteHistoryItem(item)
                },
                leadingIcon = {
                    Icon(Icons.Outlined.Delete, contentDescription = null)
                },
            )
        }
    }
}

private data class HistoryDateGroup(
    val dayKey: Long,
    val label: HistoryDateLabel,
    val items: List<HistoryItem>,
)

private enum class HistoryDateLabel { TODAY, YESTERDAY, DATE }

private fun List<HistoryItem>.groupByVisitDate(nowMillis: Long): List<HistoryDateGroup> {
    val todayKey = nowMillis.dayKey()
    val yesterdayKey = Calendar.getInstance().run {
        timeInMillis = nowMillis
        add(Calendar.DATE, -1)
        timeInMillis.dayKey()
    }
    return groupBy { it.visitedAt.dayKey() }
        .map { (dayKey, items) ->
            HistoryDateGroup(
                dayKey = dayKey,
                label = when (dayKey) {
                    todayKey -> HistoryDateLabel.TODAY
                    yesterdayKey -> HistoryDateLabel.YESTERDAY
                    else -> HistoryDateLabel.DATE
                },
                items = items,
            )
        }
        .sortedByDescending { it.dayKey }
}

private fun Long.dayKey(): Long =
    Calendar.getInstance().run {
        timeInMillis = this@dayKey
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        timeInMillis
    }

@Composable
private fun HistoryDateGroup.localizedDateLabel(): String =
    when (label) {
        HistoryDateLabel.TODAY -> stringResource(R.string.history_group_today)
        HistoryDateLabel.YESTERDAY -> stringResource(R.string.history_group_yesterday)
        HistoryDateLabel.DATE -> DateFormat.getDateInstance().format(Date(dayKey))
    }
