package com.xckevin.android.app.webview.test.ui.workbench

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xckevin.android.app.webview.test.model.HistoryItem
import java.text.DateFormat
import java.util.Date

@Composable
fun HistoryPanel(
    history: List<HistoryItem>,
    onOpenHistoryItem: (HistoryItem) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Recent history",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                OutlinedButton(
                    enabled = history.isNotEmpty(),
                    onClick = onClearHistory,
                ) {
                    Text("Clear history")
                }
            }
        }

        if (history.isEmpty()) {
            item {
                Text(
                    text = "No history yet",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            items(
                items = history,
                key = { "history-${it.id}-${it.visitedAt}-${it.url}" },
            ) { item ->
                HistoryRow(
                    item = item,
                    onOpenHistoryItem = onOpenHistoryItem,
                )
            }
        }
    }
}

@Composable
private fun HistoryRow(
    item: HistoryItem,
    onOpenHistoryItem: (HistoryItem) -> Unit,
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = item.title.ifBlank { item.url },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.url,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = DateFormat.getDateTimeInstance().format(Date(item.visitedAt)),
                style = MaterialTheme.typography.bodySmall,
            )
            Button(onClick = { onOpenHistoryItem(item) }) {
                Text("Open")
            }
        }
    }
}
