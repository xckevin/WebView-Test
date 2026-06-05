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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Save current case",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                OutlinedTextField(
                    value = caseName,
                    onValueChange = { caseName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Name") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = caseNote,
                    onValueChange = { caseNote = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Note") },
                    minLines = 2,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        enabled = canSaveCurrentCase && caseName.isNotBlank(),
                        onClick = {
                            onSaveCurrentCase(caseName.trim(), caseNote.trim())
                            caseName = ""
                            caseNote = ""
                        },
                    ) {
                        Text("Save")
                    }
                    OutlinedButton(onClick = onImport) {
                        Text("Import")
                    }
                    OutlinedButton(onClick = onExport) {
                        Text("Export")
                    }
                }
            }
        }

        if (cases.isEmpty()) {
            item {
                Text(
                    text = "No saved cases",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
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
                text = testCase.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = testCase.url,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (testCase.note.isNotBlank()) {
                Text(
                    text = testCase.note,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onOpenCase(testCase) }) {
                    Text("Open")
                }
                OutlinedButton(onClick = { onDeleteCase(testCase) }) {
                    Text("Delete")
                }
            }
        }
    }
}
