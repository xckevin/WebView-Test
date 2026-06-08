package com.xckevin.android.app.webview.test.ui.workbench

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xckevin.android.app.webview.test.R
import com.xckevin.android.app.webview.test.model.WebTestCase

@Composable
fun CasesPanel(
    cases: List<WebTestCase>,
    canSaveCurrentCase: Boolean,
    onOpenCase: (WebTestCase) -> Unit,
    onDeleteCase: (WebTestCase) -> Unit,
    onSaveCurrentCase: (name: String, note: String) -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var caseName by rememberSaveable { mutableStateOf("") }
    var caseNote by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            PanelSection(title = stringResource(R.string.cases_save_current)) {
                OutlinedTextField(
                    value = caseName,
                    onValueChange = { caseName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.cases_name)) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = caseNote,
                    onValueChange = { caseNote = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.cases_note)) },
                    minLines = 2,
                    textStyle = MaterialTheme.typography.bodyMedium,
                )
                PanelActionRow {
                    Button(
                        enabled = canSaveCurrentCase && caseName.isNotBlank(),
                        onClick = {
                            onSaveCurrentCase(caseName.trim(), caseNote.trim())
                            caseName = ""
                            caseNote = ""
                        },
                    ) {
                        Text(stringResource(R.string.action_save))
                    }
                    IconButton(onClick = onImport) {
                        Icon(
                            Icons.Outlined.FileDownload,
                            contentDescription = stringResource(R.string.action_import),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onExport) {
                        Icon(
                            Icons.Outlined.FileUpload,
                            contentDescription = stringResource(R.string.action_export),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        if (cases.isEmpty()) {
            item {
                PanelEmptyState(
                    icon = Icons.Outlined.FolderOff,
                    text = stringResource(R.string.cases_empty),
                )
            }
        } else {
            items(
                items = cases,
                key = { "case-${it.id}-${it.name}-${it.url}" },
            ) { testCase ->
                CaseRow(
                    testCase = testCase,
                    onOpenCase = onOpenCase,
                    onDeleteCase = onDeleteCase,
                )
            }
        }
    }
}

@Composable
private fun CaseRow(
    testCase: WebTestCase,
    onOpenCase: (WebTestCase) -> Unit,
    onDeleteCase: (WebTestCase) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = stringResource(R.string.action_open_case),
                role = Role.Button,
                onClick = { onOpenCase(testCase) },
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = testCase.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = testCase.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (testCase.note.isNotBlank()) {
                    Text(
                        text = testCase.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = { onDeleteCase(testCase) }) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.cases_delete_named, testCase.name),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
